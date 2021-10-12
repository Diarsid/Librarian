package diarsid.librarian.impl.logic.impl;

import java.util.List;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.librarian.api.interaction.UserChoice;
import diarsid.librarian.api.interaction.UserInteraction;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.UsersLocking;

public class UserInteractionLockingWrapper implements UserInteraction {

    private final Jdbc jdbc;
    private final UserInteraction userInteraction;
    private final UsersLocking locking;

    public UserInteractionLockingWrapper(UserInteraction userInteraction, Jdbc jdbc, UsersLocking locking) {
        this.jdbc = jdbc;
        this.userInteraction = userInteraction;
        this.locking = locking;
    }

    @Override
    public UserChoice askForChoice(User user, List<PatternToEntry> variants) {
        if ( jdbc.threadBinding().isBound() ) {
            return lockUserAndAskForChoice(user, variants);
        }
        else {
            return jdbc.doInTransaction(transaction -> {
                return lockUserAndAskForChoice(user, variants);
            });
        }
    }

    private UserChoice lockUserAndAskForChoice(User user, List<PatternToEntry> variants) {
        ThreadBoundJdbcTransaction transaction = jdbc.threadBinding().currentTransaction();
        transaction.sqlHistory().comment("user data locking transaction while interacting with user");
        transaction.doNotGuard();
        locking.lock(user);
        return userInteraction.askForChoice(user, variants);
    }
}
