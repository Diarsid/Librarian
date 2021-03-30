package diarsid.search.impl.logic.impl.jdbc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.collectors.RowsCollectorOneToManyOnesEmbeddedLists;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealLabel;

public class RowCollectorForEntriesAndLabels extends RowsCollectorOneToManyOnesEmbeddedLists<Entry, UUID, Entry.Label, UUID> {

    public RowCollectorForEntriesAndLabels(RowOperationContext context) {
        super(
                Entry::labels,
                row -> row.uuidOf("entries.uuid"),
                row -> row.uuidOf("labels.uuid"),
                row -> new RealEntry(context.get(LocalDateTime.class), context.get(UUID.class), "entries.", row),
                row -> new RealLabel("labels.", row));
    }

    public RowCollectorForEntriesAndLabels(RowOperationContext context, String entriesTable, String labelsTable) {
        super(
                Entry::labels,
                row -> row.uuidOf(entriesTable + "uuid"),
                row -> row.uuidOf(labelsTable + "uuid"),
                row -> new RealEntry(context.get(LocalDateTime.class), context.get(UUID.class), entriesTable, row),
                row -> new RealLabel(labelsTable, row));
    }

    public List<Entry> entries() {
        return super.ones();
    }
}
