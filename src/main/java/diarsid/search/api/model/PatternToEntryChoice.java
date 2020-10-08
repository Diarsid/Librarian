package diarsid.search.api.model;

import java.time.LocalDateTime;

public interface PatternToEntryChoice extends Identifiable  {

    PatternToEntry patternToEntry();

    default boolean is(PatternToEntry relation) {
        return this.patternToEntry().equals(relation);
    }

    PatternToEntryChoice changeTo(PatternToEntry newRelation);

    void actualize();

    LocalDateTime actualTime();
}
