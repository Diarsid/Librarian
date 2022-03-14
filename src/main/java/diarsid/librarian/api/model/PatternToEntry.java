package diarsid.librarian.api.model;

import java.util.UUID;

import diarsid.librarian.api.model.meta.UserScoped;
import diarsid.support.model.CreatedAt;
import diarsid.support.model.Joined;
import diarsid.support.model.Storable;
import diarsid.support.model.Unique;

public interface PatternToEntry extends Unique, Storable, CreatedAt, Joined<Pattern, Entry>, UserScoped {

    @Override
    default Pattern left() {
        return this.pattern();
    }

    @Override
    default Entry right() {
        return this.entry();
    }

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
        this.pattern().checkHasSameUser(this.entry());
        return this.entry().userUuid();
    }

    default boolean has(Pattern pattern) {
        return this.pattern().equals(pattern);
    }

    default boolean hasSamePattern(PatternToEntry other) {
        return this.has(other.pattern());
    }
}
