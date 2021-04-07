package diarsid.search.impl.logic.impl;

import java.util.List;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

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
