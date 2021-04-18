package diarsid.search.tests;

import java.io.IOException;
import java.nio.file.Paths;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static diarsid.jdbc.api.JdbcOption.SQL_HISTORY_ENABLED;
import static diarsid.search.api.Core.Mode.DEVELOPMENT;
import static diarsid.support.configuration.Configuration.configure;

public class CoreTestSetup {

    private static final Logger log = LoggerFactory.getLogger(CoreTestSetup.class);

    static {
        configure().withDefault(
                "log = true",
                "diarsid.strings.similarity.log.multiline = true",
                "diarsid.strings.similarity.log.multiline.prefix = [similarity]",
                "diarsid.strings.similarity.log.multiline.indent = 1",
                "diarsid.strings.similarity.log.base = true",
                "diarsid.strings.similarity.log.advanced = true");
    }

    private static final boolean sqlHistoryEnabled = false;

    private static final String TEST_USER_NAME = "JUNIT_TEST_USER";

    public final CoreImpl core;
    public final RealUser user;
    public final Jdbc jdbc;
    public final TestDataBase dataBase;

    CoreTestSetup(H2TestDataBase.Type type) throws SQLException, IOException {
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
        RealUser testUser;
        try {
            testUser = (RealUser) core
                    .users()
                    .findBy(TEST_USER_NAME)
                    .orElseGet(() -> core.users().create(TEST_USER_NAME));
        }
        catch (Exception e) {
            String message = e.getMessage().toLowerCase();
            if ( message.contains("table") && message.contains("not found")) {
                dataBase.executeScript(Paths.get("./src/main/resources/sql/CREATE_TABLES_INDEXES.sql"));
            }
            testUser = (RealUser) core.users().create(TEST_USER_NAME);
        }
        user = testUser;
    }

}
