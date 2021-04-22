package diarsid.librarian.tests;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.User;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JunitTestInvocationInterceptorJdbcTransactionForServerSetup.class)
public class TransactionalRollbackTestForServerSetup {

    public final static CoreTestSetup CORE_TEST_SETUP = CoreTestSetupStaticSingleton.server();
    public final static Core CORE = CORE_TEST_SETUP.core;
    public final static User USER = CORE_TEST_SETUP.user;
    public final static Jdbc JDBC = CORE_TEST_SETUP.jdbc;

}
