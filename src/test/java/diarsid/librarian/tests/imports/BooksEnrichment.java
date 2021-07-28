package diarsid.librarian.tests.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import diarsid.librarian.api.Store;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.librarian.impl.logic.impl.StringTransformations.toSimplifiedWords;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;
import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;

public class BooksEnrichment {

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

        Set<String> authors = authorsAndBooks.keySet();
        Map<String, List<String>> authorsWords = new HashMap<>();
        Map<String, Set<String>> wordsToAuthors = new HashMap<>();
        authors.forEach(author -> {
            List<String> words = toSimplifiedWords(author, CASE_TO_LOWER, true, true, true);
            authorsWords.put(author, words);
            words.forEach(word -> {
                Set<String> authorsByWord = wordsToAuthors.get(word);
                if ( isNull(authorsByWord) ) {
                    authorsByWord = new HashSet<>();
                    wordsToAuthors.put(word, authorsByWord);
                }
                authorsByWord.add(author);
            });
        });

        Map<String, Set<String>> authorsComplement = new HashMap<>();

        BiPredicate<String, String> authorsLikeness = (author, someAuthor) -> {
            if ( author.equals(someAuthor) ) {
                return false;
            }

            String authorLower = author.toLowerCase();
            String someAuthorLower = someAuthor.toLowerCase();

            if ( authorLower.equals(someAuthorLower) ) {
                return false;
            }

            var wordsInAuthor = toSimplifiedWords(author, CASE_TO_LOWER, true, true, true);
            var wordsInSomeAuthor = toSimplifiedWords(someAuthor, CASE_TO_LOWER, true, true, true);

            boolean matchButDifferent =
                    wordsInAuthor.containsAll(wordsInSomeAuthor) ^
                    wordsInSomeAuthor.containsAll(wordsInAuthor);

            return matchButDifferent;
        };

        int count = 0;
        List<String> relatedAuthors;
        for ( String author : authors ) {
            List<String> words = authorsWords.get(author);
            relatedAuthors = new ArrayList<>();
            for ( String word : words ) {
                relatedAuthors.addAll(wordsToAuthors.get(word));
            }

            Set<String> complementary = authorsComplement.get(author);

            if ( isNull(complementary) ) {
                complementary = relatedAuthors
                        .stream()
                        .filter(someAuthor -> authorsLikeness.test(author, someAuthor))
                        .collect(Collectors.toSet());

                if ( isNotEmpty(complementary) ) {
                    System.out.println("" + count + " " + author + " == " + complementary);
                }

                authorsComplement.put(author, complementary);
                complementary.forEach(complementaryAuthor -> {
                    Set<String> authorsOfComplement = authorsComplement.get(complementaryAuthor);
                    if ( isNull(authorsOfComplement) ) {
                        Set<String> set = new HashSet<>();
                        set.add(author);
                        authorsComplement.put(complementaryAuthor, set);
                    }
                    else {
                        authorsOfComplement.add(author);
                    }
                });
            }
            count++;
        }

        Map<String, Set<String>> authorsComplementRefined = new HashMap<>();

        authorsComplement
                .entrySet()
                .stream()
                .filter(entry -> isNotEmpty(entry.getValue()))
                .forEach(entry -> authorsComplementRefined.put(entry.getKey(), entry.getValue()));

        int a = 5;

        AtomicInteger addings = new AtomicInteger(0);
        booksAndAuthors.forEach((book, bookAuthors) -> {
            Set<String> additionalAuthors = new HashSet<>();
            for ( String author : bookAuthors ) {
                Set<String> complement = authorsComplementRefined.get(author);
                if ( nonNull(complement) ) {
                    additionalAuthors.addAll(complement);
                    addings.addAndGet(complement.size());
                }
            }
            if ( isNotEmpty(additionalAuthors) ) {
                bookAuthors.addAll(additionalAuthors);
                System.out.println("enrich: '" + book + "' with " + additionalAuthors.stream().collect(joining(", ")));
            }

        });

        List<String> lines = booksAndAuthors
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + ", authors: " + entry.getValue().stream().collect(joining(", ")))
                .collect(Collectors.toList());

        Files.write(Paths.get("./src/test/resources/datasets/books"), lines);

//        booksAndAuthors.forEach((book, bookAuthors) -> {
//            List<Entry.Label> authorLabels = store.labels().getOrSave(user, new ArrayList<>(bookAuthors));
//            Entry bookEntry = store.entries()
//                    .findBy(user, book)
//                    .orElseGet(() -> store.entries().save(user, book));
//            store.labeledEntries().add(bookEntry, authorLabels);
//            System.out.println("[IMPORT] " + book + "   --   " + bookAuthors.stream().collect(joining(", ")));
//        });
    }

}
