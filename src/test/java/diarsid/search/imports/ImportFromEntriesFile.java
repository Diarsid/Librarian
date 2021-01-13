package diarsid.search.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import diarsid.search.TestCoreSetup;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;

import static java.util.stream.Collectors.joining;

import static diarsid.search.api.model.Entry.Label.ConditionBindable.ENTRY_CONTAINS_LABEL_IGNORE_CASE;

public class ImportFromEntriesFile {

    private static Core core = TestCoreSetup.INSTANCE.core;
    private static User user = TestCoreSetup.INSTANCE.user;

    public static void main(String[] args) throws Exception {
        Entry.Label gameLabel = getLabel("games");
        Entry.Label toolsLabel = getLabel("tools");
        Entry.Label devLabel = getLabel("dev");
        Entry.Label serversLabel = getLabel("servers");
        Entry.Label mavenLabel = getLabel("maven");
        Entry.Label booksLabel = getLabel("books");
        Entry.Label tolkienLabel = getLabel("tolkien");

        List<Entry.Label> labels = new ArrayList<>();

        Consumer<String> saveLineAsEntry = line -> {
            core.store().entries().save(user, line, labels);
        };

        Files.readAllLines(Paths.get("./src/test/resources/entries"))
                .forEach(line -> {
                    String lineLower = line.toLowerCase();

                    if ( lineLower.endsWith("games") ) {
                        labels.add(gameLabel);
                    }
                    if ( lineLower.endsWith("tolkien") ) {
                        labels.add(tolkienLabel);
                        labels.add(booksLabel);
                    }
                    if ( lineLower.contains("book") ) {
                        labels.add(booksLabel);
                    }
                    if ( lineLower.contains("dev") ) {
                        labels.add(devLabel);
                    }
                    if ( lineLower.contains("tools") ) {
                        labels.add(toolsLabel);
                    }
                    if ( lineLower.contains("servers") ) {
                        labels.add(serversLabel);
                    }
                    if ( lineLower.contains("maven") ) {
                        labels.add(mavenLabel);
                    }

                    saveLineAsEntry.accept(line);

                    System.out.println("imported " + line + ",  labels: " + labels.stream().map(Entry.Label::name).collect(joining(", ")));

                    labels.clear();
                });
    }

    private static Entry.Label getLabel(String name) {
        return core.store().labels().getOrSave(user, name).bindableIf(ENTRY_CONTAINS_LABEL_IGNORE_CASE);
    }
}
