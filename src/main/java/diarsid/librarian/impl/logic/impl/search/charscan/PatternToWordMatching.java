package diarsid.librarian.impl.logic.impl.search.charscan;

import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.H2SqlFunctionScriptInJava;

public interface PatternToWordMatching extends H2SqlFunctionScriptInJava {

    PatternToWordMatching CURRENT_VERSION = new PatternToWordMatchingV43();

    public static void main(String[] args) {
        CURRENT_VERSION.rewriteScript();
    }

    @Override
    default String name() {
        return "EVAL_MATCHING";
    }

    long evaluate(String pattern, String word);

    MatchingCode describe(long code);

    @Override
    default List<String> stringsToRemoveInScript() {
        return List.of("public ");
    }

    @Override
    default List<String> stringsToCommentInScript() {
        return List.of("logln(\"");
    }

}
