package diarsid.search.impl.logic.impl;

import java.util.UUID;

import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.impl.logic.api.UsersLocking;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;

public class UsersLockingImpl extends ThreadTransactional implements UsersLocking {

    public UsersLockingImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public void lock(UUID userUuid) {
        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * " +
                        "FROM users " +
                        "WHERE users.uuid = ? " +
                        "FOR UPDATE",
                        userUuid);

        if ( count == 0 ) {
            throw new NotFoundException();
        }

        if ( count > 1 ) {
            throw new IllegalStateException();
        }
    }
}
