package diarsid.librarian.tests.console;

import java.util.List;
import java.util.UUID;

import diarsid.console.api.Console;
import diarsid.console.api.format.ConsoleFormat;
import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.console.api.io.operations.Operation;
import diarsid.console.api.io.operations.OperationLogic;
import diarsid.console.impl.building.ConsoleBuilding;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

import static diarsid.console.api.format.ConsoleFormatElement.NAME;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class DescriptonConsole {

    public static void main(String[] args) {
        CoreTestSetup setup = server();
        Core core = setup.core;
        User user = setup.user;
        Jdbc jdbc = setup.jdbc;

        OperationLogic getWordsOfEntry = (interaction, command) -> {
            String arg = command.args().get(1);
            UUID uuid = null;
            try {
                uuid = UUID.fromString(arg);
            }
            catch (IllegalArgumentException e) {
                // not a UUID
            }

            Entry entry;
            if ( isNull(uuid) ) {
                entry = core.store().entries().findBy(user, arg).orElseThrow();
            }
            else {
                entry = core.store().entries().getBy(user, uuid);
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
        };

        OperationLogic describePattern = (interaction, command) -> {
            return emptyList();
        };

        OperationLogic describePatternAndEntry = (interaction, command) -> {
            return emptyList();
        };

        Console console = new ConsoleBuilding()
                .withFormat(ConsoleFormat
                        .building()
                        .with(NAME, "librarian.describe"))
                .stopWhenInputIs("exit")
                .enableExitConfirmation("y", "+", "yes")
                .withOperation(building -> building
                        .named("get-words-of-entry")
                        .doing(getWordsOfEntry)
                        .matching(command -> command.firstArgIs("words") && command.hasArgs(2)))
                .done();

        console.life().start();
    }
}
