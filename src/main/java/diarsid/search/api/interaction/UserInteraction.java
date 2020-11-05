package diarsid.search.api.interaction;

import java.util.List;

import diarsid.search.api.annotations.ImplementationRequired;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.User;

@ImplementationRequired
public interface UserInteraction {

    UserChoice askForChoice(User user, List<PatternToEntry> variants);
}
