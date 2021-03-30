package diarsid.search.api;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
import diarsid.search.impl.logic.api.UsersLocking;
import diarsid.search.impl.logic.api.Words;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.BehaviorImpl;
import diarsid.search.impl.logic.impl.ChoicesImpl;
import diarsid.search.impl.logic.impl.CoreImpl;
import diarsid.search.impl.logic.impl.EntriesImpl;
import diarsid.search.impl.logic.impl.LabelsImpl;
import diarsid.search.impl.logic.impl.PatternsImpl;
import diarsid.search.impl.logic.impl.PatternsToEntriesImpl;
import diarsid.search.impl.logic.impl.PropertiesImpl;
import diarsid.search.impl.logic.impl.UsersLockingImpl;
import diarsid.search.impl.logic.impl.UsersTransactionalImpl;
import diarsid.search.impl.logic.impl.WordsImpl;
import diarsid.search.impl.logic.impl.WordsInEntriesImpl;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForPatternToEntryAndLabels;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForPatternToEntryChoice;
import diarsid.search.impl.logic.impl.jdbc.RowOperationContext;
import diarsid.search.impl.logic.impl.search.v1.SearchImpl;
import diarsid.search.impl.logic.impl.search.v2.SearchByCharsImpl;
import diarsid.search.impl.validity.StringsComparisonAlgorithmValidation;
import diarsid.support.objects.CommonEnum;
import diarsid.support.objects.GuardedPool;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import static diarsid.jdbc.api.Jdbc.WhenNoTransactionThen.IF_NO_TRANSACTION_OPEN_NEW;
import static diarsid.search.api.Core.Mode.DEVELOPMENT;

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

        RowOperationContext.Setter contextSetter = new RowOperationContext.Setter(() -> jdbc.threadBinding().currentTransaction());

        GuardedPool<RowCollectorForPatternToEntryAndLabels> rowCollectorsForPatternsAndEntries = new GuardedPool<>(
                () -> new RowCollectorForPatternToEntryAndLabels("p.", "e.", "l.", "pe."),
                contextSetter::accept);

        GuardedPool<RowCollectorForPatternToEntryChoice> rowCollectorsForChoice = new GuardedPool<>(
                () -> new RowCollectorForPatternToEntryChoice("p.", "e.", "l.", "pe.", "c."),
                contextSetter::accept);

        Behavior behavior = new BehaviorImpl(jdbc);

        UsersLocking usersLocking = new UsersLockingImpl(jdbc);
        Labels labels = new LabelsImpl(jdbc);
        Choices choices = new ChoicesImpl(jdbc, rowCollectorsForChoice);

        PatternsToEntries patternsToEntries = new PatternsToEntriesImpl(
                jdbc, algorithm, rowCollectorsForPatternsAndEntries);

        Words words = new WordsImpl(jdbc);

        WordsInEntries wordsInEntries = new WordsInEntriesImpl(jdbc, words, behavior);

        Entries entries = new EntriesImpl(
                jdbc,
                patternsToEntries,
                choices,
                wordsInEntries,
                behavior);

        Properties properties = new PropertiesImpl(jdbc);
        Patterns patterns = new PatternsImpl(jdbc);

        SearchByChars searchByChars = new SearchByCharsImpl(jdbc);

        Search search = new SearchImpl(properties, searchByChars, patterns, patternsToEntries, choices, resources);

        AtomicReference<Core.Mode> coreMode = new AtomicReference<>(Mode.DEFAULT);

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
                Core.Mode mode = coreMode.get();

                if ( isNull(mode) || mode.equalTo(DEVELOPMENT) ) {
                    return;
                }

                UUID userUuid = null;

                Object arg0 = args[0];

                if ( arg0 instanceof User) {
                    userUuid = ((User) arg0).uuid();
                }
                else if ( arg0 instanceof UserScoped) {
                    userUuid = ((UserScoped) arg0).userUuid();
                }

                if ( nonNull(userUuid) ) {
                    usersLocking.lock(userUuid);
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

        Core core = new CoreImpl(coreMode, jdbc, users, store, txSearch, txBehavior, properties);

        return core;
    }
}
