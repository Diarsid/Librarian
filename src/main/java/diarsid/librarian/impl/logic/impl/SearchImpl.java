package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import diarsid.librarian.api.Properties;
import diarsid.librarian.api.Search;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.PatternToEntryChoice;
import diarsid.librarian.api.model.User;
import diarsid.librarian.api.interaction.UserChoice;
import diarsid.librarian.api.interaction.UserInteraction;
import diarsid.librarian.impl.logic.api.Choices;
import diarsid.librarian.impl.logic.api.Patterns;
import diarsid.librarian.impl.logic.api.PatternsToEntries;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;

import static java.time.LocalDateTime.now;
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
    private final UserInteraction userInteraction;
    private final AlgorithmToModelAdapter algorithmAdapter;
    private final Map<Entry, PatternToEntry> mismatchCleanableCache;

    public SearchImpl(
            Properties properties,
            EntriesSearchByPattern entriesSearchByPattern,
            Patterns patterns,
            PatternsToEntries patternsToEntries,
            Choices choices,
            UserInteraction userInteraction,
            AlgorithmToModelAdapter algorithmAdapter) {
        this.properties = properties;
        this.entriesSearchByPattern = entriesSearchByPattern;
        this.patterns = patterns;
        this.patternsToEntries = patternsToEntries;
        this.choices = choices;
        this.userInteraction = userInteraction;
        this.algorithmAdapter = algorithmAdapter;
        this.mismatchCleanableCache = new HashMap<>();
    }

    @Override
    public List<PatternToEntry> findAllBy(User user, String patternString) {
        Optional<Pattern> possiblyStoredPattern = patterns.findBy(user, patternString);
        List<PatternToEntry> foundRelations;

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
                LocalDateTime now = now();
                List<PatternToEntry> newRelations = algorithmAdapter.analyze(pattern, matchingEntriesAfterPattern, now);

                patternsToEntries.save(newRelations);

                foundRelations = unite(newRelations, oldRelations);
            }
        }
        else {
            pattern = patterns.save(user, patternString);

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString);

            if ( matchingEntries.isEmpty() ) {
                foundRelations = emptyList();
            }
            else {
                List<PatternToEntry> newRelations = algorithmAdapter.analyze(pattern, matchingEntries, pattern.createdAt());

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
                LocalDateTime now = now();
                List<PatternToEntry> newRelations = algorithmAdapter.analyze(pattern, matchingEntriesAfterPattern, now);

                patternsToEntries.save(newRelations);

                foundRelations = unite(newRelations, oldRelations);
            }
        }
        else {
            Optional<Pattern> existingPattern = patterns.findBy(user, patternString);

            LocalDateTime newRelationsCreationTime;
            if ( existingPattern.isPresent() ) {
                pattern = existingPattern.get();
                newRelationsCreationTime = now();
            }
            else {
                pattern = patterns.save(user, patternString);
                newRelationsCreationTime = pattern.createdAt();
            }

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels);

            if ( matchingEntries.isEmpty() ) {
                foundRelations = emptyList();
            }
            else {
                List<PatternToEntry> newRelations = algorithmAdapter.analyze(pattern, matchingEntries, newRelationsCreationTime);

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
                            choices.actualize(storedChoice);
                            return Optional.of(storedChoice.patternToEntry());
                        }
                        else {
                            LocalDateTime now = now();
                            List<PatternToEntry> freshEntriesNewRelations = algorithmAdapter.analyze(storedPattern, freshEntriesWithoutRelation, now);
                            patternsToEntries.save(freshEntriesNewRelations);

                            List<Entry> oldEntries = entriesSearchByPattern.findBy(user, patternString, BEFORE, storedChoiceTime);
                            List<PatternToEntry> oldEntriesRelations = patternsToEntries.findBy(storedPattern, oldEntries);

                            List<PatternToEntry> allRelations = unite(
                                    freshEntriesNewRelations,
                                    freshEntriesStoredRelations,
                                    oldEntriesRelations);

                            try {
                                UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                                UserChoice.Decision userDecision = userChoice.decision();
                                switch ( userDecision ) {
                                    case DONE: {
                                        int chosenIndex = userChoice.chosenVariantIndex();
                                        PatternToEntry chosenRelation = allRelations.get(chosenIndex);
                                        if ( storedChoice.is(chosenRelation) ) {
                                            choices.actualize(storedChoice);
                                            return Optional.of(chosenRelation);
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
                    LocalDateTime now = now();
                    List<PatternToEntry> newRelations = algorithmAdapter.analyze(storedPattern, entriesWithoutRelations, now);
                    patternsToEntries.save(newRelations);

                    allRelations = unite(newRelations, storedRelations);
                }

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

            List<PatternToEntry> entriesNewRelations = algorithmAdapter.analyze(pattern, matchingEntries, pattern.createdAt());

            patternsToEntries.save(entriesNewRelations);

            try {
                UserChoice userChoice = userInteraction.askForChoice(user, entriesNewRelations);

                switch ( userChoice.decision() ) {
                    case DONE: {
                        int chosenIndex = userChoice.chosenVariantIndex();
                        PatternToEntry chosenRelation = entriesNewRelations.get(chosenIndex);
                        PatternToEntryChoice newChoice = choices.save(chosenRelation);
                        return Optional.of(newChoice.patternToEntry());
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
                            choices.actualize(storedChoice);
                            return Optional.of(storedChoice.patternToEntry());
                        }
                        else {
                            LocalDateTime now = now();
                            List<PatternToEntry> freshEntriesNewRelations = algorithmAdapter.analyze(storedPattern, freshEntriesWithoutRelation, now);
                            patternsToEntries.save(freshEntriesNewRelations);

                            List<Entry> oldEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels, BEFORE, storedChoiceTime);
                            List<PatternToEntry> oldEntriesRelations = patternsToEntries.findBy(storedPattern, oldEntries);

                            List<PatternToEntry> allRelations = unite(
                                    freshEntriesNewRelations,
                                    freshEntriesStoredRelations,
                                    oldEntriesRelations);

                            try {
                                UserChoice userChoice = userInteraction.askForChoice(user, allRelations);

                                UserChoice.Decision userDecision = userChoice.decision();
                                switch ( userDecision ) {
                                    case DONE: {
                                        int chosenIndex = userChoice.chosenVariantIndex();
                                        PatternToEntry chosenRelation = allRelations.get(chosenIndex);
                                        if ( storedChoice.is(chosenRelation) ) {
                                            choices.actualize(storedChoice);
                                            return Optional.of(chosenRelation);
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
                    LocalDateTime now = now();
                    List<PatternToEntry> newRelations = algorithmAdapter.analyze(storedPattern, entriesWithoutRelations, now);
                    patternsToEntries.save(newRelations);

                    allRelations = unite(newRelations, storedRelations);
                }

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
            Optional<Pattern> existingPattern = patterns.findBy(user, patternString);
            Pattern pattern;
            LocalDateTime newRelationsCreationTime;
            if ( existingPattern.isPresent() ) {
                pattern = existingPattern.get();
                newRelationsCreationTime = now();
            }
            else {
                pattern = patterns.save(user, patternString);
                newRelationsCreationTime = pattern.createdAt();
            }

            List<Entry> matchingEntries = entriesSearchByPattern.findBy(user, patternString, matching, labels);

            List<PatternToEntry> entriesNewRelations = algorithmAdapter.analyze(pattern, matchingEntries, newRelationsCreationTime);

            patternsToEntries.save(entriesNewRelations);

            try {
                UserChoice userChoice = userInteraction.askForChoice(user, entriesNewRelations);

                switch ( userChoice.decision() ) {
                    case DONE: {
                        int chosenIndex = userChoice.chosenVariantIndex();
                        PatternToEntry chosenRelation = entriesNewRelations.get(chosenIndex);
                        PatternToEntryChoice newChoice = choices.save(chosenRelation);
                        return Optional.of(newChoice.patternToEntry());
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

    private List<PatternToEntry> unite(
            List<PatternToEntry> newRelations,
            List<PatternToEntry> storedRelations) {
        newRelations.addAll(storedRelations);

        List<PatternToEntry> result = this.distinctAndSort(newRelations);

        return result;
    }

    private List<PatternToEntry> unite(
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
                .sorted(algorithmAdapter)
                .collect(toList());

        return output;
    }
}
