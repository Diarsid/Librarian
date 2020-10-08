package diarsid.search;

import java.nio.file.Files;
import java.nio.file.Paths;

import diarsid.search.api.Core;

public class Import {

    public static void main(String[] args) throws Exception {
        CoreSetup coreSetup = new CoreSetup();
        Files
                .readAllLines(Paths.get("./src/test/resources/entries-import"))
                .forEach(line -> {
                    coreSetup.core.store().save(coreSetup.user, line);
                    System.out.println("imported " + line);
                });
    }
}
