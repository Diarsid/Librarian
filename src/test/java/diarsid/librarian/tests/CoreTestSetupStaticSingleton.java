package diarsid.librarian.tests;

import static java.util.Objects.isNull;

import static diarsid.tests.db.embedded.h2.H2TestDataBase.Type.EMBEDDED;
import static diarsid.tests.db.embedded.h2.H2TestDataBase.Type.SERVER;

public class CoreTestSetupStaticSingleton {

    private static final Object serverLock = new Object();
    private static final Object embeddedLock = new Object();

    private static CoreTestSetup server;
    private static CoreTestSetup embedded;

    public static CoreTestSetup server() {
        synchronized (serverLock) {
            if ( isNull(server) ) {
                try {
                    server = new CoreTestSetup(SERVER);
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
                    embedded = new CoreTestSetup(EMBEDDED);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return embedded;
        }
    }
}
