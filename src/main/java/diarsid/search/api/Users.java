package diarsid.search.api;

import java.util.UUID;

import diarsid.search.api.model.User;

public interface Users {

    User create(String name);

    User get(UUID uuid);
}
