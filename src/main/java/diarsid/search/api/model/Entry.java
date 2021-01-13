package diarsid.search.api.model;

import java.util.List;
import java.util.function.BiPredicate;

import diarsid.search.api.model.meta.Identifiable;
import diarsid.search.api.model.meta.UserScoped;
import diarsid.support.objects.CommonEnum;

import static diarsid.support.strings.StringUtils.containsPathSeparator;
import static diarsid.support.strings.StringUtils.containsTextSeparator;

public interface Entry extends Identifiable, UserScoped {

    interface Label extends Identifiable, UserScoped {

        enum Matching implements CommonEnum<Matching> {
            ANY_OF,
            ALL_OF
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

    List<Label> labels();

    Entry.Type type();

}
