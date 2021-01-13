package diarsid.search.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.meta.Identifiable;
import diarsid.search.api.model.meta.UserScoped;

public interface PatternToEntryChoice extends Identifiable, UserScoped {

    PatternToEntry patternToEntry();

    default boolean is(PatternToEntry relation) {
        return this.patternToEntry().equals(relation);
    }

    PatternToEntryChoice changeTo(PatternToEntry newRelation);

    void actualize();

    LocalDateTime actualTime();

    @Override
    default UUID userUuid() {
        return this.patternToEntry().userUuid();
    }
}
