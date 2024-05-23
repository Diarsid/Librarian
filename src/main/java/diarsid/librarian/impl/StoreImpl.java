package diarsid.librarian.impl;

import diarsid.librarian.api.Entries;
import diarsid.librarian.api.LabeledEntries;
import diarsid.librarian.api.Labels;
import diarsid.librarian.api.Store;
import diarsid.librarian.api.Words;
import diarsid.librarian.api.WordsInEntries;

public class StoreImpl implements Store {

    private final Labels labels;
    private final Entries entries;
    private final LabeledEntries labeledEntries;
    private final Words words;
    private final WordsInEntries wordsInEntries;

    public StoreImpl(
            Labels labels,
            Entries entries,
            LabeledEntries labeledEntries,
            Words words,
            WordsInEntries wordsInEntries) {
        this.labels = labels;
        this.entries = entries;
        this.labeledEntries = labeledEntries;
        this.words = words;
        this.wordsInEntries = wordsInEntries;
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

    @Override
    public Words words() {
        return words;
    }

    @Override
    public WordsInEntries wordsInEntries() {
        return wordsInEntries;
    }
}
