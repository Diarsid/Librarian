package diarsid.search.api;

import java.lang.reflect.Proxy;

import diarsid.jdbc.JdbcFactory;
import diarsid.jdbc.JdbcTransactionThreadBindings;
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
import diarsid.search.impl.logic.impl.ChoicesImpl;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.logic.impl.PatternsImpl;
import diarsid.search.impl.logic.impl.PatternsToEntriesImpl;
import diarsid.search.impl.logic.impl.PhrasesImpl;
import diarsid.search.impl.logic.impl.PhrasesInEntriesImpl;
import diarsid.search.impl.logic.impl.PropertiesImpl;
import diarsid.search.impl.logic.impl.EntriesImpl;
import diarsid.search.impl.logic.impl.UsersImpl;
import diarsid.search.impl.logic.impl.UsersLockingImpl;
import diarsid.search.impl.logic.impl.WordsImpl;
import diarsid.search.impl.logic.impl.WordsInEntriesImpl;
import diarsid.search.impl.logic.impl.WordsInPhrasesImpl;
import diarsid.search.impl.logic.impl.chars.CharsInEntriesImpl;
import diarsid.search.impl.logic.impl.chars.CharsInPhrasesImpl;
import diarsid.search.impl.logic.impl.chars.CharsInWordsImpl;
import diarsid.search.impl.logic.impl.labels.LabelsImpl;
import diarsid.search.impl.logic.impl.labels.LabelsToCharsInEntriesImpl;
import diarsid.search.impl.logic.impl.labels.LabelsToCharsInPhrasesImpl;
import diarsid.search.impl.logic.impl.labels.LabelsToCharsInWordsImpl;
import diarsid.search.impl.logic.impl.search.SearchByCharsImpl;
import diarsid.search.impl.logic.impl.search.SearchImpl;
import diarsid.search.impl.logic.impl.support.SearchTransactionalWrapper;
import diarsid.search.impl.logic.impl.support.TransactionalProxy;
import diarsid.search.impl.validity.StringsComparisonAlgorithmValidation;

public interface Core {

    Users users();

    Store store();

    Search search();

    Properties properties();

    static Core buildWith(UserProvidedResources resources) {
        JdbcFactory transactionFactory = resources.jdbcFactory();
        JdbcTransactionThreadBindings transactionThreadBindings = new JdbcTransactionThreadBindings(transactionFactory);
        StringsComparisonAlgorithm algorithm = resources.algorithm();

        StringsComparisonAlgorithmValidation algorithmValidation = new StringsComparisonAlgorithmValidation(() -> algorithm);
//        algorithmValidation.validate();

        UsersLocking usersLocking = new UsersLockingImpl(transactionThreadBindings);
        Labels labels = new LabelsImpl(transactionThreadBindings);
        Choices choices = new ChoicesImpl(transactionThreadBindings);
        PatternsToEntries patternsToEntries = new PatternsToEntriesImpl(transactionThreadBindings, algorithm);

        CharsInEntries charsInEntries = new CharsInEntriesImpl(transactionThreadBindings);
        CharsInPhrases charsInPhrases = new CharsInPhrasesImpl(transactionThreadBindings);
        CharsInWords charsInWords = new CharsInWordsImpl(transactionThreadBindings);

        Words words = new WordsImpl(transactionThreadBindings, charsInWords);
        WordsInPhrases wordsInPhrases = new WordsInPhrasesImpl(transactionThreadBindings);
        Phrases phrases = new PhrasesImpl(transactionThreadBindings, wordsInPhrases, charsInPhrases);

        WordsInEntries wordsInEntries = new WordsInEntriesImpl(transactionThreadBindings, words);
        PhrasesInEntries phrasesInEntries = new PhrasesInEntriesImpl(transactionThreadBindings, phrases);

        LabelsToCharsInEntries labelsToCharsInEntries = new LabelsToCharsInEntriesImpl(transactionThreadBindings);
        LabelsToCharsInWords labelsToCharsInWords = new LabelsToCharsInWordsImpl(transactionThreadBindings);
        LabelsToCharsInPhrases labelsToCharsInPhrases = new LabelsToCharsInPhrasesImpl(transactionThreadBindings);

        Entries entries = new EntriesImpl(
                transactionThreadBindings,
                patternsToEntries,
                choices,
                charsInEntries,
                labelsToCharsInEntries,
                labelsToCharsInWords,
                labelsToCharsInPhrases,
                wordsInEntries,
                phrasesInEntries);

        Properties properties = new PropertiesImpl(transactionThreadBindings);
        Patterns patterns = new PatternsImpl(transactionThreadBindings);

        Users users = new UsersImpl(transactionFactory);

        SearchByChars searchByChars = new SearchByCharsImpl(transactionThreadBindings);

        Search search = new SearchImpl(properties, searchByChars, patterns, patternsToEntries, choices, resources);
        Search txSearch =  new SearchTransactionalWrapper(transactionThreadBindings, search, usersLocking);

        Entries txEntries = (Entries) Proxy.newProxyInstance(
                Core.class.getClassLoader(),
                new Class[] { Entries.class },
                new TransactionalProxy(entries, usersLocking, transactionThreadBindings));

        Labels txLabels = (Labels) Proxy.newProxyInstance(
                Core.class.getClassLoader(),
                new Class[] { Labels.class },
                new TransactionalProxy(labels, usersLocking, transactionThreadBindings));

        Store store = new StoreImpl(txLabels, txEntries);

        Core core = new CoreImpl(transactionThreadBindings, users, store, txSearch, properties);

        return core;
    }
}
