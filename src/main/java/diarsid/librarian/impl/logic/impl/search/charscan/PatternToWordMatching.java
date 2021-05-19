package diarsid.librarian.impl.logic.impl.search.charscan;

import diarsid.support.objects.references.Possible;

import static java.lang.String.format;

import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public interface PatternToWordMatching extends H2SqlFunctionScriptInJava {

    PatternToWordMatching CURRENT_VERSION = new PatternToWordMatchingV26();

    Possible<Boolean> logEnabled = simplePossibleButEmpty();

    class Description {

        public final int patternLength;
        public final int matchIndex;
        public final int wordLength;
        public final int rate;
        public final int matchLength;

        public Description(long code) {
            this.matchLength = (int) (code % 100);

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
                            ", match_L=" + matchLength +
                            ", match_Ix=" + matchIndex +
                            ", rate=" + rate;
        }
    }

    String CODE_MAP = "P-LEN_RATE_W-LEN_M-IX_M-LEN";

    static String describe(long code) {
        long matchLength = (int) (code % 100);

        code = code / 100;

        long matchIndex = (int) (code % 100);

        code = code / 100;

        long wordLength = (int) (code % 100);

        code = code / 100;

        long rate = (int) (code % 1000);

        code = code / 1000;

        long patternLength = (int) (code % 100);

        return
                "patternL:" + patternLength +
                        ", wordL:" + wordLength +
                        ", matchL:" + matchLength +
                        ", matchIx:" + matchIndex +
                        ", rate:" + rate;
    }

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

}
