package diarsid.search.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import diarsid.search.api.Properties;
import diarsid.search.api.Search;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;
import diarsid.search.api.model.User;
import diarsid.search.api.interaction.UserChoice;
import diarsid.search.api.interaction.UserInteraction;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.api.Patterns;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.api.search.SearchByChars;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static diarsid.search.impl.logic.impl.search.TimeDirection.AFTER_OR_EQUAL;
import static diarsid.search.impl.logic.impl.search.TimeDirection.BEFORE;

public class SearchImpl implements Search {

    private final Properties properties;
    private final SearchByChars searchByChars;
    private final Patterns patterns;
    private final PatternsToEntries patternsToEntries;
    private final Choices choices;
    private final UserProvidedResources implementations;
    private final Map<Entry, PatternToEntry> mismatchCleanableCache;

    public SearchImpl(
            Properties properties,
            SearchByChars searchByChars,
            Patterns patterns,
            PatternsToEntries patternsToEntries,
            Choices choices,
            UserProvidedResources implementations) {
        this.properties = properties;
        this.searchByChars = searchByChars;
        this.patterns = patterns;
        this.patternsToEntries = patternsToEntries;
        this.choices = choices;
        this.implementations = implementations;
        this.mismatchCleanableCache = new HashMap<>();
    }

    @Override
    public List<PatternToEntry> findAllBy(User user, String pattern, List<Entry.Label> labels) {
        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, pattern);
        List<PatternToEntry> foundRelations;

        if ( possiblyStoredPattern.isPresent() ) {
            Pattern storedPattern = possiblyStoredPattern.get();

            List<PatternToEntry> entriesOldRelations = patternsToEntries.findBy(storedPattern);

            List<Entry> matchingEntriesAfterPattern = searchByChars.findByChars(
                    user, storedPattern.string(), labels, AFTER_OR_EQUAL, storedPattern.time());

            if ( matchingEntriesAfterPattern.isEmpty() ) {
                foundRelations = emptyList();
            }
            else {
                List<PatternToEntry> entriesNewRelations = implementations
                        .algorithm()
                        .analyze(storedPattern, matchingEntriesAfterPattern);

                patternsToEntries.save(entriesNewRelations);

                foundRelations = union(entriesNewRelations, entriesOldRelations);
            }
        }
        else {
            Pattern newPattern = patterns.save(user, pattern);
            List<Entry> matchingEntries = searchByChars.findByChars(user, pattern, labels);

            if ( matchingEntries.isEmpty() ) {
                foundRelations = emptyList();
            }
            else {
                List<PatternToEntry> entriesNewRelations = implementations
                        .algorithm()
                        .analyze(newPattern, matchingEntries);

                patternsToEntries.save(entriesNewRelations);

                foundRelations = entriesNewRelations;
            }
        }

        return foundRelations;
    }

    @Override
    public Optional<PatternToEntry> findSingleBy(User user, String pattern, List<Entry.Label> labels) {
        LocalDateTime lastDomainModify = properties.lastModificationTime(user);

        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, pattern);

        if ( possiblyStoredPattern.isPresent() ) {
            Pattern storedPattern = possiblyStoredPattern.get();
            Optional<PatternToEntryChoice> possiblyStoredChoice = choices.findBy(storedPattern);

            if ( possiblyStoredChoice.isPresent() ) {
                PatternToEntryChoice storedChoice = possiblyStoredChoice.get();
                LocalDateTime storedChoiceTime = storedChoice.time();

                if ( lastDomainModify.isAfter(storedChoiceTime) ) {
                    List<Entry> freshEntries = searchByChars.findByChars(user, pattern, labels, AFTER_OR_EQUAL, storedChoiceTime);

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

                            List<Entry> oldEntries = searchByChars.findByChars(user, pattern, labels, BEFORE, storedChoiceTime);
                            List<PatternToEntry> oldEntriesRelations = patternsToEntries.findBy(storedPattern, oldEntries);
                            List<PatternToEntry> allRelations = union(freshEntriesNewRelations, freshEntriesStoredRelations, oldEntriesRelations);

                            UserInteraction userInteraction = implementations.userInteraction();
                            try {
                                UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                                switch ( userChoice.result() ) {
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
                                        throw new UnsupportedOperationException();
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
                List<Entry> entriesWithoutRelations = searchByChars
                        .findByChars(user, pattern, labels, AFTER_OR_EQUAL, storedPattern.time());

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

                    switch ( userChoice.result() ) {
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
                            throw new UnsupportedOperationException();
                        }
                    }
                }
                catch (Exception e) {
                    return Optional.empty();
                }
            }
        }
        else {
            Pattern newPattern = patterns.save(user, pattern);
            List<Entry> matchingEntries = searchByChars.findByChars(user, pattern, labels);

            List<PatternToEntry> entriesNewRelations = implementations
                    .algorithm()
                    .analyze(newPattern, matchingEntries);

            patternsToEntries.save(entriesNewRelations);

            UserInteraction userInteraction = implementations.userInteraction();
            try {
                UserChoice userChoice = userInteraction.askForChoice(user, entriesNewRelations);

                switch ( userChoice.result() ) {
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
                        throw new UnsupportedOperationException();
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
        // sort
        // find duplicates if any
        return newRelations;
    }

    private List<PatternToEntry> union(
            List<PatternToEntry> freshEntiresNewRelations,
            List<PatternToEntry> freshEntriesStoredRelations,
            List<PatternToEntry> oldEntriesRelations) {
        oldEntriesRelations.addAll(freshEntiresNewRelations);
        oldEntriesRelations.addAll(freshEntriesStoredRelations);
        // sort
        // find duplicates if any
        return oldEntriesRelations;
    }
}
