package diarsid.librarian.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.model.Word;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.util.stream.Collectors.toList;

import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.support.model.Unique.uuidsOf;

public class EntriesSearchByWord extends ThreadBoundTransactionalEntries {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByAnyOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByAnyOfLabelsBefore;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByAnyOfLabelsAfterOrEqual;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByAllOfLabelsBefore;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByAllOfLabelsAfterOrEqual;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByNoneOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByNoneOfLabelsBefore;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlFindByNoneOfLabelsAfterOrEqual;

    public EntriesSearchByWord(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);

        this.sqlFindByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "    )");

        this.sqlFindByAnyOfLabelsBefore = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    e.time < ? AND \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "    )");

        this.sqlFindByAnyOfLabelsAfterOrEqual = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    e.time >= ? AND \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "    )");

        this.sqlFindByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                             "?", ", \n",
                "        ) \n" +
                "        GROUP BY entry_uuid \n" +
                "        HAVING COUNT(label_uuid) = ? \n" +
                "    )");

        this.sqlFindByAllOfLabelsBefore = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    e.time < ? AND \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "        GROUP BY entry_uuid \n" +
                "        HAVING COUNT(label_uuid) = ? \n" +
                "    )");

        this.sqlFindByAllOfLabelsAfterOrEqual = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    e.time >= ? AND \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "        GROUP BY entry_uuid \n" +
                "        HAVING COUNT(label_uuid) = ? \n" +
                "    )");

        this.sqlFindByNoneOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid NOT IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "    )");

        this.sqlFindByNoneOfLabelsBefore = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    e.time < ? AND \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid NOT IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "    )");

        this.sqlFindByNoneOfLabelsAfterOrEqual = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT e.uuid \n" +
                "FROM entries e \n" +
                "    JOIN words_in_entries we \n" +
                "        ON we.entry_uuid = e.uuid \n" +
                "WHERE \n" +
                "    e.time >= ? AND \n" +
                "    we.word_uuid =? AND \n" +
                "    e.uuid NOT IN ( \n" +
                "        SELECT le.entry_uuid \n" +
                "        FROM labels_to_entries le \n" +
                "        WHERE le.label_uuid IN ( \n",
                "            ?", ", \n",
                "        ) \n" +
                "    )");
    }

    public List<Entry> findBy(Word word, TimeDirection timeDirection, LocalDateTime time) {
        switch ( timeDirection ) {
            case BEFORE: return this.findByBefore(word, time);
            case AFTER_OR_EQUAL: return this.findByAfterOrEqual(word, time);
            default: throw timeDirection.unsupported();
        }
    }

    public List<Entry> findBy(Word word, Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return this.findBy(word);
        }
        else if ( labels.size() == 1 ) {
            if ( matching.equalTo(NONE_OF) ) {
                return this.findByNotLabel(word, labels.get(0));
            }
            else {
                return this.findByLabel(word, labels.get(0));
            }            
        }
        else {
            switch ( matching ) {
                case ANY_OF: return this.findByAnyOf(word, labels);
                case ALL_OF: return this.findByAllOf(word, labels);
                case NONE_OF: return this.findByNoneOf(word, labels);
                default: throw matching.unsupported();
            }
        }
    }

    public List<Entry> findBy(Word word, Entry.Label.Matching matching, List<Entry.Label> labels, TimeDirection timeDirection, LocalDateTime time) {
        if ( labels.isEmpty() ) {
            switch ( timeDirection ) {
                case BEFORE: return this.findByBefore(word, time);
                case AFTER_OR_EQUAL: return this.findByAfterOrEqual(word, time);
                default: throw timeDirection.unsupported();
            }
        }
        else if ( labels.size() == 1 ) {
            Entry.Label label = labels.get(0);
            if ( matching.equalTo(NONE_OF) ) {
                switch ( timeDirection ) {
                    case BEFORE: return this.findByNotLabelBefore(word, label, time);
                    case AFTER_OR_EQUAL: return this.findByNotLabelAfterOrEqual(word, label, time);
                    default: throw timeDirection.unsupported();
                }
            }
            else {
                switch ( timeDirection ) {
                    case BEFORE: return this.findByLabelBefore(word, label, time);
                    case AFTER_OR_EQUAL: return this.findByLabelAfterOrEqual(word, label, time);
                    default: throw timeDirection.unsupported();
                }
            }
        }
        else {
            switch ( matching ) {
                case ANY_OF:
                    switch ( timeDirection ) {
                        case BEFORE: return this.findByAnyOfLabelsBefore(word, labels, time);
                        case AFTER_OR_EQUAL: return this.findByAnyOfLabelsAfterOrEqual(word, labels, time);
                        default: throw timeDirection.unsupported();
                    }
                case ALL_OF:
                    switch ( timeDirection ) {
                        case BEFORE: return this.findByAllOfLabelsBefore(word, labels, time);
                        case AFTER_OR_EQUAL: return this.findByAllOfLabelsAfterOrEqual(word, labels, time);
                        default: throw timeDirection.unsupported();
                    }
                case NONE_OF:
                    switch ( timeDirection ) {
                        case BEFORE: return this.findByNoneOfLabelsBefore(word, labels, time);
                        case AFTER_OR_EQUAL: return this.findByNoneOfLabelsAfterOrEqual(word, labels, time);
                        default: throw timeDirection.unsupported();
                    }
                default:
                    throw matching.unsupported();
            }
        }
    }

    public List<Entry> findBy(Word word) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "WHERE we.word_uuid = ?",
                        word.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByBefore(Word word, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "   we.word_uuid = ? AND \n" +
                        "   e.time < ? ",
                        word.uuid(), time)
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAfterOrEqual(Word word, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "   we.word_uuid = ? AND \n" +
                        "   e.time >= ? ",
                        word.uuid(), time)
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }
    
    private List<Entry> findByLabel(Word word, Entry.Label label) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "    JOIN labels_to_entries le \n" +
                        "        ON le.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "    we.word_uuid = ? AND \n" +
                        "    le.label_uuid = ? ",
                        word.uuid(), label.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByLabelBefore(Word word, Entry.Label label, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "    JOIN labels_to_entries le \n" +
                        "        ON le.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "    e.time < ? AND \n" +
                        "    we.word_uuid = ? AND \n" +
                        "    le.label_uuid = ? ",
                        time, word.uuid(), label.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByLabelAfterOrEqual(Word word, Entry.Label label, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "    JOIN labels_to_entries le \n" +
                        "        ON le.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "    e.time >= ? AND \n" +
                        "    we.word_uuid = ? AND \n" +
                        "    le.label_uuid = ? ",
                        time, word.uuid(), label.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByNotLabel(Word word, Entry.Label label) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "    we.word_uuid =? AND \n" +
                        "    e.uuid NOT IN ( \n" +
                        "        SELECT le.entry_uuid \n" +
                        "        FROM labels_to_entries le \n" +
                        "        WHERE le.label_uuid = ? \n" +
                        "    )",
                        word.uuid(), label.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByNotLabelBefore(Word word, Entry.Label label, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "    e.time < ? AND \n" +
                        "    we.word_uuid =? AND \n" +
                        "    e.uuid NOT IN ( \n" +
                        "        SELECT le.entry_uuid \n" +
                        "        FROM labels_to_entries le \n" +
                        "        WHERE le.label_uuid = ? \n" +
                        "    )",
                        time, word.uuid(), label.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByNotLabelAfterOrEqual(Word word, Entry.Label label, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "SELECT e.uuid \n" +
                        "FROM entries e \n" +
                        "    JOIN words_in_entries we \n" +
                        "        ON we.entry_uuid = e.uuid \n" +
                        "WHERE \n" +
                        "    e.time >= ? AND \n" +
                        "    we.word_uuid =? AND \n" +
                        "    e.uuid NOT IN ( \n" +
                        "        SELECT le.entry_uuid \n" +
                        "        FROM labels_to_entries le \n" +
                        "        WHERE le.label_uuid = ? \n" +
                        "    )",
                        time, word.uuid(), label.uuid())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAnyOfLabelsBefore(Word word, List<Entry.Label> labels, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByAnyOfLabelsBefore.getFor(labels),
                        time, word.uuid(), uuidsOf(labels))
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAnyOfLabelsAfterOrEqual(Word word, List<Entry.Label> labels, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByAnyOfLabelsAfterOrEqual.getFor(labels),
                        time, word.uuid(), uuidsOf(labels))
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAnyOf(Word word, List<Entry.Label> labels) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByAnyOfLabels.getFor(labels),
                        word.uuid(), uuidsOf(labels))
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAllOf(Word word, List<Entry.Label> labels) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByAllOfLabels.getFor(labels),
                        word.uuid(), uuidsOf(labels), labels.size())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAllOfLabelsBefore(Word word, List<Entry.Label> labels, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByAllOfLabelsBefore.getFor(labels),
                        time, word.uuid(), uuidsOf(labels), labels.size())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByAllOfLabelsAfterOrEqual(Word word, List<Entry.Label> labels, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByAllOfLabelsAfterOrEqual.getFor(labels),
                        time, word.uuid(), uuidsOf(labels), labels.size())
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByNoneOf(Word word, List<Entry.Label> labels) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByNoneOfLabels.getFor(labels),
                        word.uuid(), uuidsOf(labels))
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByNoneOfLabelsBefore(Word word, List<Entry.Label> labels, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByNoneOfLabelsBefore.getFor(labels),
                        time, word.uuid(), uuidsOf(labels))
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> findByNoneOfLabelsAfterOrEqual(Word word, List<Entry.Label> labels, LocalDateTime time) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        this.sqlFindByNoneOfLabelsAfterOrEqual.getFor(labels),
                        time, word.uuid(), uuidsOf(labels))
                .collect(toList());

        return super.getEntriesBy(entryUuids);
    }
}
