/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package diarsid.tests.db.embedded.h2;

import java.sql.Connection;
import java.sql.SQLException;

import diarsid.jdbc.api.SqlConnectionsSource;

/**
 *
 * @author Diarsid
 */
public class SqlConnectionsSourceTestBase implements SqlConnectionsSource {
    
    private final TestDataBase embeddedBase;
    
    public SqlConnectionsSourceTestBase(TestDataBase testDataBase) {
        this.embeddedBase = testDataBase;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return this.embeddedBase.getConnection();
    }

    @Override
    public void close() {
        this.embeddedBase.close();
    }

    public int totalConnectionsQuantity() {
        return this.embeddedBase.getConnectionsQuantity();
    }
}
