package diarsid.librarian.tests.setup.transactional;

import diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton;

public class TransactionalRollbackForEmbeddedSetup
        extends TransactionalRollback {

    public TransactionalRollbackForEmbeddedSetup() {
        super(CoreTestSetupStaticSingleton.embedded().jdbc);
    }
}
