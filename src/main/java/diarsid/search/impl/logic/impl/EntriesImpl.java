package diarsid.search.impl.logic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcOperations;
import diarsid.jdbc.api.sqltable.rows.RowGetter;
import diarsid.search.api.Behavior;
import diarsid.search.api.Entries;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealLabel;
import diarsid.search.impl.model.WordInEntry;
import diarsid.support.exceptions.UnsupportedLogicException;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;

import static diarsid.search.api.Behavior.Feature.DECOMPOSE_ENTRY_PATH;
import static diarsid.search.api.model.Entry.Type.PATH;
import static diarsid.search.api.model.meta.UserScoped.checkMustBelongToOneUser;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_TO_LOWER;
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
            PatternsToEntries patternsToEntries,
            EntriesLabelsJoinTable entriesLabelsJoinTable,
            Choices choices,
            WordsInEntries wordsInEntries,
            Behavior behavior) {
        super(jdbc);
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
        RealEntry entry = new RealEntry(entryString, user.uuid());

        boolean exists = doesEntryExistBy(user.uuid(), entry.stringLower());

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

            for ( String path : decomposePath(entry.string(), DONT_NORMALIZE, DONT_INCLUDE_ORIGINAL) ) {
                derivedEntryNotExists = ! this.doesEntryExistBy(user.uuid(), path);

                if ( derivedEntryNotExists ) {
                    newDerivedEntry = entry.newEntryWith(path);
                    newDerivedEntries.add(newDerivedEntry);
                }
            }

            if ( isNotEmpty(newDerivedEntries) ) {
                JdbcOperations.ArgsFrom<RealEntry> argsFromEntry = (realEntry) -> List.of(
                        realEntry.uuid(),
                        realEntry.string(),
                        realEntry.stringLower(),
                        realEntry.type(),
                        realEntry.createdAt(),
                        realEntry.userUuid());

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
                                argsFromEntry,
                                newDerivedEntries
                                );

                if ( insertedAll.length != newDerivedEntries.size() ) {
                    throw new IllegalStateException();
                }
            }
        }

        return entry;
    }

    @Override
    public Entry reload(Entry entry) {
        checkMustBeStored(entry);
        return this.getBy(entry.userUuid(), entry.uuid());
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
        return this.getBy(user.uuid(), entryUuid);
    }

    private Entry getBy(UUID userUUid, UUID entryUuid) {
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
            throw new NotFoundException();
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
        entryString = RealEntry.unifyOriginalString(entryString, CASE_TO_LOWER);

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
    public boolean doesExistBy(User user, String entry) {
        return this.doesEntryExistBy(user.uuid(), RealEntry.unifyOriginalString(entry, CASE_TO_LOWER));
    }

    @Override
    public Entry replace(User user, String oldEntryString, String newEntryString, PatternsTodoOnEntryReplace action) {
        Optional<Entry> foundEntry = this.findBy(user, oldEntryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        RealEntry entry = (RealEntry) foundEntry.get();

        return this.replaceInternally(entry, newEntryString, action);
    }

    private RealEntry replaceInternally(RealEntry entry, String newEntryString,  PatternsTodoOnEntryReplace action) {
        RealEntry changed = entry.changeTo(now(), newEntryString);

        boolean entryWithNewStringExists = this.doesEntryExistBy(changed.userUuid(), changed.stringLower());

        if ( entryWithNewStringExists ) {
            throw new IllegalArgumentException();
        }

        int updated = super.currentTransaction()
                .doUpdate(
                        "UPDATE entries \n" +
                        "SET \n" +
                        "   string_origin = ?, \n" +
                        "   string_lower = ?, \n" +
                        "   time = ? \n" +
                        "WHERE entries.uuid = ? ",
                        changed.string(), changed.stringLower(), changed.createdAt(), changed.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        switch ( action ) {
            case REMOVE_RELATED_PATTERN:
                this.patternsToEntries.removeAllBy(changed);
                break;
            case ANALYZE_AGAIN_RELATED_PATTERN:
                this.patternsToEntries.analyzeAgainAllRelationsOf(changed);
                break;
            default:
                throw action.unsupported();
        }

        this.choices.removeAllBy(changed);

        entry.setState(NON_STORED);

        return changed;
    }

    @Override
    public Entry replace(Entry entry, String newEntryString, PatternsTodoOnEntryReplace action) {
        checkMustBeStored(entry);
        this.checkMustExist(entry);

        return this.replaceInternally((RealEntry) entry, newEntryString, action);
    }

    @Override
    public Entry replace(User user, String oldEntry, String newEntry, PatternsTodoOnEntryReplace action, LabelsTodoOnEntryReplace reassign) {
        throw new UnsupportedLogicException();
    }

    @Override
    public Entry replace(Entry entry, String newEntry, PatternsTodoOnEntryReplace action, LabelsTodoOnEntryReplace reassign) {
        throw new UnsupportedLogicException();
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
                        "SELECT * " +
                        "FROM entries " +
                        "WHERE user_uuid = ?",
                        user.uuid());
    }

    @Override
    public void checkMustExist(Entry entry) {
        checkMustBeStored(entry);

        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * " +
                        "FROM entries " +
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
