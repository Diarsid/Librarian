package diarsid.librarian.impl.logic.impl.support;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;

public abstract class ThreadBoundTransactional {

    protected final Jdbc jdbc;

    public ThreadBoundTransactional(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    public ThreadBoundJdbcTransaction currentTransaction() {
        return this.jdbc.threadBinding().currentTransaction();
    }

    public final boolean isBoundToCurrentTransaction(TransactionalScoped transactionalScoped) {
        return currentTransaction().uuid().equals(transactionalScoped.transactionUuid());
    }

    public final boolean isNotBoundToCurrentTransaction(TransactionalScoped transactionalScoped) {
        return ! this.isBoundToCurrentTransaction(transactionalScoped);
    }
}
