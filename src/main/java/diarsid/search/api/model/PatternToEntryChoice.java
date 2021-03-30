package diarsid.search.api.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.meta.UserScoped;
import diarsid.support.model.CreatedAt;
import diarsid.support.model.Storable;
import diarsid.support.model.Unique;

public interface PatternToEntryChoice extends Unique, Storable, CreatedAt, UserScoped {

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
