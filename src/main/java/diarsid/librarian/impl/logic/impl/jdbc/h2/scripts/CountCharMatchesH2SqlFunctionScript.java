package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import diarsid.librarian.impl.logic.impl.search.charscan.count.CountCharMatches;

import static java.util.Collections.emptyList;

public class CountCharMatchesH2SqlFunctionScript extends H2SqlFunctionScriptInJava {

    public CountCharMatchesH2SqlFunctionScript() {
        super(CountCharMatches.CURRENT_VERSION, emptyList(), emptyList());
        this.overrideNameAndVersionInFile(this.name());
    }
}
