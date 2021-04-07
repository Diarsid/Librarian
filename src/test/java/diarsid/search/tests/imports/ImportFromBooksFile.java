package diarsid.search.tests.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import diarsid.search.tests.CoreTestSetup;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;

import static diarsid.tests.db.embedded.h2.H2TestDataBase.Type.REMOTE;

public class ImportFromBooksFile {

    private static CoreTestSetup coreTestSetup = new CoreTestSetup(REMOTE);
    private static Core core = coreTestSetup.core;
    private static User user = coreTestSetup.user;

    public static void main(String[] args) throws Exception {
        Entry.Label booksLabel = core.store().labels().getOrSave(user, "books");

        Map<String, List<String>> booksAndAuthors = new HashMap<>();
        Map<String, List<String>> authorsAndBooks = new HashMap<>();

        Consumer<String> aggregateLineByAuthor = line -> {
            int authorsIndex = line.indexOf(", authors:");
            String authorsLine = line.substring(authorsIndex + ", authors:".length()).trim();
            String book = line.substring(0, authorsIndex);

            List<String> authors = stream(authorsLine.split(","))
                    .map(author -> author.strip())
                    .filter(author -> author.length() > 1)
                    .collect(Collectors.toList());

            List<String> collectedAuthors = booksAndAuthors.get(book);

            if ( isNull(collectedAuthors) ) {
                collectedAuthors = new ArrayList<>();
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

        Files.readAllLines(Paths.get("./src/test/resources/book-names-2"))
                .forEach(aggregateLineByAuthor);

        int authorsCount = booksAndAuthors
                .entrySet()
                .stream()
                .mapToInt(entry -> entry.getValue().size())
                .sum();

        int a = 5;

//        booksByAuthor.forEach((author, books) -> {
//            Entry.Label authorLabel = core.store().labels().getOrSave(user, author);
//            books.forEach(book -> {
//                try {
//                    core.store().entries().save(user, book, booksLabel, authorLabel);
//                    System.out.println("imported [" + counter.get() + "]" + book);
//                }
//                catch (IllegalArgumentException e) {
//                    System.out.println("existing [" + counter.get() + "]" + book);
//                }
//                counter.incrementAndGet();
//            });
//        });
    }


}
