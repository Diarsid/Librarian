package diarsid.librarian.tests.console;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.impl.WordsImpl;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.Word;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

public class GetEntriesOfWord extends LibrarianConsoleOperationLogic {

    private final WordsImpl words;

    public GetEntriesOfWord(Core core, User user, Jdbc jdbc) {
        super(core, user, jdbc);
        this.words = new WordsImpl(jdbc, UUID::randomUUID);
    }

    @Override
    public List<String> execute(ConsoleInteraction interaction, Command command) {
        String arg;
        if ( command.args().size() > 1 ) {
            arg = command.args().get(1);
        }
        else {
            interaction.print("    word: ");
            arg = interaction
                    .readLine()
                    .orElse(null);

            if ( isNull(arg) || arg.isBlank() || arg.isEmpty() ) {
                return asList("no arguments");
            }
        }

        Optional<Word> foundWord = jdbc.doInTransaction((tx) -> {
            return words.findBy(user.uuid(), arg);
        });

        if ( foundWord.isEmpty() ) {
            return asList("no such word");
        }

        Word word = foundWord.orElseThrow();

        List<String> strings = jdbc
                .doQueryAndStream(
                        row -> new RealEntry(row, now()),
                        "SELECT e.* \n" +
                        "FROM words_in_entries we \n" +
                        "    JOIN words w \n" +
                        "        ON w.uuid = we.word_uuid \n" +
                        "    JOIN entries e \n" +
                        "        ON e.uuid = we.entry_uuid \n" +
                        "WHERE w.uuid = ?",
                        word.uuid())
                .map(entry -> format("%s %s", entry.uuid(), entry.string()))
                .collect(toList());

        return strings;
    }
}
