package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.model.RealPatternToEntry;

public class AlgorithmToModelAdapter implements Comparator<PatternToEntry> {

    private final StringsComparisonAlgorithm algorithm;
    private final UuidSupplier uuidSupplier;

    public AlgorithmToModelAdapter(StringsComparisonAlgorithm algorithm, UuidSupplier uuidSupplier) {
        this.algorithm = algorithm;
        this.uuidSupplier = uuidSupplier;
    }

    public boolean isBad(PatternToEntry relation) {
        return algorithm.isBad(relation.weight());
    }

    @Override
    /* it is implied and crucial that a.pattern.equals(b.pattern) return TRUE */
    public int compare(PatternToEntry a, PatternToEntry b) {
        if ( a.hasSamePattern(b) ) {
            return Float.compare(a.weight(), b.weight());
        }

        throw new IllegalArgumentException("Given relations has different patterns - cannot be compared!");
    }

    public PatternToEntry analyze(Pattern pattern, Entry entry, LocalDateTime time) {
        float weight = algorithm.analyze(pattern.string(), entry.string());
        PatternToEntry relation = new RealPatternToEntry(
                uuidSupplier.nextRandomUuid(), time, entry, pattern, algorithm, weight);
        return relation;
    }

    public List<PatternToEntry> analyze(Pattern pattern, List<Entry> entries, LocalDateTime time) {
        List<PatternToEntry> relations = new ArrayList<>();

        PatternToEntry relation;
        for ( Entry entry : entries ) {
            relation = this.analyze(pattern, entry, time);

            if ( this.isBad(relation) ) {
                continue;
            }

            relations.add(relation);
        }

        relations.sort(this);

        return relations;
    }
}
