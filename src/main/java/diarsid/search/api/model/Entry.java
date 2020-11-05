package diarsid.search.api.model;

import java.util.List;

import diarsid.search.api.model.meta.Identifiable;
import diarsid.search.api.model.meta.UserScoped;
import diarsid.support.objects.CommonEnum;

import static diarsid.support.strings.StringUtils.containsPathSeparator;
import static diarsid.support.strings.StringUtils.containsTextSeparator;

public interface Entry extends Identifiable, UserScoped {

    interface Label extends Identifiable, UserScoped {

        String name();
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
