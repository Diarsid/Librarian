package diarsid.search.impl.logic.impl.jdbc;

import diarsid.support.objects.PooledReusable;

public class PooledRowCollectorForEntriesAndLabels extends PooledReusable {

    private final RowCollectorForEntriesAndLabels rowCollector;

    public PooledRowCollectorForEntriesAndLabels() {
        this.rowCollector = new RowCollectorForEntriesAndLabels();
    }

    public PooledRowCollectorForEntriesAndLabels(String entriesTable, String labelsTable) {
        this.rowCollector = new RowCollectorForEntriesAndLabels(entriesTable, labelsTable);
    }

    @Override
    protected void clearForReuse() {
        this.rowCollector.clear();
    }

    public RowCollectorForEntriesAndLabels get() {
        return this.rowCollector;
    }
}
