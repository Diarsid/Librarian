package diarsid.search.api.required;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.annotations.ImplementationRequired;
import diarsid.search.api.interaction.UserInteraction;

@ImplementationRequired
public interface UserProvidedResources {

    StringsComparisonAlgorithm algorithm();

    UserInteraction userInteraction();

    Jdbc jdbc();
}
