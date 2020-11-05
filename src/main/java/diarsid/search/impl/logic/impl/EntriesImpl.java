package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.JdbcTransaction;
import diarsid.search.api.Entries;
import diarsid.search.api.Labels;
import diarsid.search.api.Store;
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
import diarsid.search.impl.logic.impl.jdbc.EntriesAndLabelsRowCollector;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.PhraseInEntry;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealLabel;
import diarsid.search.impl.model.WordInEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparated;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.stream;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import static diarsid.search.api.Entries.RelatedPatternsAction.ANALYZE_AGAIN;
import static diarsid.search.api.Entries.RelatedPatternsAction.REMOVE;
import static diarsid.search.api.model.meta.Storable.State.NON_STORED;
import static diarsid.search.api.model.meta.Storable.State.STORED;
import static diarsid.search.api.model.meta.Storable.checkMustBeStored;
import static diarsid.support.strings.StringUtils.nonEmpty;

public class EntriesImpl extends ThreadTransactional implements Entries {

    private final PatternsToEntries patternsToEntries;
    private final Choices choices;
    private final CharsInEntries charsInEntries;
    private final WordsInEntries wordsInEntries;
    private final PhrasesInEntries phrasesInEntries;
    private final LabelsToCharsInEntries labelsToCharsInEntries;
    private final LabelsToCharsInWords labelsToCharsInWords;
    private final LabelsToCharsInPhrases labelsToCharsInPhrases;
    private final StringCacheForRepeatedSeparated sqlQuestionMarks;

    public EntriesImpl(
            JdbcTransactionThreadBindings jdbcTransactionThreadBindings,
            PatternsToEntries patternsToEntries,
            Choices choices,
            CharsInEntries charsInEntries,
            LabelsToCharsInEntries labelsToCharsInEntries,
            LabelsToCharsInWords labelsToCharsInWords,
            LabelsToCharsInPhrases labelsToCharsInPhrases,
            WordsInEntries wordsInEntries,
            PhrasesInEntries phrasesInEntries) {
        super(jdbcTransactionThreadBindings);
        this.patternsToEntries = patternsToEntries;
        this.choices = choices;
        this.charsInEntries = charsInEntries;
        this.labelsToCharsInEntries = labelsToCharsInEntries;
        this.labelsToCharsInWords = labelsToCharsInWords;
        this.labelsToCharsInPhrases = labelsToCharsInPhrases;
        this.wordsInEntries = wordsInEntries;
        this.phrasesInEntries = phrasesInEntries;
        this.sqlQuestionMarks = new StringCacheForRepeatedSeparated("?", ", ");
    }

    @Override
    public Entry save(User user, String entryString) {
        JdbcTransaction transaction = super.currentTransaction();

        boolean entryExists = doesEntryExistsBy(user.uuid(), entryString);

        if ( entryExists ) {
            throw new IllegalArgumentException();
        }

        RealEntry entry = new RealEntry(entryString, user.uuid());

        int inserted = transaction
                .doUpdate(
                        "INSERT INTO entries (" +
                        "   uuid, " +
                        "   string_origin, " +
                        "   string_lower, " +
                        "   type," +
                        "   time, " +
                        "   user_uuid) " +
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

        charsInEntries.save(entry);
        List<WordInEntry> entryWords = wordsInEntries.save(entry);
        List<PhraseInEntry> entryPhrases = phrasesInEntries.save(entryWords);

        entry.setState(STORED);

        return entry;
    }

    private boolean doesEntryExistsBy(UUID userUuid, String entryString) {
        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * " +
                        "FROM entries " +
                        "WHERE " +
                        "   string_lower = ? AND " +
                        "   user_uuid = ?",
                        entryString.trim().toLowerCase(), userUuid);

        return count > 0;
    }

    @Override
    public Entry save(User user, String entryString, Entry.Label... labels) {
        checkLabelsMustBeStored(labels);

        Entry entry = this.save(user, entryString);

        this.addLabels(entry, labels);

        return entry;
    }

    @Override
    public Optional<Entry> findBy(User user, String entryString) {
        JdbcTransaction transaction = super.currentTransaction();

        Optional<Entry> foundEntry = transaction
                .doQueryAndConvertFirstRow(
                        RealEntry::new,
                        "SELECT * " +
                        "FROM entries " +
                        "WHERE " +
                        "   string_lower = ? AND " +
                        "   user_uuid = ? ",
                        entryString.trim().toLowerCase(), user.uuid());

        if ( foundEntry.isEmpty() ) {
            return foundEntry;
        }

        List<Entry.Label> labels = transaction
                .doQueryAndStream(
                        RealLabel::new,
                        "SELECT * " +
                        "FROM labels " +
                        "   JOIN entries_labels relations" +
                        "       ON relations.label_uuid = labels.uuid " +
                        "WHERE relations.entry_uuid = ?",
                        foundEntry.get().uuid())
                .collect(toList());

        foundEntry.get().labels().addAll(labels);

        return foundEntry;
    }

