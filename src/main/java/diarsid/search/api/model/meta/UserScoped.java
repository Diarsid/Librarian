package diarsid.search.api.model.meta;

import java.util.UUID;

import diarsid.search.api.model.User;

public interface UserScoped {

    UUID userUuid();

    default boolean belongsTo(User user) {
        return this.userUuid().equals(user.uuid());
    }

    default boolean hasUserUuid(UUID uuid) {
        return this.userUuid().equals(uuid);
    }

    default boolean doesNotBelongTo(User user) {
        return ! this.userUuid().equals(user.uuid());
    }

    default boolean doesNotHaveUserUuid(UUID uuid) {
        return ! this.userUuid().equals(uuid);
    }
}
