package diarsid.librarian.impl.logic.impl.search.charscan.matching;

import diarsid.librarian.impl.logic.impl.search.charscan.LoggerOwner;
import diarsid.librarian.impl.logic.impl.search.charscan.NamedAndVersionedByClassName;

public abstract class PatternToWordMatching extends LoggerOwner implements NamedAndVersionedByClassName {

    public static PatternToWordMatching currentVersion() {
        return new PatternToWordMatchingV53();
    }

    @Override
    public final String name() {
        return "EVAL_MATCHING";
    }

    public abstract long evaluate(String pattern, String word);

    public abstract MatchingCode describe(long code);

}
