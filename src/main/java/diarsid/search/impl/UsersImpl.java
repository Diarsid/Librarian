package diarsid.search.impl;

import java.util.UUID;

import diarsid.search.api.Users;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.User;
import diarsid.search.impl.model.RealUser;
import diarsid.jdbc.JdbcTransactionFactory;
import diarsid.jdbc.api.JdbcTransaction;

import static diarsid.search.api.model.Storable.State.STORED;

public class UsersImpl implements Users {

    private final JdbcTransactionFactory transactionFactory;

    public UsersImpl(JdbcTransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    @Override
    public User create(String name) {
        User user = new RealUser(name);

        JdbcTransaction transaction = transactionFactory.createTransaction();

        try {
            int updated = transaction
                    .doUpdate(
                            "INSERT INTO users (uuid, name, time) " +
                                    "VALUES (?, ?, ?);",
                            user.uuid(),
                            user.name(),
                            user.time());

            if ( updated == 1 ) {
                user.setState(STORED);
            }

            transaction.commit();

            return user;
        }
        catch (Throwable t) {
            transaction.rollbackAndProceed();
            throw t;
        }
    }

    @Override
    public User get(UUID uuid) {
        JdbcTransaction transaction = transactionFactory.createTransaction();

        try {
            User user = transaction
                .doQueryAndConvertFirstRow(
                        row -> new RealUser(row),
                        "SELECT * " +
                        "FROM users " +
                        "WHERE users.uuid = ?;",
                        uuid)
                .orElseThrow(NotFoundException::new);

            transaction.commit();

            return user;
        }
        catch (Throwable t) {
            transaction.rollbackAndProceed();
            throw t;
        }
    }
}
