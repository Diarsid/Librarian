package diarsid.librarian.tests.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import diarsid.console.api.io.Command;
import diarsid.console.api.io.ConsoleInteraction;
import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.Words;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.impl.search.CharSort;
import diarsid.librarian.impl.logic.impl.search.v2.PatternAndWords;
import diarsid.librarian.tests.model.WordMatchingCode;
import diarsid.support.strings.MultilineMessage;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.impl.logic.impl.search.charscan.count.CountCharMatchesV2.CURRENT_VERSION;
import static diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching.currentVersion;
import static diarsid.support.objects.collections.CollectionUtils.nonEmpty;

public class DescribePatternAndEntry extends LibrarianConsoleOperationLogic {

    private final Words words;

    public DescribePatternAndEntry(Core core, User user, Jdbc jdbc) {
        super(core, user, jdbc);
        this.words = core.store().words();
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

        List<String> wordsToInvestigate = null;
        if ( command.hasFlag("word") ) {
            wordsToInvestigate = command.valuesOfFlag("word");
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

        MultilineMessage message = new MultilineMessage("", "   ");

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

        Map<String, Optional<Entry.Word>> wordsToCheck = null;
        if ( nonNull(wordsToInvestigate) ) {
            List<String> nonExistingWords = new ArrayList<>();
            for ( String wordToInvestigate : wordsToInvestigate ) {
                if ( ! words.contains(wordToInvestigate) ) {
                    nonExistingWords.add(wordToInvestigate);
                }
            }

            wordsToInvestigate.removeAll(nonExistingWords);

            if ( nonEmpty(nonExistingWords) ) {
                message.newLine().add("words not exists:").addAsLines(nonExistingWords, 1);
            }

            if ( nonEmpty(wordsToInvestigate) ) {
                wordsToCheck = this.words.findAllBy(user, wordsToInvestigate);
            }
        }

        String sortedPattern = CharSort.transform(pattern);
        message.newLine().add("sorted pattern " + sortedPattern);

        List<WordMatchingCode> wordAndCodes;
        if ( nonNull(wordsToCheck) && nonEmpty(wordsToCheck) ) {
            String wordsUuids = wordsToCheck
                    .values()
                    .stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .peek(word -> message.newLine().indent().add("include word: " + word.string()))
                    .map(Entry.Word::uuid)
                    .map(UUID::toString)
                    .map(uuidS -> "'" + uuidS + "'")
                    .collect(joining(", "));

            wordAndCodes = jdbc
                    .doQueryAndStream(
                            (row) -> new WordMatchingCode(row, "w_code", "string"),
                            "WITH \n" +
                            "words_scan_raw AS ( \n" +
                            "    SELECT uuid, string, EVAL_MATCHING_V56(?, string) AS w_code \n" +
                            "    FROM words \n" +
                            "    WHERE \n" +
                            "       EVAL_LENGTH_V11(?, string, ?, string_sort, 60) > -1 AND \n" +
                            "       user_uuid = ? \n" +
                            "), \n" +
                            "words_scan AS ( \n" +
                            "    SELECT * \n" +
                            "    FROM words_scan_raw \n" +
                            "    WHERE " +
                            "       w_code > -1 OR \n" +
                            "       uuid IN (" + wordsUuids + ") \n" +
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
                            pattern, pattern, sortedPattern, user.uuid(), entry.uuid())
                    .collect(toList());
        }
        else {
            wordAndCodes = jdbc
                    .doQueryAndStream(
                            (row) -> new WordMatchingCode(row, "w_code", "string"),
                            "WITH \n" +
                            "words_scan_raw AS ( \n" +
                            "    SELECT uuid, string, EVAL_MATCHING_V56(?, string) AS w_code \n" +
                            "    FROM words \n" +
                            "    WHERE \n" +
                            "       EVAL_LENGTH_V11(?, string, ?, string_sort, 60) > -1 AND \n" +
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
                            pattern, pattern, sortedPattern, user.uuid(), entry.uuid())
                    .collect(toList());
        }

        message.newLine().add("DB execution:");
        message.addAsLines(wordAndCodes
                .stream()
                .map(wordAndCode -> wordAndCode.toString())
                .collect(toList()),
                1);

        PatternAndWords patternAndWords = new PatternAndWords(
                currentVersion(), CURRENT_VERSION, pattern, words);

        message.newLine().add("In-memory execution:");
        message.addAsLines(patternAndWords.report.composeToLines(), 1);

        return message.composeToLines();
    }
}
