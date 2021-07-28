package diarsid.tests.db.h2;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public interface TestDataBase {
    
    Connection getConnection() throws SQLException;

    void executeScript(Path path) throws SQLException, IOException;

    void executeScriptsIn(Path scriptsFolder) throws SQLException, IOException;
    
    int getConnectionsQuantity();
    
    void setupRequiredTable(String tableCreationSQLScript);
    
    int countRowsInTable(String tableName);
    
    boolean ifAllConnectionsReleased();

    void close();
}
