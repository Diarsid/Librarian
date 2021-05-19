package diarsid.librarian.tests.imports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import diarsid.librarian.api.Store;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.CoreTestSetup;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.librarian.impl.logic.impl.StringTransformations.toSimplifiedWords;
import static diarsid.librarian.tests.CoreTestSetupStaticSingleton.server;

public class DataImportFromBooks implements DataImport {

    public static final List<String> SEMANTIC_LABEL_NAMES = List.of(
            "dune", "war", "army", "tolkien", "rome", "history", "science fiction");

    public static void main(String[] args) throws Exception {
        staticExecuteUsing(server(), "ANOTHER_USER_0");
    }

    public void executeUsing(CoreTestSetup coreTestSetup) throws SQLException, IOException {
        staticExecuteUsing(coreTestSetup);
    }

    public static void staticExecuteUsing(CoreTestSetup coreTestSetup, String... users) throws SQLException, IOException {
        Store store = coreTestSetup.core.store();

        if ( users.length == 0 ) {
            executeWith(store, coreTestSetup.user);
        }
        else {
            User user;
            for ( String userName : users ) {
                user = coreTestSetup.core.users().findBy(userName).orElseGet(() -> coreTestSetup.core.users().create(userName));
                executeWith(store, user);
            }
        }
    }

    private static void executeWith(Store store, User user) throws IOException {
        Entry.Label booksLabel = store.labels().getOrSave(user, "books");
        List<Entry.Label> semanticLabels = store.labels().getOrSave(user, SEMANTIC_LABEL_NAMES);
        Map<Entry.Label, List<String>> semanticLabelsAndWords = new HashMap<>();
        semanticLabels.forEach(label -> semanticLabelsAndWords.put(label, toSimplifiedWords(label.name(), CASE_TO_LOWER, true, false, true)));

        Map<String, Set<String>> booksAndAuthors = new HashMap<>();
        Map<String, List<String>> authorsAndBooks = new HashMap<>();

        Consumer<String> aggregateLineByAuthor = line -> {
            int authorsIndex = line.indexOf(", authors:");
            String authorsLine = line.substring(authorsIndex + ", authors:".length()).strip().trim();
            String book = line.substring(0, authorsIndex);

            List<String> authors = stream(authorsLine.split(","))
                    .map(author -> author.strip())
                    .filter(author -> author.length() > 1)
                    .collect(toList());

            Set<String> collectedAuthors = booksAndAuthors.get(book);

            if ( isNull(collectedAuthors) ) {
                collectedAuthors = new HashSet<>();
                booksAndAuthors.put(book, collectedAuthors);
                collectedAuthors.addAll(authors);
                List<String> authorBooks;
                for ( String author : authors ) {
                    authorBooks = authorsAndBooks.get(author);
                    if ( isNull(authorBooks) ) {
                        authorBooks = new ArrayList<>();
                        authorBooks.add(book);
                        authorsAndBooks.put(author, authorBooks);
                    }
                    else {
                        authorBooks.add(book);
                    }
                }
            }
            else {
                List<String> authorBooks;
                for ( String author : authors ) {
                    if ( ! collectedAuthors.contains(author) ) {
                        collectedAuthors.add(author);
                        authorBooks = authorsAndBooks.get(author);
                        if ( isNull(authorBooks) ) {
                            authorBooks = new ArrayList<>();
                            authorBooks.add(book);
                            authorsAndBooks.put(author, authorBooks);
                        }
                        else {
                            authorBooks.add(book);
                        }
                    }
                }
            }
        };

        Files.readAllLines(Paths.get("./src/test/resources/datasets/books"))
                .forEach(aggregateLineByAuthor);

        AtomicInteger booksCounter = new AtomicInteger();
        booksAndAuthors.forEach((book, bookAuthors) -> {
            List<Entry.Label> bookLabels = new ArrayList<>(store.labels().getOrSave(user, new ArrayList<>(bookAuthors)));
            bookLabels.add(booksLabel);

            Entry bookEntry = store.entries()
                    .findBy(user, book)
                    .orElseGet(() -> store.entries().save(user, book));

            List<Entry.Label> applicableSemanticLabels = semanticLabels
                    .stream()
                    .filter(label -> {
                        List<String> entryWords = toSimplifiedWords(bookEntry.string(), CASE_TO_LOWER, true, false, true);
                        List<String> labelWords = semanticLabelsAndWords.get(label);
                        return entryWords.containsAll(labelWords);
                    })
                    .collect(toList());

            bookLabels.addAll(applicableSemanticLabels);

            store.labeledEntries().add(bookEntry, bookLabels);

            System.out.println("[IMPORT] " + booksCounter.getAndIncrement() + " " + book);
            bookLabels.stream().map(Entry.Label::name).forEach(label -> System.out.println("              " + label));
        });
    }
}
