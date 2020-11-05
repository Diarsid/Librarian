package diarsid.search;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Import {

    public static void main(String[] args) throws Exception {
        CoreSetup coreSetup = CoreSetup.INSTANCE;
        Files.readAllLines(Paths.get("./src/test/resources/entries-import"))
                .forEach(line -> {
                    coreSetup.core.store().entries().save(coreSetup.user, line);
                    System.out.println("imported " + line);
                });
    }
}
