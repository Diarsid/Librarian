package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeScript;

class ScriptsWrite {

    static class AggregateFunctionForAnalyzeCodes {
        public static void main(String[] args) throws Exception {
            var script = new H2AggregateFunctionForAnalyzeScript();
            script.writeScriptIntoFile();
        }
    }

    static class FunctionForPatternToWordMatching {
        public static void main(String[] args) {
            H2SqlFunctionScriptInJava script = new PatternToWordMatchingH2SqlFunctionScript();
            script.writeScriptIntoFile();
        }
    }

    static class FunctionForCountCharMatches {
        public static void main(String[] args) {
            H2SqlFunctionScriptInJava script = new CountCharMatchesH2SqlFunctionScript();
            script.writeScriptIntoFile();
        }
    }

}
