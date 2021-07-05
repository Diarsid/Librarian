package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import diarsid.librarian.api.Properties;
import diarsid.librarian.api.Search;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.PatternToEntryChoice;
import diarsid.librarian.api.model.User;
import diarsid.librarian.api.interaction.UserChoice;
import diarsid.librarian.api.interaction.UserInteraction;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.api.required.UserProvidedResources;
import diarsid.librarian.impl.logic.api.Choices;
import diarsid.librarian.impl.logic.api.Patterns;
import diarsid.librarian.impl.logic.api.PatternsToEntries;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.api.model.meta.UserScoped.checkMustBelongToUser;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.librarian.impl.logic.impl.search.TimeDirection.BEFORE;

public class SearchImpl implements Search {

    private final Properties properties;
    private final EntriesSearchByPattern entriesSearchByPattern;
    private final Patterns patterns;
    private final PatternsToEntries patternsToEntries;
    private final Choices choices;
    private final UserProvidedResources implementations;
    private final Map<Entry, PatternToEntry> mismatchCleanableCache;

    public SearchImpl(
            Properties properties,
            EntriesSearchByPattern entriesSearchByPattern,
            Patterns patterns,
            PatternsToEntries patternsToEntries,
            Choices choices,
            UserProvidedResources implementations) {
        this.properties = properties;
        this.entriesSearchByPattern = entriesSearchByPattern;
        this.patterns = patterns;
        this.patternsToEntries = patternsToEntries;
        this.choices = choices;
        this.implementations = implementations;
        this.mismatchCleanableCache = new HashMap<>();
    }

    @Override
    public List<PatternToEntry> findAllBy(User user, String patternString) {
        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, patternString);
        List<PatternToEntry> foundRelations;
        StringsComparisonAlgorithm algorithm = implementations.algorithm();

        Pattern pattern;
        if ( possiblyStoredPattern.isPresent() ) {
            pattern = possiblyStoredPattern.get();

            List<PatternToEntry> oldRelations = patternsToEntries.findBy(pattern);

            List<Entry> matchingEntriesAfterPattern = entriesSearchByPattern.findBy(
                    user, pattern.string(), AFTER_OR_EQUAL, pattern.createdAt());

            if ( matchingEntriesAfterPattern.isEmpty() ) {
                foundRelations = oldRelations;
            }
            else {
                List<PatternToEntry> newRelations = algorithm.analyze(pattern, matchingEntriesAfterPattern);

                patternsToEntries.save(newRelations);

                foundRelations = union(newRelations, oldRelations);
            }
        }
        else {
            pattern = patterns.save(user, patternString);

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString);

