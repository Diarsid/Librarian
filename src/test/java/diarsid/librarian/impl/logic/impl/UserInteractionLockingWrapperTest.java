package diarsid.librarian.impl.logic.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import diarsid.librarian.api.interaction.UserInteraction;
import diarsid.librarian.impl.interaction.RealUserChoice;
import diarsid.librarian.impl.logic.api.UsersLocking;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.tests.setup.TransactionalRollbackTestForEmbeddedSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.runAsync;

import static diarsid.librarian.api.Core.Mode.DEVELOPMENT;
import static diarsid.librarian.api.interaction.UserChoice.Decision.REJECTION;
import static diarsid.support.concurrency.threads.ThreadsUtil.sleepSafely;
import static org.assertj.core.api.Assertions.assertThat;

public class UserInteractionLockingWrapperTest extends TransactionalRollbackTestForEmbeddedSetup {

    static UsersLocking locking;
    static UserInteractionLockingWrapper interaction;

    @BeforeAll
    public static void setUp() {
        UuidSupplier uuidSupplier = new SequentialUuidTimeBasedMACSupplierImpl(new AtomicReference<>(DEVELOPMENT));

        locking = new UsersLockingImpl(JDBC, uuidSupplier);

        UserInteraction mock = (user, variants) -> {
            sleepSafely(1_000);
            return new RealUserChoice(REJECTION);
        };

        interaction = new UserInteractionLockingWrapper(mock, JDBC, locking);
    }

    @Test
    public void test() {
        AtomicLong interactEnds = new AtomicLong();
        AtomicLong competingAccessBegins = new AtomicLong();

        CompletableFuture interact = runAsync(() -> {
            interaction.askForChoice(USER, emptyList());
            interactEnds.set(currentTimeMillis());
        });

        sleepSafely(100);

        CompletableFuture competingAccess = runAsync(() -> {
            JDBC.doInTransaction((transaction) -> {
                locking.lock(USER);
            });
            competingAccessBegins.set(currentTimeMillis());
        });

        CompletableFuture.allOf(interact, competingAccess).join();

        assertThat(interactEnds.get()).isLessThanOrEqualTo(competingAccessBegins.get());
    }
}
