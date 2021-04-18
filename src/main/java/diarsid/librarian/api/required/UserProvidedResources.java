package diarsid.librarian.api.required;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.annotations.ImplementationRequired;
import diarsid.librarian.api.interaction.UserInteraction;

@ImplementationRequired
public interface UserProvidedResources {

    StringsComparisonAlgorithm algorithm();

    UserInteraction userInteraction();

    Jdbc jdbc();
}
