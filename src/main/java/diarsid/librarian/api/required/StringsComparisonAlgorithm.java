package diarsid.librarian.api.required;

import java.util.Optional;

import diarsid.librarian.api.annotations.ImplementationRequired;

@ImplementationRequired
public interface StringsComparisonAlgorithm {

    interface Version extends Comparable<StringsComparisonAlgorithm.Version> {

        int number();

        Optional<String> name();

        default boolean isAfter(StringsComparisonAlgorithm.Version other) {
            return this.number() > other.number();
        }

        default boolean isBefore(StringsComparisonAlgorithm.Version other) {
            return this.number() < other.number();
        }

        default boolean isSame(StringsComparisonAlgorithm.Version other) {
            return this.number() == other.number();
        }
    }

    String name();

    Version version();

    default String canonicalName() {
        return StringsComparisonAlgorithm.class.getSimpleName() + "."
                + this.name() + "."
                + this.version().number() + "."
                + this.version().name().orElse("[unnamed]");
    }

    int compare(float weight1, float weight2);

    boolean isBad(float weight);

    default boolean isGood(float weight) {
        return ! this.isBad(weight);
    }

    float analyze(String pattern, String entry);
}
