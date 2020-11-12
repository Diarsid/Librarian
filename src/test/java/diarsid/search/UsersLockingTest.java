package diarsid.search;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import diarsid.search.api.model.User;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.logic.impl.UsersLockingImpl;
import diarsid.search.impl.logic.api.UsersLocking;
import org.junit.Test;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.CompletableFuture.runAsync;

import static diarsid.support.concurrency.threads.ThreadsUtil.sleepSafely;
import static junit.framework.TestCase.fail;

public class UsersLockingTest {


    CoreImpl core = TestCoreSetup.INSTANCE.core;
    UsersLocking locking = new UsersLockingImpl(core.transactionThreadBindings());
    User user = TestCoreSetup.INSTANCE.user;

    @Test
    public void test() {
        AtomicReference<Long> transact1Begin = new AtomicReference<>();
        AtomicReference<Long> transact2Begin = new AtomicReference<>();
        AtomicReference<Long> transact1BeforeUnlock = new AtomicReference<>();
        AtomicReference<Long> transact2AfterLock = new AtomicReference<>();
        AtomicReference<Long> transact2End = new AtomicReference<>();

        CompletableFuture asyncTransact1 = runAsync(() -> {
            core.transactionThreadBindings().beginTransaction();
            core.transactionThreadBindings().currentTransaction().logHistoryAfterCommit();
            transact1Begin.set(currentTimeMillis());
            locking.lock(user);
            sleepSafely(500);
            transact1BeforeUnlock.set(currentTimeMillis());
            core.transactionThreadBindings().commitTransaction();
        });

        CompletableFuture asyncTransact2 = runAsync(() -> {
            sleepSafely(100);
            core.transactionThreadBindings().beginTransaction();
            core.transactionThreadBindings().currentTransaction().logHistoryAfterCommit();
            transact2Begin.set(currentTimeMillis());
            locking.lock(user);
            transact2AfterLock.set(currentTimeMillis());
            core.transactionThreadBindings().commitTransaction();
            transact2End.set(currentTimeMillis());
        });

        CompletableFuture.allOf(asyncTransact1, asyncTransact2).join();

        if ( transact1Begin.get() > transact2Begin.get() ) {
            fail();
        }

        if ( transact2AfterLock.get() < transact1BeforeUnlock.get() ) {
            fail();
        }
    }
}
