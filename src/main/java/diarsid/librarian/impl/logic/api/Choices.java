package diarsid.librarian.impl.logic.api;

import java.util.Optional;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.PatternToEntryChoice;

public interface Choices {

    PatternToEntryChoice save(PatternToEntry newRelation);

    Optional<PatternToEntryChoice> findBy(Pattern pattern);

    PatternToEntryChoice replace(PatternToEntryChoice oldChoice, PatternToEntry newRelation);

//    List<PatternToEntryChoice> findBy(Entry entry);

    void remove(PatternToEntryChoice oldChoice);

    void removeAllBy(Entry entry);

    void actualize(PatternToEntryChoice choice);
}
