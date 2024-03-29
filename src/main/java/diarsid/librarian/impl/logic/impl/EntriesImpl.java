package diarsid.librarian.impl.logic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Behavior;
import diarsid.librarian.api.Entries;
import diarsid.librarian.api.exceptions.NotFoundException;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.Choices;
import diarsid.librarian.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.librarian.impl.logic.api.PatternsToEntries;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.api.WordsInEntries;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.WordInEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
import static diarsid.librarian.api.Behavior.Feature.DECOMPOSE_ENTRY_PATH;
import static diarsid.librarian.api.model.Entry.Type.PATH;
import static diarsid.librarian.api.model.meta.UserScoped.checkMustBelongToOneUser;
import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.support.model.Joined.distinctLeftsOf;
import static diarsid.support.model.Joined.distinctRightsOf;
import static diarsid.support.model.Storable.State.NON_STORED;
import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.model.Storable.checkMustBeStored;
import static diarsid.support.model.Unique.uuidsOf;
import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;
import static diarsid.support.strings.PathUtils.decomposePath;

public class EntriesImpl extends ThreadBoundTransactional implements Entries {

    static final boolean DONT_NORMALIZE = false;
    static final boolean DONT_INCLUDE_ORIGINAL = false;

    private final PatternsToEntries patternsToEntries;
    private final Choices choices;
    private final WordsInEntries wordsInEntries;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByUuids;
    private final Behavior behavior;
    private final EntriesLabelsJoinTable entriesLabelsJoinTable;

