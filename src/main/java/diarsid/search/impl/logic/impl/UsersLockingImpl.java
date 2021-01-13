package diarsid.search.impl.logic.impl;

import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.impl.logic.api.UsersLocking;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;

public class UsersLockingImpl extends ThreadBoundTransactional implements UsersLocking {

    public UsersLockingImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public void lock(UUID userUuid) {
        int count = super.currentTransaction()
                .countQueryResults(
                        "SELECT * \n" +
                        "FROM users \n" +
                        "WHERE users.uuid = ? \n" +
                        "FOR UPDATE ",
                        userUuid);

        if ( count == 0 ) {
            throw new NotFoundException();
        }

        if ( count > 1 ) {
            throw new IllegalStateException();
        }
    }
}
