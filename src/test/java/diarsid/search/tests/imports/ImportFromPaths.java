package diarsid.search.tests.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.tests.CoreTestSetup;

import static java.util.stream.Collectors.joining;

import static diarsid.search.api.model.Entry.Label.ConditionBindable.ENTRY_CONTAINS_LABEL_IGNORE_CASE;
import static diarsid.search.tests.CoreTestSetupStaticSingleton.server;

public class ImportFromPaths {

    private static CoreTestSetup coreTestSetup = server();
    private static Core core = coreTestSetup.core;
    private static User user = coreTestSetup.user;

    public static void main(String[] args) throws Exception {
        Entry.Label gameLabel = getLabel("games");
        Entry.Label toolsLabel = getLabel("tools");
        Entry.Label devLabel = getLabel("dev");
        Entry.Label serversLabel = getLabel("servers");
        Entry.Label mavenLabel = getLabel("maven");
        Entry.Label booksLabel = core.store().labels().getOrSave(user, "books");
        Entry.Label tolkienLabel = getLabel("tolkien");

        List<Entry.Label> labels = new ArrayList<>();

        Consumer<String> saveLineAsEntry = line -> {
            Entry entry = core.store().entries().save(user, line);
            List<Entry.Labeled> labeled = core.store().labeledEntries().add(entry, labels);
        };

        Files.readAllLines(Paths.get("./src/test/resources/datasets/paths"))
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
