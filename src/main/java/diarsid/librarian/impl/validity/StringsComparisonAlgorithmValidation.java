package diarsid.librarian.impl.validity;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.RealPattern;

import static java.util.UUID.randomUUID;

public class StringsComparisonAlgorithmValidation {

    private final Supplier<StringsComparisonAlgorithm> algorithmSupplier;

    public StringsComparisonAlgorithmValidation(Supplier<StringsComparisonAlgorithm> algorithmSupplier) {
        this.algorithmSupplier = algorithmSupplier;
    }

    public void validate(
            String patternString,
            String betterEntryString,
            String worseEntryString,
            String badEntryString) {
        UUID userUuid = randomUUID();

        StringsComparisonAlgorithm algorithm = this.algorithmSupplier.get();

        Pattern pattern = new RealPattern(patternString, userUuid);

        Entry betterEntry = new RealEntry(betterEntryString, userUuid);
        Entry worseEntry = new RealEntry(worseEntryString, userUuid);
        Entry badEntry = new RealEntry(badEntryString, userUuid);

        List<PatternToEntry> relations = algorithm.analyze(pattern, List.of(badEntry, worseEntry, betterEntry));

        boolean noBadEntry = relations
                .stream()
                .map(PatternToEntry::entryString)
                .noneMatch(badEntryString::equals);

        PatternToEntry better = relations.get(0);
        PatternToEntry worse = relations.get(1);

        better.entryString().equals(betterEntryString);
        worse.entryString().equals(worseEntryString);

        better.patternString().equals(patternString);
        worse.patternString().equals(patternString);

        int comparison = algorithm.compare(better, worse);


    }
}