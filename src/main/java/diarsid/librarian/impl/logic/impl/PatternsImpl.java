package diarsid.librarian.impl.logic.impl;

import java.util.Optional;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.rows.RowGetter;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.Patterns;
import diarsid.librarian.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealPattern;

import static diarsid.support.model.Storable.State.STORED;

public class PatternsImpl extends ThreadBoundTransactional implements Patterns {

    private final RowGetter<Pattern> rowToPattern;

    public PatternsImpl(Jdbc jdbc) {
        super(jdbc);
        this.rowToPattern = RealPattern::new;
    }

    @Override
    public Optional<Pattern> findBy(User user, String patternString) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        "SELECT * \n" +
                        "FROM patterns \n" +
                        "WHERE \n" +
                        "   string = ? AND \n" +
                        "   user_uuid = ? ",
                        patternString.trim().toLowerCase(), user.uuid());

    }

    @Override
    public Pattern save(User user, String patternString) {
        RealPattern pattern = new RealPattern(patternString, user.uuid());

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO patterns (uuid, time, user_uuid, string) \n" +
                        "VALUES (?, ?, ?, ?)",
                        pattern.uuid(), pattern.createdAt(), pattern.userUuid(), pattern.string());

        if ( inserted == 1 ) {
            pattern.setState(STORED);
            return pattern;
        }
        else {
            throw new IllegalStateException();
        }
    }
}
