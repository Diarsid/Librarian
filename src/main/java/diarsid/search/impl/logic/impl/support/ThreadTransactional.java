package diarsid.search.impl.logic.impl.support;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.JdbcTransaction;

public abstract class ThreadTransactional {

    private final JdbcTransactionThreadBindings transactionThreadBindings;

    public ThreadTransactional(JdbcTransactionThreadBindings transactionThreadBindings) {
        this.transactionThreadBindings = transactionThreadBindings;
    }

    public JdbcTransaction currentTransaction() {
        return this.transactionThreadBindings.currentTransaction();
    }
}
