package diarsid.search;

import diarsid.jdbc.JdbcFactory;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.api.Core;
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
    public static void setUp() throws Exception {
        core = CoreSetup.INSTANCE.core;
        user = CoreSetup.INSTANCE.user;
        transactionFactory = CoreSetup.INSTANCE.transactionFactory;
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
            transactionThreadBindings.currentTransaction()
                    .doQuery(
                            row -> System.out.println(row.get("string")),
                            "SELECT e.string_lower, w.string  " +
                            "FROM entries e " +
                            "    JOIN words_in_entries we  " +
                            "        ON e.uuid = we.entry_uuid " +
                            "    JOIN words w  " +
                            "        ON w.uuid = we.word_uuid " +
                            "    JOIN ( " +
                            "        WITH chars AS ( " +
                            "            SELECT cw.word_uuid, 1 AS count  " +
                            "            FROM chars_in_words cw  " +
                            "            WHERE cw.ch = ? AND cw.qty >= ? " +
                            "                UNION ALL  " +
                            "            SELECT cw.word_uuid, 1 AS count  " +
                            "            FROM chars_in_words cw  " +
                            "            WHERE cw.ch = ? AND cw.qty >= ? " +
                            "                UNION ALL  " +
                            "            SELECT cw.word_uuid, 1 AS count " +
                            "            FROM chars_in_words cw      " +
                            "            WHERE cw.ch = 'r' AND cw.qty >= 1 " +
                            "                UNION ALL  " +
                            "            SELECT cw.word_uuid, 1 AS count " +
                            "            FROM chars_in_words cw      " +
                            "            WHERE cw.ch = 'r' AND cw.qty >= 2 " +
                            "                UNION ALL " +
                            "            SELECT cw.word_uuid, 1 AS count " +
                            "            FROM chars_in_words cw      " +
                            "            WHERE cw.ch = 'd' AND cw.qty >= 1 " +
                            "                UNION ALL " +
                            "            SELECT cw.word_uuid, 1 AS count " +
                            "            FROM chars_in_words cw      " +
                            "            WHERE cw.ch = 'q' AND cw.qty >= 1 " +
                            "        ) " +
                            "        SELECT c.word_uuid, SUM(c.count) " +
                            "        FROM chars c JOIN words w ON c.word_uuid = w.uuid  " +
                            "        GROUP BY c.word_uuid " +
                            "        HAVING SUM(c.count) >= 5 " +
                            "        ) r " +
                            "    ON w.uuid = r.word_uuid;    ",
                            'l', 1, 'l', 2);
            transactionThreadBindings.commitTransaction();
        }
        catch (Exception e) {
            transactionThreadBindings.rollbackTransaction();
            e.printStackTrace();
            fail();
        }
    }
}
