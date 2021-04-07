package diarsid.search.impl;

import diarsid.search.api.Entries;
import diarsid.search.api.LabeledEntries;
import diarsid.search.api.Labels;
import diarsid.search.api.Store;

public class StoreImpl implements Store {

    private final Labels labels;
    private final Entries entries;
    private final LabeledEntries labeledEntries;

    public StoreImpl(Labels labels, Entries entries, LabeledEntries labeledEntries) {
        this.labels = labels;
        this.entries = entries;
        this.labeledEntries = labeledEntries;
    }

    @Override
    public Entries entries() {
        return entries;
    }

    @Override
    public Labels labels() {
        return labels;
    }

    @Override
    public LabeledEntries labeledEntries() {
        return labeledEntries;
    }
}
