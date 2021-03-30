package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.jdbc.api.sqltable.rows.RowGetter;
import diarsid.search.api.Behavior;
import diarsid.search.api.Entries;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.impl.jdbc.PooledRowCollectorForEntriesAndLabels;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealLabel;
import diarsid.search.impl.model.WordInEntry;
import diarsid.support.objects.GuardedPool;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import static diarsid.search.api.Behavior.Feature.APPLY_PARENT_LABELS_TO_DERIVED_ENTRIES;
import static diarsid.search.api.Behavior.Feature.DECOMPOSE_ENTRY_PATH;
import static diarsid.search.api.model.Entry.Type.PATH;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_TO_LOWER;
import static diarsid.support.model.Storable.State.NON_STORED;
import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.model.Storable.checkMustBeStored;
import static diarsid.support.strings.PathUtils.decomposePath;
import static diarsid.support.strings.StringUtils.nonEmpty;

public class EntriesImpl extends ThreadBoundTransactional implements Entries {

    private static final boolean DONT_NORMALIZE = false;
    private static final boolean DONT_INCLUDE_ORIGINAL = false;

    private final PatternsToEntries patternsToEntries;
    private final Choices choices;
    private final WordsInEntries wordsInEntries;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAnyOfLabels;
    private final Behavior behavior;
    private final GuardedPool<PooledRowCollectorForEntriesAndLabels> rowCollectorsPool;
    private final RowGetter<Entry.Label> newLabel;

    public EntriesImpl(
            Jdbc jdbc,
            PatternsToEntries patternsToEntries,
            Choices choices,
            WordsInEntries wordsInEntries,
            Behavior behavior) {
        super(jdbc);
        this.patternsToEntries = patternsToEntries;
        this.choices = choices;
        this.wordsInEntries = wordsInEntries;
        this.behavior = behavior;

        this.rowCollectorsPool = new GuardedPool<>(
                () -> new PooledRowCollectorForEntriesAndLabels("e_", "l_"),
                (collector) -> {
                    ThreadBoundJdbcTransaction transaction = super.currentTransaction();
                    collector.context().fill(transaction.uuid(), transaction.created());
                });

        this.sqlSelectEntriesAndLabelsByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       ?", ", \n", " ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(label_uuid) = ? \n" +
                "   ) \n" +
                "SELECT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.time          AS e_time, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "       ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid \n" +
                "   JOIN labels l \n" +
                "       ON l.uuid = le.label_uuid ");

        this.sqlSelectEntriesAndLabelsByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid \n" +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       ?", ", \n", " ) \n" +
                "   ) \n" +
                "SELECT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.time          AS e_time, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "       ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid \n" +
                "   JOIN labels l \n" +
                "       ON l.uuid = le.label_uuid ");

        this.newLabel = (row) -> new RealLabel(row);
    }

    @Override
    public Entry save(User user, String entryString) {
        Entry entry = this.save(user, entryString, emptyList());

        return entry;
    }

    private void saveInternally(User user, RealEntry entry) {
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

    @Override
    public Entry save(User user, String entryString, List<Entry.Label> labels) {
        checkLabelsMustBeStored(labels);
        checkLabelsMustBelongToUser(user, labels);

        RealEntry entry = new RealEntry(entryString, user.uuid(), super.currentTransaction().uuid());
        boolean entryExists = this.doesEntryExistBy(user.uuid(), entry.stringLower());

        if ( entryExists ) {
            throw new IllegalArgumentException();
        }

        this.saveInternally(user, entry);
        this.addLabels(entry, labels);

        boolean decomposeEntryPathToDerivedEntries = behavior.isEnabled(user, DECOMPOSE_ENTRY_PATH);
        boolean applyParentLabelsToDerivedEntries = behavior.isEnabled(user, APPLY_PARENT_LABELS_TO_DERIVED_ENTRIES);

        if ( entry.type().equalTo(PATH) && decomposeEntryPathToDerivedEntries ) {
            Optional<Entry> existingEntry;
            RealEntry newDerivedEntry;
            for ( String path : decomposePath(entry.string(), DONT_NORMALIZE, DONT_INCLUDE_ORIGINAL) ) {
                existingEntry = this.findBy(user, path);

                if ( existingEntry.isPresent() ) {
                    if ( applyParentLabelsToDerivedEntries ) {
                        this.addLabels(existingEntry.get(), labels);
                    }
                }
                else {
                    newDerivedEntry = entry.newEntryWith(path);
                    this.saveInternally(user, newDerivedEntry);
                    if ( applyParentLabelsToDerivedEntries ) {
                        this.addLabels(newDerivedEntry, labels);
                    }
                }
            }
        }

        return entry;
    }

    @Override
    public Entry reload(Entry entry) {
        if ( super.isBoundToCurrentTransaction((RealEntry) entry) ) {
            return entry;
        }

        try (PooledRowCollectorForEntriesAndLabels entryAndLabelsCollector = this.rowCollectorsPool.give()) {

            super.currentTransaction()
                    .doQuery(
                            entryAndLabelsCollector,
                            "SELECT \n" +
                            "   e.uuid          AS e_uuid, \n" +
                            "   e.time          AS e_time, \n" +
                            "   e.string_origin AS e_string_origin, \n" +
                            "   e.string_lower  AS e_string_lower, \n" +
                            "   e.user_uuid     AS e_user_uuid, \n" +
                            "   l.uuid          AS l_uuid, \n" +
                            "   l.time          AS l_time, \n" +
                            "   l.name          AS l_name, \n" +
                            "   l.user_uuid     AS l_user_uuid \n" +
                            "FROM entries e \n" +
                            "   JOIN labels_to_entries le \n" +
                            "       ON e.uuid = le.entry_uuid \n" +
                            "   JOIN labels l \n" +
                            "       ON l.uuid = le.label_uuid \n" +
                            "WHERE e.uuid = ?",
                            entry.uuid());

            List<Entry> entries = entryAndLabelsCollector.entries();

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
    }

    @Override
    public boolean doesNotRequireReload(Entry entry) {
        return super.currentTransaction().uuid().equals(((RealEntry) entry).transactionUuid());
    }

    @Override
    public List<Entry> reload(List<Entry> entries) {
        return null;
    }

    @Override
    public Entry getBy(User user, UUID entryUuid) {
        return null;
    }

    @Override
    public List<Entry> getBy(List<UUID> uuids) {
        return null;
    }

    @Override
    public Optional<Entry> findBy(User user, String entryString) {
        ThreadBoundJdbcTransaction transaction = super.currentTransaction();

        entryString = RealEntry.unifyOriginalString(entryString, CASE_TO_LOWER);

        LocalDateTime entriesActualAt = now();

        Optional<Entry> foundEntry = transaction
                .doQueryAndConvertFirstRow(
                        row -> new RealEntry(entriesActualAt, transaction.uuid(), row),
                        "SELECT * \n" +
                        "FROM entries \n" +
                        "WHERE \n" +
                        "   string_lower = ? AND \n" +
                        "   user_uuid = ? ",
                        entryString, user.uuid());

        if ( foundEntry.isEmpty() ) {
            return foundEntry;
        }

        List<Entry.Label> labels = transaction
                .doQueryAndStream(
                        this.newLabel,
                        "SELECT * \n" +
                        "FROM labels l \n" +
                        "   JOIN labels_to_entries le \n" +
                        "       ON le.label_uuid = l.uuid \n" +
                        "WHERE le.entry_uuid = ? ",
                        foundEntry.get().uuid())
                .collect(toList());

        foundEntry.get().labels().addAll(labels);

        return foundEntry;
    }

    @Override
    public List<Entry> findAllBy(User user, Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return emptyList();
        }

        checkLabelsMustBelongToUser(user, labels);
        checkLabelsMustBeStored(labels);

        List<UUID> labelUuids = labels
                .stream()
                .map(Entry.Label::uuid)
                .collect(toList());

        if ( labels.size() == 1 ) {
            throw new UnsupportedOperationException();
        }
        else {
            switch ( matching ) {
                case ANY_OF: {
                    try (PooledRowCollectorForEntriesAndLabels entryAndLabelsCollector = this.rowCollectorsPool.give()) {

                        super.currentTransaction()
                                .doQuery(
                                        entryAndLabelsCollector,
                                        this.sqlSelectEntriesAndLabelsByAnyOfLabels.getFor(labelUuids),
                                        labelUuids);

                        return entryAndLabelsCollector.entries();
                    }
                }
                case ALL_OF: {
                    try (PooledRowCollectorForEntriesAndLabels entryAndLabelsCollector = this.rowCollectorsPool.give()) {

                        super.currentTransaction()
                                .doQuery(
                                        entryAndLabelsCollector,
                                        this.sqlSelectEntriesAndLabelsByAllOfLabels.getFor(labelUuids),
                                        labelUuids, labels.size());

                        return entryAndLabelsCollector.entries();
                    }
                }
                default:
                    throw matching.unsupported();
            }
        }
    }

    private void checkLabelsMustBeStored(List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return;
        }

        String unstoredLabelsNames = labels
                .stream()
                .filter(label -> label.hasState(NON_STORED))
                .map(Entry.Label::name)
                .collect(joining(", "));

        if ( nonEmpty(unstoredLabelsNames) ) {
            throw new IllegalArgumentException(unstoredLabelsNames + " labels are not stored!");
        }
    }

    private static void checkLabelsMustBelongToUser(User user, List<Entry.Label> labels) {
        checkLabelsMustBelongToUser(user.uuid(), labels);
    }

    private static void checkLabelsMustBelongToUser(UUID userUuid, List<Entry.Label> labels) {
        List<Entry.Label> otherLabels =labels
                .stream()
                .filter(label -> label.doesNotHaveUserUuid(userUuid))
                .collect(toList());

        if ( ! otherLabels.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Entry replace(User user, String oldEntryString, String newEntryString, RelatedPatternsAction action) {
        Optional<Entry> foundEntry = this.findBy(user, oldEntryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        RealEntry entry = (RealEntry) foundEntry.get();

        return this.replaceInternally(entry, newEntryString, action);
    }

    private RealEntry replaceInternally(RealEntry entry, String newEntryString,  RelatedPatternsAction action) {
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
            case REMOVE:
                this.patternsToEntries.removeAllBy(changed);
                break;
            case ANALYZE_AGAIN:
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
    public Entry replace(Entry entry, String newEntryString, RelatedPatternsAction action) {
        this.checkMustExistAndBeStored(entry);

        return this.replaceInternally((RealEntry) entry, newEntryString, action);
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
//        this.removeLabelsToEntries(entry);

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
        this.checkMustExistAndBeStored(entry);

        RealEntry realEntry = (RealEntry) entry;
        boolean removed = this.removeInternally(realEntry);

        if ( removed ) {
            realEntry.setState(NON_STORED);
        }

        return removed;
    }

    @Override
    public Entry addLabels(User user, String entryString, List<Entry.Label> labels) {
        Optional<Entry> foundEntry = this.findBy(user, entryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        Entry entry = foundEntry.get();

        this.addLabels(foundEntry.get(), labels);

        return entry;
    }

    @Override
    public boolean addLabels(Entry entry, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return false;
        }

        this.checkMustExistAndBeStored(entry);
        checkLabelsMustBelongToUser(entry.userUuid(), labels);
        checkMustBeStored(labels);

        if ( entry.labels().containsAll(labels) ) {
            return true;
        }

        ThreadBoundJdbcTransaction transaction = super.currentTransaction();

        LocalDateTime joiningTime = now();

        int added;
        Entry.Label.ConditionBindable bindableLabel;
        for ( Entry.Label label : labels ) {
            if ( entry.labels().contains(label) ) {
                continue;
            }

            if ( label instanceof Entry.Label.ConditionBindable ) {
                bindableLabel = (Entry.Label.ConditionBindable) label;
                if ( bindableLabel.canNotBeBoundTo(entry) ) {
                    continue;
                }
            }

            added = transaction
                    .doUpdate(
                            "INSERT INTO labels_to_entries (uuid, entry_uuid, label_uuid, time) \n" +
                            "VALUES (?, ?, ?, ?)",
                            randomUUID(), entry.uuid(), label.uuid(), joiningTime);

            if ( added != 1 ) {
                throw new IllegalStateException();
            }

            entry.labels().add(label);
        }

        return true;
    }

    @Override
    public Entry removeLabels(User user, String entry, List<Entry.Label> labels) {
        return null;
    }

    @Override
    public boolean removeLabels(Entry entry, List<Entry.Label> labels) {
        return false;
    }

    @Override
    public long countEntriesOf(User user) {
        return 0;
    }

    @Override
    public long countEntriesBy(User user, String label) {
        return 0;
    }

    @Override
    public long countEntriesBy(Entry.Label label) {
        return 0;
    }

    @Override
    public long countEntriesBy(User user, Entry.Label.Matching matching, List<String> labels) {
        return 0;
    }

    @Override
    public long countEntriesBy(Entry.Label.Matching matching, List<Entry.Label> labels) {
        return 0;
    }

    private void checkMustExistAndBeStored(Entry entry) {
        checkMustBeStored(entry);
        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * " +
                        "FROM entries " +
                        "WHERE uuid = ?",
                        entry.uuid());

        if ( count != 1 ) {
            throw new IllegalArgumentException();
        }
    }

}
