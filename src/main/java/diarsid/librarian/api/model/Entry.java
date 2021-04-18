package diarsid.librarian.api.model;

import java.util.UUID;
import java.util.function.BiPredicate;

import diarsid.librarian.api.model.meta.UserScoped;
import diarsid.support.model.CreatedAt;
import diarsid.support.model.Joined;
import diarsid.support.model.Storable;
import diarsid.support.model.Unique;
import diarsid.support.model.Updatable;
import diarsid.support.objects.CommonEnum;

import static diarsid.support.strings.StringUtils.containsPathSeparator;
import static diarsid.support.strings.StringUtils.containsTextSeparator;

public interface Entry extends Unique, Updatable, CreatedAt, UserScoped {

    interface Label extends Unique, Storable, CreatedAt, UserScoped {

        enum Matching implements CommonEnum<Matching> {
            ANY_OF,
            ALL_OF,
            NONE_OF
        }

        String name();

        ConditionBindable bindableIf(BiPredicate<Entry, Entry.Label> condition);

        interface ConditionBindable extends Label {

            BiPredicate<Entry, Entry.Label> ENTRY_CONTAINS_LABEL_IGNORE_CASE = (entry, label) ->
                    entry.string().toLowerCase().contains(label.name().toLowerCase());

            boolean canBeBoundTo(Entry entry);

            default boolean canNotBeBoundTo(Entry entry) {
                return ! this.canBeBoundTo(entry);
            }

            Entry.Label origin();
        }
    }

    interface Labeled extends Unique, Storable, CreatedAt, Joined<Entry, Label>, UserScoped {

        default Entry entry() {
            return this.left();
        }

        default Label label() {
            return this.right();
        }

        default UUID entryUuid() {
            return this.entry().uuid();
        }

        default UUID labelUuid() {
            return this.label().uuid();
        }

        default UUID userUuid() {
            return this.entry().userUuid();
        }
    }

    enum Type implements CommonEnum<Entry.Type> {

        WORD,
        PHRASE,
        PATH;

        public static Entry.Type defineTypeOf(String string) {
            if ( containsPathSeparator(string) ) {
                return PATH;
            }
            else if ( containsTextSeparator(string) ) {
                return PHRASE;
            }
            else {
                return WORD;
            }
        }
    }

    String string();

    Entry.Type type();

}
