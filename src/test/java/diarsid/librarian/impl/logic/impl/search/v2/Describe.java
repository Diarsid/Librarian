package diarsid.librarian.impl.logic.impl.search.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching;
import diarsid.librarian.tests.setup.CoreTestSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching.currentVersion;
import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class Describe {

    static Logger log = LoggerFactory.getLogger(Describe.class);

    static String pattern = "uposhtarch";
    static UUID entryUuid = fromString("ab555fe1-dea9-42be-9481-a4311572e0dd");

    public static void main(String[] args) {
        CoreTestSetup server = server();

        List<String> words = server.jdbc.doInTransaction(transaction -> {
            return transaction
                    .doQueryAndStream(
                            ColumnGetter.stringOf("string"),
                            "SELECT w.string \n" +
                            "FROM words_in_entries we \n" +
                            "    JOIN words w \n" +
                            "        ON w.uuid = we.word_uuid \n" +
                            "WHERE we.entry_uuid = ?",
                            entryUuid)
                    .collect(toList());
        });

        PatternToWordMatching matching = currentVersion();

        long code;
        List<String> wordsLog = new ArrayList<>();
        for ( String word : words ) {
            code = matching.evaluate(pattern, word);
            wordsLog.add(format("word: %s code: %s ", word, code));
        }

        for ( String logLine : wordsLog ) {
            log.info(logLine);
        }
    }
}
