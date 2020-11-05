package diarsid.search.impl;

import diarsid.search.api.Entries;
import diarsid.search.api.Labels;
import diarsid.search.api.Store;

public class StoreImpl implements Store {

    private final Labels labels;
    private final Entries entries;

    public StoreImpl(Labels labels, Entries entries) {
        this.labels = labels;
        this.entries = entries;
    }

    @Override
    public Entries entries() {
        return entries;
    }

    @Override
    public Labels labels() {
        return labels;
    }
}
