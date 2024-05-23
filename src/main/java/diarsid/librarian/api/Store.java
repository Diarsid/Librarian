package diarsid.librarian.api;

public interface Store {

    Entries entries();

    Labels labels();

    LabeledEntries labeledEntries();

    Words words();

    WordsInEntries wordsInEntries();

}
