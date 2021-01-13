package diarsid.search.impl.logic.impl;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.Behavior;
import diarsid.search.api.Core;
import diarsid.search.api.Properties;
import diarsid.search.api.Search;
import diarsid.search.api.Store;
import diarsid.search.api.Users;

public class CoreImpl implements Core {

    private final Jdbc jdbc;
    private final Users users;
    private final Store store;
    private final Search entriesSearch;
    private final Properties properties;
    private final Behavior behavior;

    public CoreImpl(
            Jdbc jdbc,
            Users users,
            Store store,
            Search entriesSearch,
            Behavior behavior,
            Properties properties) {
        this.jdbc = jdbc;
        this.users = users;
        this.store = store;
        this.entriesSearch = entriesSearch;
        this.properties = properties;
        this.behavior = behavior;
    }

    @Override
    public Behavior behavior() {
        return behavior;
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

    public Jdbc jdbc() {
        return jdbc;
    }
}
