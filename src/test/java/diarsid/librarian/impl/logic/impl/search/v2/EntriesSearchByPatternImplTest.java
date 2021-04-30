package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;
import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV17;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByPatternImpl;
import diarsid.librarian.impl.logic.impl.search.TimeDirection;
import diarsid.librarian.tests.TransactionalRollbackTestForServerSetup;
import diarsid.support.strings.MultilineMessage;
import diarsid.support.strings.StringCacheForRepeatedSeparated;
import diarsid.support.time.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.impl.logic.impl.search.CharSort.transform;
import static diarsid.librarian.impl.logic.impl.search.PatternToWordMatchingCode.describe;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.BEFORE;
import static diarsid.support.misc.Misc.methodName;
import static diarsid.support.model.Unique.uuidsOf;
import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class EntriesSearchByPatternImplTest extends TransactionalRollbackTestForServerSetup {

    static EntriesSearchByPattern entriesSearchByPattern;
    static Timer timer;
    static StringCacheForRepeatedSeparated wildcards= new StringCacheForRepeatedSeparated("?", ", ");

    static class ResultLine {

        final String entry;
        final WordCode word;

        public ResultLine(Row row) {
            this.entry = row.stringOf("string_origin");
            this.word = new WordCode(row);
        }

        String entry() {
            return entry;
        }

        WordCode word() {
            return word;
        }
    }

    String pattern;
    List<Entry.Label> labels;
    Entry.Label.Matching matching;
    TimeDirection direction;
    LocalDateTime time;
    List<Entry> resultingEntries;
    Map<String, List<WordCode>> resultingEntriesAndWords;
    Map<String, H2AggregateFunctionForAnalyzeV17> entriesAggregates;
    List<ResultLine> resultLines;

    @BeforeAll
    public static void setUp() {
        timer = new Timer();
        entriesSearchByPattern = new EntriesSearchByPatternImpl(JDBC, UUID::randomUUID);
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
        message.newLine().add("count     : ").add(resultingEntries.size());
        if ( nonNull(resultLines) && ! resultLines.isEmpty() ) {
        int i = 0;
        String entry;
        for ( Map.Entry<String, List<WordCode>> entryAndWords : resultingEntriesAndWords.entrySet() ) {
        entry = entryAndWords.getKey();
        message.newLine().indent().add(i).add(" : ").add(entry);
        message.newLine().indent().add("    ").add(entriesAggregates.get(entry).report());
        for( WordCode word : entryAndWords.getValue() ) {
            message.newLine().indent(3)
                    .add(word.string)
                    .add(" : ")
                    .add(word.code)
                    .add(" : ")
                    .add(describe(word.code));
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

    void expectNoEntries() {
        assertThat(this.resultingEntries).isEmpty();
    }

    void expectSomeEntries() {
        assertThat(this.resultingEntries.size()).isGreaterThan(0);
    }

    void expectEntriesCount(int count) {
        assertThat(this.resultingEntries.size()).isEqualTo(count);
    }

    void expectEntriesCountNoLessThan(int count) {
        assertThat(this.resultingEntries.size()).isGreaterThanOrEqualTo(count);
    }

    void expectContainingAllStringsInEveryEntry(String... entries) {
        expectSomeEntries();
        List<String> strings = asList(entries);

        List<String> entriesNotContainingAllOfStrings = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> {
                    String entryLower = entry.toLowerCase();
                    boolean entryNotOk = ! strings
                        .stream()
                        .allMatch(string -> {
                            boolean stringFound = entryLower.contains(string.toLowerCase());
                            return stringFound;
                        });
                    return entryNotOk;
                })
                .collect(toList());

        assertThat(entriesNotContainingAllOfStrings).isEmpty();
    }

    void expectContainingAllStringsInMostOfEntries(String... entries) {
        expectSomeEntries();
        List<String> strings = asList(entries);

        List<String> entriesNotContainingAllOfStrings = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> ! strings
                        .stream()
                        .allMatch(string -> entry.toLowerCase().contains(string.toLowerCase())))
                .collect(toList());

        if ( isNotEmpty(entriesNotContainingAllOfStrings) ) {
            assertThat(entriesNotContainingAllOfStrings).hasSizeLessThan(this.resultingEntries.size() / 3);
        }
    }

    void expectContainingAnyString(String... entries) {
        expectSomeEntries();
        List<String> strings = asList(entries);

        List<String> stringsNotContainingAnyOfStrings = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> strings
                        .stream()
                        .noneMatch(string -> entry.toLowerCase().contains(string.toLowerCase())))
                .collect(toList());

        assertThat(stringsNotContainingAnyOfStrings).isEmpty();
    }

    void expectContainingString(String string) {
        String fragment = string.toLowerCase();
        expectEntriesCountNoLessThan(1);

        boolean contains = this.resultingEntries
                .stream()
                .map(Entry::string)
                .anyMatch(entry -> entry.toLowerCase().contains(fragment));

        if ( ! contains ) {
            fail();
        }
    }

    void expectContainingStringInEveryEntry(String string) {
        String fragment = string.toLowerCase();
        expectEntriesCountNoLessThan(1);

        List<String> entriesNotContainingString = this.resultingEntries
                .stream()
                .map(Entry::string)
                .filter(entry -> ! entry.toLowerCase().contains(fragment))
                .collect(toList());

        assertThat(entriesNotContainingString).isEmpty();
    }

    void expectOnlyEntries(String... entries) {
        expectEntriesCount(entries.length);
    }

    private void executeSearch() throws SQLException  {
        String testMethodName = methodName(2);

        timer.start(testMethodName);
        try {
            if ( this.hasLabels() ) {
                if ( this.hasTime() ) {
                    resultingEntries = entriesSearchByPattern.findBy(USER, pattern, matching, labels, direction, time);
                }
                else {
                    resultingEntries = entriesSearchByPattern.findBy(USER, pattern, matching, labels);
                }
            }
            else {
                if ( this.hasTime() ) {
                    resultingEntries = entriesSearchByPattern.findBy(USER, pattern, direction, time);
                }
                else {
                    resultingEntries = entriesSearchByPattern.findBy(USER, pattern);
                }
            }
        }
        finally {
            timer.stop();
        }

        if ( nonNull(resultingEntries) && isNotEmpty(resultingEntries) ) {
            resultLines = new ArrayList<>();

            JDBC.threadBinding().currentTransaction().doQuery(
                    row -> resultLines.add(new ResultLine(row)),
                    "WITH \n" +
                    "words_scan_raw AS ( \n" +
                    "    SELECT uuid, string, EVAL_MATCHING_V19(?, string) AS w_code \n" +
                    "    FROM words \n" +
                    "    WHERE \n" +
                    "       EVAL_LENGTH_V4(?, string_sort, 60) > -1 AND \n" +
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
                    "WHERE we.entry_uuid IN ( " + wildcards.getFor(resultingEntries) + " ) \n" +
                    "ORDER BY e.string_origin ",
                    pattern, transform(pattern), USER.uuid(), uuidsOf(resultingEntries));

            resultingEntriesAndWords = resultLines
                    .stream()
                    .collect(
                            groupingBy(line -> line.entry,
                            mapping(line -> line.word, toList())));

            resultingEntriesAndWords.forEach((entry, wordCodes) -> {

            });

            entriesAggregates = new HashMap<>();
            for ( Map.Entry<String, List<WordCode>> entryAndWords : resultingEntriesAndWords.entrySet() ) {
                H2AggregateFunctionForAnalyzeV17 aggregate = new H2AggregateFunctionForAnalyzeV17();
                for ( WordCode wordCode : entryAndWords.getValue() ) {
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
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_before_now() throws Exception {
        search(BEFORE, now());
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_before_now_minus_year() throws Exception {
        search(BEFORE, now().minusYears(1));
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(1));
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtlok() throws Exception {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_books() throws Exception {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_books_tolkien() throws Exception {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien() throws Exception {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_before_now() throws Exception {
        search(BEFORE, now());
        expectContainingAllStringsInEveryEntry("lord", "of", "rings", "by", "tolkien");
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_before_now_minus_year() throws Exception {
        search(BEFORE, now().minusYears(1));
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(1));
        expectContainingAllStringsInEveryEntry("lord", "of", "rings", "by", "tolkien");
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books() throws Exception {
        search();
        expectContainingAllStringsInEveryEntry("lord", "of", "rings", "by", "tolkien");
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien() throws Exception {
        search();
        expectContainingAllStringsInEveryEntry("lord", "of", "rings", "tolkien");
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_books_tolkien() throws Exception {
        search();
    }

    @Test
    public void test_lorofrng() throws Exception {
        search();
        expectContainingAllStringsInEveryEntry("lord", "of", "rings");
    }

    @Test
    public void test_byjrrtolk() throws Exception {
        search();
        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
    }

    @Test
    public void test_byjrrtlok() throws Exception {
        search();
        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
    }

    @Test
    public void test_byjrtlok() throws Exception {
        search();
        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
    }

    @Test
    public void test_bytlokjr() throws Exception {
        search();
        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
    }

    @Test
    public void test_hobt() throws Exception {
        search();
//        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
    }

    @Test
    public void test_hbbt() throws Exception {
        search();
//        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
    }

    @Test
    public void test_3toolssevrirtl() throws Exception {
        search();
//        expectContainingAllStringsInMostOfEntries("by", "j.r.r", "tolkien");
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
    }

    @Test
    public void test_waltwitmn() throws Exception {
        search();
    }

    @Test
    public void test_whltwhtmn() throws Exception {
        search();
    }

    @Test
    public void test_harmurakm() throws Exception {
        search();
    }

    @Test
    public void test_virtl() throws Exception {
        search();
    }

    @Test
    public void test_virtlzt() throws Exception {
        search();
    }

    @Test
    public void test_virtual() throws Exception {
        search();
    }

    @Test
    public void test_virtlservs() throws Exception {
        search();
    }

    @Test
    public void test_servs() throws Exception {
        search();
        expectContainingString("Servers");
    }

    @Test
    public void test_tolos() throws Exception {
        search();
        expectContainingString("Tools");
    }

    @Test
    public void test_tols() throws Exception {
        search();
        expectContainingString("Tools");
    }

    @Test
    public void test_tools() throws Exception {
        search();
        expectContainingString("Tools");
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books_before() throws Exception {
        search(BEFORE, now());
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_allof_tolkien_books_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(1));
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien_books_before() throws Exception {
        search(BEFORE, now());
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien_books_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_anyof_tolkien_books_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(1));
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_books_before() throws Exception {
        search(BEFORE, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_books_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_books_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(1));
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_before() throws Exception {
        search(BEFORE, now());
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_after_now() throws Exception {
        search(AFTER_OR_EQUAL, now());
        expectNoEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_noneof_tolkien_after_now_minus_year() throws Exception {
        search(AFTER_OR_EQUAL, now().minusYears(1));
        expectSomeEntries();
    }

    @Test
    public void test_romerise() throws Exception {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_romeries() throws Exception {
        search();
        expectSomeEntries();
    }

}