            if ( matchingEntries.isEmpty() ) {
                foundRelations = emptyList();
            }
            else {
                List<PatternToEntry> newRelations = algorithm.analyze(pattern, matchingEntries);

                patternsToEntries.save(newRelations);

                foundRelations = newRelations;
            }
        }

        return foundRelations;
    }

    @Override
    public List<PatternToEntry> findAllBy(
            User user, String patternString, Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return this.findAllBy(user, patternString);
        }

        checkMustBelongToUser(user, labels);

        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, patternString, matching, labels);
        List<PatternToEntry> foundRelations;
        StringsComparisonAlgorithm algorithm = implementations.algorithm();

        Pattern pattern;
        if ( possiblyStoredPattern.isPresent() ) {
            pattern = possiblyStoredPattern.get();

            List<PatternToEntry> oldRelations = patternsToEntries.findBy(pattern);

            List<Entry> matchingEntriesAfterPattern = entriesSearchByPattern.findBy(
                    user, pattern.string(), matching, labels, AFTER_OR_EQUAL, pattern.createdAt());

            if ( matchingEntriesAfterPattern.isEmpty() ) {
                foundRelations = oldRelations;
            }
            else {
                List<PatternToEntry> newRelations = algorithm.analyze(pattern, matchingEntriesAfterPattern);

                patternsToEntries.save(newRelations);

                foundRelations = union(newRelations, oldRelations);
            }
        }
        else {
            pattern = patterns
                    .findBy(user, patternString)
                    .orElseGet(() -> patterns.save(user, patternString));

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels);

            if ( matchingEntries.isEmpty() ) {
                foundRelations = emptyList();
            }
            else {
                List<PatternToEntry> newRelations = algorithm.analyze(pattern, matchingEntries);

                patternsToEntries.save(newRelations);

                foundRelations = newRelations;
            }
        }

        return foundRelations;
    }

    @Override
    public Optional<PatternToEntry> findSingleBy(User user, String patternString) {
        LocalDateTime lastDomainModify = properties.lastModificationTime(user);

        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, patternString);

        if ( possiblyStoredPattern.isPresent() ) {
            Pattern storedPattern = possiblyStoredPattern.get();
            Optional<PatternToEntryChoice> possiblyStoredChoice = choices.findBy(storedPattern);

            if ( possiblyStoredChoice.isPresent() ) {
                PatternToEntryChoice storedChoice = possiblyStoredChoice.get();
                LocalDateTime storedChoiceTime = storedChoice.createdAt();

                if ( lastDomainModify.isAfter(storedChoiceTime) ) {
                    List<Entry> freshEntries = entriesSearchByPattern.findBy(user, patternString, AFTER_OR_EQUAL, storedChoiceTime);

                    if ( freshEntries.isEmpty() ) {
                        return Optional.of(storedChoice.patternToEntry());
                    }
                    else {
                        List<PatternToEntry> freshEntriesStoredRelations = patternsToEntries.findBy(storedPattern, freshEntries);
                        List<Entry> freshEntriesWithoutRelation = findEntriesWithoutRelations(freshEntries, freshEntriesStoredRelations);

                        if ( freshEntriesWithoutRelation.isEmpty() ) {
                            choices.assertActual(storedChoice);
                            return Optional.of(storedChoice.patternToEntry());
                        }
                        else {
                            List<PatternToEntry> freshEntriesNewRelations = implementations
                                    .algorithm()
                                    .analyze(storedPattern, freshEntriesWithoutRelation);
                            patternsToEntries.save(freshEntriesNewRelations);

                            List<Entry> oldEntries = entriesSearchByPattern.findBy(user, patternString, BEFORE, storedChoiceTime);
                            List<PatternToEntry> oldEntriesRelations = patternsToEntries.findBy(storedPattern, oldEntries);
                            List<PatternToEntry> allRelations = union(freshEntriesNewRelations, freshEntriesStoredRelations, oldEntriesRelations);

                            UserInteraction userInteraction = implementations.userInteraction();
                            try {
                                UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                                UserChoice.Decision userDecision = userChoice.decision();
                                switch ( userDecision ) {
                                    case DONE: {
                                        int chosenIndex = userChoice.chosenVariantIndex();
                                        PatternToEntry chosenRelation = allRelations.get(chosenIndex);
                                        if ( storedChoice.is(chosenRelation) ) {
                                            choices.assertActual(storedChoice);
                                        }
                                        else {
                                            PatternToEntryChoice newChoice = choices.replace(storedChoice, chosenRelation);
                                            return Optional.of(newChoice.patternToEntry());
                                        }
                                    }
                                    case NOT_DONE:
                                        return Optional.empty();
                                    case REJECTION: {
                                        choices.remove(storedChoice);
                                        return Optional.empty();
                                    }
                                    default: {
                                        throw userDecision.unsupported();
                                    }
                                }
                            }
                            catch (Exception e) {
                                return Optional.empty();
                            }
                        }
                    }
                }
                else {
                    return Optional.of(storedChoice.patternToEntry());
                }
            }
            else {
                // pattern does not have associated choice
                List<PatternToEntry> storedRelations = patternsToEntries.findBy(storedPattern);
                List<Entry> entriesWithoutRelations = entriesSearchByPattern
                        .findBy(user, patternString, AFTER_OR_EQUAL, storedPattern.createdAt());

                List<PatternToEntry> allRelations;

                if ( entriesWithoutRelations.isEmpty() ) {
                    allRelations = storedRelations;
                }
                else {
                    StringsComparisonAlgorithm algorithm = implementations.algorithm();
                    List<PatternToEntry> newRelations = algorithm.analyze(storedPattern, entriesWithoutRelations);
                    patternsToEntries.save(newRelations);

                    allRelations = union(newRelations, storedRelations);
                }

                UserInteraction userInteraction = implementations.userInteraction();
                try {
                    UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                    UserChoice.Decision decision = userChoice.decision();
                    switch (decision) {
                        case DONE: {
                            int chosenIndex = userChoice.chosenVariantIndex();
                            PatternToEntry chosenRelation = allRelations.get(chosenIndex);
                            PatternToEntryChoice newChoice = choices.save(chosenRelation);
                            return Optional.of(newChoice.patternToEntry());
                        }
                        case NOT_DONE:
                        case REJECTION: {
                            return Optional.empty();
                        }
                        default: {
                            throw decision.unsupported();
                        }
                    }
                }
                catch (Exception e) {
                    return Optional.empty();
                }
            }
        }
        else {
            Pattern pattern = patterns.save(user, patternString);

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString);

            List<PatternToEntry> entriesNewRelations = implementations
                    .algorithm()
                    .analyze(pattern, matchingEntries);

            patternsToEntries.save(entriesNewRelations);

            UserInteraction userInteraction = implementations.userInteraction();
            try {
                UserChoice userChoice = userInteraction.askForChoice(user, entriesNewRelations);

                switch ( userChoice.decision() ) {
                    case DONE: {
                        int chosenIndex = userChoice.chosenVariantIndex();
                        PatternToEntry chosenRelation = entriesNewRelations.get(chosenIndex);
                        PatternToEntryChoice newChoice = choices.save(chosenRelation);
                        return Optional.of(chosenRelation);
                    }
                    case NOT_DONE:
                    case REJECTION: {
                        return Optional.empty();
                    }
                    default: {
                        throw userChoice.decision().unsupported();
                    }
                }
            }
            catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    @Override
    public Optional<PatternToEntry> findSingleBy(
            User user, String patternString, Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return this.findSingleBy(user, patternString);
        }

        checkMustBelongToUser(user, labels);

        LocalDateTime lastDomainModify = properties.lastModificationTime(user);

        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, patternString, matching, labels);

        if ( possiblyStoredPattern.isPresent() ) {
            Pattern storedPattern = possiblyStoredPattern.get();
            Optional<PatternToEntryChoice> possiblyStoredChoice = choices.findBy(storedPattern);

            if ( possiblyStoredChoice.isPresent() ) {
                PatternToEntryChoice storedChoice = possiblyStoredChoice.get();
                LocalDateTime storedChoiceTime = storedChoice.createdAt();

                if ( lastDomainModify.isAfter(storedChoiceTime) ) {
                    List<Entry> freshEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels, AFTER_OR_EQUAL, storedChoiceTime);

                    if ( freshEntries.isEmpty() ) {
                        return Optional.of(storedChoice.patternToEntry());
                    }
                    else {
                        List<PatternToEntry> freshEntriesStoredRelations = patternsToEntries.findBy(storedPattern, freshEntries);
                        List<Entry> freshEntriesWithoutRelation = findEntriesWithoutRelations(freshEntries, freshEntriesStoredRelations);

                        if ( freshEntriesWithoutRelation.isEmpty() ) {
                            choices.assertActual(storedChoice);
                            return Optional.of(storedChoice.patternToEntry());
                        }
                        else {
                            List<PatternToEntry> freshEntriesNewRelations = implementations
                                    .algorithm()
                                    .analyze(storedPattern, freshEntriesWithoutRelation);
                            patternsToEntries.save(freshEntriesNewRelations);

                            List<Entry> oldEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels, BEFORE, storedChoiceTime);
                            List<PatternToEntry> oldEntriesRelations = patternsToEntries.findBy(storedPattern, oldEntries);
                            List<PatternToEntry> allRelations = union(freshEntriesNewRelations, freshEntriesStoredRelations, oldEntriesRelations);

                            UserInteraction userInteraction = implementations.userInteraction();
                            try {
                                UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                                UserChoice.Decision userDecision = userChoice.decision();
                                switch ( userDecision ) {
                                    case DONE: {
                                        int chosenIndex = userChoice.chosenVariantIndex();
                                        PatternToEntry chosenRelation = allRelations.get(chosenIndex);
                                        if ( storedChoice.is(chosenRelation) ) {
                                            choices.assertActual(storedChoice);
                                        }
                                        else {
                                            PatternToEntryChoice newChoice = choices.replace(storedChoice, chosenRelation);
                                            return Optional.of(newChoice.patternToEntry());
                                        }
                                    }
                                    case NOT_DONE:
                                        return Optional.empty();
                                    case REJECTION: {
                                        choices.remove(storedChoice);
                                        return Optional.empty();
                                    }
                                    default: {
                                        throw userDecision.unsupported();
                                    }
                                }
                            }
                            catch (Exception e) {
                                return Optional.empty();
                            }
                        }
                    }
                }
                else {
                    return Optional.of(storedChoice.patternToEntry());
                }
            }
            else {
                // pattern does not have associated choice
                List<PatternToEntry> storedRelations = patternsToEntries.findBy(storedPattern);
                List<Entry> entriesWithoutRelations = entriesSearchByPattern
                        .findBy(user, patternString, matching, labels, AFTER_OR_EQUAL, storedPattern.createdAt());

                List<PatternToEntry> allRelations;

                if ( entriesWithoutRelations.isEmpty() ) {
                    allRelations = storedRelations;
                }
                else {
                    StringsComparisonAlgorithm algorithm = implementations.algorithm();
                    List<PatternToEntry> newRelations = algorithm.analyze(storedPattern, entriesWithoutRelations);
                    patternsToEntries.save(newRelations);

                    allRelations = union(newRelations, storedRelations);
                }

                UserInteraction userInteraction = implementations.userInteraction();
                try {
                    UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                    UserChoice.Decision decision = userChoice.decision();
                    switch (decision) {
                        case DONE: {
                            int chosenIndex = userChoice.chosenVariantIndex();
                            PatternToEntry chosenRelation = allRelations.get(chosenIndex);
                            PatternToEntryChoice newChoice = choices.save(chosenRelation);
                            return Optional.of(newChoice.patternToEntry());
                        }
                        case NOT_DONE:
                        case REJECTION: {
                            return Optional.empty();
                        }
                        default: {
                            throw decision.unsupported();
                        }
                    }
                }
                catch (Exception e) {
                    return Optional.empty();
                }
            }
        }
        else {
            Pattern pattern = patterns
                    .findBy(user, patternString)
                    .orElseGet(() -> patterns.save(user, patternString));

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels);

            List<PatternToEntry> entriesNewRelations = implementations
                    .algorithm()
                    .analyze(pattern, matchingEntries);

            patternsToEntries.save(entriesNewRelations);

            UserInteraction userInteraction = implementations.userInteraction();
            try {
                UserChoice userChoice = userInteraction.askForChoice(user, entriesNewRelations);

                switch ( userChoice.decision() ) {
                    case DONE: {
                        int chosenIndex = userChoice.chosenVariantIndex();
                        PatternToEntry chosenRelation = entriesNewRelations.get(chosenIndex);
                        PatternToEntryChoice newChoice = choices.save(chosenRelation);
                        return Optional.of(chosenRelation);
                    }
                    case NOT_DONE:
                    case REJECTION: {
                        return Optional.empty();
                    }
                    default: {
                        throw userChoice.decision().unsupported();
                    }
                }
            }
            catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    private List<Entry> findEntriesWithoutRelations(List<Entry> freshEntriesByChars, List<PatternToEntry> storedRelations) {
        storedRelations.forEach(this::cache);

        List<Entry> mismatch = freshEntriesByChars
                .stream()
                .filter(this::isNotCached)
                .collect(toList());

        mismatchCleanableCache.clear();

        return mismatch;
    }

    private void cache(PatternToEntry patternToEntry) {
        mismatchCleanableCache.put(patternToEntry.entry(), patternToEntry);
    }

    private boolean isNotCached(Entry entry) {
        return mismatchCleanableCache.containsKey(entry);
    }

    private List<PatternToEntry> union(
            List<PatternToEntry> newRelations,
            List<PatternToEntry> storedRelations) {
        newRelations.addAll(storedRelations);

        List<PatternToEntry> result = this.distinctAndSort(newRelations);

        return result;
    }

    private List<PatternToEntry> union(
            List<PatternToEntry> freshEntiresNewRelations,
            List<PatternToEntry> freshEntriesStoredRelations,
            List<PatternToEntry> oldEntriesRelations) {
        oldEntriesRelations.addAll(freshEntiresNewRelations);
        oldEntriesRelations.addAll(freshEntriesStoredRelations);

        List<PatternToEntry> result = this.distinctAndSort(oldEntriesRelations);

        return result;
    }

    private List<PatternToEntry> distinctAndSort(List<PatternToEntry> input) {
        List<PatternToEntry> output = input
                .stream()
                .distinct()
                .sorted(implementations.algorithm())
                .collect(toList());

        return output;
    }
}
