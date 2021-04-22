package diarsid.librarian.tests;

import diarsid.librarian.tests.imports.DataImportFromBooks;
import diarsid.librarian.tests.imports.DataImportFromPaths;

import static java.util.Objects.isNull;

import static diarsid.tests.db.h2.H2TestDataBase.Type.INMEMORY;
import static diarsid.tests.db.h2.H2TestDataBase.Type.SERVER_EMBEDDED;

public class CoreTestSetupStaticSingleton {

    private static final Object serverLock = new Object();
    private static final Object embeddedLock = new Object();

    private static CoreTestSetup server;
    private static CoreTestSetup embedded;

    public static CoreTestSetup server() {
        synchronized (serverLock) {
            if ( isNull(server) ) {
                try {
                    server = new CoreTestSetup(SERVER_EMBEDDED, new DataImportFromBooks(), new DataImportFromPaths());
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return server;
        }
    }

    public static CoreTestSetup embedded() {
        synchronized ( embeddedLock ) {
            if ( isNull(embedded) ) {
                try {
                    embedded = new CoreTestSetup(INMEMORY);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return embedded;
        }
    }
}
