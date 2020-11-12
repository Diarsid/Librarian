package diarsid.search;

import java.util.List;

import diarsid.jdbc.JdbcFactory;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.search.SearchByCharsImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Collections.emptyList;

import static junit.framework.TestCase.fail;

public class SearchByCharsImplTest {

    static Core core;
    static User user;
    static JdbcFactory transactionFactory;
    static JdbcTransactionThreadBindings transactionThreadBindings;
    static SearchByChars searchByChars;

    @BeforeClass
    public static void setUp() {
        core = TestCoreSetup.INSTANCE.core;
        user = TestCoreSetup.INSTANCE.user;
        transactionFactory = TestCoreSetup.INSTANCE.transactionFactory;
        transactionThreadBindings = new JdbcTransactionThreadBindings(transactionFactory);
        searchByChars = new SearchByCharsImpl(transactionThreadBindings);
    }

    @Test
    public void test() {
        transactionThreadBindings.beginTransaction();
        transactionThreadBindings.currentTransaction().logHistoryAfterCommit();
        try {
            searchByChars.findByChars(user, "darle", emptyList());
            transactionThreadBindings.commitTransaction();
        }
        catch (Exception e) {
            transactionThreadBindings.rollbackTransaction();
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void atest() {
        transactionThreadBindings.beginTransaction();
        transactionThreadBindings.currentTransaction().logHistoryAfterCommit();
        try {
            List<Entry> entries = searchByChars.findByChars(user, "statusproject", emptyList());
            transactionThreadBindings.commitTransaction();
        }
        catch (Exception e) {
            transactionThreadBindings.rollbackTransaction();
            e.printStackTrace();
            fail();
        }
    }
}
