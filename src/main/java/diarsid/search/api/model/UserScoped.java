package diarsid.search.api.model;

import java.util.UUID;

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
