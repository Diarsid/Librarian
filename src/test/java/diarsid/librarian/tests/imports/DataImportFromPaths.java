package diarsid.librarian.tests.imports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static diarsid.librarian.api.model.Entry.Label.ConditionBindable.ENTRY_CONTAINS_LABEL_IGNORE_CASE;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class DataImportFromPaths implements DataImport {

    public static void main(String[] args) throws Exception {
        staticExecuteUsing(server());
    }

    public static void staticExecuteUsing(CoreTestSetup coreTestSetup) throws SQLException, IOException {
        Core core = coreTestSetup.core;
        User user = coreTestSetup.user;

        Entry.Label gameLabel = getLabel(core, user, "games");
        Entry.Label toolsLabel = getLabel(core, user, "tools");
        Entry.Label devLabel = getLabel(core, user, "dev");
        Entry.Label serversLabel = getLabel(core, user, "servers");
        Entry.Label mavenLabel = getLabel(core, user, "maven");
        Entry.Label booksLabel = core.store().labels().getOrSave(user, "books");
        Entry.Label tolkienLabel = getLabel(core, user, "tolkien");

        List<Entry.Label> labels = new ArrayList<>();

        Consumer<String> saveLineAsEntry = line -> {
            Entry entry = core
                    .store()
                    .entries()
                    .findBy(user, line)
                    .orElseGet(() -> core
                            .store()
                            .entries()
                            .save(user, line));

            List<Entry.Labeled> labeled = core.store().labeledEntries().add(entry, labels);
        };

        AtomicInteger pathsCounter = new AtomicInteger();
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

                    System.out.println("[IMPORT] " + pathsCounter.getAndIncrement() + " " + line);
                    labels.stream().map(Entry.Label::name).forEach(label -> System.out.println("              " + label));

                    labels.clear();
                });
    }

    private static Entry.Label getLabel(Core core, User user, String name) {
        return core.store().labels().getOrSave(user, name).bindableIf(ENTRY_CONTAINS_LABEL_IGNORE_CASE);
    }

    @Override
    public void executeUsing(CoreTestSetup setup) throws SQLException, IOException {
        staticExecuteUsing(setup);
    }
}
