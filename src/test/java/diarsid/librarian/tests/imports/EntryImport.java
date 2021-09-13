package diarsid.librarian.tests.imports;

import diarsid.librarian.api.Store;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class EntryImport {

    static String text = "D:/DEV/1__Projects/UkrPoshta/UkrPoshta_API";


    public static void main(String[] args) {
        CoreTestSetup server = server();
        Store store = server.core.store();
        User user = server.user;
        Entry entry = store.entries().save(user, text);
        System.out.println(entry.uuid());
    }
}
