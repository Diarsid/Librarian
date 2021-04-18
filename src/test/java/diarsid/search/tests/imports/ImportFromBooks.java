package diarsid.search.tests.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import diarsid.search.api.Store;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.tests.CoreTestSetup;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;

import static diarsid.search.tests.CoreTestSetupStaticSingleton.server;

public class ImportFromBooks {

    public static void main(String[] args) throws Exception {
        executeUsing(server());
    }

    public static void executeUsing(CoreTestSetup coreTestSetup) throws Exception {
        Store store = coreTestSetup.core.store();
        User user = coreTestSetup.user;

        Entry.Label booksLabel = store.labels().getOrSave(user, "books");

        Map<String, Set<String>> booksAndAuthors = new HashMap<>();
        Map<String, List<String>> authorsAndBooks = new HashMap<>();

        Consumer<String> aggregateLineByAuthor = line -> {
            int authorsIndex = line.indexOf(", authors:");
            String authorsLine = line.substring(authorsIndex + ", authors:".length()).trim();
            String book = line.substring(0, authorsIndex);

            List<String> authors = stream(authorsLine.split(","))
                    .map(author -> author.strip())
                    .filter(author -> author.length() > 1)
                    .collect(Collectors.toList());

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

            store.labeledEntries().add(bookEntry, bookLabels);

            System.out.println("[IMPORT] " + booksCounter.getAndIncrement() + " " + book);
            bookLabels.stream().map(Entry.Label::name).forEach(label -> System.out.println("              " + label));
        });
    }
}
