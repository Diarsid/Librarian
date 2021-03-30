package diarsid.search.api.model;

import java.util.UUID;

import diarsid.search.api.model.meta.UserScoped;
import diarsid.support.model.CreatedAt;
import diarsid.support.model.Storable;
import diarsid.support.model.Unique;

public interface PatternToEntry extends Unique, Storable, CreatedAt, UserScoped {

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
