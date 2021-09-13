package diarsid.librarian.tests.setup;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcOption;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.interaction.UserInteraction;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.api.required.UserProvidedResources;
import diarsid.librarian.api.required.impl.SceptreStringsComparisonAlgorithm;
import diarsid.librarian.impl.logic.impl.CoreImpl;
import diarsid.librarian.impl.model.RealUser;
import diarsid.librarian.tests.imports.DataImport;
import diarsid.sceptre.WeightAnalyzeReal;
import diarsid.sceptre.api.WeightAnalyze;
import diarsid.strings.similarity.api.Similarity;
import diarsid.support.configuration.Configuration;
import diarsid.support.objects.Pools;
import diarsid.tests.db.h2.H2TestDataBase;
import diarsid.tests.db.h2.SqlConnectionsSourceTestBase;
import diarsid.tests.db.h2.TestDataBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static diarsid.jdbc.api.JdbcOption.SQL_HISTORY_ENABLED;
import static diarsid.librarian.api.Core.Mode.DEVELOPMENT;
import static diarsid.support.configuration.Configuration.actualConfiguration;
import static diarsid.support.configuration.Configuration.configure;

public class CoreTestSetup {

    private static final Logger log = LoggerFactory.getLogger(CoreTestSetup.class);

    static {
        configure().withDefault(
                "log = true",
                "analyze.result.variants.limit = 100",
                "diarsid.strings.similarity.log.multiline = true",
                "diarsid.strings.similarity.log.multiline.prefix = [similarity]",
                "diarsid.strings.similarity.log.multiline.indent = 1",
                "diarsid.strings.similarity.log.base = false",
                "diarsid.strings.similarity.log.advanced = true",
                "analyze.weight.base.log = false",
                "analyze.weight.positions.search.log = true",
                "analyze.weight.positions.clusters.log = true",
//                "analyze.result.variants.limit = 11",
                "analyze.similarity.log.base = false",
                "analyze.similarity.log.advanced = true");
    }

    private static final boolean sqlHistoryEnabled = true;

    private static final String TEST_USER_NAME = "JUNIT_TEST_USER";

    public final CoreImpl core;
    public final RealUser user;
    public final Jdbc jdbc;
    public final TestDataBase dataBase;

    CoreTestSetup(H2TestDataBase.Type type, DataImport... dataImports) throws SQLException, IOException {
        H2TestDataBase dataBase = new H2TestDataBase(type, "search");
        this.dataBase = dataBase;

        SqlConnectionsSourceTestBase connections = new SqlConnectionsSourceTestBase(dataBase);
        Map<JdbcOption, Object> options = Map.of(SQL_HISTORY_ENABLED, sqlHistoryEnabled);
        jdbc = Jdbc.init(connections, options);

        Configuration configuration = actualConfiguration();
        Pools pools = new Pools();
        Similarity similarity = Similarity.createInstance(configuration);
        WeightAnalyze weightAnalyze = new WeightAnalyzeReal(configuration, similarity, pools);
        SceptreStringsComparisonAlgorithm algorithm = new SceptreStringsComparisonAlgorithm(weightAnalyze);

        UserProvidedResources impl = new UserProvidedResources() {

            @Override
            public StringsComparisonAlgorithm algorithm() {
                return algorithm;
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

        boolean isInitialized = false;
        try {
            testUser = (RealUser) core
                    .users()
                    .findBy(TEST_USER_NAME)
                    .orElseGet(() -> core.users().create(TEST_USER_NAME));
            isInitialized = true;
        }
        catch (Exception e) {
            String message = e.getMessage().toLowerCase();
            if ( message.contains("table") && message.contains("not found") ) {
                dataBase.executeScriptsIn(Paths.get("./src/main/resources/sql/h2"));
            }
            testUser = (RealUser) core.users().create(TEST_USER_NAME);
        }
        user = testUser;

        if ( ! isInitialized ) {
            jdbc.change(SQL_HISTORY_ENABLED, false);
            for ( DataImport dataImport : dataImports ) {
                dataImport.executeUsing(this);
            }
            jdbc.change(SQL_HISTORY_ENABLED, sqlHistoryEnabled);
        }
    }

}
