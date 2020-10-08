package diarsid.search.impl;

import java.util.List;
import java.util.Optional;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.api.Search;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.User;
import diarsid.search.impl.api.internal.UsersLocking;

import static java.util.Collections.emptyList;

public class SearchTransactionalWrapper implements Search {

    private final JdbcTransactionThreadBindings jdbcTransactionThreadBindings;
    private final UsersLocking usersLocking;
    private final Search wrappedSearch;

    public SearchTransactionalWrapper(
            JdbcTransactionThreadBindings jdbcTransactionThreadBindings,
            Search wrappedSearch,
            UsersLocking usersLocking) {
        this.jdbcTransactionThreadBindings = jdbcTransactionThreadBindings;
        this.usersLocking = usersLocking;
        this.wrappedSearch = wrappedSearch;
    }

    @Override
    public List<PatternToEntry> findAllBy(User user, String pattern) {
        this.jdbcTransactionThreadBindings.beginTransaction();
        try {
            this.usersLocking.lock(user);
            List<PatternToEntry> relations = this.wrappedSearch.findAllBy(user, pattern);
            this.jdbcTransactionThreadBindings.commitTransaction();
            return relations;
        }
        catch (Throwable t) {
            this.jdbcTransactionThreadBindings.rollbackTransaction();
            return emptyList();
        }
    }

    @Override
    public Optional<PatternToEntry> findSingleBy(User user, String pattern) {
        this.jdbcTransactionThreadBindings.beginTransaction();
        try {
            this.usersLocking.lock(user);
            Optional<PatternToEntry> relation = this.wrappedSearch.findSingleBy(user, pattern);
            this.jdbcTransactionThreadBindings.commitTransaction();
            return relation;
        }
        catch (Throwable t) {
            this.jdbcTransactionThreadBindings.rollbackTransaction();
            return Optional.empty();
        }
    }
}
