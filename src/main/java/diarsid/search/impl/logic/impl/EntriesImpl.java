package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.search.api.Behavior;
import diarsid.search.api.Entries;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.api.PhrasesInEntries;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.api.chars.CharsInEntries;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInEntries;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInPhrases;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInWords;
import diarsid.search.impl.logic.impl.jdbc.PooledRowCollectorForEntriesAndLabels;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForEntriesAndLabels;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.PhraseInEntry;
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
import static diarsid.search.api.Entries.RelatedPatternsAction.ANALYZE_AGAIN;
import static diarsid.search.api.Entries.RelatedPatternsAction.REMOVE;
import static diarsid.search.api.model.Entry.Type.PATH;
import static diarsid.search.api.model.meta.Storable.State.NON_STORED;
import static diarsid.search.api.model.meta.Storable.State.STORED;
import static diarsid.search.api.model.meta.Storable.checkMustBeStored;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_TO_LOWER;
import static diarsid.support.strings.PathUtils.decomposePath;
import static diarsid.support.strings.StringUtils.nonEmpty;

public class EntriesImpl extends ThreadBoundTransactional implements Entries {

    private static final boolean DONT_NORMALIZE = false;
    private static final boolean DONT_INCLUDE_ORIGINAL = false;

    private final PatternsToEntries patternsToEntries;
    private final Choices choices;
    private final CharsInEntries charsInEntries;
    private final WordsInEntries wordsInEntries;
    private final PhrasesInEntries phrasesInEntries;
    private final LabelsToCharsInEntries labelsToCharsInEntries;
    private final LabelsToCharsInWords labelsToCharsInWords;
    private final LabelsToCharsInPhrases labelsToCharsInPhrases;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAnyOfLabels;
    private final Behavior behavior;
    private final GuardedPool<PooledRowCollectorForEntriesAndLabels> rowCollectorsPool;
    private final boolean useLabelsToChars = false;

    public EntriesImpl(
            Jdbc jdbc,
            PatternsToEntries patternsToEntries,
            Choices choices,
            CharsInEntries charsInEntries,
            LabelsToCharsInEntries labelsToCharsInEntries,
            LabelsToCharsInWords labelsToCharsInWords,
            LabelsToCharsInPhrases labelsToCharsInPhrases,
            WordsInEntries wordsInEntries,
            PhrasesInEntries phrasesInEntries,
            Behavior behavior) {
        super(jdbc);
        this.patternsToEntries = patternsToEntries;
        this.choices = choices;
        this.charsInEntries = charsInEntries;
        this.labelsToCharsInEntries = labelsToCharsInEntries;
        this.labelsToCharsInWords = labelsToCharsInWords;
        this.labelsToCharsInPhrases = labelsToCharsInPhrases;
        this.wordsInEntries = wordsInEntries;
        this.phrasesInEntries = phrasesInEntries;
        this.behavior = behavior;

        this.rowCollectorsPool = new GuardedPool<>(() -> new PooledRowCollectorForEntriesAndLabels("e_", "l_"));

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
                        entry.time(),
                        entry.userUuid());

        if ( inserted != 1 ) {
            throw new IllegalStateException();
        }

        this.charsInEntries.save(entry);
        List<WordInEntry> entryWords = this.wordsInEntries.save(user, entry);
        List<PhraseInEntry> entryPhrases = this.phrasesInEntries.save(entryWords);

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

        RealEntry entry = new RealEntry(entryString, user.uuid());
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
    public Optional<Entry> findBy(User user, String entryString) {
        ThreadBoundJdbcTransaction transaction = super.currentTransaction();

        entryString = RealEntry.unifyOriginalString(entryString, CASE_TO_LOWER);

        Optional<Entry> foundEntry = transaction
                .doQueryAndConvertFirstRow(
                        RealEntry::new,
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
                        RealLabel::new,
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
                    try (PooledRowCollectorForEntriesAndLabels pooledRowCollector = this.rowCollectorsPool.give()) {
                        RowCollectorForEntriesAndLabels entryAndLabelsCollector = pooledRowCollector.get();

                        super.currentTransaction()
                                .doQuery(
                                        entryAndLabelsCollector,
                                        this.sqlSelectEntriesAndLabelsByAnyOfLabels.getFor(labelUuids),
                                        labelUuids);

                        return entryAndLabelsCollector.entries();
                    }
                }
                case ALL_OF: {
                    try (PooledRowCollectorForEntriesAndLabels pooledRowCollector = this.rowCollectorsPool.give()) {
                        RowCollectorForEntriesAndLabels entryAndLabelsCollector = pooledRowCollector.get();

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
        String unifiedNewEntryString = RealEntry.unifyOriginalString(newEntryString, CASE_TO_LOWER);
        Optional<Entry> foundEntry = this.findBy(user, oldEntryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        boolean entryWithNewStringExists = this.doesEntryExistBy(user.uuid(), unifiedNewEntryString);

        if ( entryWithNewStringExists ) {
            throw new IllegalArgumentException();
        }

        RealEntry entry = (RealEntry) foundEntry.get();

        return this.replaceInternally(entry, newEntryString, unifiedNewEntryString, action);
    }

    private RealEntry replaceInternally(
            RealEntry entry,
            String newEntryString, String unifiedNewEntryString,
            RelatedPatternsAction action) {
        if ( action.equalTo(REMOVE) ) {
            this.patternsToEntries.removeAllBy(entry);
        }

        int updated = super.currentTransaction()
                .doUpdate(
                        "UPDATE entries \n" +
                        "SET \n" +
                        "   string_origin = ?, \n" +
                        "   string_lower = ?, \n" +
                        "   time = ? \n" +
                        "WHERE entries.uuid = ? ",
                        newEntryString, unifiedNewEntryString, entry.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        entry.changeTo(newEntryString, unifiedNewEntryString);

        if ( action.equalTo(ANALYZE_AGAIN) ) {
            this.patternsToEntries.analyzeAgainAllRelationsOf(entry);
        }

        this.choices.removeAllBy(entry);

        return entry;
    }

    @Override
    public Entry replace(Entry entry, String newEntryString, RelatedPatternsAction action) {
        checkMustBeStored(entry);
        String unifiedNewEntryString = RealEntry.unifyOriginalString(newEntryString, CASE_TO_LOWER);

        boolean entryWithNewStringExists = this.doesEntryExistBy(entry.userUuid(), unifiedNewEntryString);

        if ( entryWithNewStringExists ) {
            throw new IllegalArgumentException();
        }

        return this.replaceInternally((RealEntry) entry, newEntryString, unifiedNewEntryString, action);
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

        checkMustBeStored(entry);
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

            if ( this.useLabelsToChars ) {
                this.labelsToCharsInEntries.join(entry, label, joiningTime);
                this.labelsToCharsInPhrases.join(entry, label, joiningTime);
                this.labelsToCharsInWords.join(entry, label, joiningTime);
            }

            entry.labels().add(label);
        }

        return true;
    }

}
