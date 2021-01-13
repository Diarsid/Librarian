package diarsid.search;

import java.util.List;

import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.User;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static java.lang.Boolean.TRUE;

import static diarsid.search.api.model.Entry.Label.ConditionBindable.ENTRY_CONTAINS_LABEL_IGNORE_CASE;
import static diarsid.search.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.search.api.model.meta.Storable.State.STORED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class Testx {

    static Core core;
    static User user;

    @BeforeClass
    public static void setUp() {
        core = TestCoreSetup.INSTANCE.core;
        user = TestCoreSetup.INSTANCE.user;
    }

    @Test
    public void save() {
        List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "tolkien");
        core.store().entries().save(user, "Silmarillion by J.R.R Tolkien", labels);
        core.store().entries().save(user, "Hobbit by J.R.R Tolkien", labels);
        core.store().entries().save(user, "Lost Tales by J.R.R Tolkien", labels);
        core.store().entries().save(user, "Sigurd and Gudrun by J.R.R Tolkien", labels);
        core.store().entries().save(user, "Beren and Luthien by J.R.R Tolkien", labels);
        core.store().entries().save(user, "Childrens of Hurin by J.R.R Tolkien", labels);
    }

    @Test
    public void test_1() {
        List<Entry.Label> labels = core.store().labels().getOrSave(user, "books", "subpath");

        Entry entry = core.store().entries().save(user, "Lord of the Rings by J.R.R Tolkien", labels);
        assertThat(entry.state(), equalTo(STORED));

//        List<PatternToEntry> relations = core.search().findAllBy(user, "lortjrrtolk");
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
        assertThat(label.state(), equalTo(STORED));

        Entry entry = core.store().entries().save(user, "Gerald Darrell - Three tickets to Adventure");
        assertThat(entry.state(), equalTo(STORED));

        boolean added = core.store().entries().addLabel(entry, label);
        assertThat(added, equalTo(TRUE));
    }

    @Test
    public void test_3() {
        List<Entry.Label> labels = core.store().labels().getOrSave(user, "tools", "servers", "dev");

        Entry entry = core.store().entries().save(user, "D:\\DEV\\3__Tools\\Servers\\Web_Servers\\Apache_Tomcat", labels);
        assertThat(entry.state(), equalTo(STORED));

        int a = 5;
    }

    @Test
    public void test_4() {
        Entry.Label label1 = core.store().labels().getOrSave(user, "other_path");
        Entry.Label label2 = core.store().labels().getOrSave(user, "custom_path");

        Entry entry = core.store().entries().save(user, "My/Path/To/Target");
        assertThat(entry.state(), equalTo(STORED));

        core.store().entries().addLabel(entry, label1);
        core.store().entries().addLabel(entry, label2);
    }

    @Ignore
    @Test
    public void test_4_api() {
        Entry.Label label1 = core.store().labels().getOrSave(user, "other_path");
        Entry.Label label2 = core.store().labels().getOrSave(user, "custom_path");

        Entry entry = core.store().entries().save(user, "My/Path/To/Target",
                label1.bindableIf(ENTRY_CONTAINS_LABEL_IGNORE_CASE),
                label2);
        assertThat(entry.state(), equalTo(STORED));

        core.store().entries().addLabel(entry, label1);
        core.store().entries().addLabel(entry, label2);
    }

    @Test
    public void test_w() {
        Entry.Label label1 = core.store().labels().getOrSave(user, "other_path");

        Entry entry = core.store().entries().save(user, "Path/To/My/Target");
        assertThat(entry.state(), equalTo(STORED));

        core.store().entries().addLabel(entry, label1);
    }

    @Test
    public void test_getByLabel() {
        Entry.Label label = core.store().labels().getOrSave(user, "servers");

        List<Entry> entries = core.store().entries().findAllBy(user, label);
        int a = 5;
    }
}
