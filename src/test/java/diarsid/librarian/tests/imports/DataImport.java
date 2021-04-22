package diarsid.librarian.tests.imports;

import java.io.IOException;
import java.sql.SQLException;

import diarsid.librarian.tests.CoreTestSetup;

public interface DataImport {

    void executeUsing(CoreTestSetup setup) throws SQLException, IOException;
}
