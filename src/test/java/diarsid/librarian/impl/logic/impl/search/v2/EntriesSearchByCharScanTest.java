package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;
import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV26;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByCharScan;
import diarsid.librarian.impl.logic.impl.search.TimeDirection;
import diarsid.librarian.tests.model.EntriesResult;
import diarsid.librarian.tests.model.WordMatchingCode;
import diarsid.librarian.tests.setup.transactional.TransactionalRollbackTestForServerSetup;
import diarsid.support.strings.MultilineMessage;
import diarsid.support.strings.StringCacheForRepeatedSeparated;
import diarsid.support.time.Timer;

import static java.time.LocalDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import static org.assertj.core.api.Assertions.assertThat;

import static diarsid.librarian.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.impl.logic.impl.search.CharSort.transform;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.BEFORE;
import static diarsid.support.misc.Misc.methodName;
import static diarsid.support.model.Unique.uuidsOf;

public class EntriesSearchByCharScanTest extends TransactionalRollbackTestForServerSetup {

    static EntriesSearchByPattern entriesSearchByCharScan;
    static Timer timer;
    static StringCacheForRepeatedSeparated wildcards= new StringCacheForRepeatedSeparated("?", ", ");

    static class ResultLine {

        final String entry;
        final WordMatchingCode word;

        public ResultLine(Row row) {
            this.entry = row.stringOf("string_origin");
            this.word = new WordMatchingCode(row);
        }

        String entry() {
            return entry;
        }

        WordMatchingCode word() {
            return word;
        }
    }

    String pattern;
    List<Entry.Label> labels;
    Entry.Label.Matching matching;
    TimeDirection direction;
    LocalDateTime time;
    EntriesResult entriesResult;
    Map<String, List<WordMatchingCode>> resultingEntriesAndWords;
    Map<String, H2AggregateFunctionForAnalyzeV26> entriesAggregates;
    List<ResultLine> resultLines;

    @BeforeAll
    public static void setUp() {
        timer = new Timer();
        entriesSearchByCharScan = new EntriesSearchByCharScan(JDBC, UUID::randomUUID);
    }

    private boolean hasTime() {
        return nonNull(direction) && nonNull(time);
    }

    private boolean hasLabels() {
        return nonNull(labels) && ! labels.isEmpty();
    }

    @BeforeEach
    public void setUpCase() {
    }

    @AfterEach
    public void tearDownCase() {
        MultilineMessage message = new MultilineMessage("", "   ");
        message.newLine().add("name      : ").add(timer.last().name());
        message.newLine().add("pattern   : ").add(pattern);
        if ( hasLabels() ) {
        message.newLine().add("matching  : ").add(matching.name().toLowerCase());
        message.newLine().add("labels    : ").add(labels.stream().map(Entry.Label::name).collect(joining(", ")));
        }
        if ( this.hasTime() ) {
        message.newLine().add("time case : ").add(direction.name().toLowerCase()).add(" ").add(time.toString());
        }
        message.newLine().add("time      : ").add(timer.last().millis()).add(" ms");
        message.newLine().add("count     : ").add(entriesResult.size());

        if ( nonNull(resultLines) && ! resultLines.isEmpty() ) {
            int i = 0;
            String entry;
            for ( Map.Entry<String, List<WordMatchingCode>> entryAndWords : resultingEntriesAndWords.entrySet() ) {
                entry = entryAndWords.getKey();
                message.newLine().indent().add(i).add(" : ").add(entry);
                message.newLine().indent().add("    ").add(entriesAggregates.get(entry).report());
                List<WordMatchingCode> words = entryAndWords.getValue();
                words.sort(WordMatchingCode.RATE_COMPARATOR);
                for( WordMatchingCode word : words ) {
                    message.newLine().indent(3)
                            .add(word.string)
                            .add(" : ")
                            .add(word.code)
                            .add(" : ")
                            .add(word.description);
                }
                i++;
            }
        }
        System.out.println(message.compose());
    }

