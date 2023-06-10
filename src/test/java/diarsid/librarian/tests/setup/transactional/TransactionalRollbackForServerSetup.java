package diarsid.librarian.tests.setup.transactional;

import diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton;

public class TransactionalRollbackForServerSetup
        extends TransactionalRollback {

    public TransactionalRollbackForServerSetup() {
        super(CoreTestSetupStaticSingleton.server().jdbc);
    }
}
