package diarsid.librarian.tests.console;

import java.util.List;
import java.util.UUID;

import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.console.api.io.operations.OperationLogic;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.impl.search.v2.PatternAndWords;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching.currentVersion;

public class DescribePatternAndEntry extends LibrarianConsoleOperationLogic {

    public DescribePatternAndEntry(Core core, User user, Jdbc jdbc) {
        super(core, user, jdbc);
    }

    @Override
    public List<String> execute(ConsoleInteraction interaction, Command command) {
        String pattern = command
                .valueOfFlag("pattern")
                .orElseGet(() -> {
                    interaction.print("    pattern: ");
                    return interaction
                            .readLine()
                            .orElse(null);
                });

        if ( isNull(pattern) || pattern.isBlank() || pattern.isEmpty() ) {
            return asList("no pattern");
        }

        UUID uuid = null;
        String entryString = null;
        if ( command.hasFlag("uuid") ) {
            uuid = command
                    .valueOfFlag("uuid")
                    .map(UUID::fromString)
                    .orElse(null);
        }
        else if ( command.hasFlag("entry") ) {
            entryString = command
                    .valueOfFlag("entry")
                    .orElse(null);
        }
        else {
            interaction.print("    uuid or entry: ");
            String input = interaction
                    .readLine()
                    .orElse(null);

            if ( isNull(input) || input.isEmpty() || input.isBlank() ) {
                return asList("no entry");
            }

            try {
                uuid = UUID.fromString(input);
            }
            catch (Throwable t) {
                entryString = input;
            }
        }

        Entry entry = null;
        if ( nonNull(uuid) ) {
            entry = core.store().entries().findBy(user, uuid).orElse(null);
        }
        else if ( nonNull(entryString) ) {
            entry = core.store().entries().findBy(user, entryString).orElse(null);
        }

        if ( isNull(entry) ) {
            return asList("no entry");
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

        PatternAndWords patternAndWords = new PatternAndWords(currentVersion(), pattern, words);

        return patternAndWords.report.composeToLines();
    }
}
