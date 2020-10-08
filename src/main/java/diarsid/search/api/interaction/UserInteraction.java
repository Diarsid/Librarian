package diarsid.search.api.interaction;

import java.util.List;

import diarsid.search.api.annotations.ImplementationRequired;
import diarsid.search.api.model.PatternToEntry;

@ImplementationRequired
public interface UserInteraction {

    UserChoice askForChoice(List<PatternToEntry> variants);
}
