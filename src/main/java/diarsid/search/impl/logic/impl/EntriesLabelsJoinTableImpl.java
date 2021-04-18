package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.List;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.LabelToEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;

import static diarsid.support.model.Unique.uuidsOf;

public class EntriesLabelsJoinTableImpl extends ThreadBoundTransactional implements EntriesLabelsJoinTable {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlDeleteWhereEntryAndLabels;

    public EntriesLabelsJoinTableImpl(Jdbc jdbc) {
        super(jdbc);

        this.sqlDeleteWhereEntryAndLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "DELETE \n" +
                "FROM labels_to_entries \n" +
                "WHERE \n" +
                "   entry_uuid = ? AND \n" +
                "   label_uuid IN ( \n",
                "       ?", ", \n",
                ") ");
    }

    @Override
    public List<Entry.Labeled> getAllJoinedTo(Entry entry) {
        LocalDateTime entriesAtualAt = now();
        return super.currentTransaction()
                .doQueryAndStream(
                        row -> new LabelToEntry(entriesAtualAt, row, "j_", "e_", "l_"),
                        "SELECT \n" +
                                "   le.uuid         AS j_uuid, \n" +
                                "   le.time         AS j_time, \n" +
                                "   e.uuid          AS e_uuid, \n" +
                                "   e.time          AS e_time, \n" +
                                "   e.user_uuid     AS e_user_uuid, \n" +
                                "   e.string_origin AS e_string_origin, \n" +
                                "   e.string_lower  AS e_string_lower, \n" +
                                "   l.uuid          AS l_uuid, \n" +
                                "   l.time          AS l_time, \n" +
                                "   l.user_uuid     AS l_user_uuid, \n" +
                                "   l.name          AS l_name \n" +
                                "FROM labels_to_entries le \n" +
                                "   JOIN labels l \n" +
                                "       ON le.label_uuid = l.uuid \n" +
                                "   JOIN entries e \n" +
                                "       ON le.entry_uuid = e.uuid \n" +
                                "WHERE le.entry_uuid = ? ",
                        entry.uuid())
                .collect(toList());
    }

    @Override
    public int removeAllBy(Entry entry) {
        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE \n" +
                        "FROM labels_to_entries \n" +
                        "WHERE entry_uuid = ?",
                        entry.uuid());

        return removed;
    }

    @Override
    public boolean removeBy(Entry entry, Entry.Label label) {
        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE \n" +
                        "FROM labels_to_entries \n" +
                        "WHERE \n" +
                        "   entry_uuid = ? AND \n" +
                        "   label_uuid = ? ",
                        entry.uuid(), label.uuid());

        if ( removed > 1 ) {
            throw new IllegalStateException();
        }

        return removed == 1;
    }

    @Override
    public boolean removeBy(Entry entry, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return false;
        }

        int removed = super.currentTransaction()
                .doUpdate(
                        this.sqlDeleteWhereEntryAndLabels.getFor(labels),
                        entry.uuid(), uuidsOf(labels));

        return removed == labels.size();
    }
}
