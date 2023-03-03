package diarsid.librarian.tests.setup.scripts;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeScript;
import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.CountCharMatchesH2SqlFunctionScript;
import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.H2SqlFunctionScriptInJava;
import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.PatternToWordMatchingH2SqlFunctionScript;

import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

class ServerScripts {

    static class CreateFunctionPatternToWordMatching {
        public static void main(String[] args) throws Exception {
            H2SqlFunctionScriptInJava script = new PatternToWordMatchingH2SqlFunctionScript();
            executeInServer(script.scriptLinesJoined());
        }
    }

    static class CreateFunctionCountCharMatches {
        public static void main(String[] args) throws Exception {
            H2SqlFunctionScriptInJava script = new CountCharMatchesH2SqlFunctionScript();
            executeInServer(script.scriptLinesJoined());
        }
    }

    static class CreateAggregateFunctionEvalCodes {
        public static void main(String[] args) throws Exception {
            var script = new H2AggregateFunctionForAnalyzeScript();
            var sql = script.scriptLinesJoined();
            executeInServer(sql);
        }
    }

    private static void executeInServer(String sql) {
        server().jdbc.doInTransaction(tx -> {
            tx.doUpdate(sql);
        });
    }
}
