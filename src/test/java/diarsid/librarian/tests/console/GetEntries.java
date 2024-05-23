package diarsid.librarian.tests.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.Words;
import diarsid.librarian.api.WordsInEntries;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

public class GetEntries extends LibrarianConsoleOperationLogic {

    private final Words words;
    private final WordsInEntries wordsInEntries;
    private final Command.Flag wordFlag;

    public GetEntries(
            Core core,
            User user,
            Jdbc jdbc,
            Command.Flag wordFlag,
            Words words,
            WordsInEntries wordsInEntries) {
        super(core, user, jdbc);
        this.words = words;
        this.wordsInEntries = wordsInEntries;
        this.wordFlag = wordFlag;
    }

    @Override
    public List<String> execute(ConsoleInteraction interaction, Command command) {
        List<String> wordStrings = command.valuesOf(wordFlag);

        List<Entry.Word> words;
        if ( wordStrings.isEmpty() ) {
            words = collectWords(interaction);
        }
        else {
            words = findWords(interaction, wordStrings);
        }

        List<Entry> entries = wordsInEntries.findEntriesBy(words);

        List<String> strings = entries
                .stream()
                .map(entry -> format("%s %s", entry.uuid(), entry.string()))
                .collect(toList());

        return strings;
    }

    private List<Entry.Word> findWords(ConsoleInteraction interaction, List<String> wordStrings) {
        List<String> notFoundWords = new ArrayList<>();
        Map<String, Optional<Entry.Word>> wordsByStrings = words.findAllBy(user, wordStrings);

        List<Entry.Word> words = wordsByStrings
                .entrySet()
                .stream()
                .map(stringAndWord -> {
                    Optional<Entry.Word> word = stringAndWord.getValue();
                    if ( word.isEmpty() ) {
                        notFoundWords.add(stringAndWord.getKey());
                        return null;
                    }
                    else {
                        return word.get();
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());

        if ( notFoundWords.size() > 0 ) {
            interaction.println("    words not found: " + join(", ", notFoundWords));
        }

        return words;
    }

    private List<Entry.Word> collectWords(ConsoleInteraction interaction) {
        String wordString;
        Optional<Entry.Word> word;
        List<Entry.Word> collectedWords = new ArrayList<>();

        while ( true ) {
            interaction.print("    word: ");
            wordString = interaction
                    .readLine()
                    .orElse(null);

            if ( isNull(wordString) || wordString.isBlank() || wordString.isEmpty() ) {
                continue;
            }

            if ( wordString.equals(".") ) {
                break;
            }

            word = words.findBy(user, wordString);

            if ( word.isEmpty() ) {
                interaction.print("    no such word found!");
                continue;
            }

            collectedWords.add(word.get());
        }

        return collectedWords;
    }
}
