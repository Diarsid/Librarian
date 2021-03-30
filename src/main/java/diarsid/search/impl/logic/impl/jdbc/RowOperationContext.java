package diarsid.search.impl.logic.impl.jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;

import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;

public class RowOperationContext {

    public static class Setter implements Consumer<ContextBoundRowOperation> {

        private final Supplier<ThreadBoundJdbcTransaction> transactionSource;

        public Setter(Supplier<ThreadBoundJdbcTransaction> transactionSource) {
            this.transactionSource = transactionSource;
        }

        public Setter(ThreadBoundTransactional transactional) {
            this.transactionSource = transactional::currentTransaction;
        }

        @Override
        public void accept(ContextBoundRowOperation rowOperation) {
            ThreadBoundJdbcTransaction transaction = transactionSource.get();
            rowOperation.context().fill(transaction.uuid(), transaction.created());
        }
    }

    private List objects;

    public RowOperationContext() {
        this.objects = new ArrayList();
    }

    public void fill(Object o1) {
        if ( isNotEmpty(objects) ) {
            throw new IllegalStateException();
        }

        objects.add(o1);
    }

    public void fill(Object o1, Object o2) {
        if ( isNotEmpty(objects) ) {
            throw new IllegalStateException();
        }

        objects.add(o1);
        objects.add(o2);
    }

    public void fill(Object o1, Object o2, Object o3) {
        if ( isNotEmpty(objects) ) {
            throw new IllegalStateException();
        }

        objects.add(o1);
        objects.add(o2);
        objects.add(o3);
    }

    public void fill(Object o1, Object o2, Object o3, Object o4) {
        if ( isNotEmpty(objects) ) {
            throw new IllegalStateException();
        }

        objects.add(o1);
        objects.add(o2);
        objects.add(o3);
        objects.add(o4);
    }

    public void fill(Object... newObjects) {
        if ( isNotEmpty(objects) ) {
            throw new IllegalStateException();
        }

        for ( Object o : newObjects ) {
            objects.add(o);
        }
    }

    public void clear() {
        if ( objects.isEmpty() ) {
            throw new IllegalStateException();
        }

        objects.clear();
    }

    public <T> T get(int i, Class<T> type) {
        if ( objects.isEmpty() ) {
            throw new IllegalStateException();
        }

        return (T) objects.get(i);
    }

    public <T> T get(Class<T> type) {
        if ( objects.isEmpty() ) {
            throw new IllegalStateException();
        }

        Object obj;
        for (int i = 0; i < objects.size(); i++) {
            obj = objects.get(i);
            if ( obj.getClass().equals(type) ) {
                return (T) obj;
            }
        }

        throw new IllegalArgumentException();
    }
}
