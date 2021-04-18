package diarsid.librarian.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.tests.EmbeddedTransactionalRollbackTest;
import org.junit.jupiter.api.Test;

import static diarsid.librarian.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.support.model.Joined.distinctLeftsOf;
import static diarsid.support.model.Joined.distinctRightsOf;
import static diarsid.support.model.Joined.mapLeftKeys;
import static diarsid.support.model.Storable.State.STORED;
import static org.assertj.core.api.Assertions.assertThat;

public class StoreTest extends EmbeddedTransactionalRollbackTest {

    @Test
    public void getOrSave_getByUserAndName_findBy() {
        String name = "books";
        Entry.Label label = CORE.store().labels().getOrSave(USER, name);
        assertThat(label.state()).isEqualTo(STORED);
        assertThat(label.name()).isEqualTo(name);
        assertThat(label.belongsTo(USER)).isTrue();

        JDBC.threadBinding().isBound();
        Entry.Label stored1 = CORE.store().labels().getBy(USER, label.uuid());
        assertThat(stored1).isEqualTo(label);

        Optional<Entry.Label> found = CORE.store().labels().findBy(USER, name);
        assertThat(found).isPresent();
        assertThat(found).hasValue(label);
    }

    @Test
    public void reload() {
        Entry entry = CORE.store().entries().save(USER, "Lord of the Rings by J.R.R Tolkien");
        assertThat(entry.state()).isEqualTo(STORED);

        Entry reloadedEntry = CORE.store().entries().reload(entry);

        assertThat(entry == reloadedEntry).isFalse();
        assertThat(entry).isEqualTo(reloadedEntry);
    }

    @Test
    public void addLabels_findByLabels() {
        Entry entry = CORE.store().entries().save(USER, "My servers");
        Entry.Label serverLabel = CORE.store().labels().getOrSave(USER, "servers");
        List<Entry.Label> labels2 = CORE.store().labels().getOrSave(USER, "tools", "dev");
        List<Entry.Label> labelsAll = CORE.store().labels().getOrSave(USER, "tools", "servers", "dev");

        int allLabelsQty = labelsAll.size();

        List<Entry.Labeled> allLabeled = CORE.store().labeledEntries().add(entry, labelsAll);

        assertThat(allLabeled).hasSize(allLabelsQty);

        List<Entry.Labeled> labeledFoundByEntry = CORE.store().labeledEntries().findAllBy(entry);
        assertThat(labeledFoundByEntry).hasSize(allLabelsQty);
        assertThat(labeledFoundByEntry).containsAll(allLabeled);

        List<Entry.Labeled> labeledFoundBy1Label = CORE.store().labeledEntries().findAllBy(serverLabel);
        assertThat(labeledFoundBy1Label).hasSize(allLabelsQty);

        List<Entry.Labeled> labeledFoundBy2Label = CORE.store().labeledEntries().findAllBy(ALL_OF, labels2);
        assertThat(labeledFoundBy2Label).hasSize(allLabelsQty);

        assertThat(labeledFoundByEntry).isEqualTo(labeledFoundBy1Label);
        assertThat(labeledFoundByEntry).isEqualTo(labeledFoundBy2Label);

        List<Entry.Label> labelsFound = distinctRightsOf(labeledFoundByEntry);
        assertThat(labelsFound).containsAll(labelsAll);

        Map<Entry, List<Entry.Label>> entriesAndLabels = mapLeftKeys(labeledFoundByEntry);
        assertThat(entriesAndLabels).hasSize(1);
        assertThat(entriesAndLabels.get(entry)).hasSize(allLabelsQty);
        assertThat(entriesAndLabels.get(entry)).isEqualTo(labelsAll);
    }

    @Test
    public void test_entry_findByName() {
        String name = "entry_one";
        Entry entry = CORE.store().entries().save(USER, name);
        assertThat(entry.state()).isEqualTo(STORED);

        Optional<Entry> entryFound = CORE.store().entries().findBy(USER, name);
        assertThat(entryFound).isPresent();
        assertThat(entryFound).hasValue(entry);
    }

    @Test
    public void test_assingManyEntriesToLabel() {
        String baseName = "entry_";
        AtomicInteger counter = new AtomicInteger(1);
        Entry entry1 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry2 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry3 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry4control = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());

        Entry.Label labelForE1 = CORE.store().labels().getOrSave(USER, "label_for_entry_1");
        Entry.Label labelForE2 = CORE.store().labels().getOrSave(USER, "label_for_entry_2");
        Entry.Label commonLabel = CORE.store().labels().getOrSave(USER, "label");
        Entry.Label label4Control = CORE.store().labels().getOrSave(USER, "label_for_control_entry");

        CORE.store().labeledEntries().add(entry1, labelForE1);
        CORE.store().labeledEntries().add(entry2, labelForE2);
        CORE.store().labeledEntries().add(entry4control, label4Control);

        List<Entry> entries = List.of(entry1, entry2, entry3);

        List<Entry.Labeled> newLabeleds = CORE.store().labeledEntries().add(entries, commonLabel);
        assertThat(newLabeleds).hasSize(3);

        List<Entry.Labeled> allLabeleds = CORE.store().labeledEntries().findAllBy(commonLabel);
        assertThat(allLabeleds).hasSize(5);
        assertThat(allLabeleds).containsAll(newLabeleds);

