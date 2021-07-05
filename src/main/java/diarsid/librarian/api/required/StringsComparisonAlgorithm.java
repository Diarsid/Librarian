package diarsid.librarian.api.required;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import diarsid.librarian.api.annotations.ImplementationRequired;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;

@ImplementationRequired
public interface StringsComparisonAlgorithm extends Comparator<PatternToEntry> {

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

    double badEstimateThreshold();

    boolean isBad(PatternToEntry relation);

    /* it is implied and crucial that a.pattern.equals(b.pattern) return TRUE */
    int compareTo(PatternToEntry a, PatternToEntry b);

    PatternToEntry analyze(Pattern pattern, Entry entry);

    default List<PatternToEntry> analyze(Pattern pattern, List<Entry> entries) {
        List<PatternToEntry> relations = new ArrayList<>();

        PatternToEntry relation;
        for ( Entry entry : entries ) {
            relation = this.analyze(pattern, entry);

            if ( this.isBad(relation) ) {
                continue;
            }

            relations.add(relation);
        }

        relations.sort(this);

        return relations;
    }
}
