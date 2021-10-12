package diarsid.librarian.impl.logic.impl.jdbc.h2.extensions;

import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.H2SqlAggregateFunctionScriptInJava;

public class H2AggregateFunctionForAnalyzeScript implements H2SqlAggregateFunctionScriptInJava {

    @Override
    public Class aggregateClass() {
        return H2AggregateFunctionForAnalyzeV21.class;
    }

    @Override
    public String name() {
        return "EVAL_CODES";
    }

    @Override
    public int version() {
        return 21;
    }

    public static void main(String[] args) throws Exception {
        var script = new H2AggregateFunctionForAnalyzeScript();
        script.rewriteScript();
    }
}
