package diarsid.search.impl.logic.impl.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.api.model.User;
import diarsid.search.api.model.meta.UserScoped;
import diarsid.search.impl.logic.api.UsersLocking;

import static java.util.Objects.isNull;

public class TransactionalProxy implements InvocationHandler {

    private final Object transactionalService;
    private final UsersLocking usersLocking;
    private final JdbcTransactionThreadBindings transactionThreadBindings;

    public TransactionalProxy(
            Object transactionalService,
            UsersLocking usersLocking,
            JdbcTransactionThreadBindings transactionThreadBindings) {
        this.transactionalService = transactionalService;
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
            Object result = method.invoke(this.transactionalService, args);
            this.transactionThreadBindings.commitTransaction();
            return result;
        }
        catch (Throwable t) {
            this.transactionThreadBindings.rollbackTransaction();
            throw t;
        }
    }
}
