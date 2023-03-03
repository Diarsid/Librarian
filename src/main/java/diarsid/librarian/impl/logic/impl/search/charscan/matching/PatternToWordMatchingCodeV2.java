package diarsid.librarian.impl.logic.impl.search.charscan.matching;

import diarsid.librarian.api.Matching;

public abstract class PatternToWordMatchingCodeV2 extends PatternToWordMatching implements Matching {

    @Override
    public final MatchingCodeV2 describe(long code) {
        return new MatchingCodeV2(code);
    }

    @Override
    public Match find(String pattern, String word) {
        long code = this.evaluate(pattern, word);
        return this.describe(code);
    }
}
