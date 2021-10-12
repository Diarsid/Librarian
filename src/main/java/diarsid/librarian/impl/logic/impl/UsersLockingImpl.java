package diarsid.librarian.impl.logic.impl;

import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.exceptions.NotFoundException;
import diarsid.librarian.impl.logic.api.UsersLocking;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;

public class UsersLockingImpl extends ThreadBoundTransactional implements UsersLocking {

    public UsersLockingImpl(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);
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
