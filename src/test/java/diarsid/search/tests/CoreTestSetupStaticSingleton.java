package diarsid.search.tests;

import java.nio.file.Paths;

import static java.util.Objects.isNull;

import static diarsid.tests.db.embedded.h2.H2TestDataBase.Type.EMBEDDED;
import static diarsid.tests.db.embedded.h2.H2TestDataBase.Type.REMOTE;

public class CoreTestSetupStaticSingleton {

    private static final Object remoteLock = new Object();
    private static final Object embeddedLock = new Object();

    private static CoreTestSetup remote;
    private static CoreTestSetup embedded;

    public static CoreTestSetup remote() {
        synchronized ( remoteLock ) {
            if ( isNull(remote) ) {
                remote = new CoreTestSetup(REMOTE);
            }
            return remote;
        }
    }

    public static CoreTestSetup embedded() {
        synchronized ( embeddedLock ) {
            if ( isNull(embedded) ) {
                try {
                    embedded = new CoreTestSetup(EMBEDDED, Paths.get("./src/main/resources/sql/CREATE_ALL.sql"));
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return embedded;
        }
    }
}