    @AfterAll
    public static void tearDown() {
        for ( Timer.Timing timing : timer.timings() ) {
            System.out.println(timing);
        }
    }

    void search() throws SQLException {
        String testMethodName = methodName(1);
        if ( ! testMethodName.startsWith("test_") ) {
            testMethodName = methodName(2);
        }
        String[] methodDeclarationWords = testMethodName.split("_");
        assertThat(methodDeclarationWords).hasSizeGreaterThanOrEqualTo(2);
        String test = methodDeclarationWords[0];
        assertThat(test).isEqualTo("test");
        this.pattern = methodDeclarationWords[1];
        this.labels = new ArrayList<>();

        String word;
        boolean expectManyLabels = false;
        for (int i = 2; i < methodDeclarationWords.length; i++) {
            word = methodDeclarationWords[i].toLowerCase();

            if ( word.equals("before") || word.equals("after") || word.equals("equal") || word.equals("or") ) {
                break;
            }

            if ( i == 2 ) {
                Entry.Label.Matching matchingFromWord;
                switch (methodDeclarationWords[2].toLowerCase()) {
                    case "allof" : matchingFromWord = ALL_OF; break;
                    case "anyof" : matchingFromWord = ANY_OF; break;
                    case "noneof" : matchingFromWord = NONE_OF; break;
                    default: matchingFromWord = null;
                }

                if ( nonNull(matchingFromWord) ) {
                    expectManyLabels = true;
                    this.matching = matchingFromWord;
                }
                else {
                    expectManyLabels = false;
                    this.labels.add(CORE.store().labels().getOrSave(USER, word));
                    this.matching = ANY_OF;
                }
            }
            else if ( expectManyLabels ) {
                this.labels.add(CORE.store().labels().getOrSave(USER, word));
            }
        }

        executeSearch();
    }

    void search(TimeDirection direction, LocalDateTime time) throws Exception {
        this.direction = direction;
        this.time = time;
        this.search();
    }

    private void executeSearch() throws SQLException  {
        String testMethodName = methodName(2);

        timer.start(testMethodName);
        try {
            List<Entry> entries;
            if ( this.hasLabels() ) {
                if ( this.hasTime() ) {
                    entries = entriesSearchByCharScan.findBy(USER, pattern, matching, labels, direction, time);
                }
                else {
                    entries = entriesSearchByCharScan.findBy(USER, pattern, matching, labels);
                }
            }
            else {
                if ( this.hasTime() ) {
                    entries = entriesSearchByCharScan.findBy(USER, pattern, direction, time);
                }
                else {
                    entries = entriesSearchByCharScan.findBy(USER, pattern);
                }
            }
            entriesResult = new EntriesResult(entries);
        }
        finally {
            timer.stop();
        }

        if ( entriesResult.hasAny() ) {
            resultLines = new ArrayList<>();

            JDBC.threadBinding().currentTransaction().doQuery(
                    row -> resultLines.add(new ResultLine(row)),
                    "WITH \n" +
                    "words_scan_raw AS ( \n" +
                    "    SELECT uuid, string, EVAL_MATCHING_V53(?, string) AS w_code \n" +
                    "    FROM words \n" +
                    "    WHERE \n" +
                    "       EVAL_LENGTH_V7(?, string_sort, 60) > -1 AND \n" +
                    "       USER_uuid = ? \n" +
                    "), \n" +
                    "words_scan AS ( \n" +
                    "    SELECT * \n" +
                    "    FROM words_scan_raw \n" +
                    "    WHERE w_code > -1 \n" +
                    ") \n" +
                    "SELECT e.string_origin, ws.string, ws.w_code \n" +
                    "FROM words_scan ws\n" +
                    "    JOIN words_in_entries we \n" +
                    "        ON we.word_uuid = ws.uuid \n" +
                    "    JOIN entries e \n" +
                    "        ON e.uuid = we.entry_uuid \n" +
                    "WHERE we.entry_uuid IN ( " + wildcards.getFor(entriesResult.list()) + " ) \n" +
                    "ORDER BY e.string_origin ",
                    pattern, transform(pattern), USER.uuid(), uuidsOf(entriesResult.list()));

            resultingEntriesAndWords = resultLines
                    .stream()
                    .collect(
                            groupingBy(line -> line.entry,
                            mapping(line -> line.word, toList())));

            resultingEntriesAndWords.forEach((entry, wordCodes) -> {

            });

            entriesAggregates = new HashMap<>();
            for ( Map.Entry<String, List<WordMatchingCode>> entryAndWords : resultingEntriesAndWords.entrySet() ) {
                H2AggregateFunctionForAnalyzeV26 aggregate = new H2AggregateFunctionForAnalyzeV26();
                for ( WordMatchingCode wordCode : entryAndWords.getValue() ) {
                    aggregate.add(wordCode.code);
                }
                entriesAggregates.put(entryAndWords.getKey(), aggregate);
                aggregate.getResult();
            }

        }
    }

