package diarsid.librarian.impl.logic.impl.search.v2;

import java.util.List;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.tests.setup.transactional.TransactionalRollbackTestForServerSetup;
import diarsid.support.time.Timer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;

public class CountTest extends TransactionalRollbackTestForServerSetup {

    static Timer timer;

    @BeforeAll
    public static void setUp() {
        timer = new Timer();
    }

    @Test
    public void noneOfTolkien() {
        Entry.Label tolkien = CORE.store().labels().getOrSave(USER, "tolkien");
        long count = CORE.store().labeledEntries().countEntriesBy(NONE_OF, tolkien);
        System.out.println(count);
    }

    @Test
    public void noneOfBooks() {
        Entry.Label books = CORE.store().labels().getOrSave(USER, "books");
        long count = CORE.store().labeledEntries().countEntriesBy(NONE_OF, books);
        System.out.println(count);
    }

    @Test
    public void noneOfBooksTolkien() {
        List<Entry.Label> labels = CORE.store().labels().getOrSave(USER, "books", "tolkien");
        long count = CORE.store().labeledEntries().countEntriesBy(NONE_OF, labels);
        System.out.println(count);
    }

    @Test
    public void anyOfBooksTolkien() {
        List<Entry.Label> labels = CORE.store().labels().getOrSave(USER, "books", "tolkien");
        long count = CORE.store().labeledEntries().countEntriesBy(ANY_OF, labels);
        System.out.println(count);
    }
}
