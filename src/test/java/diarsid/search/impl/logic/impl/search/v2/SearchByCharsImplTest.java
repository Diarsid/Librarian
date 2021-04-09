package diarsid.search.impl.logic.impl.search.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.tests.CoreTestSetup;
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
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import static diarsid.search.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.search.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.search.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.search.impl.logic.impl.search.TimeDirection.BEFORE;
import static diarsid.search.impl.logic.impl.search.v2.CharSort.transform;
import static diarsid.support.misc.Misc.methodName;
import static diarsid.support.model.Unique.uuidsOf;
import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;
import static diarsid.tests.db.embedded.h2.H2TestDataBase.Type.REMOTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SearchByCharsImplTest {

    static Core core;
    static User user;
    static Jdbc jdbc;
    static SearchByChars searchByChars;
    static Timer timer;
    static StringCacheForRepeatedSeparated wildcards= new StringCacheForRepeatedSeparated("?", ", ");

    static class ResultLine {

        final String entry;
        final ResultWord word;

        public ResultLine(Row row) {
            this.entry = row.stringOf("string_origin");
            this.word = new ResultWord(row);
        }

        String entry() {
            return entry;
        }

        ResultWord word() {
            return word;
        }
    }

    static class ResultWord {
        final String string;
        final long code;

        public ResultWord(Row row) {
            this.string = row.stringOf("string");
            this.code = row.longOf("w_code");
        }
    }

    String pattern;
    List<Entry.Label> labels;
    Entry.Label.Matching matching;
    List<Entry> resultingEntries;
    Map<String, List<ResultWord>> resultingEntriesAndWords;
    List<ResultLine> resultLines;

    @BeforeAll
    public static void setUp() {
        CoreTestSetup coreTestSetup = new CoreTestSetup(REMOTE);
        core = coreTestSetup.core;
        user = coreTestSetup.user;
        jdbc = coreTestSetup.jdbc;
        searchByChars = new SearchByCharsImpl(jdbc);
        timer = new Timer();
    }

    @BeforeEach
    public void setUpCase() {
    }

    @AfterEach
    public void tearDownCase() {
        MultilineMessage message = new MultilineMessage("", "   ");
        message.newLine().add("name     : ").add(timer.last().name());
        message.newLine().add("pattern  : ").add(pattern);
        if ( ! labels.isEmpty() ) {
        message.newLine().add("matching : ").add(matching.name().toLowerCase());
        message.newLine().add("labels   : ").add(labels.stream().map(Entry.Label::name).collect(joining(", ")));
        }
        message.newLine().add("time     : ").add(timer.last().millis()).add(" ms");
        message.newLine().add("count    : ").add(resultingEntries.size());
        if ( nonNull(resultLines) && ! resultLines.isEmpty() ) {
        int i = 0;
        for ( Map.Entry<String, List<ResultWord>> entryAndWords : resultingEntriesAndWords.entrySet() ) {
        message.newLine().indent().add(i).add(" : ").add(entryAndWords.getKey());
        for( ResultWord word : entryAndWords.getValue() ) {
            message.newLine().indent(3).add(word.string).add(" : ").add(word.code).add(" - ").add(new PatternToWordMatchingCode.Description(word.code).toString());
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

    void search(Runnable testAction) {
        String testMethodName = methodName(1);
        jdbc.doInTransaction(transaction -> {
            timer.start(testMethodName);
            try {
                testAction.run();
            }
            finally {
                timer.stop();
            }
        });
    }

    void search() {
        String testMethodName = methodName(1);
        String[] methodDeclarationWords = testMethodName.split("_");
        assertThat(methodDeclarationWords).hasSizeGreaterThanOrEqualTo(2);
        String test = methodDeclarationWords[0];
        assertThat(test).isEqualTo("test");
        this.pattern = methodDeclarationWords[1];

        if ( methodDeclarationWords.length == 2 ) {
            this.labels = emptyList();
            this.matching = ANY_OF;
        }
        else if ( methodDeclarationWords.length == 3 ) {
            this.matching = ANY_OF;
            this.labels = List.of(core.store().labels().getOrSave(user, methodDeclarationWords[2]));
        }
        else {
            this.matching = Entry.Label.Matching.valueOf(
                    methodDeclarationWords[2].toUpperCase() + "_" + methodDeclarationWords[3].toUpperCase());
            this.labels = new ArrayList<>();
            for (int i = 4; i < methodDeclarationWords.length; i++) {
                this.labels.add(core.store().labels().getOrSave(user, methodDeclarationWords[i]));
            }
        }

        executeSearch();
    }

    void search(String pattern) {
        this.pattern = pattern;
        this.labels = emptyList();
        this.matching = ANY_OF;
        executeSearch();
    }

    void search(String pattern, String label) {
        this.pattern = pattern;
        this.labels = List.of(core.store().labels().getOrSave(user, label));
        this.matching = ANY_OF;
        executeSearch();
    }

    void search(String pattern, Entry.Label.Matching matching, String... labels) {
        this.pattern = pattern;
        this.labels = core.store().labels().getOrSave(user, labels);
        this.matching = matching;
        executeSearch();
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

    void expectContainingEntries(String... entries) {
        expectEntriesCountNoLessThan(entries.length);

    }

    void expectContainingStrings(String... entries) {
        expectEntriesCountNoLessThan(entries.length);
        List<String> strings = asList(entries);
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

    void expectOnlyEntries(String... entries) {
        expectEntriesCount(entries.length);

    }

    private void executeSearch() {
        String testMethodName = methodName(2);

        jdbc.doInTransaction(transaction -> {
            timer.start(testMethodName);
            try {
                resultingEntries = searchByChars.findBy(user, pattern, matching, labels);
            }
            finally {
                timer.stop();
            }
        });

        if ( nonNull(resultingEntries) && isNotEmpty(resultingEntries) ) {
            resultLines = new ArrayList<>();

            jdbc.doInTransaction(transaction -> {
                transaction.doQuery(
                        row -> resultLines.add(new ResultLine(row)),
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "    SELECT uuid, string, MY_MATCHING_19(?, string) AS w_code \n" +
                        "    FROM words \n" +
                        "    WHERE \n" +
                        "       MYLENGTH_4(?, string_sort, 60) > -1 AND \n" +
                        "       user_uuid = ? \n" +
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
                        pattern, transform(pattern), user.uuid(), uuidsOf(resultingEntries));
            });

            resultingEntriesAndWords = resultLines
                    .stream()
                    .collect(
                            groupingBy(line -> line.entry,
                            mapping(line -> line.word, toList())));
        }
    }

    @Test
    public void test_lorofrngbyjrrtolk() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtlok() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_books() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_any_of_books_tolkien() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_tolkien() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_all_of_tolkien_books() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_none_of_tolkien() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_lorofrngbyjrrtolk_none_of_books_tolkien() {
        search();
    }

    @Test
    public void test_lorofrng() {
        search();
    }

    @Test
    public void test_byjrrtolk() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_byjrrtlok() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_byjrtlok() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_bytlokjr() {
        search();
        expectSomeEntries();
    }

    @Test
    public void test_jrtolkguide() {
        search();
    }

    @Test
    public void test_jrrtolkguide() {
        search();
    }

    @Test
    public void test_jeschrstpassn() {
        search();
    }

    @Test
    public void test_waltwitmn() {
        search();
    }

    @Test
    public void test_whltwhtmn() {
        search();
    }

    @Test
    public void test_harmurakm() {
        search();
    }

    @Test
    public void test_virtl() {
        search();
    }

    @Test
    public void test_virtlzt() {
        search();
    }

    @Test
    public void test_virtual() {
        search();
    }

    @Test
    public void test_virtlservs() {
        search();
    }

    @Test
    public void test_servs() {
        search();
        expectContainingString("Servers");
    }

    @Test
    public void test_tolos() {
        search();
        expectContainingString("Tools");
    }

    @Test
    public void test_tols() {
        search("tols");
        expectContainingString("Tools");
    }

    @Test
    public void test_tools() {
        search("tools");
        expectContainingString("Tools");
    }

    @Test
    public void test_get() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lorofrng", ANY_OF, labels);
            int a = 5;
        });
    }

    @Test
    public void test_short_allOf_2() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            resultingEntries = searchByChars.findBy(user, "tolos", ALL_OF, labels);
            int a = 5;
        });
    }

    @Test
    public void test_long_allOf_2() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lordofng", ALL_OF, labels);
        });
    }

    @Test
    public void test_long_anyOf_2() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lordofng", ANY_OF, labels);
        });
    }

    @Test
    public void test_long_allOf_2_beforeTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lordofng", ALL_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_long_allOf_2_afterTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lordofng", ALL_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_long_anyOf_2_beforeTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lordofng", ANY_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_long_anyOf_2_afterTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            resultingEntries = searchByChars.findBy(user, "lordofng", ANY_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_afterTime() {
        jdbc.doInTransaction(transaction -> {
            resultingEntries = searchByChars.findBy(user, "tolos", null, emptyList(), AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_beforeTime() {
        jdbc.doInTransaction(transaction -> {
            resultingEntries = searchByChars.findBy(user, "tolos", null, emptyList(), BEFORE, now());
        });
    }

    @Test
    public void test_short_allOf_2_afterTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            resultingEntries = searchByChars.findBy(user, "tolos", ALL_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_allOf_2_beforeTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            resultingEntries = searchByChars.findBy(user, "tolos", ALL_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_short_anyOf_2_afterTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            resultingEntries = searchByChars.findBy(user, "tolos", ANY_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_anyOf_2_beforeTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            resultingEntries = searchByChars.findBy(user, "tolos", ANY_OF, labels, BEFORE, now());
        });
    }
}
