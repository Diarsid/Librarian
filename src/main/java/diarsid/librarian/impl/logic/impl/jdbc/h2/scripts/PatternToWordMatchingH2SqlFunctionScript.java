package diarsid.librarian.impl.logic.impl.jdbc.h2.scripts;

import java.util.List;

import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching;

public class PatternToWordMatchingH2SqlFunctionScript extends H2SqlFunctionScriptInJava {

    public PatternToWordMatchingH2SqlFunctionScript() {
        super(PatternToWordMatching.currentVersion(), List.of("logln(\""), List.of("public "));
        super.overrideNameAndVersionInFile(this.name());
    }
}
