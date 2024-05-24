package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.util.List;

import diarsid.librarian.impl.logic.impl.search.charscan.count.CountCharMatchesV2;

import static java.util.Collections.emptyList;

public class CountCharMatchesH2SqlFunctionScript extends H2SqlFunctionScriptInJava {

    public CountCharMatchesH2SqlFunctionScript() {
        super(CountCharMatchesV2.CURRENT_VERSION, emptyList(), List.of("public "));
        this.overrideNameAndVersionInFile(this.name());
    }
}
