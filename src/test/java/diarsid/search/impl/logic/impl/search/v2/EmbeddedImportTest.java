package diarsid.search.impl.logic.impl.search.v2;

import diarsid.search.tests.CoreTestSetupStaticSingleton;
import diarsid.search.tests.imports.ImportFromBooks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EmbeddedImportTest {

    @BeforeAll
    public static void setUp() throws Exception {
        CoreTestSetupStaticSingleton.embedded();
//        ImportFromBooks.executeUsing(CoreTestSetupStaticSingleton.embedded());
    }

    @Test
    public void test() {

    }
}
