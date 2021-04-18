package diarsid.librarian.api;

import java.util.concurrent.atomic.AtomicReference;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.TransactionAware;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;
import diarsid.librarian.api.required.UserProvidedResources;
import diarsid.librarian.impl.StoreImpl;
import diarsid.librarian.impl.logic.api.Choices;
import diarsid.librarian.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;
import diarsid.librarian.impl.logic.api.Patterns;
import diarsid.librarian.impl.logic.api.PatternsToEntries;
import diarsid.librarian.impl.logic.api.UsersLocking;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.api.WordsInEntries;
import diarsid.librarian.impl.logic.impl.BehaviorImpl;
import diarsid.librarian.impl.logic.impl.ChoicesImpl;
import diarsid.librarian.impl.logic.impl.CoreImpl;
import diarsid.librarian.impl.logic.impl.EntriesImpl;
import diarsid.librarian.impl.logic.impl.EntriesLabelsJoinTableImpl;
import diarsid.librarian.impl.logic.impl.LabeledEntriesImpl;
import diarsid.librarian.impl.logic.impl.LabelsImpl;
import diarsid.librarian.impl.logic.impl.PatternsImpl;
import diarsid.librarian.impl.logic.impl.PatternsToEntriesImpl;
import diarsid.librarian.impl.logic.impl.PropertiesImpl;
import diarsid.librarian.impl.logic.impl.SearchImpl;
import diarsid.librarian.impl.logic.impl.UsersImpl;
import diarsid.librarian.impl.logic.impl.UsersLockingImpl;
import diarsid.librarian.impl.logic.impl.WordsImpl;
import diarsid.librarian.impl.logic.impl.WordsInEntriesImpl;
import diarsid.librarian.impl.logic.impl.jdbc.UsersTransactionalLocking;
import diarsid.librarian.impl.logic.impl.search.EntriesSearchByPatternImpl;
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
