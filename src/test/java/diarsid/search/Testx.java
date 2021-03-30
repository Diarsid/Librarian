package diarsid.search;

import java.util.List;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.model.EntryJoinLabel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static diarsid.search.api.model.Entry.Label.ConditionBindable.ENTRY_CONTAINS_LABEL_IGNORE_CASE;
import static diarsid.search.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.support.model.Storable.State.STORED;
import static org.assertj.core.api.Assertions.assertThat;

public class Testx {

    static Core core;
    static User user;
    static Jdbc jdbc;

    @BeforeAll
    public static void setUp() {
        core = TestCoreSetup.INSTANCE.core;
        user = TestCoreSetup.INSTANCE.user;
        jdbc = TestCoreSetup.INSTANCE.jdbc;
    }

    @Test
    public void save() {
        Entry.Label label = core.store().labels().getOrSave(user, "books");
        Entry.Labeled labeledEntry = new EntryJoinLabel(null, null, null);

        labeledEntry.entry();
    }

    @Test
    public void test_1() {
        Entry entry = core.store().entries().findBy(user, "Lord of the Rings by J.R.R Tolkien").orElseThrow();
        assertThat(entry.state()).isEqualTo(STORED);

        Entry reloadedEntry = core.store().entries().reload(entry);

        assertThat(entry == reloadedEntry).isFalse();

        jdbc.doInTransaction(tx -> {
            Entry entry1 = core.store().entries().findBy(user, "Lord of the Rings by J.R.R Tolkien").orElseThrow();
            Entry entry1Reloaded = core.store().entries().reload(entry1);
            assertThat(entry1 == entry1Reloaded).isTrue();
        });
    }

    @Test
    public void getAllByLabel() {
        Entry.Label label = core.store().labels().getOrSave(user, "servers");

        List<Entry> entries = core.store().entries().findAllBy(user, label);
        int a = 5;
    }

    @Test
    public void getAllByLabels() {
        List<Entry.Label> labels = core.store().labels().getOrSave(user, "tools", "servers", "dev");

        List<Entry> entries = core.store().entries().findAllBy(user, ALL_OF, labels);
        int a = 5;
    }

    @Test
    public void test_2() {
        Entry.Label label = core.store().labels().getOrSave(user, "Darrell");
        assertThat(label.state()).isEqualTo(STORED);

        Entry entry = core.store().entries().save(user, "Gerald Darrell - Three tickets to Adventure");
        assertThat(entry.state()).isEqualTo(STORED);

        boolean added = core.store().entries().addLabel(entry, label);
        assertThat(added).isTrue();
    }

    @Test
    public void test_3() {
        List<Entry.Label> labels = core.store().labels().getOrSave(user, "tools", "servers", "dev");

        Entry entry = core.store().entries().save(user, "D:\\DEV\\3__Tools\\Servers\\Web_Servers\\Apache_Tomcat", labels);
        assertThat(entry.state()).isEqualTo(STORED);

        int a = 5;
    }

    @Test
    public void test_4() {
        Entry.Label label1 = core.store().labels().getOrSave(user, "other_path");
        Entry.Label label2 = core.store().labels().getOrSave(user, "custom_path");

        Entry entry = core.store().entries().save(user, "My/Path/To/Target");
        assertThat(entry.state()).isEqualTo(STORED);

        core.store().entries().addLabel(entry, label1);
        core.store().entries().addLabel(entry, label2);
    }

    @Disabled
    @Test
    public void test_4_api() {
        Entry.Label label1 = core.store().labels().getOrSave(user, "other_path");
        Entry.Label label2 = core.store().labels().getOrSave(user, "custom_path");

        Entry entry = core.store().entries().save(user, "My/Path/To/Target",
                label1.bindableIf(ENTRY_CONTAINS_LABEL_IGNORE_CASE),
                label2);
        assertThat(entry.state()).isEqualTo(STORED);

        core.store().entries().addLabel(entry, label1);
        core.store().entries().addLabel(entry, label2);
    }

    @Test
    public void test_w() {
        Entry.Label label1 = core.store().labels().getOrSave(user, "other_path");

        Entry entry = core.store().entries().save(user, "Path/To/My/Target");
        assertThat(entry.state()).isEqualTo(STORED);

        core.store().entries().addLabel(entry, label1);
    }

    @Test
    public void test_getByLabel() {
        Entry.Label label = core.store().labels().getOrSave(user, "servers");

        List<Entry> entries = core.store().entries().findAllBy(user, label);
        int a = 5;
    }
}
