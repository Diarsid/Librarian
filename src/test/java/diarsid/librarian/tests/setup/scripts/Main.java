package diarsid.librarian.tests.setup.scripts;

import diarsid.librarian.impl.logic.impl.search.charscan.H2SqlFunctionScriptInJava;
import diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching;
import diarsid.librarian.tests.setup.CoreTestSetup;

import static java.util.stream.Collectors.joining;

import static diarsid.librarian.tests.setup.CoreTestSetupStaticSingleton.server;

public class Main {

    public static void main(String[] args) throws Exception {
        H2SqlFunctionScriptInJava function = PatternToWordMatching.CURRENT_VERSION;
        CoreTestSetup setup = server();
        String scriptCode = function.scriptLines().stream().collect(joining(" \n"));
        setup.jdbc.doInTransaction(tx -> {
            tx.doUpdate(scriptCode);
        });
    }
}
