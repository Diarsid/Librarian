package diarsid.librarian.tests.model;

import java.util.List;

import diarsid.librarian.api.model.Entry;

public class EntriesResult {

    final List<Entry> resultingEntries;
    final Expectations expectations;

    public EntriesResult(List<Entry> resultingEntries) {
        this.resultingEntries = resultingEntries;
        this.expectations = new Expectations(this.resultingEntries);
    }

    public int size() {
        return resultingEntries.size();
    }

    public boolean hasAny() {
        return resultingEntries.size() > 0;
    }

    public List<Entry> list() {
        return resultingEntries;
    }

    public Expectations expect() {
        return this.expectations;
    }
}
