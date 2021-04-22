package diarsid.librarian.impl.logic.impl.search.v2;

import diarsid.librarian.tests.CoreTestSetupStaticSingleton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EmbeddedDataImportTest {

    @BeforeAll
    public static void setUp() throws Exception {
        CoreTestSetupStaticSingleton.embedded();
//        ImportFromBooks.executeUsing(CoreTestSetupStaticSingleton.embedded());
    }

    @Test
    public void test() {

    }
}
