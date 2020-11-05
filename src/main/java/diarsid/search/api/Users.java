package diarsid.search.api;

import java.util.Optional;
import java.util.UUID;

import diarsid.search.api.model.User;

public interface Users {

    User create(String name);

    Optional<User> findBy(String name);

    User getBy(UUID uuid);
}
