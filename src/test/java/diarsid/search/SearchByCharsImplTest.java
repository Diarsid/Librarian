package diarsid.search;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.search.SearchByCharsImpl;
import diarsid.support.time.Timer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;

import static diarsid.search.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.search.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.search.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.search.impl.logic.impl.search.TimeDirection.BEFORE;

public class SearchByCharsImplTest {

    static Core core;
    static User user;
    static Jdbc jdbc;
    static SearchByChars searchByChars;
    static Timer timer;
    static List<Entry> results;

    @BeforeClass
    public static void setUp() {
        core = TestCoreSetup.INSTANCE.core;
        user = TestCoreSetup.INSTANCE.user;
        jdbc = TestCoreSetup.INSTANCE.jdbc;
        searchByChars = new SearchByCharsImpl(jdbc);
        timer = new Timer();
    }

    @Before
    public void setUpCase() {
    }

    @After
    public void tearDownCase() {
        results.stream().map(Entry::string).forEach(System.out::println);
        results = null;
    }

    @AfterClass
    public static void tearDown() {
        for ( Timer.Timing timing : timer.timings() ) {
            System.out.println(timing);
        }
    }

    void doTest(String name, Runnable testAction) {
        jdbc.doInTransaction(transaction -> {
            timer.start(name);
            testAction.run();
            timer.stop();
        });
    }

    @Test
    public void test_short() {
        doTest("short", () -> {
            results = searchByChars.findBy(user, "tolos");
        });
    }

    @Test
    public void test_get() {
        doTest("short", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "tolknebyjr", ALL_OF, labels);
        });
    }

    @Test
    public void test_short_allOf_2() {
        doTest("short, all of 2", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "dev", "servers");
            results = searchByChars.findBy(user, "tolos", ALL_OF, labels);
        });
    }

    @Test
    public void test_long_allOf_2() {
        doTest("long, all of 2", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ALL_OF, labels);
        });
    }

    @Test
    public void test_long_anyOf_2() {
        doTest("long_anyOf_2", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ANY_OF, labels);
        });
    }

    @Test
    public void test_long_allOf_2_beforeTime() {
        doTest("long_allOf_2_beforeTime", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ALL_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_long_allOf_2_afterTime() {
        doTest("long_allOf_2_afterTime", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ALL_OF, labels, AFTER_OR_EQUAL, now().minusYears(1));
        });
    }

    @Test
    public void test_long_anyOf_2_beforeTime() {
        doTest("long_anyOf_2_beforeTime", () -> {
            List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
            results = searchByChars.findBy(user, "lordofng", ANY_OF, labels, BEFORE, now());
        });
    }

    @Test
    public void test_long_anyOf_2_afterTime() {
        doTest("long_anyOf_2_afterTime", () -> {
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
