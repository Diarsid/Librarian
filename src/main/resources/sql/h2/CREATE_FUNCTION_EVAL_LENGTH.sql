-- generated 
--   by diarsid.librarian.impl.logic.impl.jdbc.h2.scripts.CountCharMatchesH2SqlFunctionScript
--   at 2024-05-24T01:20:01.798075600
CREATE ALIAS EVAL_LENGTH_V10 AS $$
    int evaluate(
            String string1 /* longer */ ,
            String string2 /* shorter */ ,
            int requiredRatio /* 1 - 100 */ ) {

        if ( requiredRatio < 1 || requiredRatio > 100 ) {
            return -1;
        }

        int matchLength;
        int maxLength;
        int lengthDiff;
        if ( string1.length() < string2.length() ) {
            matchLength = string1.length();
            String swap = string1;
            string1 = string2;
            string2 = swap;
        }
        else {
            matchLength = string2.length();
        }

        if ( matchLength == 1 ) {
            if ( string1.contains(string2) ) {
                return 1;
            }
            else {
                return -1;
            }
        }

        maxLength = string1.length();
        lengthDiff = maxLength - matchLength;

        int length1 = string1.length();
        int length2 = string2.length();
        int s1Last = length1 - 1;
        int s2Last = length2 - 1;

        int match = 0;

        int i1 = 0;
        int i2 = 0;
        char c1;
        char c2;
        char c1Prev = '_';
        char c2Prev = '_';
        int c1Duplicates = 0;
        int c2Duplicates = 0;

        while ( (i1 < length1) && (i2 < length2) ) {
            c1 = string1.charAt(i1);
            c2 = string2.charAt(i2);
            if ( c1Prev == c1 ) {
                c1Duplicates++;
            }
            if ( c2Prev == c2 ) {
                c2Duplicates++;
            }

            if ( c1 == c2 ) {
                match++;
                i1++;
                i2++;
                c1Prev = c1;
                c2Prev = c2;
            }
            else {

                if ( c1 > c2 ) {
                    while ( c1 > c2 && i2 < s2Last ) {
                        i2++;
                        c2Prev = c2;
                        c2 = string2.charAt(i2);
                        if ( c2Prev == c2 ) {
                            c2Duplicates++;
                        }
                    }
                    if ( i2 == s2Last ) {
                        if ( c1 == c2 ) {
                            match++;
                        }
                        else {
                            while ( c1 < c2 && i1 < s1Last ) {
                                i1++;
                                c1Prev = c1;
                                c1 = string1.charAt(i1);
                                if ( c1Prev == c1 ) {
                                    c1Duplicates++;
                                }
                                if ( c1 == c2 ) {
                                    match++;
                                }
                            }
                        }
                        break;
                    }
                }
                else /* c1 < c2 */ {
                    while ( c1 < c2 && i1 < s1Last ) {
                        i1++;
                        c1Prev = c1;
                        c1 = string1.charAt(i1);
                        if ( c1Prev == c1 ) {
                            c1Duplicates++;
                        }
                    }
                    if ( i1 == s1Last ) {
                        if ( c1 == c2 ) {
                            match++;
                        }
                        if ( i2 == s2Last ) {
                            break;
                        }
                        else {
                            do {
                                i2++;
                                c2Prev = c2;
                                c2 = string2.charAt(i2);
                                if ( c2Prev == c2 ) {
                                    c2Duplicates++;
                                }

                                if ( c1 == c2 ) {
                                    match++;
                                }
                            } while ( c2 > c1 && i2 < s2Last );
                        }
                    }
                }
            }
        }

        int maxDuplicates = Math.max(c1Duplicates, c2Duplicates);
        boolean hasDuplicates = maxDuplicates > 0;

        matchLength = matchLength - c2Duplicates;
        if ( hasDuplicates ) {
            int length1Unique = length1 - c1Duplicates;
            int length2Unique = length2 - c2Duplicates;
            lengthDiff = length1Unique - length2Unique;
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
                    threshold = hasDuplicates ? 2 : 3;
                    break;
                case 5:
                    threshold = 3;
                    break;
                case 6:
                    if ( hasDuplicates ) {
                        if ( lengthDiff <= 3 ) {
                            threshold = 3;
                            break;
                        }
                    }
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
$$