    @Override
    public List<Entry> findAllBy(User user, Entry.Label... labels) {
        checkLabelsMustBelongToUser(user.uuid(), labels);
        checkLabelsMustBeStored(labels);

        List<UUID> labelUuids = stream(labels)
                .map(Entry.Label::uuid)
                .collect(toList());

        EntriesAndLabelsRowCollector entryToLabelsCollector = new EntriesAndLabelsRowCollector();

        super.currentTransaction()
                .doQuery(
                        entryToLabelsCollector,
                        "SELECT * " +
//                        "   entries.uuid AS e_uuid, " +
//                        "   entries.string AS e_string, " +
//                        "   entries.time AS e_time, " +
//                        "   entries.user_uuid AS e_user_uuid, " +
//                        "   labels.uuid AS m_uuid, " +
//                        "   labels.time AS m_time," +
//                        "   labels.name AS m_name," +
//                        "   labels.value AS m_value" +
                        "FROM labels " +
                        "   JOIN entries_labels relations " +
                        "       ON relations.label_uuid = labels.uuid " +
                        "   JOIN entries " +
                        "       ON relations.entry_uuid = entry.uuid" +
                        "WHERE labels.uuid IN (:in)".replace(":in", this.sqlQuestionMarks.getFor(labelUuids)),
                        labelUuids);

        return entryToLabelsCollector.ones();
    }

    private void checkLabelsMustBeStored(Entry.Label... labels) {
        String unstoredLabelsNames = stream(labels)
                .filter(label -> label.hasState(NON_STORED))
                .map(Entry.Label::name)
                .collect(joining(", "));

        if ( nonEmpty(unstoredLabelsNames) ) {
            throw new IllegalArgumentException(unstoredLabelsNames + " labels are not stored!");
        }
    }

    private static void checkLabelsMustBelongToUser(UUID userUuid, Entry.Label[] labels) {
        List<Entry.Label> otherLabels = stream(labels)
                .filter(label -> label.doesNotHaveUserUuid(userUuid))
                .collect(toList());

        if ( ! otherLabels.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Entry replace(User user, String oldEntryString, String newEntryString, RelatedPatternsAction action) {
        newEntryString = newEntryString.trim();

        Optional<Entry> foundEntry = this.findBy(user, oldEntryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        boolean entryWithNewStringExists = this.doesEntryExistsBy(user.uuid(), newEntryString);

        if ( entryWithNewStringExists ) {
            throw new IllegalArgumentException();
        }

        RealEntry entry = (RealEntry) foundEntry.get();

        return this.replaceInternally(entry, newEntryString, action);
    }

    private RealEntry replaceInternally(RealEntry entry, String newEntryString, RelatedPatternsAction action) {
        JdbcTransaction transaction = super.currentTransaction();

        if ( action.equalTo(REMOVE) ) {
            this.patternsToEntries.removeAllBy(entry);
        }

        int updated = transaction
                .doUpdate(
                        "UPDATE entries" +
                        "SET " +
                        "   stringOrigin = ?, " +
                        "   stringLower = ?, " +
                        "   time = ? " +
                        "WHERE entries.uuid = ? ",
                        newEntryString, newEntryString.toLowerCase(), now(), entry.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        entry.changeTo(newEntryString);

        if ( action.equalTo(ANALYZE_AGAIN) ) {
            this.patternsToEntries.analyzeAgainAllRelationsOf(entry);
        }

        this.choices.removeAllBy(entry);

        return entry;
    }

    @Override
    public Entry replace(Entry entry, String newEntryString, RelatedPatternsAction action) {
        checkMustBeStored(entry);

        boolean entryWithNewStringExists = doesEntryExistsBy(entry.userUuid(), newEntryString);

        if ( entryWithNewStringExists ) {
            throw new IllegalArgumentException();
        }

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

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM entries " +
                        "WHERE " +
                        "   entries.stringLower = ? AND " +
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
    public Entry addLabels(User user, String entryString, Entry.Label... labels) {
        Optional<Entry> foundEntry = this.findBy(user, entryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        Entry entry = foundEntry.get();

        this.addLabels(foundEntry.get(), labels);

        return entry;
    }

    @Override
    public boolean addLabels(Entry entry, Entry.Label... labels) {
        checkMustBeStored(entry);
        checkLabelsMustBelongToUser(entry.userUuid(), labels);
        checkMustBeStored(labels);

        JdbcTransaction transaction = super.currentTransaction();

        LocalDateTime joiningTime = now();

        int added;
        for ( Entry.Label label : labels ) {
            if ( entry.labels().contains(label) ) {
                continue;
            }

            added = transaction
                    .doUpdate(
                            "INSERT INTO labels_to_entries (uuid, entry_uuid, label_uuid, time) " +
                            "VALUES (?, ?, ?, ?)",
                            randomUUID(), entry.uuid(), label.uuid(), joiningTime);

            if ( added != 1 ) {
                throw new IllegalStateException();
            }

            this.labelsToCharsInEntries.join(entry, label, joiningTime);
            this.labelsToCharsInPhrases.join(entry, label, joiningTime);
            this.labelsToCharsInWords.join(entry, label, joiningTime);

            entry.labels().add(label);
        }

        return true;
    }
}
