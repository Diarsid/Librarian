package diarsid.search.impl;

import java.util.Optional;

import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.User;
import diarsid.search.impl.api.internal.Patterns;
import diarsid.search.impl.model.RealPattern;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.rows.RowGetter;

import static diarsid.search.api.model.Storable.State.STORED;

public class PatternsImpl extends ThreadTransactional implements Patterns {

    private final RowGetter<Pattern> rowToPattern;

    public PatternsImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
        this.rowToPattern = row -> new RealPattern(row);
    }

    @Override
    public Optional<Pattern> findBy(User user, String patternString) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        "SELECT * " +
                        "FROM patterns " +
                        "WHERE " +
                        "   string = ? AND " +
                        "   user_uuid = ? ",
                        patternString.trim().toLowerCase(), user.uuid());

    }

    @Override
    public Pattern save(User user, String patternString) {
        RealPattern pattern = new RealPattern(patternString, user.uuid());

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO patterns (uuid, time, user_uuid, string) " +
                        "VALUES (?, ?, ?, ?)",
                        pattern.uuid(), pattern.time(), pattern.userUuid(), pattern.string());

        if ( inserted == 1 ) {
            pattern.setState(STORED);
            return pattern;
        }
        else {
            throw new IllegalStateException();
        }
    }
}
