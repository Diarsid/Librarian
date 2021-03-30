package diarsid.search.impl.logic.impl.search.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.TestCoreSetup;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.support.objects.Pair;
import diarsid.support.strings.MultilineMessage;
import diarsid.support.strings.StringCacheForRepeatedSeparated;
import diarsid.support.time.Timer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;

import static diarsid.search.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.search.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.search.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.search.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.search.impl.logic.impl.search.TimeDirection.BEFORE;
import static diarsid.search.impl.logic.impl.search.v2.CharSort.transform;
import static diarsid.support.misc.Misc.methodName;
import static diarsid.support.model.Unique.uuidsOf;
import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class SearchByCharsImplTest {

    static Core core;
    static User user;
    static Jdbc jdbc;
    static SearchByChars searchByChars;
    static Timer timer;
    static StringCacheForRepeatedSeparated wildcards= new StringCacheForRepeatedSeparated("?", ", ");

    static class ResultDescription {

        final String entry;
        final String word;
        final long wordCode;

        public ResultDescription(Row row) {
            this.entry = row.stringOf("string_origin");
            this.word = row.stringOf("string");
            this.wordCode = row.longOf("w_code");
        }

        String string() {
            return format("%s   %s    %s", entry, word, wordCode);
        }
    }

    String pattern;
    List<Entry.Label> labels;
    Entry.Label.Matching matching;
    List<Entry> results;
    List<ResultDescription> resultDescriptions;

    @BeforeAll
    public static void setUp() {
        core = TestCoreSetup.INSTANCE.core;
        user = TestCoreSetup.INSTANCE.user;
        jdbc = TestCoreSetup.INSTANCE.jdbc;
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
        message.newLine().add("count    : ").add(results.size());
        if ( nonNull(resultDescriptions) && ! resultDescriptions.isEmpty() ) {
        int i = 0;
        for ( ResultDescription description : resultDescriptions ) {
        message.newLine().indent().add(i).add(" : ").add(description.string());
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
        assertThat(this.results.size()).isGreaterThan(0);
    }

    void expectEntriesCount(int count) {
        assertThat(this.results.size()).isEqualTo(count);
    }

    void expectEntriesCountNoLessThan(int count) {
        assertThat(this.results.size()).isGreaterThanOrEqualTo(count);
    }

    void expectContainingEntries(String... entries) {
        expectEntriesCountNoLessThan(entries.length);

    }

    void expectContainingStrings(String... entries) {
        expectEntriesCountNoLessThan(entries.length);
        List<String> strings = asList(entries);
    }

    void expectContainingString(String entry) {
        expectEntriesCountNoLessThan(1);

        boolean contains = this.results
                .stream()
                .map(Entry::string)
                .anyMatch(string -> string.contains(entry));

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
                results = searchByChars.findBy(user, pattern, matching, labels);
            }
            finally {
                timer.stop();
            }
        });

        if ( nonNull(results) && isNotEmpty(results) ) {
            resultDescriptions = new ArrayList<>();

            jdbc.doInTransaction(transaction -> {
                transaction.doQuery(row -> {
                            resultDescriptions.add(new ResultDescription(row));
                        },
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "    SELECT uuid, string, MY_MATCHING_18(?, string) AS w_code \n" +
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
                        "WHERE we.entry_uuid IN ( " + wildcards.getFor(results) + " ) \n" +
                        "ORDER BY e.string_origin ",
                        pattern, transform(pattern), user.uuid(), uuidsOf(results));
            });
        }
    }

    @Test
    public void test_short() {
        search(() -> {
            results = searchByChars.findBy(user, "tolos");
        });
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_withoutLabels() {
        search("lorofrngbyjrrtolk");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtlok_withoutLabels() {
        search("lorofrngbyjrrtlok");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_with_label_books() {
        search("lorofrngbyjrrtolk", ANY_OF, "books");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_with_label_books_tolkien() {
        search("lorofrngbyjrrtolk", ANY_OF, "books", "tolkien");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_with_single_label_tolkien() {
        search("lorofrngbyjrrtolk", "tolkien");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_with_all_of_label_tolkien_books() {
        search("lorofrngbyjrrtolk", ALL_OF, "tolkien", "books");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_with_none_of_label_tolkien() {
        search("lorofrngbyjrrtolk", NONE_OF, "tolkien");
        expectSomeEntries();
    }

    @Test
    public void test_search_lorofrngbyjrrtolk_with_none_of_label_books_tolkien() {
        search("lorofrngbyjrrtolk", NONE_OF, "tolkien", "books");
    }

    @Test
    public void test_search_lorofrng_withoutLabels() {
        search("lorofrng");
    }

    @Test
    public void test_search_byjrrtolk_without_labels() {
        search("byjrrtolk");
        expectSomeEntries();
    }

    @Test
    public void test_search_byjrrtlok_without_labels() {
        search("byjrrtlok");
        expectSomeEntries();
    }

    @Test
    public void test_search_byjrtlok_without_labels() {
        search("byjrtlok");
        expectSomeEntries();
    }

    @Test
    public void test_search_bytlokjr_without_labels() {
        search("bytlokjr");
        expectSomeEntries();
    }

    @Test
    public void test_search_jrtolkguide_withoutLabels() {
        search("jrtolkguide");
    }

    @Test
    public void test_search_jrrtolkguide_withoutLabels() {
        search("jrrtolkguide");
    }

    @Test
    public void test_search_jeschrstpassn_withoutLabels() {
        search("jeschrstpassn");
    }

    @Test
    public void test_search_waltwitmn_withoutLabels() {
        search("whaltwhitmn");
    }

    @Test
    public void test_search_whltwhtmn_withoutLabels() {
        search("whltwhtmn");
    }

    @Test
    public void test_search_harmurakm_withoutLabels() {
        search("harmurakm");
    }

    @Test
    public void test_search_virtl_withoutLabels() {
        search("virtl");
    }

    @Test
    public void test_search_virtlzt_withoutLabels() {
        search("virtlzt");
    }

    @Test
    public void test_search_virtual_withoutLabels() {
        search("virtual");
    }

    @Test
    public void test_search_virtlservs_withoutLabels() {
        search("virtlservs");
    }

    @Test
    public void test_search_tolos_withoutLabels() {
        search("tolos");
        expectContainingString("Tools");
    }

    @Test
    public void test_search_tols_withoutLabels() {
        search("tols");
        expectContainingString("Tools");
    }

    @Test
    public void test_search_tools_withoutLabels() {
        search("tools");
        expectContainingString("Tools");
    }

    @Test
    public void test_get() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lorofrng", ANY_OF, labels);
            int a = 5;
        });
    }

    @Test
    public void test_short_allOf_2() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            results = searchByChars.findBy(user, "tolos", ALL_OF, labels);
            int a = 5;
        });
    }

    @Test
    public void test_long_allOf_2() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ALL_OF, labels);
        });
    }

    @Test
    public void test_long_anyOf_2() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ANY_OF, labels);
        });
    }

    @Test
    public void test_long_allOf_2_beforeTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ALL_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_long_allOf_2_afterTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ALL_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_long_anyOf_2_beforeTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ANY_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_long_anyOf_2_afterTime() {
        search(() -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ANY_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_afterTime() {
        jdbc.doInTransaction(transaction -> {
            results = searchByChars.findBy(user, "tolos", null, emptyList(), AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_beforeTime() {
        jdbc.doInTransaction(transaction -> {
            results = searchByChars.findBy(user, "tolos", null, emptyList(), BEFORE, now());
        });
    }

    @Test
    public void test_short_allOf_2_afterTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            results = searchByChars.findBy(user, "tolos", ALL_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_allOf_2_beforeTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            results = searchByChars.findBy(user, "tolos", ALL_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_short_anyOf_2_afterTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            results = searchByChars.findBy(user, "tolos", ANY_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_short_anyOf_2_beforeTime() {
        jdbc.doInTransaction(transaction -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            results = searchByChars.findBy(user, "tolos", ANY_OF, labels, BEFORE, now());
        });
    }
}