    public EntriesImpl(
            Jdbc jdbc,
            UuidSupplier uuidSupplier,
            PatternsToEntries patternsToEntries,
            EntriesLabelsJoinTable entriesLabelsJoinTable,
            Choices choices,
            WordsInEntries wordsInEntries,
            Behavior behavior) {
        super(jdbc, uuidSupplier);
        this.patternsToEntries = patternsToEntries;
        this.entriesLabelsJoinTable = entriesLabelsJoinTable;
        this.choices = choices;
        this.wordsInEntries = wordsInEntries;
        this.behavior = behavior;

        this.sqlSelectEntriesByUuids = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT \n" +
                "   e.uuid, \n" +
                "   e.time, \n" +
                "   e.string_origin, \n" +
                "   e.string_lower, \n" +
                "   e.user_uuid \n" +
                "FROM entries e \n" +
                "WHERE \n" +
                "   e.user_uuid = ? AND \n" +
                "   e.uuid IN ( \n",
                "       ?",  ", \n",
                " ) ");
    }

    @Override
    public Entry save(User user, String entryString) {
        UUID userUuid = user.uuid();
        RealEntry entry = new RealEntry(super.nextRandomUuid(), entryString, userUuid);

        boolean exists = doesEntryExistBy(userUuid, entry.stringLower());

        if ( exists ) {
            throw new IllegalArgumentException();
        }

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO entries ( \n" +
                        "   uuid, \n" +
                        "   string_origin, \n" +
                        "   string_lower, \n" +
                        "   type, \n" +
                        "   time, \n" +
                        "   user_uuid) \n" +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        entry.uuid(),
                        entry.string(),
                        entry.stringLower(),
                        entry.type(),
                        entry.createdAt(),
                        entry.userUuid());

        if ( inserted != 1 ) {
            throw new IllegalStateException();
        }

        List<WordInEntry> entryWords = this.wordsInEntries.save(user, entry);

        entry.setState(STORED);

        boolean decomposeEntryPathToDerivedEntries = behavior.isEnabled(user, DECOMPOSE_ENTRY_PATH);

        if ( entry.type().equalTo(PATH) && decomposeEntryPathToDerivedEntries ) {
            boolean derivedEntryNotExists;
            RealEntry newDerivedEntry;
            List<RealEntry> newDerivedEntries = new ArrayList<>();

            List<String> decomposedPaths = decomposePath(entry.string(), DONT_NORMALIZE, DONT_INCLUDE_ORIGINAL);
            for ( String path : decomposedPaths ) {
                derivedEntryNotExists = ! this.doesEntryExistBy(userUuid, StringTransformations.simplify(path, CASE_TO_LOWER));

                if ( derivedEntryNotExists ) {
                    newDerivedEntry = entry.newEntryWith(super.nextRandomUuid(), path);
                    newDerivedEntries.add(newDerivedEntry);
                }
            }

            if ( isNotEmpty(newDerivedEntries) ) {

                int[] insertedAll = super.currentTransaction()
                        .doBatchUpdate(
                                "INSERT INTO entries ( \n" +
                                "   uuid, \n" +
                                "   string_origin, \n" +
                                "   string_lower, \n" +
                                "   type, \n" +
                                "   time, \n" +
                                "   user_uuid) \n" +
                                "VALUES (?, ?, ?, ?, ?, ?)",
                                (realEntry, params) -> {
                                    params.addNext(realEntry.uuid());
                                    params.addNext(realEntry.string());
                                    params.addNext(realEntry.stringLower());
                                    params.addNext(realEntry.type());
                                    params.addNext(realEntry.createdAt());
                                    params.addNext(realEntry.userUuid());
                                },
                                newDerivedEntries);

                mustAllBe(1, insertedAll);
                if ( insertedAll.length != newDerivedEntries.size() ) {
                    throw new IllegalStateException();
                }
            }
        }

        return entry;
    }

    @Override
    public Entry getOrSave(User user, String entry) {
        Optional<Entry> foundEntry = this.findBy(user, entry);
        if ( foundEntry.isPresent() ) {
            return foundEntry.get();
        }
        else {
            return this.save(user, entry);
        }
    }

    @Override
    public Entry reload(Entry entry) {
        checkMustBeStored(entry);
        Entry saved = this.getByOrNull(entry.userUuid(), entry.uuid());

        if ( isNull(saved) ) {
            throw new NotFoundException();
        }

        return saved;
    }

    @Override
    public List<Entry> reload(List<Entry> entries) {
        checkMustBeStored(entries);
        checkMustBelongToOneUser(entries);

        List<Entry> entriesReload = this.getBy(entries.get(0).userUuid(), uuidsOf(entries));

        if ( entriesReload.size() != entries.size() ) {
            throw new IllegalStateException();
        }

        return entries;
    }

    @Override
    public Entry getBy(User user, UUID entryUuid) {
        Entry entry = this.getByOrNull(user.uuid(), entryUuid);

        if ( isNull(entry) ) {
            throw new NotFoundException();
        }

        return entry;
    }

    private Entry getByOrNull(UUID userUUid, UUID entryUuid) {
        List<Entry> entries = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealEntry(row, now()),
                        "SELECT \n" +
                        "   e.uuid, \n" +
                        "   e.time, \n" +
                        "   e.string_origin, \n" +
                        "   e.string_lower, \n" +
                        "   e.user_uuid \n" +
                        "FROM entries e \n" +
                        "WHERE \n" +
                        "   e.user_uuid = ? AND \n" +
                        "   e.uuid = ? ",
                        userUUid, entryUuid)
                .collect(toList());

        if ( entries.size() == 1 ) {
            return entries.get(0);
        }
        else if ( entries.isEmpty() ) {
            return null;
        }
        else {
            throw new IllegalStateException();
        }
    }

    @Override
    public List<Entry> getBy(User user, List<UUID> uuids) {
        return this.getBy(user.uuid(), uuids);
    }

    private List<Entry> getBy(UUID userUuid, List<UUID> uuids) {
        List<Entry> entries = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealEntry(row, now()),
                        this.sqlSelectEntriesByUuids.getFor(uuids),
                        userUuid, uuids)
                .collect(toList());

        return entries;
    }

    @Override
    public Optional<Entry> findBy(User user, String entryString) {
        entryString = StringTransformations.simplify(entryString, CASE_TO_LOWER);

        Optional<Entry> foundEntry = super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        row -> new RealEntry(row, now()),
                        "SELECT * \n" +
                        "FROM entries \n" +
                        "WHERE \n" +
                        "   string_lower = ? AND \n" +
                        "   user_uuid = ? ",
                        entryString, user.uuid());

        return foundEntry;
    }

    @Override
    public Optional<Entry> findBy(User user, UUID entryUuid) {
        return Optional.ofNullable(this.getByOrNull(user.uuid(), entryUuid));
    }

    @Override
    public boolean doesExistBy(User user, String entry) {
        return this.doesEntryExistBy(user.uuid(), StringTransformations.simplify(entry, CASE_TO_LOWER));
    }

    @Override
    public Entry update(
            Entry entry,
            String newEntryString,
            OnUpdate.RemovingLabels removingLabels,
            OnUpdate.RemovingPatterns removingPatterns) {
        checkMustBeStored(entry);
        this.checkMustExist(entry);

        RealEntry oldEntry = (RealEntry) entry;
        RealEntry newEntry = oldEntry.changeTo(now(), newEntryString);

        boolean entryWithNewStringExists = this.doesEntryExistBy(newEntry.userUuid(), newEntry.stringLower());

        if ( entryWithNewStringExists ) {
            throw new IllegalArgumentException();
        }

        int updated = super.currentTransaction()
                .doUpdate(
                        "UPDATE entries \n" +
                        "SET \n" +
                        "   string_origin = ?, \n" +
                        "   string_lower = ?, \n" +
                        "   time = ? \n" + // TODO introduce updateTime ???
                        "WHERE entries.uuid = ? ",
                        newEntry.string(), newEntry.stringLower(), newEntry.createdAt(), newEntry.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        this.choices.removeAllBy(oldEntry);

        List<Entry.Labeled> labelJoins = this.entriesLabelsJoinTable.getAllJoinedTo(oldEntry);
        if ( isNotEmpty(labelJoins) ) {
            List<Entry.Label> labelsToRemove = removingLabels.toRemoveFrom(distinctRightsOf(labelJoins));
            if ( isNotEmpty(labelsToRemove) ) {
                this.entriesLabelsJoinTable.removeBy(oldEntry, labelsToRemove);
            }
        }

        List<PatternToEntry> patternJoins = this.patternsToEntries.findBy(oldEntry);
        if ( isNotEmpty(patternJoins) ) {
            List<Pattern> patternsToRemove = removingPatterns.toRemoveFrom(distinctLeftsOf(patternJoins));
            if ( isNotEmpty(patternsToRemove) ) {
                this.patternsToEntries.removeBy(oldEntry, patternsToRemove);
            }
        }

        oldEntry.setState(NON_STORED);
        newEntry.setState(STORED);

        return newEntry;
    }

    @Override
    public boolean remove(User user, String entryString) {
        entryString = entryString.trim();

        Optional<Entry> foundEntry = this.findBy(user, entryString);

        if ( foundEntry.isEmpty() ) {
            return false;
        }

        RealEntry entry = (RealEntry) foundEntry.get();

        return this.removeInternally(entry);
    }

    private boolean removeInternally(RealEntry entry) {
        this.choices.removeAllBy(entry);
        this.patternsToEntries.removeAllBy(entry);
        this.entriesLabelsJoinTable.removeAllBy(entry);

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM entries \n" +
                        "WHERE \n" +
                        "   entries.string_lower = ? AND \n" +
                        "   entries.user_uuid = ? ",
                        entry.stringLower(), entry.userUuid());

        if ( removed == 0 ) {
            return false;
        }
        else if ( removed > 1 ) {
            return true;
        }
        else {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean remove(Entry entry) {
        checkMustBeStored(entry);
        this.checkMustExist(entry);

        RealEntry realEntry = (RealEntry) entry;
        boolean removed = this.removeInternally(realEntry);

        if ( removed ) {
            realEntry.setState(NON_STORED);
        }

        return removed;
    }

    @Override
    public long countEntriesOf(User user) {
        return super.currentTransaction()
                .countQueryResults(
                        "SELECT * \n" +
                        "FROM entries \n" +
                        "WHERE user_uuid = ?",
                        user.uuid());
    }

    @Override
    public void checkMustExist(Entry entry) {
        checkMustBeStored(entry);

        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * \n" +
                        "FROM entries \n" +
                        "WHERE uuid = ?",
                        entry.uuid());

        if ( count != 1 ) {
            throw new NotFoundException();
        }
    }

    @Override
    public void checkMustExist(List<Entry> entries) throws NotFoundException {
        checkMustBelongToOneUser(entries);
        checkMustBeStored(entries);

        int count = super.currentTransaction()
                .countQueryResults(
                        this.sqlSelectEntriesByUuids.getFor(entries),
                        uuidsOf(entries));

        if ( count != entries.size() ) {
            throw new NotFoundException();
        }
    }

    private boolean doesEntryExistBy(UUID userUuid, String unifiedEntryString) {
        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * \n" +
                        "FROM entries \n" +
                        "WHERE \n" +
                        "   string_lower = ? AND \n" +
                        "   user_uuid = ? ",
                        unifiedEntryString, userUuid);

        return count > 0;
    }
}
