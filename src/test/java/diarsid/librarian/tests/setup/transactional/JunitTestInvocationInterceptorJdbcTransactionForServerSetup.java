package diarsid.librarian.tests.setup.transactional;

import java.lang.reflect.Method;

import diarsid.librarian.tests.setup.CoreTestSetup;
import diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public class JunitTestInvocationInterceptorJdbcTransactionForServerSetup implements InvocationInterceptor {

    private final CoreTestSetup serverSetup = CoreTestSetupStaticSingleton.server();

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        serverSetup.jdbc.doInTransaction(transaction -> {
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
