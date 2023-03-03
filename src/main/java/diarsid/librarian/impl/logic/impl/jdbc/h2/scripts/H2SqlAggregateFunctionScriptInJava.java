package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.util.ArrayList;
import java.util.List;

import diarsid.support.model.versioning.Version;

import static java.lang.String.format;

public abstract class H2SqlAggregateFunctionScriptInJava extends H2SqlScriptFileInJava {

    public H2SqlAggregateFunctionScriptInJava(Object source, String name, Version version) {
        super(source, name, version);
        super.overrideNameAndVersionInFile(this.name());
    }

    @Override
    public final String scriptType() {
        return "AGGREGATE_FUNCTION";
    }

    @Override
    public final List<String> scriptLines() {
        List<String> lines = new ArrayList<>();

        lines.add("CREATE AGGREGATE " + super.nameAndVersion());
        lines.add(format("FOR \"%s\"", super.source.getClass().getCanonicalName()));

        return lines;
    }
}
