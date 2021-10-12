package diarsid.librarian.api;

import java.util.concurrent.atomic.AtomicReference;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.TransactionAware;
import diarsid.librarian.api.interaction.UserInteraction;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.api.required.UserProvidedResources;
import diarsid.librarian.impl.StoreImpl;
import diarsid.librarian.impl.logic.api.Choices;
import diarsid.librarian.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;
import diarsid.librarian.impl.logic.api.Patterns;
import diarsid.librarian.impl.logic.api.PatternsToEntries;
import diarsid.librarian.impl.logic.api.UsersLocking;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.api.WordsInEntries;
import diarsid.librarian.impl.logic.impl.AlgorithmToModelAdapter;
import diarsid.librarian.impl.logic.impl.BehaviorImpl;
import diarsid.librarian.impl.logic.impl.ChoicesImpl;
import diarsid.librarian.impl.logic.impl.CoreImpl;
import diarsid.librarian.impl.logic.impl.EntriesImpl;
import diarsid.librarian.impl.logic.impl.EntriesLabelsJoinTableImpl;
import diarsid.librarian.impl.logic.impl.UserInteractionLockingWrapper;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByPatternImpl;
import diarsid.librarian.impl.logic.impl.LabeledEntriesImpl;
import diarsid.librarian.impl.logic.impl.LabelsImpl;
import diarsid.librarian.impl.logic.impl.PatternsImpl;
import diarsid.librarian.impl.logic.impl.PatternsToEntriesImpl;
import diarsid.librarian.impl.logic.impl.PropertiesImpl;
import diarsid.librarian.impl.logic.impl.SearchImpl;
import diarsid.librarian.impl.logic.impl.SequentialUuidTimeBasedMACSupplierImpl;
import diarsid.librarian.impl.logic.impl.UsersImpl;
import diarsid.librarian.impl.logic.impl.UsersLockingImpl;
import diarsid.librarian.impl.logic.impl.WordsImpl;
import diarsid.librarian.impl.logic.impl.WordsInEntriesImpl;
import diarsid.librarian.impl.logic.impl.jdbc.UsersTransactionalLocking;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByCharScan;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByWord;
import diarsid.librarian.impl.validity.StringsComparisonAlgorithmValidation;
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
        AtomicReference<Core.Mode> coreMode = new AtomicReference<>(Mode.DEFAULT);
        UuidSupplier uuidSupplier = new SequentialUuidTimeBasedMACSupplierImpl(coreMode);
        Jdbc jdbc = resources.jdbc();
        StringsComparisonAlgorithm algorithm = resources.algorithm();

        StringsComparisonAlgorithmValidation algorithmValidation = new StringsComparisonAlgorithmValidation(() -> algorithm);
        algorithmValidation.validate(
                "pattern",
                "pattrn",
                "paxtrn",
                "abcdfgh");
        AlgorithmToModelAdapter algorithmAdapter = new AlgorithmToModelAdapter(algorithm, uuidSupplier);

        Behavior behavior = new BehaviorImpl(jdbc, uuidSupplier);

        UsersLocking usersLocking = new UsersLockingImpl(jdbc, uuidSupplier);
        Labels labels = new LabelsImpl(jdbc, uuidSupplier);
        Choices choices = new ChoicesImpl(jdbc, uuidSupplier);

        PatternsToEntries patternsToEntries = new PatternsToEntriesImpl(jdbc, uuidSupplier, algorithmAdapter);

        Words words = new WordsImpl(jdbc, uuidSupplier);

        WordsInEntries wordsInEntries = new WordsInEntriesImpl(jdbc, uuidSupplier, words, behavior);

        EntriesLabelsJoinTable entriesLabelsJoinTable = new EntriesLabelsJoinTableImpl(jdbc, uuidSupplier);

        Entries entries = new EntriesImpl(
                jdbc,
                uuidSupplier,
                patternsToEntries,
                entriesLabelsJoinTable,
                choices,
                wordsInEntries,
                behavior);

        LabeledEntries labeledEntries = new LabeledEntriesImpl(jdbc, uuidSupplier, entries, labels, behavior, entriesLabelsJoinTable);

        Properties properties = new PropertiesImpl(jdbc, uuidSupplier);
        Patterns patterns = new PatternsImpl(jdbc, uuidSupplier);

        EntriesSearchByCharScan searchByCharScan = new EntriesSearchByCharScan(jdbc, uuidSupplier);
        EntriesSearchByWord searchByWord = new EntriesSearchByWord(jdbc, uuidSupplier);

        EntriesSearchByPattern entriesSearchByPattern = new EntriesSearchByPatternImpl(words, searchByCharScan, searchByWord);

        UserInteraction lockingUserInteraction = new UserInteractionLockingWrapper(resources.userInteraction(), jdbc, usersLocking);
        Search search = new SearchImpl(properties, entriesSearchByPattern, patterns, patternsToEntries, choices, lockingUserInteraction, algorithmAdapter);

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
                Users.class, new UsersImpl(jdbc, uuidSupplier), usersLockingOnOpenAndJoin, IF_NO_TRANSACTION_OPEN_NEW);

        Core core = new CoreImpl(coreMode, jdbc, txUsers, store, txSearch, txBehavior, properties);

        return core;
    }
}
