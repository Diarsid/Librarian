package diarsid.search.api.model.meta;

import java.util.List;
import java.util.UUID;

import diarsid.search.api.model.User;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

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

    default void checkHasSameUser(UserScoped other) {
        if ( ! this.userUuid().equals(other.userUuid()) ) {
            throw new IllegalArgumentException();
        }
    }

    default void checkAllHaveSameUser(UserScoped... others) {
        checkMustBelongToUser(this.userUuid(), others);
    }

    default void checkAllHaveSameUser(List<? extends UserScoped> others) {
        checkMustBelongToUser(this.userUuid(), others);
    }

    default void checkMustBelongTo(User user) {
        checkMustBelongToUser(user, this);
    }

    static void checkMustBelongToUser(User user, List<? extends UserScoped> userScopeds) {
        checkMustBelongToUser(user.uuid(), userScopeds);
    }

    static void checkMustBelongToUser(UUID userUuid, List<? extends UserScoped> userScopeds) {
        List<UserScoped> otherLabels = userScopeds
                .stream()
                .filter(userScoped -> userScoped.doesNotHaveUserUuid(userUuid))
                .collect(toList());

        if ( ! otherLabels.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    static void checkMustBelongToUser(User user, UserScoped... userScopeds) {
        checkMustBelongToUser(user.uuid(), userScopeds);
    }

    static void checkMustBelongToUser(UUID userUuid, UserScoped... userScopeds) {
        List<UserScoped> otherLabels = stream(userScopeds)
                .filter(userScoped -> userScoped.doesNotHaveUserUuid(userUuid))
                .collect(toList());

        if ( ! otherLabels.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    static void checkMustBelongToOneUser(List<? extends UserScoped> userScopeds) {
        if ( userScopeds.isEmpty() ) {
            return;
        }
        UUID userUuid = userScopeds.get(0).userUuid();
        checkMustBelongToUser(userUuid, userScopeds);
    }

    static void checkMustBelongToOneUser(UserScoped... userScopeds) {
        if ( userScopeds.length == 0 ) {
            return;
        }
        UUID userUuid = userScopeds[0].userUuid();
        checkMustBelongToUser(userUuid, userScopeds);
    }
}
