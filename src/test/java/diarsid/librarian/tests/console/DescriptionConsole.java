package diarsid.librarian.tests.console;

import java.util.UUID;

import diarsid.console.api.Console;
import diarsid.console.api.format.ConsoleFormat;
import diarsid.console.api.io.Command;
import diarsid.console.api.io.operations.OperationLogic;
import diarsid.console.impl.building.ConsoleBuilding;
import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static diarsid.console.api.format.ConsoleFormatElement.NAME;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class DescriptionConsole {

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
        Command.Flag wordFlag = Command.Flag.withAnyValues("word", "w", true);

        OperationLogic getWordsOfEntry = new GetWordsOfEntry(core, user, jdbc);
        OperationLogic describePatternAndEntry = new DescribePatternAndEntry(core, user, jdbc);
        OperationLogic getEntries = new GetEntries(core, user, jdbc, wordFlag, core.store().words(), core.store().wordsInEntries());

        Console console = new ConsoleBuilding()
                .withFormat(ConsoleFormat
                        .building()
                        .with(NAME, "librarian.describe"))
                .stopWhenInputIs("exit")
                .withFlags(
                        uuidFlag,
                        patternFlag,
                        entryFlag,
                        wordFlag)
                .enableExitConfirmation("y", "+", "yes")
                .withOperation(building -> building
                        .named("get-words-of-entry")
                        .doing(getWordsOfEntry)
                        .matching(command -> command.firstArgIs("words")))
                .withOperation(builder -> builder
                        .named("get-entries-of-word")
                        .doing(getEntries)
                        .matching(command -> command.firstArgIs("entries")))
                .withOperation(building -> building
                        .named("describe-entry-and-pattern")
                        .doing(describePatternAndEntry)
                        .matching(command ->
                                command.firstArgIs("describe") ||
                                command.firstArgIs("desc")))
                .done();

        console.life().start();
    }
}
