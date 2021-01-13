package diarsid.search;

import java.util.Map;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcOption;
import diarsid.search.api.Core;
import diarsid.search.api.interaction.UserInteraction;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.model.RealUser;
import diarsid.tests.db.embedded.h2.H2TestDataBase;
import diarsid.tests.db.embedded.h2.SqlConnectionsSourceTestBase;

import static diarsid.jdbc.api.JdbcOption.SQL_HISTORY_ENABLED;
import static diarsid.support.configuration.Configuration.configure;

public class TestCoreSetup {

    static {
        configure().withDefault(
                "log = true",
                "diarsid.strings.similarity.log.multiline = true",
                "diarsid.strings.similarity.log.multiline.prefix = [similarity]",
                "diarsid.strings.similarity.log.multiline.indent = 1",
                "diarsid.strings.similarity.log.base = true",
                "diarsid.strings.similarity.log.advanced = true");
    }

    private static final boolean sqlHistory = true;

    public static final TestCoreSetup INSTANCE = new TestCoreSetup(sqlHistory);
    public static final String TEST_USER_NAME = "JUNIT_TEST_USER";

    public final CoreImpl core;
    public final RealUser user;
    public final Jdbc jdbc;

    private TestCoreSetup(boolean enableSqlHistory) {
        H2TestDataBase dataBase = new H2TestDataBase("search");

        SqlConnectionsSourceTestBase connections = new SqlConnectionsSourceTestBase(dataBase);
        Map<JdbcOption, Object> options = Map.of(SQL_HISTORY_ENABLED, enableSqlHistory);
        jdbc = Jdbc.init(connections, options);

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
            public Jdbc jdbc() {
                return jdbc;
            }
        };

        core = (CoreImpl) Core.buildWith(impl);
        user = (RealUser) core
                .users()
                .findBy(TEST_USER_NAME)
                .orElseGet(() -> core.users().create(TEST_USER_NAME));
    }
}
