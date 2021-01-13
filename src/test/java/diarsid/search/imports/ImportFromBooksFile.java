package diarsid.search.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import diarsid.search.TestCoreSetup;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;

public class ImportFromBooksFile {

    private static Core core = TestCoreSetup.INSTANCE.core;
    private static User user = TestCoreSetup.INSTANCE.user;

    public static void main(String[] args) throws Exception {
        Entry.Label booksLabel = core.store().labels().getOrSave(user, "books");

        List<Entry.Label> labels = List.of(booksLabel);

        AtomicInteger counter = new AtomicInteger();

        Consumer<String> saveLineAsEntry = line -> {
            core.store().entries().save(user, line, labels);
            System.out.println("imported [" + counter.get() + "]" + line);
            counter.incrementAndGet();
        };

        Files.readAllLines(Paths.get("./src/test/resources/book-names"))
                .forEach(saveLineAsEntry);
    }
}
