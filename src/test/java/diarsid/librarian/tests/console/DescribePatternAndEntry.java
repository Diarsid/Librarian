package diarsid.librarian.tests.console;

import java.util.ArrayList;
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
import diarsid.librarian.impl.logic.impl.search.CharSort;
import diarsid.librarian.impl.logic.impl.search.v2.PatternAndWords;
import diarsid.librarian.tests.model.WordMatchingCode;
import diarsid.sceptre.impl.collections.ListInt;
import diarsid.support.strings.MultilineMessage;

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

        String sortedPattern = CharSort.transform(pattern);

        List<WordMatchingCode> wordAndCodes = jdbc
                .doQueryAndStream(
                        (row) -> new WordMatchingCode(row, "w_code", "string"),
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "    SELECT uuid, string, EVAL_MATCHING_V54(?, string) AS w_code \n" +
                        "    FROM words \n" +
                        "    WHERE \n" +
                        "       EVAL_LENGTH_V8(?, string_sort, 60) > -1 AND \n" +
                        "       user_uuid = ? \n" +
                        "), \n" +
                        "words_scan AS ( \n" +
                        "    SELECT * \n" +
                        "    FROM words_scan_raw \n" +
                        "    WHERE w_code > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "    SELECT we.entry_uuid, ws.string, w_code \n" +
                        "    FROM words_scan ws \n" +
                        "        JOIN words_in_entries we \n" +
                        "            ON we.word_uuid = ws.uuid \n" +
                        "    WHERE we.entry_uuid = ?\n" +
                        ") \n" +
                        "SELECT * \n" +
                        "FROM entries_scan",
                        pattern, sortedPattern, user.uuid(), entry.uuid())
                .collect(toList());

        MultilineMessage message = new MultilineMessage("", "   ");

        message.newLine().add("DB execution:");
        message.addAsLines(wordAndCodes
                .stream()
                .map(wordAndCode -> wordAndCode.toString())
                .collect(toList()),
                1);

        PatternAndWords patternAndWords = new PatternAndWords(currentVersion(), pattern, words);

        message.newLine().add("In-memory execution:");
        message.addAsLines(patternAndWords.report.composeToLines(), 1);

        return message.composeToLines();
    }
}
