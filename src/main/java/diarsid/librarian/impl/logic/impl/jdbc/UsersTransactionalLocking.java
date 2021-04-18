package diarsid.librarian.impl.logic.impl.jdbc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import diarsid.jdbc.api.TransactionAware;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.User;
import diarsid.librarian.api.model.meta.UserScoped;
import diarsid.librarian.impl.logic.api.UsersLocking;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import static diarsid.librarian.api.Core.Mode.DEVELOPMENT;
import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;

public class UsersTransactionalLocking implements TransactionAware {

    private final AtomicReference<Core.Mode> coreMode;
    private final UsersLocking usersLocking;

    public UsersTransactionalLocking(AtomicReference<Core.Mode> coreMode, UsersLocking usersLocking) {
        this.coreMode = coreMode;
        this.usersLocking = usersLocking;
    }

    @Override
    public void afterTransactionOpenFor(Method method, Object[] args) {
        this.tryLockOnUserUuidWhenFoundIn(args);
    }

    @Override
    public void beforeTransactionJoinFor(Method method, Object[] args) {
        this.tryLockOnUserUuidWhenFoundIn(args);
    }

    private void tryLockOnUserUuidWhenFoundIn(Object[] args) {
        Core.Mode mode = coreMode.get();

        if ( isNull(mode) || mode.equalTo(DEVELOPMENT) ) {
            return;
        }

        UUID userUuid = null;

        for ( Object arg : args ) {
            userUuid = tryFindUserUuidIn(arg);
            if ( nonNull(userUuid) ) {
                break;
            }
        }

        if ( nonNull(userUuid) ) {
            usersLocking.lock(userUuid);
        }
    }

    private static UUID tryFindUserUuidIn(Object arg) {
        if ( arg instanceof User) {
            return ((User) arg).uuid();
        }
        else if ( arg instanceof UserScoped) {
            return ((UserScoped) arg).userUuid();
        }
        else if ( arg instanceof Collection) {
            Collection collection = (Collection) arg;
            if ( isNotEmpty(collection) ) {
                UUID firstUserUuid = null;
                UUID currentUserUuid;
                for ( Object object : collection ) {
                    if ( isNull(firstUserUuid) ) {
                        firstUserUuid = discoverUserUuidFrom(object);
                    }
                    else {
                        currentUserUuid = discoverUserUuidFrom(object);
                        if ( ! currentUserUuid.equals(firstUserUuid) ) {
                            throw new IllegalArgumentException("User uuids mismatch!");
                        }
                    }
                }
                return firstUserUuid;
            }
        }
        else if ( arg instanceof Object[]) {
            Object[] array = (Object[]) arg;
            if ( array.length > 0 ) {
                UUID firstUserUuid = null;
                UUID currentUserUuid;
                for ( Object object : array ) {
                    if ( isNull(firstUserUuid) ) {
                        firstUserUuid = discoverUserUuidFrom(object);
                    }
                    else {
                        currentUserUuid = discoverUserUuidFrom(object);
                        if ( ! currentUserUuid.equals(firstUserUuid) ) {
                            throw new IllegalArgumentException("User uuids mismatch!");
                        }
                    }
                }
                return firstUserUuid;
            }
        }

        return null;
    }

    private static UUID discoverUserUuidFrom(Object obj) {
        UUID userUuid = null;

        if ( obj instanceof User) {
            userUuid = ((User) obj).uuid();
        }
        else if ( obj instanceof UserScoped) {
            userUuid = ((UserScoped) obj).userUuid();
        }

        return userUuid;
    }
}
