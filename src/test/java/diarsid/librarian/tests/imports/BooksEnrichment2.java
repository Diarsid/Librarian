package diarsid.librarian.tests.imports;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.librarian.impl.logic.impl.StringTransformations.toSimplifiedWords;

public class BooksEnrichment2 {

    public static void main(String[] args) throws Exception {
        List<String> resultingBooks = new ArrayList<>();

        Consumer<String> aggregateLineByAuthor = line -> {
            int authorsIndex = line.indexOf(", authors: ");

            String bookLine = line.substring(0, authorsIndex);
            String authorsLine = line.substring(authorsIndex + ", authors: ".length()).trim();

            List<String> authors = asList(authorsLine.split(", "))
                    .stream()
                    .map(s -> s.strip())
                    .filter(s -> ! s.isEmpty())
                    .filter(s -> ! s.equalsIgnoreCase("unknown"))
                    .collect(Collectors.toList());

            if ( authors.size() > 1 ) {
                Map<String, List<String>> authorsWords = new HashMap<>();
                authors.forEach(author -> {
                    authorsWords.put(
                            author,
                            toSimplifiedWords(author, CASE_TO_LOWER, true, false, true, false));
                });

                List<String> redundantAuthors = new ArrayList<>();
                boolean isRedunant;
                for ( Map.Entry<String, List<String>> authorAndItsWords : authorsWords.entrySet() ) {
                    List<String> authorWords = authorAndItsWords.getValue();
                    isRedunant = authorsWords
                            .entrySet()
                            .stream()
                            .filter(otherAuthorAndWords -> ! (otherAuthorAndWords.getValue() == authorWords))
                            .anyMatch(otherAuthorWords -> otherAuthorWords.getValue().containsAll(authorWords));

                    if ( isRedunant ) {
                        redundantAuthors.add(authorAndItsWords.getKey());
                    }
                }

                authors.removeAll(redundantAuthors);
            }

            String result;
            int authorsCount = authors.size();
            if ( authorsCount == 0 ) {
                result = line;
            }
            else if ( authorsCount == 1 ) {
                result = bookLine + " by " + authors.get(0) + ", authors: " + authorsLine;
            }
            else if ( authorsCount == 2 ) {
                result = bookLine + " by " + authors.get(0) + " and " + authors.get(1) + ", authors: " + authorsLine;
            }
            else if ( authorsCount == 3 ) {
                result = bookLine + " by " + authors.get(0) + " and " + authors.get(1)+ ", and " + authors.get(2) + ", authors: " + authorsLine;
            }
            else {
                result = line;
            }

            resultingBooks.add(result);
        };

        Files.readAllLines(Paths.get("./src/test/resources/datasets/books"))
                .forEach(aggregateLineByAuthor);

        Files.write(Paths.get("./src/test/resources/datasets/books"), resultingBooks);
    }
}
