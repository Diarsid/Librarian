package diarsid.librarian.impl.logic.impl.search.v2;

import org.junit.jupiter.api.Test;

import diarsid.librarian.impl.model.WordInEntry;
import diarsid.librarian.tests.setup.transactional.TransactionalRollbackTestForServerSetup;

import static org.assertj.core.api.Assertions.assertThat;

public class WordsInEntriesTest extends TransactionalRollbackTestForServerSetup {

    @Test
    public void test() {
        JDBC.doInTransaction(transaction -> {
            var wordInEntry = transaction.doQueryAndConvertFirstRow(
                    WordInEntry::new,
                    "SELECT * " +
                    "FROM words_in_entries " +
                    "FETCH FIRST 1 ROW ONLY ");

            assertThat(wordInEntry).isPresent();
        });
    }
}
