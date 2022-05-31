package diarsid.librarian.impl.logic.impl;

import java.util.Comparator;
import java.util.List;

import diarsid.librarian.api.Search;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.console.LoggingSearchObserver;
import diarsid.librarian.tests.setup.transactional.TransactionalRollbackTestForServerSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class SearchTest extends TransactionalRollbackTestForServerSetup {

    private static final Logger log = LoggerFactory.getLogger(SearchTest.class);

    static Search search;
    static User user;
    static Search.Observer observer;

    @BeforeAll
    public static void setUp() {
        search = CORE.search();
        user = USER;

        Comparator<PatternToEntry> comparator = (o1, o2) -> {
            return CORE_TEST_SETUP.algorithm.compare(o1.weight(), o2.weight());
        };

        observer = new LoggingSearchObserver(comparator);
    }

    @Test
    public void test() {
        List<PatternToEntry> relations = search.findAllBy(user, "jrtolkbylord");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_lorofrngbyjrtolk() {
        List<PatternToEntry> relations = search.findAllBy(user, "lorofrngbyjrtolk");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_servs() {
        List<PatternToEntry> relations = search.findAllBy(user, "servs");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_tolos() {
        List<PatternToEntry> relations = search.findAllBy(user, "tolos");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_tolosvitrl() {
        List<PatternToEntry> relations = search.findAllBy(user, "tolosvirtl");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_tolsvitrl() {
        List<PatternToEntry> relations = search.findAllBy(user, "tolsvirtl", observer);
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_goldpath() {
        List<PatternToEntry> relations = search.findAllBy(user, "goldpath", observer);
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_virtualize() {
        List<PatternToEntry> relations = search.findAllBy(user, "virtualize");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_boys() {
        List<PatternToEntry> relations = search.findAllBy(user, "boys");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_boysflm() {
        List<PatternToEntry> relations = search.findAllBy(user, "boysflm");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_boysserl() {
        List<PatternToEntry> relations = search.findAllBy(user, "boysserl");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_swchjava() {
        List<PatternToEntry> relations = search.findAllBy(user, "swchjava", observer);
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_drsprojs() {
        List<PatternToEntry> relations = search.findAllBy(user, "drsprojs");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_projsdrs() {
        List<PatternToEntry> relations = search.findAllBy(user, "projsdrs", observer);
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_kwistzhadrch() {
        List<PatternToEntry> relations = search.findAllBy(user, "kwistzhadrch");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

    @Test
    public void test_kwizachoderah() {
        List<PatternToEntry> relations = search.findAllBy(user, "kwizachoderah");
        for ( PatternToEntry relation : relations ) {
            log.info(format("%s %s", relation.weight(), relation.entryString()));
        }
    }

//    @Test
//    public void test_projsdrs() {
//        List<PatternToEntry> relations = search.findAllBy(user, "projsdrs");
//        for ( PatternToEntry relation : relations ) {
//            log.info(format("%s %s", relation.weight(), relation.entryString()));
//        }
//    }
}
