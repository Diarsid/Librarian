package diarsid.librarian.impl.logic.impl.search;

public final class CountCharMatches {

    private CountCharMatches() {}

    public static int evaluate(String string1, String string2, int requiredRatio /* 1 - 100 */) {
        return evaluateV4(string1, string2, requiredRatio);
    }

    private static int evaluateV4(String string1, String string2, int requiredRatio /* 1 - 100 */) {
        if ( requiredRatio < 1 || requiredRatio > 100 ) {
            return -1;
        }

        int matchLength;
        if ( string1.length() < string2.length() ) {
            matchLength = string1.length();
            String swap = string1;
            string1 = string2;
            string2 = swap;
        }
        else {
            matchLength = string2.length();
        }

        int len1 = string1.length();
        int len2 = string2.length();
        int len1m = len1 - 1;
        int len2m = len2 - 1;

        int match = 0;

        int i1 = 0;
        int i2 = 0;
        char c1;
        char c2;

        while ( (i1 < len1) && (i2 < len2) ) {
            c1 = string1.charAt(i1);
            c2 = string2.charAt(i2);

            if ( c1 == c2 ) {
                match++;
                i1++;
                i2++;
            }
            else {

                if ( c1 > c2 ) {
                    while ( c1 > c2 && i2 < len2m ) {
                        i2++;
                        c2 = string2.charAt(i2);
                    }
                    if ( i2 == len2m ) {
                        if ( c1 == c2 ) {
                            match++;
                        }
                        break;
                    }
                }
                else /* c1 < c2 */ {
                    while ( c1 < c2 && i1 < len1m) {
                        i1++;
                        c1 = string1.charAt(i1);
                    }
                    if ( i1 == len1m ) {
                        if ( c1 == c2 ) {
                            match++;
                        }
                        break;
                    }
                }
            }
        }

        if ( match > 4 ) {
            return match;
        }

        if ( matchLength < 9 ) {
            int threshold;
            switch ( matchLength ) {
                case 1:
                case 2:
                case 3:
                    threshold = 2;
                    break;
                case 4:
                case 5:
                    threshold = 3;
                    break;
                case 6:
                case 7:
                    threshold = 4;
                    break;
                default:
                    threshold = 5;
            }
            if ( match < threshold ) {
                return -1;
            }
            else {
                return match;
            }
        }

        int ratio = (match * 100) / matchLength;

        if ( ratio >= requiredRatio ) {
            return match;
        }
        else {
            return -1;
        }
    }
}
