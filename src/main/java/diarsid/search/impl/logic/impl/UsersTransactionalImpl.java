package diarsid.search.impl.logic.impl;

import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcTransaction;
import diarsid.search.api.Users;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.User;
import diarsid.search.impl.model.RealUser;

import static diarsid.jdbc.api.JdbcTransaction.ThenDo.CLOSE;
import static diarsid.search.api.model.meta.Storable.State.STORED;

public class UsersTransactionalImpl implements Users {

    private final Jdbc jdbc;

    public UsersTransactionalImpl(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public User create(String name) {
        User user = new RealUser(name);

        JdbcTransaction transaction = jdbc.createTransaction();

        try {
            int updated = transaction
                    .doUpdate(
                            "INSERT INTO users (uuid, name, time) \n" +
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
        JdbcTransaction transaction = jdbc.createTransaction();

        try {
            Optional<User> user = transaction
                    .doQueryAndConvertFirstRow(
                            RealUser::new,
                            "SELECT * \n" +
                            "FROM users \n" +
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
        JdbcTransaction transaction = jdbc.createTransaction();

        try {
            User user = transaction
                .doQueryAndConvertFirstRow(
                        RealUser::new,
                        "SELECT * \n" +
                        "FROM users \n" +
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
