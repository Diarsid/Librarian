/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package diarsid.tests.db.embedded.h2;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import diarsid.support.objects.CommonEnum;
import diarsid.support.objects.references.Possible;
import diarsid.support.strings.MultilineMessage;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

import static diarsid.support.objects.references.References.simplePossibleButEmpty;
import static diarsid.support.objects.references.References.simplePossibleWith;
import static org.h2.tools.Server.createTcpServer;


/**
 *
 * @author Diarsid
 */
public class H2TestDataBase implements TestDataBase {

    public enum Type implements CommonEnum<Type> {
        EMBEDDED, SERVER
    }
    
    private static final Logger logger = LoggerFactory.getLogger(H2TestDataBase.class);

    private static final String H2_SERVER_TEST_BASE_URL_TEMPLATE =
            "jdbc:h2:tcp://localhost:9092/%s;DB_CLOSE_DELAY=-1";
    private static final String H2_EMBEDDED_TEST_BASE_URL_TEMPLATE =
            "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1";
    private static final int POOL_SIZE = 5;
    
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (Exception e) {     
            logger.error("", e);
        }
    }
    
    private final JdbcConnectionPool connectionsPool;
    private final Possible<Server> server;
    
    public H2TestDataBase(Type type, String name) throws SQLException {
        String dataBaseUrl;
        switch ( type ) {
            case EMBEDDED: {
                server = simplePossibleButEmpty();
                dataBaseUrl = format(H2_EMBEDDED_TEST_BASE_URL_TEMPLATE, name);
                break;
            }
            case SERVER: {
                String databasePath = Paths
                        .get(".")
                        .toAbsolutePath()
                        .resolve("src")
                        .resolve("test")
                        .resolve("resources")
                        .resolve("database")
                        .resolve(name)
                        .normalize()
                        .toString();
                server = simplePossibleWith(createTcpServer("-tcpPort", "9092", "-tcpAllowOthers", "-ifNotExists"));
                server.orThrow().start();
                dataBaseUrl = format(H2_SERVER_TEST_BASE_URL_TEMPLATE, databasePath);
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
        try (Connection con = this.connectionsPool.getConnection();
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

    public void executeScript(Reader reader) throws SQLException {
        RunScript.execute(this.getConnection(), reader);
    }

    @Override
    public void executeScript(Path script) throws SQLException, IOException {
        this.executeScript(new FileReader(script.toFile()));
        MultilineMessage importing = new MultilineMessage("[SCRIPT] ");
        Files.readAllLines(script).forEach(line -> importing.add(line).newLine());
        logger.info(importing.compose());
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
        this.server.ifPresent(tcpServer -> {
            tcpServer.stop();
            tcpServer.shutdown();
        });
    }
}
