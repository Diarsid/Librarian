package diarsid.search.impl.logic.impl.support;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;

public abstract class ThreadBoundTransactional {

    private final Jdbc jdbc;

    public ThreadBoundTransactional(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    public ThreadBoundJdbcTransaction currentTransaction() {
        return this.jdbc.threadBinding().currentTransaction();
    }
}
