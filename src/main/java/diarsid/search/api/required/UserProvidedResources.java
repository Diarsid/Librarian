package diarsid.search.api.required;

import diarsid.search.api.annotations.ImplementationRequired;
import diarsid.search.api.interaction.UserInteraction;
import diarsid.jdbc.JdbcTransactionFactory;

@ImplementationRequired
public interface UserProvidedResources {

    StringsComparisonAlgorithm algorithm();

    UserInteraction userInteraction();

    JdbcTransactionFactory transactionFactory();
}
