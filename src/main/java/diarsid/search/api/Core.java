package diarsid.search.api;

import java.lang.reflect.Method;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.TransactionAware;
import diarsid.search.api.model.User;
import diarsid.search.api.model.meta.UserScoped;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.search.impl.StoreImpl;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.api.Patterns;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.api.Phrases;
import diarsid.search.impl.logic.api.PhrasesInEntries;
import diarsid.search.impl.logic.api.UsersLocking;
import diarsid.search.impl.logic.api.Words;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.api.WordsInPhrases;
import diarsid.search.impl.logic.api.chars.CharsInEntries;
import diarsid.search.impl.logic.api.chars.CharsInPhrases;
import diarsid.search.impl.logic.api.chars.CharsInWords;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInEntries;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInPhrases;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInWords;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.BehaviorImpl;
import diarsid.search.impl.logic.impl.ChoicesImpl;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.logic.impl.EntriesImpl;
import diarsid.search.impl.logic.impl.PatternsImpl;
import diarsid.search.impl.logic.impl.PatternsToEntriesImpl;
import diarsid.search.impl.logic.impl.PhrasesImpl;
import diarsid.search.impl.logic.impl.PhrasesInEntriesImpl;
import diarsid.search.impl.logic.impl.PropertiesImpl;
import diarsid.search.impl.logic.impl.UsersLockingImpl;
import diarsid.search.impl.logic.impl.UsersTransactionalImpl;
import diarsid.search.impl.logic.impl.WordsImpl;
import diarsid.search.impl.logic.impl.WordsInEntriesImpl;
import diarsid.search.impl.logic.impl.WordsInPhrasesImpl;
import diarsid.search.impl.logic.impl.chars.CharsInEntriesImpl;
import diarsid.search.impl.logic.impl.chars.CharsInPhrasesImpl;
import diarsid.search.impl.logic.impl.chars.CharsInWordsImpl;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForPatternToEntryAndLabels;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForPatternToEntryChoice;
import diarsid.search.impl.logic.impl.labels.LabelsImpl;
import diarsid.search.impl.logic.impl.labels.LabelsToCharsInEntriesImpl;
import diarsid.search.impl.logic.impl.labels.LabelsToCharsInPhrasesImpl;
import diarsid.search.impl.logic.impl.labels.LabelsToCharsInWordsImpl;
import diarsid.search.impl.logic.impl.search.SearchByCharsImpl;
import diarsid.search.impl.logic.impl.search.SearchImpl;
import diarsid.search.impl.validity.StringsComparisonAlgorithmValidation;
import diarsid.support.objects.GuardedPool;

import static java.util.Objects.nonNull;

import static diarsid.jdbc.api.Jdbc.WhenNoTransactionThen.IF_NO_TRANSACTION_OPEN_NEW;


public interface Core {

    Behavior behavior();

    Users users();

    Store store();

    Search search();

    Properties properties();

    static Core buildWith(UserProvidedResources resources) {
        Jdbc jdbc = resources.jdbc();
        StringsComparisonAlgorithm algorithm = resources.algorithm();

        StringsComparisonAlgorithmValidation algorithmValidation = new StringsComparisonAlgorithmValidation(() -> algorithm);
//        algorithmValidation.validate();

        GuardedPool<RowCollectorForPatternToEntryAndLabels> rowCollectorsForPatternsAndEntries = new GuardedPool<>(
                () -> new RowCollectorForPatternToEntryAndLabels("p.", "e.", "l.", "pe."));
        GuardedPool<RowCollectorForPatternToEntryChoice> rowCollectorsForChoice = new GuardedPool<>(
                () -> new RowCollectorForPatternToEntryChoice("p.", "e.", "l.", "pe.", "c."));

        Behavior behavior = new BehaviorImpl(jdbc);

        UsersLocking usersLocking = new UsersLockingImpl(jdbc);
        Labels labels = new LabelsImpl(jdbc);
        Choices choices = new ChoicesImpl(jdbc, rowCollectorsForChoice);

        PatternsToEntries patternsToEntries = new PatternsToEntriesImpl(
                jdbc, algorithm, rowCollectorsForPatternsAndEntries);

        CharsInEntries charsInEntries = new CharsInEntriesImpl(jdbc);
        CharsInPhrases charsInPhrases = new CharsInPhrasesImpl(jdbc);
        CharsInWords charsInWords = new CharsInWordsImpl(jdbc);

        Words words = new WordsImpl(jdbc, charsInWords);
        WordsInPhrases wordsInPhrases = new WordsInPhrasesImpl(jdbc);
        Phrases phrases = new PhrasesImpl(jdbc, wordsInPhrases, charsInPhrases);

        WordsInEntries wordsInEntries = new WordsInEntriesImpl(jdbc, words, behavior);
        PhrasesInEntries phrasesInEntries = new PhrasesInEntriesImpl(jdbc, phrases);

        LabelsToCharsInEntries labelsToCharsInEntries = new LabelsToCharsInEntriesImpl(jdbc);
        LabelsToCharsInWords labelsToCharsInWords = new LabelsToCharsInWordsImpl(jdbc);
        LabelsToCharsInPhrases labelsToCharsInPhrases = new LabelsToCharsInPhrasesImpl(jdbc);

        Entries entries = new EntriesImpl(
                jdbc,
                patternsToEntries,
                choices,
                charsInEntries,
                labelsToCharsInEntries,
                labelsToCharsInWords,
                labelsToCharsInPhrases,
                wordsInEntries,
                phrasesInEntries,
                behavior);

        Properties properties = new PropertiesImpl(jdbc);
        Patterns patterns = new PatternsImpl(jdbc);

        SearchByChars searchByChars = new SearchByCharsImpl(jdbc);

        Search search = new SearchImpl(properties, searchByChars, patterns, patternsToEntries, choices, resources);

        TransactionAware usersLockingOnOpenAndJoin = new TransactionAware() {

            @Override
            public void afterTransactionOpenFor(Method method, Object[] args) {
                this.tryLockOnUserUuidWhenFoundIn(args);
            }

            @Override
            public void beforeTransactionJoinFor(Method method, Object[] args) {
                this.tryLockOnUserUuidWhenFoundIn(args);
            }

            private void tryLockOnUserUuidWhenFoundIn(Object[] args) {
                UUID userUuid = null;

                Object arg0 = args[0];

                if ( arg0 instanceof User) {
                    userUuid = ((User) arg0).uuid();
                }
                else if ( arg0 instanceof UserScoped) {
                    userUuid = ((UserScoped) arg0).userUuid();
                }

                if ( nonNull(userUuid) ) {
//                    usersLocking.lock(userUuid);
                }
            }
        };

        Search txSearch = jdbc.createTransactionalProxyFor(
                Search.class, search, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Entries txEntries = jdbc.createTransactionalProxyFor(
                Entries.class, entries, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Labels txLabels = jdbc.createTransactionalProxyFor(
                Labels.class, labels, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Behavior txBehavior = jdbc.createTransactionalProxyFor(
                Behavior.class, behavior, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Store store = new StoreImpl(txLabels, txEntries);

        Users users = new UsersTransactionalImpl(jdbc);

        Core core = new CoreImpl(jdbc, users, store, txSearch, txBehavior, properties);

        return core;
    }
}