    @Test
    public void test_lorofrngbyjrrtolk() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_before_now() throws Exception {
        search(BEFORE, now());
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_before_now_minus_year() throws Exception {
        search(BEFORE, now().minusYears(10));
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(10));
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtlok() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_books() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_books_tolkien() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_before_now() throws Exception {
        search(BEFORE, now());
        entriesResult.expect().containingAllStringsInEveryEntry("lord", "of", "rings", "by", "tolkien").andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_before_now_minus_year() throws Exception {
        search(BEFORE, now().minusYears(10));
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(10));
        entriesResult.expect().containingAllStringsInEveryEntry("lord", "of", "rings", "by", "tolkien").andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("lord", "of", "rings", "by", "tolkien").andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("lord", "of", "rings", "tolkien").andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_books_tolkien() throws Exception {
        search();
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrng() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("lord", "of", "rings").andAssert();
    }

    @Test
    public void test_lororng() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("lord", "of", "rings").andAssert();
    }

    @Test
    public void test_byjrrtolk() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("by", "j.r.r", "tolkien").andAssert();
    }

    @Test
    public void test_byjrrtlok() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("by", "j.r.r", "tolkien").andAssert();
    }

    @Test
    public void test_byjrtlok() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("by", "j.r.r", "tolkien").andAssert();
    }

    @Test
    public void test_bytlokjr() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("by", "j.r.r", "tolkien").andAssert();
    }

    @Test
    public void test_hobt() throws Exception {
        search();
        entriesResult.expect().containingStringInMostOfEntries("hobbit").andAssert();
    }

    @Test
    public void test_yasnrkawbata() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("yasunari", "kawabata").andAssert();
    }

    @Test
    public void test_yasnrkwabta() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("yasunari", "kawabata").andAssert();
    }

    @Test
    public void test_yasnkawbata() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("yasunari", "kawabata").andAssert();
    }

    @Test
    public void test_hbbt() throws Exception {
        search();
        entriesResult.expect().containingStringInMostOfEntries("hobbit", 0.5f).andAssert();
    }

    @Test
    public void test_3toolssevrirtl() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("3", "tools", "servers", "virtualization").andAssert();
    }

    @Test
    public void test_jrtolkguide() throws Exception {
        search();
    }

    @Test
    public void test_jrrtolkguide() throws Exception {
        search();
    }

    @Test
    public void test_jeschrstpassn() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("passion", "jesus", "christ").andAssert();
    }

    @Test
    public void test_waltwitmn() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("walt", "whitman").andAssert();
    }

    @Test
    public void test_whltwhtmn() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("walt", "whitman").andAssert();
    }

    @Test
    public void test_waltwthmn() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("walt", "whitman").andAssert();
    }

    @Test
    public void test_harmurakm() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("haruki", "murakami").andAssert();
    }

    @Test
    public void test_virtl() throws Exception {
        search();
        entriesResult.expect().containingStringInSignificantCountOfEntries("virtual").andAssert();
    }

    @Test
    public void test_virtlzt() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_virtlzn() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_virtual() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_virtlservs() throws Exception {
        search();
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_servs() throws Exception {
        search();
        entriesResult.expect().containingString("Servers").andAssert();
    }

    @Test
    public void test_tolos() throws Exception {
        search();
        entriesResult.expect().containingString("Tools").andAssert();
    }

    @Test
    public void test_tolosvirtl() throws Exception {
        search();
        entriesResult.expect().containingString("Tools").andAssert();
    }

    @Test
    public void test_tolsvirtl() throws Exception {
        search();
        entriesResult.expect().containingString("Tools").andAssert();
    }

    @Test
    public void test_tols() throws Exception {
        search();
        entriesResult.expect().containingString("Tools").andAssert();
    }

    @Test
    public void test_tools() throws Exception {
        search();
        entriesResult.expect().containingString("Tools").andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books_before() throws Exception {
        search(BEFORE, now());
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(10));
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien_books_before() throws Exception {
        search(BEFORE, now());
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien_books_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien_books_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(10));
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_books_before() throws Exception {
        search(BEFORE, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_books_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_books_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(10));
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_before() throws Exception {
        search(BEFORE, now());
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        entriesResult.expect().noEntries().andAssert();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(10));
        entriesResult.expect().someEntries().andAssert();
    }

    @Test
    public void test_romerise() throws Exception {
        search();
        entriesResult.expect().containingStringsInSignificantCountOfEntries("rome", "rise").andAssert();
    }

    @Test
    public void test_romeries() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("rome", "rise").andAssert();
    }

    @Test
    public void test_immnlknt() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("immanuel", "kant").andAssert();
    }

    @Test
    public void test_imnlknt() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("immanuel", "kant").andAssert();
    }

    @Test
    public void test_ilovyo() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("i", "love", "you").andAssert();
    }

    @Test
    public void test_iloveyo() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInEveryEntry("i", "love", "you").andAssert();
    }

    @Test
    public void test_kwistz() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz").andAssert();
    }

    @Test
    public void test_kwistzhadrch() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach").andAssert();
    }

    @Test
    public void test_kwiszhedrah() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach").andAssert();
    }

    @Test
    public void test_kwizachaderah() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach").andAssert();
    }

    @Test
    public void test_kwizachederah() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach").andAssert();
    }

    @Test
    public void test_kwisahaderh() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach").andAssert();
    }

    @Test
    public void test_kwizachederahatrids() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach", "atreides").andAssert();
    }

    @Test
    public void test_kwizachoderah() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("kwisatz", "haderach").andAssert();
    }

    @Test
    public void test_kwezachoderah() throws Exception {
        search();
        entriesResult.expect().notContainingString("kwisatz").andAssert(); // KWezAc is too far from KWisAtz ez-is + c-tz
    }

    @Test
    public void test_drklalver() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("dracula", "lover").andAssert();
    }

    @Test
    public void test_naplon() throws Exception {
        search();
        entriesResult.expect().containingStringInMostOfEntries("napoleon").andAssert();
    }

    @Test
    public void test_lancstr() throws Exception {
        search();
        entriesResult.expect().containingStringInSignificantCountOfEntries("lancaster").andAssert();
    }

    @Test
    public void test_upshstnoftftclnt() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("ukrposhta", "notification", "client").andAssert();
    }

    @Test
    public void test_ukrptnotfclntsts() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("ukrposhta", "notification", "client").andAssert();
    }

    @Test
    public void test_jrtolkbylord() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInMostOfEntries("tolkien", "lord", "rings").andAssert();
    }

    @Test
    public void test_goldpath() throws Exception {
        search();
        entriesResult.expect().containingAllStringsInAtLeastOneEntry("gold", "path").andAssert();
    }

}
