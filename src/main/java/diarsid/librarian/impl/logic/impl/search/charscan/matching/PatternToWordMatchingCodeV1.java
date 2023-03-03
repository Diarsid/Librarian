package diarsid.librarian.impl.logic.impl.search.charscan.matching;

public abstract class PatternToWordMatchingCodeV1 extends PatternToWordMatching {

    @Override
    public final MatchingCode describe(long code) {
        return new MatchingCodeV1(code);
    }
}
