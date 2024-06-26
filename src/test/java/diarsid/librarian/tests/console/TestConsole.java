package diarsid.librarian.tests.console;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import diarsid.console.api.Console;
import diarsid.console.api.format.ConsoleFormat;
import diarsid.console.api.io.Command;
import diarsid.console.api.io.operations.OperationLogic;
import diarsid.console.impl.building.ConsoleBuilding;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcTransaction;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.Search;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import static diarsid.console.api.format.ConsoleFormatElement.NAME;
import static diarsid.jdbc.api.JdbcTransaction.ThenDo.CLOSE;
import static diarsid.librarian.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;
import static diarsid.support.model.Joined.distinctLeftsOf;

public class TestConsole {

    public static void main(String[] args) {
        Command.Flag labelFlag = Command.Flag.withAnyValues("label", "l", true);
        Command.Flag singleFlag = Command.Flag.withNoValue("single", "s");
        Command.Flag commitFlag = Command.Flag.withNoValue("commit", "c");
        Command.Flag labelsModeFlag = Command.Flag.withAllowedValues("labelsmode", "lm", false, "any", "all", "none");
        CoreTestSetup setup = server();
        Core core = setup.core;
        User user = setup.user;
        Jdbc jdbc = setup.jdbc;

        Comparator<PatternToEntry> comparator = (o1, o2) -> {
            return setup.algorithm.compare(o1.weight(), o2.weight());
        };

        Search.Observer observer = new LoggingSearchObserver(comparator);

        OperationLogic findByMatching = (interaction, command) -> {
            JdbcTransaction tx = jdbc.createTransaction();
            try {
                String pattern = command.argAt(1);

                List<String> labelNames = command.valuesOf(labelFlag);
                String labelsMode = command.valueOf(labelsModeFlag).orElse("any");

                Entry.Label.Matching matching = matchingOfFlag(labelsMode);
                List<Entry.Label> labels = core.store().labels().getOrSave(user, labelNames);

                List<PatternToEntry> entries = core.search().findAllBy(user, pattern, matching, labels, observer);

                List<String> strings = new ArrayList<>();
                strings.add("pattern: " + pattern);

                var entriesStrings = entries
                        .stream()
                        .map(pair -> format("%s : %s ", pair.weight(), pair.entryString()))
                        .collect(toList());

                strings.addAll(entriesStrings);

                return strings;
            }
            finally {
                if ( command.has(commitFlag) ) {
                    tx.commitAndClose();
                }
                else {
                    tx.rollbackAnd(CLOSE);
                }
            }
        };

        OperationLogic getByLabels = (interaction, command) -> {
            String labelName = command.argAt(1);
            Optional<Entry.Label> label = core.store().labels().findBy(user, labelName);

            if ( label.isPresent() ) {
                List<Entry.Labeled> labeleds = core.store().labeledEntries().findAllBy(label.get());
                if ( labeleds.isEmpty() ) {
                    return singletonList("No entries for this label");
                }
                else {
                    List<Entry> entries = distinctLeftsOf(labeleds);
                    return entries
                            .stream()
                            .map(Entry::string)
                            .collect(toList());
                }
            }
            else {
                return singletonList("No such label");
            }
        };

        Console console = new ConsoleBuilding()
                .withFlags(labelFlag, singleFlag, commitFlag, labelsModeFlag)
                .withFormat(ConsoleFormat.building()
                        .with(NAME, "librarian"))
                .stopWhenInputIs("exit")
                .enableExitConfirmation("y", "+", "yes")
                .withOperation(building -> building
                        .named("find-by-matching")
                        .doing(findByMatching)
                        .matching(command ->
                                command.args().size() == 2 &&
                                command.argAt(0).equalsIgnoreCase("find") &&
                                command.hasNot(singleFlag)))
                .withOperation(building -> building
                        .named("get-all-by-label")
                        .doing(getByLabels)
                        .matching(command ->
                                command.args().size() == 2 &&
                                command.argAt(0).equalsIgnoreCase("get")))
                .done();

        console.life().start();
    }

    static Entry.Label.Matching matchingOfFlag(String flagValue) {
        switch ( flagValue ) {
            case "any" : return ANY_OF;
            case "all" : return ALL_OF;
            case "none" : return NONE_OF;
            default: throw new IllegalArgumentException();
        }
    }
}