        Map<Entry, List<Entry.Label>> entriesAndLabels = mapLeftKeys(allLabeleds);
        assertThat(entriesAndLabels).hasSize(3);

        List<Entry.Labeled> labeledsByAllEntries = CORE.store().labeledEntries().findAllBy(entries);
        assertThat(labeledsByAllEntries).containsExactlyInAnyOrderElementsOf(allLabeleds);
    }

    @Test
    public void test_findAllBy_NONEOF_ANYOF_ALLOF() {
        String baseName = "entry_";
        AtomicInteger counter = new AtomicInteger(1);
        Entry entry1 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry2 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry3 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry4control = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());

        Entry.Label labelForE1 = CORE.store().labels().getOrSave(USER, "label_for_entry_1");
        Entry.Label labelForE2 = CORE.store().labels().getOrSave(USER, "label_for_entry_2");
        Entry.Label labelForAnyOfTest = CORE.store().labels().getOrSave(USER, "label_for_ANY_OF");
        Entry.Label commonLabel = CORE.store().labels().getOrSave(USER, "label");
        Entry.Label label4Control = CORE.store().labels().getOrSave(USER, "label_for_control_entry");

        CORE.store().labeledEntries().add(entry1, labelForE1);
        CORE.store().labeledEntries().add(entry1, labelForAnyOfTest);
        CORE.store().labeledEntries().add(entry2, labelForE2);
        CORE.store().labeledEntries().add(entry2, labelForAnyOfTest);
        CORE.store().labeledEntries().add(entry4control, label4Control);
        List<Entry> entries = List.of(entry1, entry2, entry3);

        List<Entry.Labeled> newLabeleds = CORE.store().labeledEntries().add(entries, commonLabel);
        List<Entry.Labeled> allLabeleds = CORE.store().labeledEntries().findAllBy(commonLabel);

        List<Entry.Labeled> labeledByNotControlLabel = CORE.store().labeledEntries().findAllBy(NONE_OF, label4Control);
        assertThat(labeledByNotControlLabel).containsExactlyInAnyOrderElementsOf(allLabeleds);

        List<Entry.Labeled> labeledOf1And3 = CORE.store().labeledEntries().findAllBy(entry1, entry3);
        List<Entry.Labeled> labeledByNot2Labels = CORE.store().labeledEntries().findAllBy(NONE_OF, label4Control, labelForE2);
        assertThat(labeledByNot2Labels).containsExactlyInAnyOrderElementsOf(labeledOf1And3);

        List<Entry.Labeled> labeledByAllOf2Labels = CORE.store().labeledEntries().findAllBy(ALL_OF, commonLabel, labelForE2);
        assertThat(labeledByAllOf2Labels).hasSize(3);
        List<Entry> entriesOfLabeled2 = distinctLeftsOf(labeledByAllOf2Labels);
        assertThat(entriesOfLabeled2).hasSize(1);
        assertThat(entriesOfLabeled2.get(0)).isEqualTo(entry2);

        List<Entry.Labeled> labeledByAnyOf2Labels = CORE.store().labeledEntries().findAllBy(ANY_OF, labelForAnyOfTest, labelForE2);
        List<Entry.Labeled> expected = new ArrayList<>();
        expected.addAll(CORE.store().labeledEntries().findAllBy(entry1));
        expected.addAll(CORE.store().labeledEntries().findAllBy(entry2));
        assertThat(expected).hasSize(6);
        assertThat(labeledByAnyOf2Labels).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void addLabels_count() {
        String baseName = "entry_";
        AtomicInteger counter = new AtomicInteger(1);
        Entry entry1 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry2 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry3 = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());
        Entry entry4control = CORE.store().entries().save(USER, baseName + counter.getAndIncrement());

        Entry.Label labelForE1 = CORE.store().labels().getOrSave(USER, "label_for_entry_1");
        Entry.Label labelForE2 = CORE.store().labels().getOrSave(USER, "label_for_entry_2");
        Entry.Label commonLabel = CORE.store().labels().getOrSave(USER, "label");
        Entry.Label label4Control = CORE.store().labels().getOrSave(USER, "label_for_control_entry");

        CORE.store().labeledEntries().add(entry1, labelForE1);
        CORE.store().labeledEntries().add(entry2, labelForE2);
        CORE.store().labeledEntries().add(entry4control, label4Control);

        List<Entry> entries = List.of(entry1, entry2, entry3);

        CORE.store().labeledEntries().add(entries, commonLabel);

        long countByOneLabel = CORE.store().labeledEntries().countEntriesBy(commonLabel);
        assertThat(countByOneLabel).isEqualTo(entries.size());

        long countByNotLabel = CORE.store().labeledEntries().countEntriesBy(NONE_OF, label4Control);
        assertThat(countByNotLabel).isEqualTo(entries.size());

        long countByAllOf2Labels = CORE.store().labeledEntries().countEntriesBy(ALL_OF, labelForE2, commonLabel);
        assertThat(countByAllOf2Labels).isEqualTo(1);

        long countByAnyOf2Labels = CORE.store().labeledEntries().countEntriesBy(ANY_OF, label4Control, commonLabel);
        assertThat(countByAnyOf2Labels).isEqualTo(4);

        long countByNoneOf2Labels = CORE.store().labeledEntries().countEntriesBy(NONE_OF, labelForE2, label4Control);
        assertThat(countByNoneOf2Labels).isEqualTo(2);
    }

}
