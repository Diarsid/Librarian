package diarsid.librarian.tests.setup.scripts;

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

    private static void executeInServer(String sql) {
        server().jdbc.doInTransaction(tx -> {
            tx.doUpdate(sql);
        });
    }
}
