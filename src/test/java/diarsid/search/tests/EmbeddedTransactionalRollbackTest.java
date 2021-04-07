package diarsid.search.tests;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.Core;
import diarsid.search.api.model.User;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JunitTestInvocationJdbcTransactionInterceptor.class)
public class EmbeddedTransactionalRollbackTest {

    public final static Core CORE = CoreTestSetupStaticSingleton.embedded().core;
    public final static User USER = CoreTestSetupStaticSingleton.embedded().user;
    public final static Jdbc JDBC = CoreTestSetupStaticSingleton.embedded().jdbc;

}
