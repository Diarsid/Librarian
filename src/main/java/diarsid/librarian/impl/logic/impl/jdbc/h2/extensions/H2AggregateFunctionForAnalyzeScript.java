package diarsid.librarian.impl.logic.impl.jdbc.h2.extensions;

import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.H2SqlAggregateFunctionScriptInJava;
import diarsid.support.model.versioning.VersionedByClassName;

public class H2AggregateFunctionForAnalyzeScript extends H2SqlAggregateFunctionScriptInJava {

    private static final String NAME = "EVAL_CODES";
    private static final H2AggregateFunctionForAnalyzeV32 CURRENT_VERSION = new H2AggregateFunctionForAnalyzeV32();

    public H2AggregateFunctionForAnalyzeScript() {
        super(CURRENT_VERSION, NAME, VersionedByClassName.versionOf(CURRENT_VERSION));
    }
}
