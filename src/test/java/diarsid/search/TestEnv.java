package diarsid.search;

import diarsid.search.api.Core;
import diarsid.search.api.model.User;
import org.junit.jupiter.api.BeforeAll;

public class TestEnv {

    static Core core;
    static User user;

    @BeforeAll
    public static void setUp() {
        core = TestCoreSetup.INSTANCE.core;
        user = TestCoreSetup.INSTANCE.user;
    }
}
