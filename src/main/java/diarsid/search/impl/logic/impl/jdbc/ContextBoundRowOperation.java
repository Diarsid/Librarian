package diarsid.search.impl.logic.impl.jdbc;

import diarsid.jdbc.api.sqltable.rows.RowOperation;

public interface ContextBoundRowOperation extends RowOperation {

    RowOperationContext context();
}
