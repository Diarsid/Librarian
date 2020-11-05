package diarsid.search.api.model;

import java.time.LocalDateTime;

import diarsid.search.api.model.meta.Identifiable;

public interface PatternToEntryChoice extends Identifiable {

    PatternToEntry patternToEntry();

    default boolean is(PatternToEntry relation) {
        return this.patternToEntry().equals(relation);
    }

    PatternToEntryChoice changeTo(PatternToEntry newRelation);

    void actualize();

    LocalDateTime actualTime();
}
