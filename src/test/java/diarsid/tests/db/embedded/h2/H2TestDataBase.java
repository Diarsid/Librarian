/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package diarsid.tests.db.embedded.h2;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import diarsid.support.objects.CommonEnum;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;


/**
 *
 * @author Diarsid
 */
public class H2TestDataBase implements TestDataBase {

    public enum Type implements CommonEnum<Type> {
        EMBEDDED, REMOTE
    }
    
    private static final Logger logger = LoggerFactory.getLogger(H2TestDataBase.class);
    private static final String H2_REMOTE_TEST_BASE_URL_TEMPLATE =
            "jdbc:h2:tcp://localhost:9092/~/databases/%s;DB_CLOSE_DELAY=-1";
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
    
    private final JdbcConnectionPool conPool;
    private final String dataBaseName = "H2_server_test_base";
    
    public H2TestDataBase(Type type, String name) {
        String dataBaseUrl;
        switch ( type ) {
            case EMBEDDED: dataBaseUrl = format(H2_EMBEDDED_TEST_BASE_URL_TEMPLATE, name); break;
            case REMOTE: dataBaseUrl = format(H2_REMOTE_TEST_BASE_URL_TEMPLATE, name); break;
            default: throw type.unsupported();
        }
        this.conPool = JdbcConnectionPool.create(dataBaseUrl, "sa", "sa");
        this.conPool.setMaxConnections(POOL_SIZE);
        logger.info(format("H2 test based established with URL: %s", dataBaseUrl));
    }    
    
    @Override
    public Connection getConnection() throws SQLException {        
        return this.conPool.getConnection();
    }
        
    @Override
    public void setupRequiredTable(String tableCreationSQLScript) {
        try (Connection con = this.conPool.getConnection();
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
    public void executeScript(Path path) throws SQLException, FileNotFoundException {
        this.executeScript(new FileReader(path.toFile()));
    }
    
    @Override
    public int countRowsInTable(String tableName) {
        try (   Connection con = this.conPool.getConnection();
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
        return ( this.conPool.getActiveConnections() == 0 );
    }

    @Override
    public int getConnectionsQuantity() {
        return POOL_SIZE;
    }

    @Override
    public void close() {
        this.conPool.dispose();
    }
}
