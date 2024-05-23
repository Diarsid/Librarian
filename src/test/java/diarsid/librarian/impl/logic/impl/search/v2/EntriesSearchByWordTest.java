package diarsid.librarian.impl.logic.impl.search.v2;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import diarsid.librarian.api.Labels;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.impl.WordsImpl;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByWord;
import diarsid.librarian.tests.model.EntriesResult;
import diarsid.librarian.tests.setup.transactional.TransactionalRollbackTestForServerSetup;

import static java.time.LocalDateTime.now;

import static diarsid.librarian.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.BEFORE;

public class EntriesSearchByWordTest extends TransactionalRollbackTestForServerSetup {

    static Labels labels = CORE.store().labels();
    static Words words;
    static EntriesSearchByWord entriesSearchByWord;

    EntriesResult entriesResult;

    @BeforeAll
    public static void setUp() {
        words = new WordsImpl(JDBC, UUID::randomUUID);
        entriesSearchByWord = new EntriesSearchByWord(JDBC, UUID::randomUUID);
    }

    private static Entry.Word word(String word) {
        return words.findBy(USER, word).orElseThrow();
    }

    private static List<Entry.Label> labels(String... labelNames) {
        return labels.getOrSave(USER, labelNames);
    }

    private void results(List<Entry> entries) {
        entriesResult = new EntriesResult(entries);
    }

    @Test
    public void findByLabel() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev");
        results(entriesSearchByWord.findBy(word, ANY_OF, labels));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByLabelBefore() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev");
        results(entriesSearchByWord.findBy(word, ANY_OF, labels, BEFORE, now()));
    }

    @Test
    public void findByLabelAfterOrEqual() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev");
        results(entriesSearchByWord.findBy(word, ANY_OF, labels, AFTER_OR_EQUAL, now().minusYears(1)));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByAnyOf() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "books");
        results(entriesSearchByWord.findBy(word, ANY_OF, labels));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByAnyOfLabelsBefore() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "books");
        results(entriesSearchByWord.findBy(word, ANY_OF, labels, BEFORE, now()));
        entriesResult.expect().someEntries();
    }
    
    @Test
    public void findByAnyOfLabelsAfterOrEqual() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "books");
        results(entriesSearchByWord.findBy(word, ANY_OF, labels, AFTER_OR_EQUAL, now().minusYears(1)));
        entriesResult.expect().someEntries();
    }
    
    @Test
    public void findByBefore() {
        Entry.Word word = word("tools");
        results(entriesSearchByWord.findBy(word, BEFORE, now()));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByAfterOrEqual() {
        Entry.Word word = word("tools");
        results(entriesSearchByWord.findBy(word, AFTER_OR_EQUAL, now().minusYears(1)));
        entriesResult.expect().someEntries();
    }
    
    @Test
    public void findByNotLabel() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev");
        results(entriesSearchByWord.findBy(word, NONE_OF, labels));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByNotLabelBefore() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev");
        results(entriesSearchByWord.findBy(word, NONE_OF, labels, BEFORE, now()));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByNotLabelAfterOrEqual() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev");
        results(entriesSearchByWord.findBy(word, NONE_OF, labels, AFTER_OR_EQUAL, now().minusYears(1)));
        entriesResult.expect().someEntries();}

    @Test
    public void findByNoneOf() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "servers");
        results(entriesSearchByWord.findBy(word, NONE_OF, labels));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByNoneOfLabelsBefore() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "servers");
        results(entriesSearchByWord.findBy(word, NONE_OF, labels, BEFORE, now()));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByNoneOfLabelsAfterOrEqual() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "servers");
        results(entriesSearchByWord.findBy(word, NONE_OF, labels, AFTER_OR_EQUAL, now().minusYears(1)));
        entriesResult.expect().someEntries();
    }
    
    @Test
    public void findByAllOf() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "servers");
        results(entriesSearchByWord.findBy(word, ALL_OF, labels));
        entriesResult.expect().someEntries();
    }
    
    @Test
    public void findByAllOfLabelsBefore() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "servers");
        results(entriesSearchByWord.findBy(word, ALL_OF, labels, BEFORE, now()));
        entriesResult.expect().someEntries();
    }

    @Test
    public void findByAllOfLabelsAfterOrEqual() {
        Entry.Word word = word("tools");
        List<Entry.Label> labels = labels("dev", "servers");
        results(entriesSearchByWord.findBy(word, ALL_OF, labels, AFTER_OR_EQUAL, now().minusYears(1)));
        entriesResult.expect().someEntries();
    }
}
