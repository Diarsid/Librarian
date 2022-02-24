package diarsid.librarian.impl.logic.impl.search.charscan;

public interface PatternToWordMatchingCodeV2 extends PatternToWordMatching {

    @Override
    default MatchingCode describe(long code) {
        return new MatchingCodeV2(code);
    }
}
