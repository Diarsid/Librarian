package diarsid.search.impl.logic.impl;

import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.JdbcFactory;
import diarsid.jdbc.api.JdbcTransaction;
import diarsid.search.api.Users;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.User;
import diarsid.search.impl.model.RealUser;

import static diarsid.jdbc.api.JdbcTransaction.Behavior.CLOSE;
import static diarsid.search.api.model.meta.Storable.State.STORED;

public class UsersImpl implements Users {

    private final JdbcFactory transactionFactory;

    public UsersImpl(JdbcFactory transactionFactory) {
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

            transaction.commitAndClose();

            return user;
        }
        catch (Throwable t) {
            transaction.rollbackAnd(CLOSE);
            throw t;
        }
    }

    @Override
    public Optional<User> findBy(String name) {
        JdbcTransaction transaction = transactionFactory.createTransaction();

        try {
            Optional<User> user = transaction
                    .doQueryAndConvertFirstRow(
                            RealUser::new,
                            "SELECT * " +
                            "FROM users " +
                            "WHERE users.name = ?;",
                            name);

            transaction.commitAndClose();

            return user;
        }
        catch (Throwable t) {
            transaction.rollbackAnd(CLOSE);
            return Optional.empty();
        }
    }

    @Override
    public User getBy(UUID uuid) {
        JdbcTransaction transaction = transactionFactory.createTransaction();

        try {
            User user = transaction
                .doQueryAndConvertFirstRow(
                        RealUser::new,
                        "SELECT * " +
                        "FROM users " +
                        "WHERE users.uuid = ?;",
                        uuid)
                .orElseThrow(NotFoundException::new);

            transaction.commitAndClose();

            return user;
        }
        catch (Throwable t) {
            transaction.rollbackAnd(CLOSE);
            throw t;
        }
    }
}
