package diarsid.search;

import diarsid.jdbc.JdbcTransactionFactory;
import diarsid.jdbc.JdbcTransactionFactoryBuilder;
import diarsid.search.api.Core;
import diarsid.search.api.interaction.UserInteraction;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.support.strings.CharactersCount;
import diarsid.tests.db.embedded.h2.H2TestDataBase;
import diarsid.tests.db.embedded.h2.JdbcConnectionsSourceTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;

import static diarsid.search.api.model.Storable.State.STORED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class Testx {

    static Core core;
    static User user;

    @BeforeClass
    public static void setUp() throws Exception {

    }

    @Test
    public void test_1() {
        Entry entry = core.store().save(user, "Lord of the Rings by J.R.R Tolkien");
        assertThat(entry.state(), equalTo(STORED));

//        core.search().findAllBy(user, "lortjrrtolk");
    }

    @Test
    public void test_2() {
        Entry.Label label = core.store().getOrSave(user, "Darrell");
        assertThat(label.state(), equalTo(STORED));

        Entry entry = core.store().save(user, "Gerald Darrell - Three tickets to Adventure");
        assertThat(entry.state(), equalTo(STORED));

        boolean added = core.store().addLabels(entry, label);
        assertThat(added, equalTo(TRUE));
    }

    public static void main(String... args) {
        CharactersCount count = new CharactersCount();
        count.calculateIn("lortjrrtolk");
        count.forEach((c, i) -> System.out.println(c + " " + i));
    }
}
