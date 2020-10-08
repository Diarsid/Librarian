package diarsid.search;

import diarsid.jdbc.JdbcTransactionFactory;
import diarsid.jdbc.JdbcTransactionFactoryBuilder;
import diarsid.search.api.Core;
import diarsid.search.api.interaction.UserInteraction;
import diarsid.search.api.model.User;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.tests.db.embedded.h2.H2TestDataBase;
import diarsid.tests.db.embedded.h2.JdbcConnectionsSourceTestBase;

import static java.util.UUID.randomUUID;

public class CoreSetup {

    Core core;
    User user;

    public CoreSetup() {
        H2TestDataBase dataBase = new H2TestDataBase("search");

        JdbcConnectionsSourceTestBase connections = new JdbcConnectionsSourceTestBase(dataBase);
        JdbcTransactionFactory transactionFactory = JdbcTransactionFactoryBuilder
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
            public JdbcTransactionFactory transactionFactory() {
                return transactionFactory;
            }
        };

        core = Core.buildWith(impl);
        String userName = randomUUID().toString();
        user = core.users().create(userName);
    }
}
