package diarsid.librarian.impl.logic.impl.search.charscan;

import static diarsid.librarian.impl.logic.impl.search.charscan.MatchingCode.Version.V2;

public class MatchingCodeV2 extends MatchingCode {

    public final int patternLength;
    public final int matchIndex;
    public final int matchSpan;
    public final int wordLength;
    public final int rate;
    public final int found;

    public MatchingCodeV2(long code) {
        super(code);

        this.found = (int) (code % 100);

        code = code / 100;

        this.matchSpan = (int) (code % 100);

        code = code / 100;

        this.matchIndex = (int) (code % 100);

        code = code / 100;

        this.wordLength = (int) (code % 100);

        code = code / 100;

        this.rate = (int) (code % 1000);

        code = code / 1000;

        this.patternLength = (int) (code % 100);
    }

    @Override
    public String toString() {
        return
                "pattern_L=" + patternLength +
                ", word_L=" + wordLength +
                ", found=" + found +
                ", match_Ix=" + matchIndex +
                ", match_Span=" + matchSpan +
                ", rate=" + rate;
    }

    @Override
    public Version version() {
        return V2;
    }
}
