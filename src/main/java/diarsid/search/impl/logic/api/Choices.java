package diarsid.search.impl.logic.api;

import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;

public interface Choices {

    PatternToEntryChoice save(PatternToEntry newRelation);

    Optional<PatternToEntryChoice> findBy(Pattern pattern);

    PatternToEntryChoice replace(PatternToEntryChoice oldChoice, PatternToEntry newRelation);

//    List<PatternToEntryChoice> findBy(Entry entry);

    void remove(PatternToEntryChoice oldChoice);

    void removeAllBy(Entry entry);

    void assertActual(PatternToEntryChoice choice);
}
