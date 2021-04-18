package diarsid.librarian.impl.logic.api;

import java.util.UUID;

import diarsid.librarian.api.model.User;

public interface UsersLocking {

    default void lock(User user) {
        this.lock(user.uuid());
    }

    void lock(UUID userUuid);
}
