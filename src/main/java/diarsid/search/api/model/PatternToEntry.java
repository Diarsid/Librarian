package diarsid.search.api.model;

import java.util.UUID;

import diarsid.search.api.model.meta.Identifiable;
import diarsid.search.api.model.meta.UserScoped;

public interface PatternToEntry extends Identifiable, UserScoped {

    Entry entry();

    Pattern pattern();

    String algorithmCanonicalName();

    float weight();

    default String entryString() {
        return this.entry().string();
    }

    default String patternString() {
        return this.pattern().string();
    }

    @Override
    default UUID userUuid() {
        return this.entry().userUuid();
    }
}
