package diarsid.search.impl.logic.api;

import java.util.UUID;

import diarsid.search.api.model.User;

public interface UsersLocking {

    default void lock(User user) {
        this.lock(user.uuid());
    }

    void lock(UUID userUuid);
}
