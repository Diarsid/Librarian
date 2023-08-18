package diarsid.librarian.tests.console;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.console.api.Console;
import diarsid.console.api.format.ConsoleFormat;
import diarsid.console.api.io.Command;
import diarsid.console.api.io.operations.OperationLogic;
import diarsid.console.impl.building.ConsoleBuilding;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static java.util.Arrays.asList;
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

        Command.Flag.Validator uuidValidator = (s) -> {
            try {
                UUID.fromString(s);
            }
            catch (Throwable t) {
                throw new IllegalArgumentException("not a uuid!");
            }
        };

        Command.Flag uuidFlag = Command.Flag.withValidatedValues("uuid", "u", false, uuidValidator);
        Command.Flag patternFlag = Command.Flag.withAnyValues("pattern", "p", false);
        Command.Flag entryFlag = Command.Flag.withAnyValues("entry", "e", false);

        OperationLogic getWordsOfEntry = new GetWordsOfEntry(core, user, jdbc);
        OperationLogic describePatternAndEntry = new DescribePatternAndEntry(core, user, jdbc);

        Console console = new ConsoleBuilding()
                .withFormat(ConsoleFormat
                        .building()
                        .with(NAME, "librarian.describe"))
                .stopWhenInputIs("exit")
                .withFlags(
                        uuidFlag,
                        patternFlag,
                        entryFlag)
                .enableExitConfirmation("y", "+", "yes")
                .withOperation(building -> building
                        .named("get-words-of-entry")
                        .doing(getWordsOfEntry)
                        .matching(command -> command.firstArgIs("words")))
                .withOperation(building -> building
                        .named("describe-entry-and-pattern")
                        .doing(describePatternAndEntry)
                        .matching(command -> command.firstArgIs("describe")))
                .done();

        console.life().start();
    }
}
