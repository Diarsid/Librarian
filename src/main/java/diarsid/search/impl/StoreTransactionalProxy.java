package diarsid.search.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import diarsid.search.api.Store;
import diarsid.search.api.model.User;
import diarsid.search.api.model.UserScoped;
import diarsid.search.impl.api.internal.UsersLocking;
import diarsid.jdbc.JdbcTransactionThreadBindings;

import static java.util.Objects.isNull;

public class StoreTransactionalProxy implements InvocationHandler {

    private final Store store;
    private final UsersLocking usersLocking;
    private final JdbcTransactionThreadBindings transactionThreadBindings;

    public StoreTransactionalProxy(
            Store store,
            UsersLocking usersLocking,
            JdbcTransactionThreadBindings transactionThreadBindings) {
        this.store = store;
        this.usersLocking = usersLocking;
        this.transactionThreadBindings = transactionThreadBindings;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        UUID userUuid = null;

        Object arg0 = args[0];

        if ( arg0 instanceof User ) {
            userUuid = ((User) arg0).uuid();
        }
        else if ( arg0 instanceof UserScoped ) {
            userUuid = ((UserScoped) arg0).userUuid();
        }

        if ( isNull(userUuid) ) {
            throw new IllegalStateException();
        }

        this.transactionThreadBindings.beginTransaction();
        try {
            this.usersLocking.lock(userUuid);
            Object result = method.invoke(this.store, args);
            this.transactionThreadBindings.commitTransaction();
            return result;
        }
        catch (Throwable t) {
            this.transactionThreadBindings.rollbackTransaction();
            throw t;
        }
    }
}
