package diarsid.search.tests;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.sql.SQLException;
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
import diarsid.tests.db.embedded.h2.TestDataBase;

import static diarsid.jdbc.api.JdbcOption.SQL_HISTORY_ENABLED;
import static diarsid.search.api.Core.Mode.DEVELOPMENT;
import static diarsid.support.configuration.Configuration.configure;

public class CoreTestSetup {

    static {
        configure().withDefault(
                "log = true",
                "diarsid.strings.similarity.log.multiline = true",
                "diarsid.strings.similarity.log.multiline.prefix = [similarity]",
                "diarsid.strings.similarity.log.multiline.indent = 1",
                "diarsid.strings.similarity.log.base = true",
                "diarsid.strings.similarity.log.advanced = true");
    }

    private static final boolean sqlHistoryEnabled = true;

    private static final String TEST_USER_NAME = "JUNIT_TEST_USER";

    public final CoreImpl core;
    public final RealUser user;
    public final Jdbc jdbc;
    public final TestDataBase dataBase;

    public CoreTestSetup(H2TestDataBase.Type type) {
        H2TestDataBase dataBase = new H2TestDataBase(type, "search");
        this.dataBase = dataBase;

        SqlConnectionsSourceTestBase connections = new SqlConnectionsSourceTestBase(dataBase);
        Map<JdbcOption, Object> options = Map.of(SQL_HISTORY_ENABLED, sqlHistoryEnabled);
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
        core.setMode(DEVELOPMENT);
        user = (RealUser) core
                .users()
                .findBy(TEST_USER_NAME)
                .orElseGet(() -> core.users().create(TEST_USER_NAME));
    }

    public CoreTestSetup(H2TestDataBase.Type type, Path script) throws SQLException, FileNotFoundException {
        H2TestDataBase dataBase = new H2TestDataBase(type, "search");
        this.dataBase = dataBase;
        this.dataBase.executeScript(script);

        SqlConnectionsSourceTestBase connections = new SqlConnectionsSourceTestBase(dataBase);
        Map<JdbcOption, Object> options = Map.of(SQL_HISTORY_ENABLED, sqlHistoryEnabled);
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
        core.setMode(DEVELOPMENT);
        user = (RealUser) core
                .users()
                .findBy(TEST_USER_NAME)
                .orElseGet(() -> core.users().create(TEST_USER_NAME));
    }

}
