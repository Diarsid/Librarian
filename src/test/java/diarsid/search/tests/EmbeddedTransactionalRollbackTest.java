package diarsid.search.tests;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.Core;
import diarsid.search.api.model.User;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JunitTestInvocationJdbcTransactionInterceptor.class)
public class EmbeddedTransactionalRollbackTest {

    public final static CoreTestSetup CORE_TEST_SETUP = CoreTestSetupStaticSingleton.embedded();
    public final static Core CORE = CORE_TEST_SETUP.core;
    public final static User USER = CORE_TEST_SETUP.user;
    public final static Jdbc JDBC = CORE_TEST_SETUP.jdbc;

}
