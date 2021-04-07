package diarsid.search.tests;

import java.lang.reflect.Method;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public class JunitTestInvocationJdbcTransactionInterceptor implements InvocationInterceptor {

    private final CoreTestSetup embeddedSetup = CoreTestSetupStaticSingleton.embedded();

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        embeddedSetup.jdbc.doInTransaction(transaction -> {
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
