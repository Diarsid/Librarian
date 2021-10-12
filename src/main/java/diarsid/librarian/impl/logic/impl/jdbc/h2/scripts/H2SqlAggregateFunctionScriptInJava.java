package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public interface H2SqlAggregateFunctionScriptInJava extends H2SqlScriptInJava {

    Class aggregateClass();

    @Override
    default String scriptType() {
        return "AGGREGATE_FUNCTION";
    }

    @Override
    default List<String> scriptLines() {
        List<String> lines = new ArrayList<>();

        lines.add("CREATE AGGREGATE " + this.nameAndVersion());
        lines.add(format("FOR \"%s\"", this.aggregateClass().getCanonicalName()));

        return lines;
    }
}
