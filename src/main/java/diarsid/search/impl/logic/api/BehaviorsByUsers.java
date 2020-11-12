package diarsid.search.impl.logic.api;

import diarsid.search.api.Behavior;
import diarsid.search.api.model.User;

public interface BehaviorsByUsers {

    Behavior getBehaviorFor(User user);
}
