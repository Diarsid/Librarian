package diarsid.librarian.impl.logic.impl.search.charscan;

import diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.H2SqlFunctionScriptInJava;
import diarsid.support.objects.references.Possible;

import static java.lang.String.format;

import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public interface PatternToWordMatching extends H2SqlFunctionScriptInJava {

    PatternToWordMatching CURRENT_VERSION = new PatternToWordMatchingV39();

    Possible<Boolean> logEnabled = simplePossibleButEmpty();

    static void logln(String s) {
        if ( logEnabled.isNotPresent() || logEnabled.get() ) {
            System.out.println(s);
        }
    }

    static void logln(String s, Object... args) {
        if ( logEnabled.isNotPresent() || logEnabled.get() ) {
            System.out.println(format(s, args));
        }
    }

    public static void main(String[] args) throws Exception {
        CURRENT_VERSION.rewriteScript();
    }

    @Override
    default String name() {
        return "EVAL_MATCHING";
    }

    long evaluate(String pattern, String word);

    MatchingCode describe(long code);

}
