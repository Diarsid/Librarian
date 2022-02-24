package diarsid.librarian.impl.logic.impl.search.charscan;

public interface PatternToWordMatchingCodeV1 extends PatternToWordMatching {

    @Override
    default MatchingCode describe(long code) {
        return new MatchingCodeV1(code);
    }
}
