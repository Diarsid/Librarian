package diarsid.librarian.impl.validity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.AlgorithmToModelAdapter;
import diarsid.librarian.impl.logic.impl.SequentialUuidTimeBasedMACSupplierImpl;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.RealPattern;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

import static diarsid.librarian.api.Core.Mode.DEVELOPMENT;

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
        AtomicReference<Core.Mode> coreMode = new AtomicReference<>(DEVELOPMENT);
        UuidSupplier uuidSupplier = new SequentialUuidTimeBasedMACSupplierImpl(coreMode);

        StringsComparisonAlgorithm algorithm = this.algorithmSupplier.get();
        AlgorithmToModelAdapter algorithmAdapter = new AlgorithmToModelAdapter(algorithm, uuidSupplier);

        Pattern pattern = new RealPattern(randomUUID(), patternString, userUuid);

        Entry betterEntry = new RealEntry(randomUUID(), betterEntryString, userUuid);
        Entry worseEntry = new RealEntry(randomUUID(), worseEntryString, userUuid);
        Entry badEntry = new RealEntry(randomUUID(), badEntryString, userUuid);

        List<PatternToEntry> relations = algorithmAdapter.analyze(pattern, List.of(
                badEntry,
                worseEntry,
                betterEntry),
                now());

        boolean badEntryAbsent = relations
                .stream()
                .map(PatternToEntry::entryString)
                .noneMatch(badEntryString::equals);

        if ( ! badEntryAbsent ) {
            throw new IllegalStateException();
        }

        PatternToEntry better = relations.get(0);
        PatternToEntry worse = relations.get(1);

        if ( ! better.entryString().equals(betterEntryString) ) {
            throw new IllegalStateException();
        }

        if ( ! worse.entryString().equals(worseEntryString) ) {
            throw new IllegalStateException();
        }

        if ( ! better.patternString().equals(patternString) ) {
            throw new IllegalStateException();
        }

        if ( ! worse.patternString().equals(patternString) ) {
            throw new IllegalStateException();
        }

        int comparison = algorithmAdapter.compare(better, worse);
    }
}
