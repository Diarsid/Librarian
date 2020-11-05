package diarsid.search;

import diarsid.jdbc.JdbcFactory;
import diarsid.jdbc.JdbcFactoryBuilder;
import diarsid.search.api.Core;
import diarsid.search.api.interaction.UserInteraction;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.model.RealUser;
import diarsid.tests.db.embedded.h2.H2TestDataBase;
import diarsid.tests.db.embedded.h2.SqlConnectionsSourceTestBase;

import static diarsid.support.configuration.Configuration.configure;

public class CoreSetup {

    static {
        configure().withDefault();
    }

    public static final CoreSetup INSTANCE = new CoreSetup();
    public static final String TEST_USER_NAME = "JUNIT_TEST_USER";

    public final CoreImpl core;
    public final RealUser user;
    public final JdbcFactory transactionFactory;

    private CoreSetup() {
        H2TestDataBase dataBase = new H2TestDataBase("search");

        SqlConnectionsSourceTestBase connections = new SqlConnectionsSourceTestBase(dataBase);
        transactionFactory = JdbcFactoryBuilder
                .buildTransactionFactoryWith(connections)
                .done();

        UserProvidedResources impl = new UserProvidedResources() {
            @Override
            public StringsComparisonAlgorithm algorithm() {
                return null;
            }

            @Override
            public UserInteraction userInteraction() {
                return null;
            }

            @Override
            public JdbcFactory jdbcFactory() {
                return transactionFactory;
            }
        };

        core = (CoreImpl) Core.buildWith(impl);
        user = (RealUser) core
                .users()
                .findBy(TEST_USER_NAME)
                .orElseGet(() -> core.users().create(TEST_USER_NAME));
    }
}
