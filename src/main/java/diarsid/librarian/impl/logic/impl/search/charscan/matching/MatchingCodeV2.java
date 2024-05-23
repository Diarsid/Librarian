package diarsid.librarian.impl.logic.impl.search.charscan.matching;

import diarsid.librarian.api.Matching;
import diarsid.support.objects.CommonEnum;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import static diarsid.librarian.impl.logic.impl.search.charscan.matching.MatchingCode.Version.V2;
import static diarsid.librarian.impl.logic.impl.search.charscan.matching.MatchingCodeV2.Type.FIRST_CHAR_ONLY;
import static diarsid.librarian.impl.logic.impl.search.charscan.matching.MatchingCodeV2.Type.FIRST_SECOND_CHARS_ONLY;

public class MatchingCodeV2 extends MatchingCode implements Matching.Match {

    public static enum Type implements CommonEnum<Type> {

        MOVABLE(3),
        FIRST_CHAR_ONLY(2),
        FULL(1),
        FIRST_SECOND_CHARS_ONLY(4);

        public final int mask;

        Type(int mask) {
            this.mask = mask;
        }

        public static Type ofMask(int mask) {
            switch ( mask ) {
                case 1 : return FULL;
                case 2 : return FIRST_CHAR_ONLY;
                case 3 : return MOVABLE;
                case 4 : return FIRST_SECOND_CHARS_ONLY;
                default: throw new IllegalArgumentException();
            }
        }
    }

    public final Type type;
    public final int patternLength;
    public final int matchIndex;
    public final int matchSpan;
    public final int wordLength;
    public final int rate;
    public final int found;

    public MatchingCodeV2(long code) {
        super(code);

        if ( code < 0 ) {
            this.type = null;
            this.wordLength = -1;
            this.patternLength = -1;
            this.matchIndex = -1;
            this.rate = -1;
            this.found = -1;
            this.matchSpan = -1;
            return;
        }

        long shortCode = code / 1000000;

        if ( shortCode == FIRST_CHAR_ONLY.mask) {
            this.type = FIRST_CHAR_ONLY;

            this.wordLength = (int) (code % 100);

            code = code / 100;

            this.patternLength = (int) (code % 100);

            this.matchIndex = 0;
            this.rate = 0;
            this.found = 1;
            this.matchSpan = 1;
        }
        else if ( shortCode == FIRST_SECOND_CHARS_ONLY.mask ) {
            this.type = FIRST_SECOND_CHARS_ONLY;

            this.wordLength = (int) (code % 100);

            code = code / 100;

            this.patternLength = (int) (code % 100);

            this.matchIndex = 0;
            this.rate = 0;
            this.found = 2;
            this.matchSpan = 2;
        }
        else {
            int typeMask = (int) (code / 10000000000000L);
            this.type = Type.ofMask(typeMask);

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
    }

    @Override
    public String toString() {
        if ( type == null ) {
            return "mismatch";
        }
        else {
            return type.name() +
                    ", pattern_L=" + patternLength +
                    ", word_L=" + wordLength +
                    ", found=" + found +
                    ", match_Ix=" + matchIndex +
                    ", match_Span=" + matchSpan +
                    ", rate=" + rate;
        }
    }

    @Override
    public Version version() {
        return V2;
    }

    @Override
    public int rate() {
        return this.rate;
    }

    @Override
    public int index() {
        return this.matchIndex;
    }

    @Override
    public int length() {
        return this.matchSpan;
    }

    @Override
    public int charsFound() {
        return this.found;
    }

    public boolean isPositive() {
        return nonNull(this.type);
    }

    public boolean isNegative() {
        return isNull(this.type);
    }

}
