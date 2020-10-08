package diarsid.search.impl;

import diarsid.search.api.Core;
import diarsid.search.api.Properties;
import diarsid.search.api.Search;
import diarsid.search.api.Store;
import diarsid.search.api.Users;

public class RealCore implements Core {

    private final Users users;
    private final Store store;
    private final Search entriesSearch;
    private final Properties properties;

    public RealCore(
            Users users,
            Store store,
            Search entriesSearch,
            Properties properties) {
        this.users = users;
        this.store = store;
        this.entriesSearch = entriesSearch;
        this.properties = properties;
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
}
