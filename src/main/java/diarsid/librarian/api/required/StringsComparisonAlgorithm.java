package diarsid.librarian.api.required;

import diarsid.librarian.api.annotations.ImplementationRequired;
import diarsid.support.model.versioning.Version;

@ImplementationRequired
public interface StringsComparisonAlgorithm {

    String name();

    Version version();

    default String canonicalName() {
        return StringsComparisonAlgorithm.class.getSimpleName() + "["
                + this.name() + "."
                + this.version().fullName
                + ']';
    }

    int compare(float weight1, float weight2);

    boolean isBad(float weight);

    default boolean isGood(float weight) {
        return ! this.isBad(weight);
    }

    float analyze(String pattern, String entry);
}
