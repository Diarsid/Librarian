package diarsid.librarian.impl;

import diarsid.librarian.api.Entries;
import diarsid.librarian.api.LabeledEntries;
import diarsid.librarian.api.Labels;
import diarsid.librarian.api.Store;

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
