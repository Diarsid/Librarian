package diarsid.search.impl.logic.impl;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.api.Behavior;
import diarsid.search.api.Core;
import diarsid.search.api.Properties;
import diarsid.search.api.Search;
import diarsid.search.api.Store;
import diarsid.search.api.Users;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.BehaviorsByUsers;

public class CoreImpl implements Core {

    private final JdbcTransactionThreadBindings transactionThreadBindings;
    private final Users users;
    private final Store store;
    private final Search entriesSearch;
    private final Properties properties;
    private final BehaviorsByUsers behaviorsByUsers;

    public CoreImpl(
            JdbcTransactionThreadBindings transactionThreadBindings,
            Users users,
            Store store,
            Search entriesSearch,
            BehaviorsByUsers behaviorsByUsers,
            Properties properties) {
        this.transactionThreadBindings = transactionThreadBindings;
        this.users = users;
        this.store = store;
        this.entriesSearch = entriesSearch;
        this.properties = properties;
        this.behaviorsByUsers = behaviorsByUsers;
    }

    @Override
    public Behavior behavior(User user) {
        return behaviorsByUsers.getBehaviorFor(user);
    }

    @Override
    public Users users() {
        return users;
    }

    @Override
    public Store store() {
        return store;
    }

    @Override
    public Search search() {
        return entriesSearch;
    }

    @Override
    public Properties properties() {
        return properties;
    }

    public JdbcTransactionThreadBindings transactionThreadBindings() {
        return transactionThreadBindings;
    }
}
