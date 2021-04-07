package diarsid.search.api;

public interface Store {

    Entries entries();

    Labels labels();

    LabeledEntries labeledEntries();

}
