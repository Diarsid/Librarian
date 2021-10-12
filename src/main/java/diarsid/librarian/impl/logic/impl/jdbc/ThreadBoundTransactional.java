package diarsid.librarian.impl.logic.impl.jdbc;

import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.librarian.impl.logic.api.UuidSupplier;

public abstract class ThreadBoundTransactional {

    private final UuidSupplier uuidSupplier;
    protected final Jdbc jdbc;

    public ThreadBoundTransactional(Jdbc jdbc, UuidSupplier uuidSupplier) {
        this.uuidSupplier = uuidSupplier;
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

    public UUID nextRandomUuid() {
        return this.uuidSupplier.nextRandomUuid();
    }
}
