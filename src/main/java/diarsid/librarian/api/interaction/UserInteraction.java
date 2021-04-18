package diarsid.librarian.api.interaction;

import java.util.List;

import diarsid.librarian.api.annotations.ImplementationRequired;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;

@ImplementationRequired
public interface UserInteraction {

    UserChoice askForChoice(User user, List<PatternToEntry> variants);
}
