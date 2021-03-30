package diarsid.search.impl.logic.impl.jdbc;

import java.util.List;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;
import diarsid.support.objects.PooledReusable;

public class PooledRowCollectorForEntriesAndLabels extends PooledReusable implements ContextBoundRowOperation {

    private final RowCollectorForEntriesAndLabels rowCollector;
    private final RowOperationContext context;

    public PooledRowCollectorForEntriesAndLabels() {
        this.context = new RowOperationContext();
        this.rowCollector = new RowCollectorForEntriesAndLabels(context);
    }

    public PooledRowCollectorForEntriesAndLabels(String entriesTable, String labelsTable) {
        this.context = new RowOperationContext();
        this.rowCollector = new RowCollectorForEntriesAndLabels(context, entriesTable, labelsTable);
    }

    @Override
    protected void clearForReuse() {
        this.rowCollector.clear();
        this.context.clear();
    }

    @Override
    public RowOperationContext context() {
        return this.context;
    }

    @Override
    public void process(Row row) {
        this.rowCollector.process(row);
    }

    public List<Entry> entries() {
        return this.rowCollector.entries();
    }
}
