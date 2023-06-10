package diarsid.librarian.tests.setup.transactional;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import diarsid.jdbc.api.Jdbc;

public class TransactionalRollback implements InvocationInterceptor {

    private final Jdbc jdbc;

    public TransactionalRollback(Jdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) {
        jdbc.doInTransaction(transaction -> {
            try {
                invocation.proceed();
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
            finally {
                transaction.rollbackAndProceed();
            }
        });
    }
}
