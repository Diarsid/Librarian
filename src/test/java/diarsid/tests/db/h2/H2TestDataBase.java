package diarsid.tests.db.h2;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import diarsid.support.objects.CommonEnum;
import diarsid.support.strings.MultilineMessage;
import diarsid.support.strings.PathUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import static diarsid.support.lang.Booleans.isNot;
import static diarsid.support.strings.PathUtils.pathIsDirectory;

public class H2TestDataBase implements TestDataBase {

    public enum Type implements CommonEnum<Type> {

        INMEMORY("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1"),
        SERVER_EMBEDDED("jdbc:h2:tcp://localhost:" + TcpServer.PORT + "/%s;DB_CLOSE_DELAY=-1");

        private final String urlTemplate;

        Type(String urlTemplate) {
            this.urlTemplate = urlTemplate;
        }

        public String urlTo(String databaseName) {
            return format(this.urlTemplate, databaseName);
        }
    }

    public static class TcpServer {

        public static final String PORT = "53487";

        public static class Start {

            public static void main(String... args) throws SQLException {
                Server server = Server.createTcpServer("-tcpPort", PORT, "-tcpPassword", "admin", "-tcpAllowOthers", "-ifNotExists");
                server.start();
                logger.info("H2 server started at " + PORT);
            }
        }

        public static class Shutdown {

            public static void main(String... args) throws SQLException {
                Server.shutdownTcpServer("tcp://localhost:" + PORT, "admin", true, true);
                logger.info("H2 server shutdown.");
            }
        }

        public static void tryStart() throws SQLException {
            try {
                TcpServer.Start.main();
            }
            catch (SQLException e) {
                Throwable t = e.getCause();
                if ( isNot(t instanceof BindException) ) {
                    throw e;
                }
                else {
                    logger.info("H2 server is already started.");
                }
            }
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(H2TestDataBase.class);

    private static final int POOL_SIZE = 5;
    
    private final JdbcConnectionPool connectionsPool;
    
    public H2TestDataBase(Type type, String name) throws SQLException {
        String dataBaseUrl;
        switch ( type ) {
            case INMEMORY: {
                dataBaseUrl = type.urlTo(name);
                break;
            }
            case SERVER_EMBEDDED: {
                String databasePath = Paths
                        .get(".")
                        .toAbsolutePath()
                        .resolve("src/test/resources/database/h2")
                        .resolve(name)
                        .normalize()
                        .toString();
                TcpServer.tryStart();
                dataBaseUrl = type.urlTo(databasePath);
                break;
            }
            default: throw type.unsupported();
        }
        this.connectionsPool = JdbcConnectionPool.create(dataBaseUrl, "", "");
        this.connectionsPool.setMaxConnections(POOL_SIZE);
        logger.info(format("H2 test based established with URL: %s", dataBaseUrl));
    }    
    
    @Override
    public Connection getConnection() throws SQLException {        
        return this.connectionsPool.getConnection();
    }
        
    @Override
    public void setupRequiredTable(String tableCreationSQLScript) {
        try (Connection con = this.getConnection();
             Statement st = con.createStatement();) {
            st.executeUpdate(tableCreationSQLScript);
            logger.info(format("Test table setup with SQL: %s", tableCreationSQLScript));
        } catch (SQLException e) {
            logger.error("creation required table:");
            logger.error(tableCreationSQLScript);
            logger.error("", e);
            throw new RuntimeException();
        }
    }

    private void executeScript(Reader reader) throws SQLException {
        try (Connection con = this.getConnection()) {
            RunScript.execute(con, reader);
        }
    }

    @Override
    public void executeScript(Path script) throws SQLException, IOException {
        if ( pathIsDirectory(script) ) {
            throw new IllegalArgumentException(script.toString() + " is directory!");
        }
        this.executeScript(new FileReader(script.toFile()));
        MultilineMessage importing = new MultilineMessage("[SCRIPT] ");
        Files.readAllLines(script).forEach(line -> importing.add(line).newLine());
        logger.info(importing.compose());
    }

    @Override
    public void executeScriptsIn(Path scriptsFolder) throws SQLException, IOException {
        if ( ! pathIsDirectory(scriptsFolder) ) {
            throw new IllegalArgumentException(scriptsFolder.toString() + " is not a directory!");
        }
        try ( Stream<Path> paths = Files.list(scriptsFolder) ) {
            List<Path> scripts = paths
                    .filter(Predicate.not(PathUtils::pathIsDirectory))
                    .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".sql"))
                    .collect(toList());

            MultilineMessage importing = new MultilineMessage("[SCRIPT] ");
            for ( Path script : scripts ) {
                this.executeScript(new FileReader(script.toFile()));
                importing.newLine();
                importing.add(script.getFileName().toString()).newLine();
                Files.readAllLines(script).forEach(line -> importing.add(line).newLine());
            }
            logger.info(importing.compose());
        }
    }

    @Override
    public int countRowsInTable(String tableName) {
        try (Connection con = this.connectionsPool.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + tableName)) {
            int rows = 0;
            while ( rs.next() ) {
                rows++;
            }
            return rows;
        } catch (SQLException e) {
            logger.error("Exception during counting rows in table: " + tableName);
            logger.error("", e);
            throw new RuntimeException();
        }
    }
    
    @Override
    public boolean ifAllConnectionsReleased() {
        return ( this.connectionsPool.getActiveConnections() == 0 );
    }

    @Override
    public int getConnectionsQuantity() {
        return POOL_SIZE;
    }

    @Override
    public void close() {
        this.connectionsPool.dispose();
        try {
            TcpServer.Shutdown.main();
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
