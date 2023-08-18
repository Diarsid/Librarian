package diarsid.librarian.tests.console;

import java.util.List;
import java.util.UUID;

import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

public class GetWordsOfEntry extends LibrarianConsoleOperationLogic {

    public GetWordsOfEntry(Core core, User user, Jdbc jdbc) {
        super(core, user, jdbc);
    }

    @Override
    public List<String> execute(ConsoleInteraction interaction, Command command) {
        String arg;
        if ( command.args().size() > 1 ) {
            arg = command.args().get(1);
        }
        else {
            interaction.print("    uuid or entry: ");
            arg = interaction
                    .readLine()
                    .orElse(null);

            if ( isNull(arg) || arg.isBlank() || arg.isEmpty() ) {
                return asList("no arguments");
            }
        }

        UUID uuid = null;
        try {
            uuid = UUID.fromString(arg);
        }
        catch (IllegalArgumentException e) {
            // not a UUID
        }

        Entry entry;
        if ( isNull(uuid) ) {
            entry = core.store().entries().findBy(user, arg).orElse(null);
        }
        else {
            entry = core.store().entries().getBy(user, uuid);
        }

        if ( isNull(entry) ) {
            return asList("no entries found");
        }

        List<String> words = jdbc
                .doQueryAndStream(
                        ColumnGetter.stringOf("string"),
                        "SELECT w.string \n" +
                        "FROM words_in_entries we \n" +
                        "    JOIN words w \n" +
                        "        ON w.uuid = we.word_uuid \n" +
                        "WHERE we.entry_uuid = ?",
                        entry.uuid())
                .collect(toList());

        return words;
    }
}
