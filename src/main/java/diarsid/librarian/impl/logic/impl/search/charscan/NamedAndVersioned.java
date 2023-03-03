package diarsid.librarian.impl.logic.impl.search.charscan;

import diarsid.support.model.Named;
import diarsid.support.model.versioning.Version;
import diarsid.support.model.versioning.Versioned;

public interface NamedAndVersioned extends Named, Versioned {

    public static final String DFEAULT_FORMAT = "%s_V%s";

    public static String nameAndVersionOf(String name, Version version) {
        return name + "_V" + version.fullName;
    }

    public static String nameAndVersionOf(String format, String name, Version version) {
        return String.format(format, name, version.fullName);
    }

    default String nameAndVersionFormat() {
        return null;
    }

    default String nameAndVersion() {
        String format = this.nameAndVersionFormat();
        if ( format == null ) {
            return nameAndVersionOf(this.name(), this.version());
        }
        else {
            return nameAndVersionOf(format, this.name(), this.version());
        }
    }
}
