package diarsid.librarian.tests.setup.scripts;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeScript;
import diarsid.librarian.impl.logic.impl.search.charscan.CountCharMatches;
import diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching;

import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class ServerScripts {

    static class CreateFunctionPatternToWordMatching {
        public static void main(String[] args) throws Exception {
            executeInServer(PatternToWordMatching.CURRENT_VERSION.scriptLinesJoined());
        }
    }

    static class CreateFunctionCountCharMatches {
        public static void main(String[] args) throws Exception {
            executeInServer(CountCharMatches.CURRENT_VERSION.scriptLinesJoined());
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
