package diarsid.search;

import diarsid.search.api.Core;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.Boolean.TRUE;

import static diarsid.search.api.model.meta.Storable.State.STORED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class Testx {

    static Core core;
    static User user;

    @BeforeClass
    public static void setUp() throws Exception {
        core = CoreSetup.INSTANCE.core;
        user = CoreSetup.INSTANCE.user;
    }

    @Test
    public void test_1() {
        Entry entry = core.store().entries().save(user, "Lord of the Rings by J.R.R Tolkien");
        assertThat(entry.state(), equalTo(STORED));

//        core.search().findAllBy(user, "lortjrrtolk");
    }

    @Test
    public void test_2() {
        Entry.Label label = core.store().labels().getOrSave(user, "Darrell");
        assertThat(label.state(), equalTo(STORED));

        Entry entry = core.store().entries().save(user, "Gerald Darrell - Three tickets to Adventure");
        assertThat(entry.state(), equalTo(STORED));

        boolean added = core.store().entries().addLabels(entry, label);
        assertThat(added, equalTo(TRUE));
    }
}
