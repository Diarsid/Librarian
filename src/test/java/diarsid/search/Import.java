package diarsid.search;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Import {

    public static void main(String[] args) throws Exception {
        TestCoreSetup testCoreSetup = TestCoreSetup.INSTANCE;
        Files.readAllLines(Paths.get("./src/test/resources/entries-import"))
                .forEach(line -> {
                    testCoreSetup.core.store().entries().save(testCoreSetup.user, line);
                    System.out.println("imported " + line);
                });
    }
}
