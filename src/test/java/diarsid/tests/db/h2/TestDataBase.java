/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package diarsid.tests.db.h2;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author Diarsid
 */
public interface TestDataBase {
    
    Connection getConnection() throws SQLException;

    void executeScript(Path path) throws SQLException, IOException;
    
    int getConnectionsQuantity();
    
    void setupRequiredTable(String tableCreationSQLScript);
    
    int countRowsInTable(String tableName);
    
    boolean ifAllConnectionsReleased();

    void close();
}
