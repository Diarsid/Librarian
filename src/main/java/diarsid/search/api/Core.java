package diarsid.search.api;

import java.lang.reflect.Proxy;

import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.api.required.UserProvidedResources;
import diarsid.search.impl.ChoicesImpl;
import diarsid.search.impl.EntriesByCharsImpl;
import diarsid.search.impl.LabelsImpl;
import diarsid.search.impl.PatternsImpl;
import diarsid.search.impl.PatternsToEntriesImpl;
import diarsid.search.impl.PropertiesImpl;
import diarsid.search.impl.RealCore;
import diarsid.search.impl.SearchImpl;
import diarsid.search.impl.SearchTransactionalWrapper;
import diarsid.search.impl.StoreImpl;
import diarsid.search.impl.StoreTransactionalProxy;
import diarsid.search.impl.UsersImpl;
import diarsid.search.impl.UsersLockingImpl;
import diarsid.search.impl.api.internal.Choices;
import diarsid.search.impl.api.internal.EntriesByChars;
import diarsid.search.impl.api.internal.Labels;
import diarsid.search.impl.api.internal.Patterns;
import diarsid.search.impl.api.internal.PatternsToEntries;
import diarsid.search.impl.api.internal.UsersLocking;
import diarsid.search.impl.validity.StringsComparisonAlgorithmValidation;
import diarsid.jdbc.JdbcTransactionFactory;
import diarsid.jdbc.JdbcTransactionThreadBindings;

public interface Core {

    Users users();

    Store store();

    Search search();

    Properties properties();

    static Core buildWith(UserProvidedResources resources) {
        JdbcTransactionFactory transactionFactory = resources.transactionFactory();
        JdbcTransactionThreadBindings transactionThreadBindings = new JdbcTransactionThreadBindings(transactionFactory);
        StringsComparisonAlgorithm algorithm = resources.algorithm();

        StringsComparisonAlgorithmValidation algorithmValidation = new StringsComparisonAlgorithmValidation(() -> algorithm);
//        algorithmValidation.validate();

        UsersLocking usersLocking = new UsersLockingImpl(transactionThreadBindings);
        Labels labels = new LabelsImpl(transactionThreadBindings);
        Choices choices = new ChoicesImpl(transactionThreadBindings);
        PatternsToEntries patternsToEntries = new PatternsToEntriesImpl(transactionThreadBindings, algorithm);
        EntriesByChars entriesByChars = new EntriesByCharsImpl(transactionThreadBindings);
        Store store = new StoreImpl(transactionThreadBindings, patternsToEntries, labels, choices);
        Properties properties = new PropertiesImpl(transactionThreadBindings);
        Patterns patterns = new PatternsImpl(transactionThreadBindings);

        Users users = new UsersImpl(transactionFactory);

        Search search = new SearchImpl(properties, entriesByChars, patterns, patternsToEntries, choices, resources);
        Search txSearch =  new SearchTransactionalWrapper(transactionThreadBindings, search, usersLocking);
        Store txStore = (Store) Proxy.newProxyInstance(
                Core.class.getClassLoader(),
                new Class[] { Store.class },
                new StoreTransactionalProxy(store, usersLocking, transactionThreadBindings));

        Core core = new RealCore(users, txStore, txSearch, properties);

        return core;
    }
}
