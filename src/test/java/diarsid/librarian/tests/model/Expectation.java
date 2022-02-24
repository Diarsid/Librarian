package diarsid.librarian.tests.model;

import java.util.List;
import java.util.function.Consumer;

import diarsid.librarian.api.model.Entry;

class Expectation {

    private final Consumer<List<Entry>> entriesAssertion;
    private AssertionError assertionError;

    Expectation(Consumer<List<Entry>> entriesAssertion) {
        this.entriesAssertion = entriesAssertion;
    }

    void test(List<Entry> entries) {
        try {
            this.entriesAssertion.accept(entries);
        }
        catch (AssertionError e) {
            this.assertionError = e;
        }
    }

    boolean isFailed() {
        return this.assertionError != null;
    }

    AssertionError error() {
        return this.assertionError;
    }
}
