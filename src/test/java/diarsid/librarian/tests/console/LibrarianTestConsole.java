package diarsid.librarian.tests.console;

import java.util.List;
import java.util.function.BiFunction;

import diarsid.console.api.Console;
import diarsid.console.api.format.ConsoleFormat;
import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.console.impl.ConsoleBuilding;
import diarsid.console.impl.ProcessingBuilding;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcTransaction;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import static diarsid.console.api.format.ConsoleFormatElement.NAME;
import static diarsid.jdbc.api.JdbcTransaction.ThenDo.CLOSE;
import static diarsid.librarian.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;
import static diarsid.support.configuration.Configuration.configure;

public class LibrarianTestConsole {

    static {
//        configure().withDefault(
//                "log = true",
//                "analyze.result.variants.limit = 100",
//                "diarsid.strings.similarity.log.multiline = true",
//                "diarsid.strings.similarity.log.multiline.prefix = [similarity]",
//                "diarsid.strings.similarity.log.multiline.indent = 1",
//                "diarsid.strings.similarity.log.base = true",
//                "diarsid.strings.similarity.log.advanced = true",
//                "analyze.weight.base.log = false",
//                "analyze.weight.positions.search.log = true",
//                "analyze.weight.positions.clusters.log = true",
////                "analyze.result.variants.limit = 11",
//                "analyze.similarity.log.base = false",
//                "analyze.similarity.log.advanced = true");
    }

    public static void main(String[] args) {
        Command.Flag labelFlag = Command.Flag.openValue("label", "l", true);
        Command.Flag singleFlag = Command.Flag.noValue("single", "s");
        Command.Flag commitFlag = Command.Flag.noValue("commit", "c");
        Command.Flag labelsModeFlag = Command.Flag.restrictingValues("labelsmode", "lm", false, "any", "all", "none");
        CoreTestSetup setup = server();
        Core core = setup.core;
        User user = setup.user;
        Jdbc jdbc = setup.jdbc;

        BiFunction<ConsoleInteraction, Command, List<String>> logic = (interaction, command) -> {
            JdbcTransaction tx = jdbc.createTransaction();

            String arg = command.argAt(0);
            List<String> labelNames = command.valuesOf(labelFlag);
            String labelsMode = command.valueOf(labelsModeFlag).orElse("any");

            Entry.Label.Matching matching = matchingOfFlag(labelsMode);
            List<Entry.Label> labels = core.store().labels().getOrSave(user, labelNames);

            List<PatternToEntry> entries = core.search().findAllBy(user, arg, matching, labels);

            if ( command.has(commitFlag) ) {
                tx.commitAndClose();
            }
            else {
                tx.rollbackAnd(CLOSE);
            }

            return entries
                    .stream()
                    .map(pair -> format("%s : %s ", pair.weight(), pair.entryString()))
                    .collect(toList());
        };

        Console console = new ConsoleBuilding()
                .withFlags(labelFlag, singleFlag, commitFlag, labelsModeFlag)
                .withFormat(ConsoleFormat.building()
                        .with(NAME, "librarian"))
                .stopWhenInputIs("exit")
                .enableExitConfirmation("y", "+", "yes")
                .withProcessing(ProcessingBuilding
                        .named("logic")
                        .doing(logic)
                        .matching(command -> command.args().size() == 1 && command.hasNot(singleFlag)))
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
