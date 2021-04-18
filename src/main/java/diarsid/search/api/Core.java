package diarsid.search.api;

import java.util.concurrent.atomic.AtomicReference;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.TransactionAware;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.search.impl.StoreImpl;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.search.impl.logic.api.Patterns;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.api.UsersLocking;
import diarsid.search.impl.logic.api.Words;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.api.EntriesSearchByPattern;
import diarsid.search.impl.logic.impl.BehaviorImpl;
import diarsid.search.impl.logic.impl.ChoicesImpl;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.logic.impl.EntriesImpl;
import diarsid.search.impl.logic.impl.EntriesLabelsJoinTableImpl;
import diarsid.search.impl.logic.impl.LabeledEntriesImpl;
import diarsid.search.impl.logic.impl.LabelsImpl;
import diarsid.search.impl.logic.impl.PatternsImpl;
import diarsid.search.impl.logic.impl.PatternsToEntriesImpl;
import diarsid.search.impl.logic.impl.PropertiesImpl;
import diarsid.search.impl.logic.impl.UsersLockingImpl;
import diarsid.search.impl.logic.impl.UsersImpl;
import diarsid.search.impl.logic.impl.WordsImpl;
import diarsid.search.impl.logic.impl.WordsInEntriesImpl;
import diarsid.search.impl.logic.impl.jdbc.UsersTransactionalLocking;
import diarsid.search.impl.logic.impl.SearchImpl;
import diarsid.search.impl.logic.impl.search.EntriesSearchByPatternImpl;
import diarsid.search.impl.validity.StringsComparisonAlgorithmValidation;
import diarsid.support.objects.CommonEnum;

import static diarsid.jdbc.api.Jdbc.WhenNoTransactionThen.IF_NO_TRANSACTION_OPEN_NEW;

public interface Core {

    enum Mode implements CommonEnum<Mode> {
        PRODUCTION,
        DEVELOPMENT;

        public static Mode DEFAULT = PRODUCTION;
    }

    Behavior behavior();

    Users users();

    Store store();

    Search search();

    Properties properties();

    Core.Mode mode();

    void setMode(Core.Mode mode);

    static Core buildWith(UserProvidedResources resources) {
        Jdbc jdbc = resources.jdbc();
        StringsComparisonAlgorithm algorithm = resources.algorithm();

        StringsComparisonAlgorithmValidation algorithmValidation = new StringsComparisonAlgorithmValidation(() -> algorithm);
//        algorithmValidation.validate();

        Behavior behavior = new BehaviorImpl(jdbc);

        UsersLocking usersLocking = new UsersLockingImpl(jdbc);
        Labels labels = new LabelsImpl(jdbc);
        Choices choices = new ChoicesImpl(jdbc);

        PatternsToEntries patternsToEntries = new PatternsToEntriesImpl(jdbc, algorithm);

        Words words = new WordsImpl(jdbc);

        WordsInEntries wordsInEntries = new WordsInEntriesImpl(jdbc, words, behavior);

        EntriesLabelsJoinTable entriesLabelsJoinTable = new EntriesLabelsJoinTableImpl(jdbc);

        Entries entries = new EntriesImpl(
                jdbc,
                patternsToEntries,
                entriesLabelsJoinTable,
                choices,
                wordsInEntries,
                behavior);

        LabeledEntries labeledEntries = new LabeledEntriesImpl(jdbc, entries, labels, behavior, entriesLabelsJoinTable);

        Properties properties = new PropertiesImpl(jdbc);
        Patterns patterns = new PatternsImpl(jdbc);

        EntriesSearchByPattern entriesSearchByPattern = new EntriesSearchByPatternImpl(jdbc);

        Search search = new SearchImpl(properties, entriesSearchByPattern, patterns, patternsToEntries, choices, resources);

        AtomicReference<Core.Mode> coreMode = new AtomicReference<>(Mode.DEFAULT);

        TransactionAware usersLockingOnOpenAndJoin = new UsersTransactionalLocking(coreMode, usersLocking);

        Search txSearch = jdbc.createTransactionalProxyFor(
                Search.class, search, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Entries txEntries = jdbc.createTransactionalProxyFor(
                Entries.class, entries, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Labels txLabels = jdbc.createTransactionalProxyFor(
                Labels.class, labels, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        LabeledEntries txLabeledEntries = jdbc.createTransactionalProxyFor(
                LabeledEntries.class, labeledEntries, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Behavior txBehavior = jdbc.createTransactionalProxyFor(
                Behavior.class, behavior, usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Store store = new StoreImpl(txLabels, txEntries, txLabeledEntries);

        Users txUsers = jdbc.createTransactionalProxyFor(
                Users.class, new UsersImpl(jdbc), usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Core core = new CoreImpl(coreMode, jdbc, txUsers, store, txSearch, txBehavior, properties);

        return core;
    }
}
