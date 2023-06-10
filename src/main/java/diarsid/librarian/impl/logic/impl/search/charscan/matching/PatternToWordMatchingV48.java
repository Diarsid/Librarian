package diarsid.librarian.impl.logic.impl.search.charscan.matching;

public final class PatternToWordMatchingV48 extends PatternToWordMatchingCodeV2 {

    @Override
    /* script */
    public long evaluate(String pattern, String word) {
        logln("%s : PATTERN:%s  <--->  WORD:%s", this.nameAndVersion(), pattern, word);

        final long CODE_V2_BASE_FIRST_CHAR_MATCH_ONLY = 2000000L;
        final long CODE_V2_BASE = 10000000000000L;

        final int wordLength = word.length();
        final int patternLength = pattern.length();
        final int bothLength = wordLength + patternLength;
        final int matchLength;
        final int rangeLength;
        final int diffLength;
        final int lengthRatioType;

        final int LENGTHS_APPROX_EQUAL = 1;
        final int LENGTHS_DIFF_INSIGNIFICANT = 2;
        final int LENGTHS_DIFF_SUBSTANTIAL = 3;

        if ( patternLength >= wordLength) {
            matchLength = wordLength;
            rangeLength = patternLength;

            int wordInPatternIndex = pattern.indexOf(word);
            if ( wordInPatternIndex > -1 ) {
                int rate = wordLength*10 + wordLength*7 + wordLength*5;
                rate = rate * 2;
                logln("   " + rate);
                long code = CODE_V2_BASE;

                code = code + patternLength         * 100000000000L;
                code = code + rate                  * 100000000L;
                code = code + wordLength            * 1000000L;
                code = code + wordInPatternIndex    * 10000L;
                code = code + wordLength            * 100L;
                code = code + wordLength;

                return code;
            }
            else if ( wordLength == 2 ) {
                char wordChar0 = word.charAt(0);
                char patternChar0 = pattern.charAt(0);
                if ( wordChar0 == patternChar0 ) {
                    logln("   first char of a short word! [length 2]");

                    long code = CODE_V2_BASE_FIRST_CHAR_MATCH_ONLY;

                    code = code + patternLength * 100L;
                    code = code + wordLength;

                    return code;
                }
                else {
                    logln("  [c]");
                    return -1;
                }
            }
            else if ( bothLength < 7 ) {
                logln("  [d]");
                return -1;
            }
        }
        else {
            matchLength = patternLength;
            rangeLength = wordLength;

            int patternInWordIndex = word.indexOf(pattern);
            if ( patternInWordIndex > -1 ) {
                int rate = patternLength*10 + patternLength*7 + patternLength*5;
                rate = rate * 2;
                logln("   " + rate);
                long code = CODE_V2_BASE;

                code = code + patternLength         * 100000000000L;
                code = code + rate                  * 100000000L;
                code = code + wordLength            * 1000000L;
                code = code + patternInWordIndex    * 10000L;
                code = code + patternLength         * 100L;
                code = code + patternLength;

                return code;
            }
            else if ( patternLength == 2 ) {
                logln("  [a]");
                return -1;
            }
            else if ( bothLength < 7 ) {
                logln("  [b]");
                return -1;
            }
        }

        final boolean STRICT = wordLength < 6;

        diffLength = rangeLength - matchLength;

        if ( diffLength < matchLength / 2 ) {
            lengthRatioType = LENGTHS_APPROX_EQUAL;
            logln("length diff: LENGTHS_APPROX_EQUAL");
        }
        else if ( diffLength >= matchLength / 2 && diffLength < matchLength - 1 ) {
            lengthRatioType = LENGTHS_DIFF_INSIGNIFICANT;
            logln("length diff: LENGTHS_DIFF_INSIGNIFICANT");
        }
        else {
            lengthRatioType = LENGTHS_DIFF_SUBSTANTIAL;
            logln("length diff: LENGTHS_DIFF_SUBSTANTIAL");
        }

        int wordChar_0_Missed = 0;
        int wordChar_1_Missed = 0;
        int wordChar_2_Missed = 0;
        int wordChar_3_Missed = 0;

        int mismatchesOnlyWord = 0;
        int mismatches = 0;
        int foundDuplicates = 0;
        int found = 0;
        int matchFull = 0;
        int matchInPattern = 0;
        int matchInPatternWeak = 0;
        boolean matchInPatternWeakTooMuchNoStrong = false;
        int matchInPatternStrong = 0;
        int matchInPatternStrengthBonus = 0;
        int backwardMatches = 0;
        int typoMatches = 0;
        int order = 0;
        int gaps = 0;
        int diffInWordSum = 0;
        int diffInPatternSum = 0;

        int mismatchWordChars = 0;
        boolean strongWordEnd = false;
        boolean Ab_c_pattern = false;

        int lastInWord = wordLength - 1;
        int lastInPattern = pattern.length() - 1;

        int matchSpanInWord = 0;

        char wc;
        char wcPrev = '_';
        char wcPrevPrev = '_';
        int wcInPattern;
        int wcPrevInPattern = -1;
        int wcPrevPrevInPattern = -1;
        int iPrev = -1;
        int iPrevPrev = -1;
        int firstWcInPatternIndex = -1;
        int firstFoundWcInPatternIndex = -1;
        int wordInPatternLength = 0;
        int diffInPattern = -1;
        int diffInWord = -1;
        boolean iterationIsStrong = false;
        boolean iterationIsWeak = false;
        boolean backwardMatchInCurr = false;
        boolean typoAbcAcbInCurr = false;
        boolean backwardMatchInPrev = false;
        boolean typoAbcAcbInPrev = false;
        int typoAbcAcbCount = 0;

        boolean endOfPatternReached = false;

        int wordDuplication2Index = -1;

        int longestDiffInWord = -1;
        int longestDiffInPattern = -1;

        int wordFirstFoundCharIndex = -1;
        int wordLastFoundCharIndex = -1;

        final int PREV_CHAR_UNINITIALIZED = -1;
        final int PREV_CHAR_UNKNOWN = 0;
        final int PREV_CHAR_NOT_FOUND = 1;
        final int PREV_CHAR_FOUND = 2;
        final int PREV_CHAR_MATCH_PATTERN = 3;
        final int PREV_CHAR_MATCH_FULL = 4;

        final String PREV_CHAR_STRENGTH_IS_WEAK = "WEAK";
        final String PREV_CHAR_STRENGTH_IS_STRONG = "STRONG";

        int prevCharResult = PREV_CHAR_UNINITIALIZED;
        String prevCharMatchStrength = null;

        final int PREV_CHAR_FEATURE__NO = -1;
        final int PREV_CHAR_FEATURE__IS_DUPLICATE = 101;

        int prevCharFeature = PREV_CHAR_FEATURE__NO;
        int prevCharFeatureI = -1;

        int rescanStartIndexInclusive = -1;

        wordCharsIterating: for (int i = 0; i < wordLength; i++) {
            wc = word.charAt(i);

            if ( i == 0 && rescanStartIndexInclusive > -1 ) {
                wcInPattern = pattern.indexOf(wc, rescanStartIndexInclusive);
            }
            else {
                wcInPattern = pattern.indexOf(wc, wcPrevInPattern + 1);
            }

            diffInPattern = -1;
            diffInWord = -1;

            iterationIsStrong = false;
            iterationIsWeak = false;
            backwardMatchInCurr = false;
            typoAbcAcbInCurr = false;

            if ( prevCharFeature != PREV_CHAR_FEATURE__NO ) {
                if ( prevCharFeatureI != i - 1 ) {
                    prevCharFeature = PREV_CHAR_FEATURE__NO;
                }
            }

            if ( wcInPattern < 0 ) {
                if ( i == 0 ) {
                    logln("   WORD:%s not found! ", wc, wcPrev);
                    wordChar_0_Missed++;
                    mismatches++;
                }
                else if ( i == 1 ) {
                    logln("   WORD:%s not found! ", wc, wcPrev);
                    wordChar_1_Missed++;
                    if ( wordChar_0_Missed > 0 ) {
                        logln("      mismatches major!");
                        return -1;
                    }
                }
                else if ( wc == wcPrev && i == iPrev+1 ) {
                    logln("   WORD:%s not found, but is duplicate of WORD:%s[PATTERN:%s] - %s : %s", wc, wcPrev, wcPrevInPattern, higlightChar(word, i), higlightChar(pattern, wcPrevInPattern));
                    foundDuplicates++;
                    prevCharFeature = PREV_CHAR_FEATURE__IS_DUPLICATE;
                    prevCharFeatureI = i;
                    order++;
                    matchInPattern++;
                    wordLastFoundCharIndex = i;
                    if ( iPrev > -1 ) {
                        iPrevPrev = iPrev;
                    }
                    iPrev = i;

                    if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                        logln("        strong++ [duplicate]");
                        matchInPatternStrong++;
                        iterationIsStrong = true;
                        prevCharResult = PREV_CHAR_MATCH_FULL;
                    }
                    else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND ) {
                        if ( prevCharMatchStrength == PREV_CHAR_STRENGTH_IS_WEAK ) {
                            prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                        }
                        logln("        weak++ [duplicate] [1]");
                        matchInPatternWeak++;
                        iterationIsWeak = true;
                        prevCharResult = PREV_CHAR_MATCH_PATTERN;
                    }
                    else {
                        logln("        weak++ [duplicate] [2]");
                        matchInPatternWeak++;
                        iterationIsWeak = true;
                        prevCharResult = PREV_CHAR_FOUND;
                    }
                }
                else if ( wc == wcPrev ) {
                    boolean prohibition = false;
                    if ( wordDuplication2Index > -1 ) {
                        if ( wordDuplication2Index == i || wordDuplication2Index == i-1 ) {
                            prohibition = true;
                            logln("      is duplicate of previous found char, but is duplication of word chars!");
                        }
                    }

                    if ( ! prohibition ) {
                        logln("   WORD:%s not found after WORD:%s[PATTERN:%s] - %s : %s", wc, wcPrev, wcPrevInPattern, higlightChar(word, i), higlightChar(pattern, wcPrevInPattern));

                        int iWordFromIncl = iPrev + 1;
                        int wordCharsToIterate = i - iWordFromIncl;
                        int patternCharsToIterate = wcPrevInPattern - 1;
                        int iPatternFromIncl = 1;
                        if ( patternCharsToIterate > wordCharsToIterate ) {
                            iPatternFromIncl = wcPrevInPattern - wordCharsToIterate;
                            patternCharsToIterate = wcPrevInPattern - iPatternFromIncl;
                        }

                        logln("      is duplicate of previous found char, backward scan in ranges word:%s, pattern:%s", wordCharsToIterate, patternCharsToIterate);

                        char wcX;
                        char pcX;
                        wordIterationX: for ( int iWord = iWordFromIncl; iWord < i; iWord++ ) {
                            wcX = word.charAt(iWord);
                            for ( int iPattern = iPatternFromIncl; iPattern < wcPrevInPattern; iPattern++ ) {
                                pcX = pattern.charAt(iPattern);

                                if ( wcX == pcX ) {
                                    logln("         found potential missed char: '%s' pattern:%s word:%s", wcX, iPattern, iWord);
                                    if ( iWord == i-1 && iPattern == wcPrevInPattern-1 ) {
                                        if ( ! backwardMatchInPrev ) {
                                            logln("            strong, accept new prev-char");

                                            wordLastFoundCharIndex = i;
                                            if ( iPrev > -1 ) {
                                                iPrevPrev = iPrev;
                                            }
                                            iPrev = i;
                                            prevCharResult = PREV_CHAR_FOUND;
                                            prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;

                                            diffInWordSum--;
                                            diffInPatternSum--;

                                            break wordIterationX;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    logln("   WORD:%s not found after WORD:%s[PATTERN:%s] - %s : %s", wc, wcPrev, wcPrevInPattern, higlightChar(word, i), higlightChar(pattern, wcPrevInPattern));

                    if ( typoAbcAcbInPrev ) {
                        boolean denyTypo;

                        if ( wc == wcPrev ) {
                            denyTypo = false;
                        }
                        else {
                            if ( patternLength < 5 ) {
                                denyTypo = false;
                            }
                            else {
                                char wCharTypo = word.charAt(i - 3);
                                char pCharTypo = pattern.charAt(wcPrevInPattern - 1);
                                denyTypo = wCharTypo != pCharTypo;
                            }
                        }

                        if ( denyTypo ) {
                            logln("        prev is aBC-aCB typo, deny typo!");
                            typoAbcAcbCount--;
                            order--;
                            matchInPattern--;
                            matchFull--;
                            matchFull--;
                            matchInPatternStrong--;
                            matchInPatternStrong--;
                            iterationIsStrong = true;
                            matchInPatternWeak++;
                            diffInWordSum++;
                            diffInPatternSum++;
                            prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                            prevCharResult = PREV_CHAR_FOUND;
                        }
                        else {
                            logln("        prev is aBC-aCB typo, typo confirmed");
                        }
                    }

                    int wcInPatternWeak = pattern.indexOf(wc, firstFoundWcInPatternIndex);
                    if ( wcInPatternWeak < 0 ) {
                        logln("      WORD:%s not found from word-in-pattern beginning [PATTERN:%s]", wc, firstFoundWcInPatternIndex);
                        prevCharResult = PREV_CHAR_NOT_FOUND;
                    }
                    else {
                        if ( wcPrevInPattern > -1 ) {
                            int distanceFromItoWordEnd = lastInWord - i;
                            int distanceFromCPrevToC = wcPrevInPattern - wcInPatternWeak;
                            if ( distanceFromCPrevToC > distanceFromItoWordEnd && distanceFromCPrevToC > 2 && matchLength > 6 ) {
                                logln("      WORD:%s[PATTERN:%s] too far [1]!", wc, wcInPatternWeak);
                                break wordCharsIterating;
                            }
                            else {
//                                if ( i != lastInWord ) {
                                    if ( wcInPatternWeak != firstWcInPatternIndex ) {
                                        if ( wcInPatternWeak == wcPrevInPattern - 1 ) {
                                            if ( i == iPrev + 1 ) {
                                                if ( gaps > 0 ) {
                                                    boolean backwardOrTypoForbidden = false;
                                                    if ( iPrevPrev > -1 ) {
                                                        int lastWordDiff = iPrev - iPrevPrev - 1;
                                                        if ( lastWordDiff > 2 && i == lastInWord ) {
                                                            backwardOrTypoForbidden = true;
                                                        }
                                                    }

                                                    if ( backwardOrTypoForbidden ) {

                                                    }
                                                    else {
                                                        logln("      WORD:%s[PATTERN:%s] backward match [+1] from word-in-pattern beginning [PATTERN:%s] before WORD:%s[PATTERN:%s]",  wc, wcInPatternWeak, firstFoundWcInPatternIndex, wcPrev, wcPrevInPattern);
                                                        backwardMatches++;
                                                        backwardMatchInCurr = true;
                                                        found++;
                                                        if ( wordFirstFoundCharIndex < 0 ) {
                                                            wordFirstFoundCharIndex = i;
                                                        }
                                                        wordLastFoundCharIndex = i;
                                                        logln("      found++[1]");
                                                        gaps--;

                                                        boolean firstCharsTypo =
                                                                wcInPatternWeak == firstWcInPatternIndex + 1 &&
                                                                        (prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND);
                                                        if ( firstCharsTypo ) {
                                                            if ( found == i+1 ) {
                                                                logln("        aBC-aCB typo!");
                                                                typoAbcAcbCount++;
                                                                typoAbcAcbInCurr = true;
                                                                order++;
                                                                matchInPattern++;
                                                                matchFull++;
                                                                matchFull++;
                                                                matchInPatternStrong++;
                                                                matchInPatternStrong++;
                                                                iterationIsStrong = true;
                                                                matchInPatternWeak--;
                                                                if ( matchInPatternWeakTooMuchNoStrong ) {
                                                                    matchInPatternWeakTooMuchNoStrong = false;
                                                                }
                                                                diffInWordSum--;
                                                                diffInPatternSum--;
                                                                prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                                                prevCharResult = PREV_CHAR_MATCH_FULL;
                                                            }
                                                            else {
                                                                logln("        aBC-aCB typo denied - not found char before aBC-aCB");
                                                            }
                                                        }

                                                        diffInPatternSum = diffInPatternSum - (wcPrevInPattern - wcInPatternWeak) + 1;
                                                        if ( i != lastInWord && wcPrevInPattern+2 < patternLength-1 ) {
                                                            char wcNext = word.charAt(i+1);
                                                            if ( wcPrev != wcNext ) {
                                                                if ( wcPrevInPattern > -1 ) {
                                                                    wcPrevPrevInPattern = wcPrevInPattern;
                                                                }
                                                                wcPrevInPattern = wcInPatternWeak;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else if ( i == iPrev + 2 ) {
                                                boolean backwardMatch2Prohibited =
                                                        (i+1)/2 > matchInPattern+1 ||
                                                        matchFull+1 > 2 ||
                                                        wordChar_1_Missed > 0;
                                                if ( backwardMatch2Prohibited ) {
                                                    logln("         WORD:%s[PATTERN:%s] backward match [+2] prohibited!",  wc, wcInPatternWeak);
                                                    mismatchesOnlyWord++;
                                                }
                                                else {
                                                    if ( gaps > 0 ) {
                                                        if ( i != lastInWord ) {
                                                            char wcNext = word.charAt(i + 1);

                                                            if ( wcNext == wcPrev ) {
                                                                logln("      WORD:%s[PATTERN:%s] backward match [+2] prohibited - is duplication in word!",  wc, wcInPatternWeak);
                                                                wordDuplication2Index = i;
                                                            }
                                                            else {
                                                                logln("      WORD:%s[PATTERN:%s] backward match [+2] from word-in-pattern beginning [PATTERN:%s] before WORD:%s[PATTERN:%s]",  wc, wcInPatternWeak, firstWcInPatternIndex, wcPrev, wcPrevInPattern);
                                                                if ( wcPrevInPattern > -1 ) {
                                                                    wcPrevPrevInPattern = wcPrevInPattern;
                                                                }
                                                                wcPrevInPattern = wcInPatternWeak;
                                                                backwardMatches++;
                                                                backwardMatchInCurr = true;
                                                                found++;
                                                                if ( wordFirstFoundCharIndex < 0 ) {
                                                                    wordFirstFoundCharIndex = i;
                                                                }
                                                                wordLastFoundCharIndex = i;
                                                                logln("      found++[2]");
                                                                gaps--;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else {
                                                mismatchesOnlyWord++;
                                            }
                                        }
                                        else if ( wcInPatternWeak == wcPrevPrevInPattern+1 ) {
                                            if ( wcPrevInPattern > wcInPatternWeak+1 ) {
                                                logln("      ignore previous match!");
                                                matchInPatternWeak--;
                                                matchInPatternStrong++;
                                                diffInWordSum++;
                                                diffInPatternSum = diffInPatternSum - 2;
                                                if ( diffInPatternSum == 0 ) {
                                                    longestDiffInPattern = 0;
                                                }
                                                gaps--;
                                            }
                                        }
                                        else {
                                            if ( i != lastInWord ) {
                                                if ( wcInPatternWeak != wcPrevPrevInPattern ) {
                                                    typoMatches++;
                                                    logln("      WORD:%s[PATTERN:%s] typo match from word-in-pattern beginning", wc, wcInPatternWeak);
                                                }
                                            }
                                        }
                                    }
//                                }
                            }
                        }
                    }
                }

                if ( i == 2 ) {
                    wordChar_2_Missed++;
                }
                else if ( i == 3 ) {
                    wordChar_3_Missed++;
                }
            }
            else /* wcInPattern >= 0 */ {
                if ( i == 0 ) {
                    firstWcInPatternIndex = wcInPattern;
                    firstFoundWcInPatternIndex = wcInPattern;
                }
                else if ( firstFoundWcInPatternIndex < 0 ) {
                    firstFoundWcInPatternIndex = wcInPattern;
                }

                if ( wcPrevInPattern > -1 ) {
                    int distanceFromCPrevToC = wcInPattern - wcPrevInPattern;

                    if ( distanceFromCPrevToC > 1 ) {
                        order++;
                        logln("      order++ [2]");
                        boolean distanceFromPrevToCurrentTooHigh = distanceFromCPrevToC > 2 && distanceFromCPrevToC >= matchInPattern;
                        boolean rescanAllowed = false;

                        if ( distanceFromPrevToCurrentTooHigh ) {
                            rescanAllowed = true;
                        }
                        else {
                            boolean tooBadMatching = diffInPatternSum > 2 && i < 4 && matchFull == 0;
                            if ( tooBadMatching ) {
                                rescanAllowed = true;
                            }
                        }

                        int distanceFromItoWordEnd = wordLength - i;

                        if ( rescanAllowed ) {
                            logln("      rescan allowed!");
                            if ( matchFull < 1 ) {
                                // no full matches - can loop to find stronger matching of prev char
                                int wcPrevInPatternN = wcPrevInPattern;
                                int wcPrevInPatternNprev = -1;
                                boolean goFurther = true;
                                while ( goFurther ) {
                                    wcPrevInPatternNprev = wcPrevInPatternN;
                                    wcPrevInPatternN = pattern.indexOf(wcPrev, wcPrevInPatternN + 1);
                                    logln("         search: WORD:%s[PATTERN:%s]", wcPrev, wcPrevInPatternN);
                                    goFurther = wcPrevInPatternN > -1 && wcPrevInPatternN < wcInPattern;
                                }

                                if ( wcPrevInPatternNprev != wcPrevInPattern ) {
                                    if ( firstWcInPatternIndex == wcPrevInPattern ) {
                                        if ( wcPrevInPatternNprev > wcPrevInPattern && wcPrevInPatternNprev < wcInPattern ) {
                                            logln("      move: WORD:%s[PATTERN:%s] -> WORD:%s[PATTERN:%s] switch check [move 1]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            int matchesInLoop = 0;
                                            char patternLoopChar;
                                            int iWord = i+1;
                                            int wordLoopCharIndex;
                                            patternLoop: for ( int iPattern = wcPrevInPattern+1 ; iPattern < wcPrevInPatternNprev ; iPattern++ ) {
                                                patternLoopChar = pattern.charAt(iPattern);
                                                wordLoopCharIndex = word.indexOf(patternLoopChar, iWord);
                                                if ( wordLoopCharIndex > -1 ) {
                                                    iWord = wordLoopCharIndex + 1;
                                                    matchesInLoop++;
                                                    logln("          '%s' WORD:%s[PATTERN:%s] found", patternLoopChar, wordLoopCharIndex, iPattern);
                                                }
                                            }

                                            if ( matchesInLoop > 1 ) {
                                                logln("          too much chars matches, switch rejected");
                                            }
                                            else {
                                                logln("          switch approved");
                                                firstWcInPatternIndex = wcPrevInPatternNprev;
                                                firstFoundWcInPatternIndex = wcPrevInPatternNprev;
                                                if ( wcPrevInPattern > -1 ) {
                                                    wcPrevPrevInPattern = wcPrevInPattern;
                                                }
                                                wcPrevInPattern = wcPrevInPatternNprev;

                                                if ( iPrev == 0 ) {
                                                    i = 0;
                                                    order--;
                                                    wordChar_0_Missed = 0;
                                                    wordChar_1_Missed = 0;
                                                    wordChar_2_Missed = 0;
                                                    wordChar_3_Missed = 0;
                                                    logln("          switch is word-in-pattern start, scan word from beginnig!");
                                                    continue wordCharsIterating;
                                                }
                                            }
                                        }
                                    }
                                    else {
//                                        int distanceChange = wcPrevInPatternNprev - wcPrevInPattern;
//                                        if ( distanceChange > 0 && distanceChange < distanceFromItoWordEnd && wcPrevInPatternNprev < wcInPattern ) {
//                                            logln("      move: WORD:%s[PATTERN:%s] --> WORD:%s[PATTERN:%s] [move 2]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
//                                            wcPrevInPattern = wcPrevInPatternNprev;
//                                            if ( diffInPatternSum > 0 ) {
//                                                diffInPatternSum = diffInPatternSum + distanceChange - 1;
//                                            }
//                                            distanceFromCPrevToC = wcInPattern - wcPrevInPattern;
//                                            if ( distanceFromCPrevToC > (distanceFromItoWordEnd + 2) ) {
//                                                logln("   WORD:%s not found on reasonable length [1] after WORD:%s[PATTERN:%s], found at [PATTERN:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
//                                                order--;
//                                                logln("   order-- [2][1]");
//                                                prevCharResult = PREV_CHAR_NOT_FOUND;
//                                                continue wordCharsIterating;
//                                            }
//                                        }
//                                        else {
//                                            logln("   WORD:%s not found on reasonable length [2] after WORD:%s[PATTERN:%s], found at [PATTERN:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
//                                            prevCharResult = PREV_CHAR_NOT_FOUND;
//                                            order--;
//                                            logln("   order-- [2][2]");
//                                            continue wordCharsIterating;
//                                        }
                                    }
                                }
                                else {
                                    if ( i == 3 ) {
                                        char firstWordChar = word.charAt(0);
                                        int next = pattern.indexOf(firstWordChar, firstWcInPatternIndex + 1);

                                        if ( next > -1 ) {
                                            if ( next < wcPrevInPattern ) {
                                                i = -1;

                                                rescanStartIndexInclusive = next;

                                                /* reset state */
                                                wordChar_0_Missed = 0;
                                                wordChar_1_Missed = 0;
                                                wordChar_2_Missed = 0;
                                                wordChar_3_Missed = 0;

                                                mismatchesOnlyWord = 0;
                                                mismatches = 0;
                                                foundDuplicates = 0;
                                                found = 0;
                                                matchFull = 0;
                                                matchInPattern = 0;
                                                matchInPatternWeak = 0;
                                                matchInPatternWeakTooMuchNoStrong = false;
                                                matchInPatternStrong = 0;
                                                matchInPatternStrengthBonus = 0;
                                                backwardMatches = 0;
                                                typoMatches = 0;
                                                order = 0;
                                                gaps = 0;
                                                diffInWordSum = 0;
                                                diffInPatternSum = 0;

                                                mismatchWordChars = 0;
                                                strongWordEnd = false;
                                                Ab_c_pattern = false;

                                                lastInWord = wordLength - 1;
                                                lastInPattern = pattern.length() - 1;

                                                matchSpanInWord = 0;

                                                wcPrev = '_';
                                                wcPrevPrev = '_';

                                                wcPrevInPattern = -1;
                                                wcPrevPrevInPattern = -1;
                                                iPrev = -1;
                                                iPrevPrev = -1;
                                                firstWcInPatternIndex = -1;
                                                firstFoundWcInPatternIndex = -1;
                                                wordInPatternLength = 0;
                                                diffInPattern = -1;
                                                diffInWord = -1;
                                                iterationIsStrong = false;
                                                iterationIsWeak = false;
                                                backwardMatchInCurr = false;
                                                typoAbcAcbInCurr = false;
                                                backwardMatchInPrev = false;
                                                typoAbcAcbInPrev = false;
                                                typoAbcAcbCount = 0;

                                                wordDuplication2Index = -1;

                                                longestDiffInWord = -1;
                                                longestDiffInPattern = -1;

                                                wordFirstFoundCharIndex = -1;
                                                wordLastFoundCharIndex = -1;

                                                prevCharResult = PREV_CHAR_UNINITIALIZED;
                                                prevCharMatchStrength = null;

                                                prevCharFeature = PREV_CHAR_FEATURE__NO;
                                                prevCharFeatureI = -1;
                                                /* reset state end */

                                                logln("      move: WORD:%s[PATTERN:%s] -> WORD:%s[PATTERN:%s] switch check [move 2]", firstWordChar, firstWcInPatternIndex, firstWordChar, next);
                                                logln("          scan word from beginnig !");
                                                continue wordCharsIterating;
                                            }
                                        }
                                    }
                                }
                            }
                            else /* matchFull >= 1 */ {
                                logln("   WORD:%s not found on reasonable length after [3] WORD:%s[PATTERN:%s], found at [PATTERN:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                boolean charsDiffers = wc != wcPrev;
                                if ( prevCharResult != PREV_CHAR_NOT_FOUND && charsDiffers ) {
                                    mismatchWordChars++;
                                }

                                boolean foundInGap = false;
                                if ( gaps > 0 ) {
                                    int from = firstFoundWcInPatternIndex;
                                    if ( from > -1 ) {
                                        from++;
                                        char pc;
                                        logln("      scanning gaps from [PATTERN:%s] to [PATTERN:%s]", from, wcPrevInPattern-1);
                                        for ( int i2 = from; i2 < wcPrevInPattern; i2++ ) {
                                            pc = pattern.charAt(i2);
                                            if ( wc == pc ) {
                                                logln("        found at [PATTERN:%s]", i2);
                                                foundInGap = true;
                                                backwardMatches++;
                                                backwardMatchInCurr = true;
                                                found++;
                                                if ( wordFirstFoundCharIndex < 0 ) {
                                                    wordFirstFoundCharIndex = i;
                                                }
                                                wordLastFoundCharIndex = i;
                                                logln("      found++[3]");
                                                if ( prevCharResult != PREV_CHAR_NOT_FOUND && charsDiffers ) {
                                                    mismatchWordChars--;
                                                }
                                            }
                                        }
                                    }
                                }

                                if ( ! foundInGap ) {
                                    logln("        not found in gaps");
                                    if ( distanceFromCPrevToC > found ) {
                                        char i2wc;
                                        int i2FirstWcInPattern = -1;
                                        int i2wcInPattern = -1;
                                        int i2wcInPatternPrev = wcPrevInPattern;
                                        int i2FullMatch = 0;
                                        int i2MatchInPattern = 0;
                                        int i2 = 0;
                                        int i2Order = 0;
                                        int allowed = Math.max(matchFull+1, found);

                                        logln("      duplication search from 0[incl] to %s[incl]", allowed-1);
                                        duplicateSearch: for (; i2 < allowed; i2++) {
                                            i2wc = word.charAt(i2);
                                            i2wcInPattern = pattern.indexOf(i2wc, i2wcInPatternPrev + 1);
                                            if ( i2wcInPattern > -1 ) {
                                                if ( i2 == 0 ) {
                                                    i2FirstWcInPattern = i2wcInPattern;
                                                }

                                                if ( i2wcInPattern < wcInPattern ) {
                                                    logln("        duplication search: WORD:%s[PATTERN:%s]", i2wc, i2wcInPattern);
                                                    if ( i2 > 0 ) {
                                                        i2MatchInPattern++;
                                                        if ( i2wcInPatternPrev + 1 == i2wcInPattern ) {
                                                            i2FullMatch++;
                                                        }
                                                        i2Order++;
                                                    }
                                                    i2wcInPatternPrev = i2wcInPattern;
                                                }
                                            }
                                            else {
                                                if ( i2 == 0 ) {
                                                    break duplicateSearch;
                                                }
                                            }

                                            if ( i2+1 < allowed ) {
                                                if ( i2FullMatch >= matchFull && allowed < i ) {
                                                    allowed++;
                                                }
                                            }
                                        }

                                        boolean change =
                                                i2FullMatch > matchFull ||
                                                (i2FullMatch == matchFull && i2MatchInPattern >= matchInPattern);

                                        if ( change ) {
                                            logln("        duplication search: change fullMatches %s starting from %s", i2FullMatch, i2FirstWcInPattern);
                                            i = i2 - 1;
                                            order = i2Order;
                                            firstWcInPatternIndex = i2FirstWcInPattern;
                                            firstFoundWcInPatternIndex = firstWcInPatternIndex;
                                            if ( wcPrevInPattern > -1 ) {
                                                wcPrevPrevInPattern = wcPrevInPattern;
                                            }
                                            wcPrevInPattern = i2wcInPatternPrev;
                                            if ( iPrev > -1 ) {
                                                iPrevPrev = iPrev;
                                            }
                                            iPrev = i;
                                            prevCharResult = PREV_CHAR_FOUND;
                                        }
                                        else {
                                            order--;
                                            prevCharResult = PREV_CHAR_NOT_FOUND;
                                        }

                                        continue wordCharsIterating;
                                    }
                                    else {
                                        logln("      WORD:%s[PATTERN:%s] too far [3]!", wc, wcInPattern);
                                        prevCharResult = PREV_CHAR_NOT_FOUND;
                                        order--;
                                        continue wordCharsIterating;
                                    }
                                }
                            }
                        }
                    }
                    else if /* distanceFromCPrevToC <= 1 */ ( distanceFromCPrevToC > 0 ) {
                        logln("      order++ [1]");
                        order++;
                    }
                }

                logln("   WORD:%s[PATTERN:%s]", wc, wcInPattern);
                if ( wcPrevInPattern > -1 ) {
                    diffInPattern = wcInPattern - wcPrevInPattern;
                    diffInWord = i - iPrev;

                    if ( diffInPattern == 1 ) {
                        logln("      PATTERN MATCH");
                        matchInPattern++;
                        if ( diffInWord == 1 ) {
                            logln("      FULL MATCH [1]");
                            matchFull++;
                            if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                                matchInPatternStrong++;
                                iterationIsStrong = true;
                                logln("        strong, prev match is full [1]");
                                prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                            }
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND ) {
                                if ( matchInPatternWeak > 0 ) {
                                    if ( diffInWordSum >= 3 && matchFull < 2 ) {
                                        logln("        weak++ [?]");
                                        matchInPatternWeak++;
                                        iterationIsWeak = true;
                                        if ( matchInPatternStrong == 0 && matchInPatternWeak-foundDuplicates > 2 && ! matchInPatternWeakTooMuchNoStrong ) {
                                            matchInPatternWeakTooMuchNoStrong = true;
                                            logln("        too weak, no strong");
                                        }
                                        prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                                    }
                                    else {
                                        if ( diffInWordSum == 1 ) {
                                            logln("        weak-- strong++");
                                            matchInPatternStrong++;
                                            matchInPatternWeak--;
                                        }
                                        matchInPatternStrong++;
                                        iterationIsStrong = true;
                                        logln("        strong, prev match is pattern or found {1}");
                                        prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                        if ( mismatches == 0 && diffInWordSum <= 3 ) {
                                            logln("        strong bonus");
                                            matchInPatternStrengthBonus++;
                                        }
                                    }
                                }
                                else {
                                    matchInPatternStrong++;
                                    iterationIsStrong = true;
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                    logln("        strong, full match");
                                }
                            }
                            prevCharResult = PREV_CHAR_MATCH_FULL;
                        }
                        else {
                            if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                                if ( i == lastInWord ) {
                                    matchInPatternStrong++;
                                    iterationIsStrong = true;
                                    logln("        strong, prev match is full and char finishes the word");
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                }
                                else {
                                    if ( matchInPattern > 2 && matchFull > 1 ) {
                                        matchInPatternStrong++;
                                        iterationIsStrong = true;
                                        logln("        strong, prev match is full [2]");
                                        prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                    }
                                    else {
                                        matchInPatternWeak++;
                                        iterationIsWeak = true;
                                        logln("        weak++ [1]");
                                        if ( matchInPatternStrong == 0 && matchInPatternWeak-foundDuplicates > 2 && ! matchInPatternWeakTooMuchNoStrong ) {
                                            matchInPatternWeakTooMuchNoStrong = true;
                                            logln("        too weak, no strong");
                                        }
                                        prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                                    }
                                }
                            }
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN ) {
                                if ( i == lastInWord ) {
                                    matchInPatternStrong++;
                                    iterationIsStrong = true;
                                    logln("        strong, prev match is pattern but char finishes the word");
                                    if ( prevCharResult == PREV_CHAR_MATCH_PATTERN && prevCharMatchStrength == PREV_CHAR_STRENGTH_IS_WEAK ) {
                                        matchInPatternWeak--;
                                        matchInPatternStrong++;
                                    }
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                }
                                else {
                                    matchInPatternWeak++;
                                    iterationIsWeak = true;
                                    logln("        weak++ [2]");
                                    if ( matchInPatternStrong == 0 && matchInPatternWeak-foundDuplicates > 2 && ! matchInPatternWeakTooMuchNoStrong ) {
                                        matchInPatternWeakTooMuchNoStrong = true;
                                        logln("        too weak, no strong");
                                    }
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                                }
                            }
                            else if ( prevCharResult == PREV_CHAR_NOT_FOUND
                                    || prevCharResult == PREV_CHAR_FOUND) {
                                if ( i <= 3 && matchFull > 0 && diffInPatternSum == 0 ) {
                                    Ab_c_pattern = true;
                                    logln("Ab_c pattern");
                                }
                                if ( wcPrevInPattern == 0 && diffInPattern == 1 && diffInWord <= 3 ) {
                                    matchInPatternStrong++;
                                    iterationIsStrong = true;
                                    logln("        strong?");
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                }
                                else {
                                    matchInPatternWeak++;
                                    iterationIsWeak = true;
                                    logln("        weak++ [3]");
                                    if ( matchInPatternStrong == 0 && matchInPatternWeak-foundDuplicates > 2 && ! matchInPatternWeakTooMuchNoStrong ) {
                                        matchInPatternWeakTooMuchNoStrong = true;
                                        logln("        too weak, no strong");
                                    }
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                                }
                            }
                            prevCharResult = PREV_CHAR_MATCH_PATTERN;
                        }
                    }
                    else {
                        int gap = diffInPattern - 1;
                        if ( i <= gap ) {
                            boolean gapToBigToAccept =
                                    gap > 2
                                            || diffInPattern > (patternLength / 2);
                            if ( gapToBigToAccept ) {
                                logln("      WORD:%s[PATTERN:%s] too far [4], ignore and continue!", wc, wcInPattern);

                                if ( i == 1 ) {
                                    wordChar_1_Missed++;
                                }
                                else if ( i == 2 ) {
                                    wordChar_2_Missed++;
                                }
                                else if ( i == 3 ) {
                                    wordChar_3_Missed++;
                                }

                                order--;
                                logln("      order--");
                                if ( iterationIsStrong ) {
                                    matchInPatternStrong--;
                                }
                                if ( iterationIsWeak ) {
                                    matchInPatternWeak--;
                                }

                                diffInPattern = 1;

                                continue wordCharsIterating;
                            }
                        }

                        boolean cPrevIndexBeforeCIndex = wcPrevInPattern < wcInPattern;
                        if ( cPrevIndexBeforeCIndex ) {
                            matchInPattern++;
                            logln("      PATTERN MATCH, prev before current");
                            if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                                if ( prevCharFeature == PREV_CHAR_FEATURE__NO ) {
                                    matchInPatternStrong++;
                                    iterationIsStrong = true;
                                    logln("        strong, prev match is full [3]");
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                }
                                else if ( prevCharFeature == PREV_CHAR_FEATURE__IS_DUPLICATE ) {
                                    matchInPatternWeak++;
                                    iterationIsWeak = true;
                                    logln("        weak, prev match is full, but duplicate");
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                                }
                            }
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN
                                    || prevCharResult == PREV_CHAR_FOUND
                                    || prevCharResult == PREV_CHAR_NOT_FOUND ) {
                                matchInPatternWeak++;
                                iterationIsWeak = true;
                                logln("        weak [4]");
                                if ( matchInPatternStrong == 0 && matchInPatternWeak-foundDuplicates > 2 && ! matchInPatternWeakTooMuchNoStrong ) {
                                    matchInPatternWeakTooMuchNoStrong = true;
                                    logln("        too weak, no strong");
                                }
                                prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                            }
                            prevCharResult = PREV_CHAR_MATCH_PATTERN;
                        }

                        boolean diffMoreThanOne = diffInPattern > 1;
                        int diffInPatternOld = diffInPattern;
                        int wcPrevInPatternN = wcPrevInPattern;
                        boolean gapNotFixed = true;
                        gapFixing: while ( diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                            gaps++;
                            logln("      gap between char WORD:%s[PATTERN:%s] and previous char WORD:%s[PATTERN:%s]!", wc, wcInPattern, wcPrev, wcPrevInPattern);

                            if ( wcPrev != '_' ) {
                                wcPrevPrev = wcPrev;
                            }
                            wcPrev = word.charAt(i - 1);
                            wcPrevInPatternN = pattern.indexOf(wcPrev, wcPrevInPatternN + 1);
                            if ( wcPrevInPatternN < 0 ) {
                                logln("      WORD:%s not found after PATTERN:%s", wcPrev, wcPrevInPattern);
                                gap = diffInPattern - 1;
                                if ( matchFull == 0 && gap > found ) {
                                    logln("         no full-matches, gap[%s] > found[%s]", gap, found);
                                    return -1;
                                }
                                break gapFixing;
                            }
                            if ( wcPrevInPatternN > wcInPattern ) {
                                logln("      WORD:%s found only after current WORD:%s[PATTERN:%s]", wcPrev, wc, wcInPattern);
                                break gapFixing;
                            }
                            if ( wcPrevInPatternN == wcInPattern) {
                                break gapFixing;
                            }
                            diffInPattern = wcInPattern - wcPrevInPatternN;

                            if ( (diffInPattern + i) > wordLength) {
                                logln("         PATTERN:%s to far", wcPrevInPatternN);
                                break gapFixing;
                            }

                            diffMoreThanOne = diffInPattern > 1;
                            cPrevIndexBeforeCIndex = wcPrevInPatternN < wcInPattern;

                            if ( ! diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                                if ( wcPrevInPattern > -1 ) {
                                    wcPrevPrevInPattern = wcPrevInPattern;
                                }
                                wcPrevInPattern = wcPrevInPatternN;
                                logln("      gap fixed char WORD:%s[PATTERN:%s] and previous char WORD:%s[PATTERN:%s] - PATTERN:%s!", wc, wcInPattern, wcPrev, wcPrevInPattern, wcPrevInPatternN);
                                gaps--;
                                logln("      MATCH, gap fixed");
                                gapNotFixed = false;
                                if ( diffInWord == 1 ) {
                                    logln("      FULL MATCH [2]");
                                    matchFull++;
                                    if ( prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND ) {
                                        if ( matchInPatternWeak > 0 ) {
                                            matchInPatternWeak--;
                                            matchInPatternStrong++;
                                            logln("        weak-- strong++ [1]");
                                            prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                            if ( mismatches == 0 && diffInWordSum < 2 ) {
                                                logln("        strong bonus");
                                                matchInPatternStrengthBonus++;
                                            }
                                        }
                                        else {
                                            if ( ! iterationIsStrong ) {
                                                matchInPatternStrong++;
                                                iterationIsStrong = true;
                                                logln("        strong++ [2]");
                                            }
                                        }
                                    }
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_STRONG;
                                    prevCharResult = PREV_CHAR_MATCH_FULL;
                                }
                            }
                        }

                        if ( gapNotFixed ) {
                            diffInPattern = diffInPatternOld;
                            int patternRemnant = patternLength - wcInPattern - 1;
                            if ( lengthRatioType == LENGTHS_APPROX_EQUAL || lengthRatioType == LENGTHS_DIFF_INSIGNIFICANT ) {
                                if ( gap > 1 && gap > patternRemnant ) {
                                    int iNext = word.indexOf(wc, i + 1);
                                    if ( iNext > -1 ) {
                                        int wordRemnant = wordLength - i - 1;
                                        if ( wordRemnant >= patternRemnant ) {
                                            logln("      WORD:%s[PATTERN:%s] too far [5], ignore and continue!", wc, wcInPattern);
                                            order--;

                                            if ( i == 1 ) {
                                                wordChar_1_Missed++;
                                            }
                                            else if ( i == 2 ) {
                                                wordChar_2_Missed++;
                                            }
                                            else if ( i == 3 ) {
                                                wordChar_3_Missed++;
                                            }

                                            logln("      order--");
                                            if ( iterationIsStrong ) {
                                                matchInPatternStrong--;
                                            }
                                            if ( iterationIsWeak ) {
                                                matchInPatternWeak--;
                                            }
                                            continue wordCharsIterating;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ( longestDiffInWord < 0 || diffInWord-1 > longestDiffInWord ) {
                        longestDiffInWord = diffInWord-1;
                    }

                    if ( longestDiffInPattern < 0 || diffInPattern-1 > longestDiffInPattern ) {
                        longestDiffInPattern = diffInPattern-1;
                    }
                }

                if ( wcPrev != '_' ) {
                    wcPrevPrev = wcPrev;
                }
                wcPrev = wc;
                wordInPatternLength = wcInPattern - firstFoundWcInPatternIndex + 1;

                if ( wordInPatternLength > wordLength + 2 ) {
                    logln("      word-in-pattern-length is substantially longer than word");
                    found++;
                    if ( wordFirstFoundCharIndex < 0 ) {
                        wordFirstFoundCharIndex = i;
                    }
                    wordLastFoundCharIndex = i;
                    break wordCharsIterating;
                }

                if ( diffInWord > -1 ) {
                    diffInWordSum = diffInWordSum + diffInWord - 1;
                    diffInPatternSum = diffInPatternSum + diffInPattern - 1;
                    logln("      diff++");
                }

                if ( wcPrevInPattern > -1 ) {
                    wcPrevPrevInPattern = wcPrevInPattern;
                }
                wcPrevInPattern = wcInPattern;
                if ( iPrev > -1 ) {
                    iPrevPrev = iPrev;
                }
                iPrev = i;

                if ( backwardMatchInCurr ) {
                    backwardMatchInPrev = true;
                    if ( typoAbcAcbInCurr ) {
                        typoAbcAcbInPrev = true;
                    }
                    else {
                        typoAbcAcbInPrev = false;
                    }
                }
                else {
                    backwardMatchInPrev = false;
                    typoAbcAcbInPrev = false;
                }

                found++;
                if ( wordFirstFoundCharIndex < 0 ) {
                    wordFirstFoundCharIndex = i;
                }
                wordLastFoundCharIndex = i;
                logln("      found++[4]");
                if ( prevCharResult == PREV_CHAR_UNINITIALIZED ) {
                    prevCharResult = PREV_CHAR_FOUND;
                }

                if ( wcInPattern == lastInPattern ) {
                    if ( rangeLength < 6 && found < matchLength && gaps > 0 ) {
                        if ( wcPrevInPattern > -1 ) {
                            wcPrevPrevInPattern = wcPrevInPattern;
                        }
                        wcPrevInPattern = firstFoundWcInPatternIndex;
                        continue wordCharsIterating;
                    }
                    else {
                        logln("      end of pattern reached");
                        endOfPatternReached = true;

                        boolean finish = true;
                        if ( firstFoundWcInPatternIndex > 0 ) {
                            int charsInWordLeft = lastInWord - i;
                            if ( i < 2 ) {
                                if ( found < wordInPatternLength ) {
                                    logln("      ...continue! [1]");
                                }
                                finish = false;
                            }
                            else if ( firstFoundWcInPatternIndex > 0 && charsInWordLeft < found && found >= 3 ) {
                                finish = false;
                                logln("      ...continue! [2]");
                            }
                        }

                        if ( i == lastInWord-1 ) {
                            boolean noDoubleChars = wc != word.charAt(lastInWord);
                            boolean longEnough = wordLength > 4;
                            boolean notGoodEnough = matchFull < 3;
                            boolean diffsTooHigh = diffInWordSum > 1 || diffInPatternSum > 1;
                            boolean moreWeakThanStrong = matchInPatternStrong <= matchInPatternWeak;
                            int conditions =
                                    (longEnough ? 1 : 0) +
                                            (notGoodEnough ? 1 : 0) +
                                            (diffsTooHigh ? 1 : 0) +
                                            (moreWeakThanStrong ? 1 : 0);
                            if ( noDoubleChars && conditions > 2 ) {
                                logln("      only 1 char remain in word");
                                if ( prevCharMatchStrength == PREV_CHAR_STRENGTH_IS_STRONG ) {
                                    matchInPatternWeak++;
                                    iterationIsWeak = true;
                                    logln("        weak++ strong--");
                                    if ( matchInPatternStrong == 0 && matchInPatternWeak-foundDuplicates > 2 && ! matchInPatternWeakTooMuchNoStrong ) {
                                        matchInPatternWeakTooMuchNoStrong = true;
                                        logln("        too weak, no strong");
                                    }
                                    matchInPatternStrong--;
                                    prevCharMatchStrength = PREV_CHAR_STRENGTH_IS_WEAK;
                                }
                                else if ( prevCharMatchStrength == PREV_CHAR_STRENGTH_IS_WEAK ) {

                                }
                            }
                        }

                        if ( finish ) {

                            break wordCharsIterating;
                        }
                    }
                }
            }

            if ( backwardMatchInCurr ) {
                backwardMatchInPrev = true;
                if ( typoAbcAcbInCurr ) {
                    typoAbcAcbInPrev = true;
                }
                else {
                    typoAbcAcbInPrev = false;
                }
            }
            else {
                backwardMatchInPrev = false;
                typoAbcAcbInPrev = false;
            }
        } /* wordCharsIterating loop end */

        boolean patternStartsWithWord = firstWcInPatternIndex == 0;

        if ( foundDuplicates > 0 ) {
            if ( found > 2 ) {
                found = found + foundDuplicates;
            }
            else {
                order = order - foundDuplicates;
                matchInPattern = matchInPattern - foundDuplicates;
                matchInPatternStrong = matchInPatternStrong - foundDuplicates;
            }
        }

        if ( prevCharMatchStrength == PREV_CHAR_STRENGTH_IS_STRONG ) {
            if ( diffInPatternSum == 0 && prevCharResult == PREV_CHAR_MATCH_FULL || prevCharResult == PREV_CHAR_NOT_FOUND ) {
                if ( endOfPatternReached || wordLastFoundCharIndex == lastInWord ) {
                    strongWordEnd = true;
                }
            }
        }

        if ( matchFull > 0 ) {
            matchFull++;
        }
        if ( matchInPattern > 0 ) {
            matchInPattern++;
        }
        if ( backwardMatches > 0 ) {
            backwardMatches++;
        }
        if ( order > 0 ) {
            order++;
        }

        if ( found == 1 && wordLength < 6 && patternStartsWithWord ) {
            logln("   first char of a short word!");

            long code = CODE_V2_BASE_FIRST_CHAR_MATCH_ONLY;

            code = code + patternLength * 100L;
            code = code + wordLength;

            return code;
        }

        if ( found == 0 || order == 0 || matchFull + matchInPattern == 0 ) {
            logln("   not found anything!");
            return -1;
        }

//        if ( patternLength < wordLength && patternLength < 5 ) {
//            if ( found < patternLength ) {
//                logln("   pattern is too short to accept missed chars!");
//                return -1;
//            }
//        }

        final int WORD_IS_PATTERN_FRAGMENT = 1;
        final int PATTERN_IS_WORD_FRAGMENT = 2;

        int matchType = WORD_IS_PATTERN_FRAGMENT;
        int notFound = matchLength - found;
        if ( patternLength <= wordLength ) {
            if ( found == matchLength ) {
                matchType = PATTERN_IS_WORD_FRAGMENT;
            }
            else if ( firstWcInPatternIndex > 1 ) {
                matchType = WORD_IS_PATTERN_FRAGMENT;
            }
            else {
                if ( notFound == 1 ) {
                    matchType = PATTERN_IS_WORD_FRAGMENT;
                }
                else if ( notFound == 2 && found > 3 ) {
                    matchType = PATTERN_IS_WORD_FRAGMENT;
                }
                else if ( notFound == 2 && found >= 2 && firstWcInPatternIndex == 0) {
                    matchType = PATTERN_IS_WORD_FRAGMENT;
                }
            }
        }

        logln("match type: %s", matchType == WORD_IS_PATTERN_FRAGMENT ? "WORD_IS_PATTERN_FRAGMENT" : "PATTERN_IS_WORD_FRAGMENT");

        if ( matchType == PATTERN_IS_WORD_FRAGMENT ) {
            if ( firstWcInPatternIndex != 0 ) {
//                firstCharMissed++;
            }
            wordChar_1_Missed = wordChar_1_Missed + mismatchesOnlyWord;
        }

        int matchInPatternWeight = 7;
        if ( order == found ) {
            matchInPatternWeight = 9;
        }

        int mismatchesMinorWeight = 5;
        if ( matchType == PATTERN_IS_WORD_FRAGMENT ) {
            mismatchesMinorWeight = 7;
        }

        int typoMatchesWeight = 5;
        switch ( lengthRatioType ) {
            case LENGTHS_APPROX_EQUAL: {
                typoMatchesWeight = 5;
                break;
            }
            case LENGTHS_DIFF_INSIGNIFICANT: {
                if ( diffInWordSum <= matchLength ) {

                }
                else if ( diffInWordSum <= diffLength / 2 ) {
                    matchInPatternWeight--;
                }
                else if ( diffInWordSum < diffLength ) {
                    matchInPatternWeight = matchInPatternWeight - 2;
                }
                else {
                    matchInPatternWeight = matchInPatternWeight - 4;
                }
                typoMatchesWeight = 3;
                break;
            }
            case LENGTHS_DIFF_SUBSTANTIAL: {
                if ( diffInWordSum <= matchLength ) {

                }
                else if ( diffInWordSum <= diffLength / 2 ) {
                    matchInPatternWeight = matchInPatternWeight - 2;
                }
                else if ( diffInWordSum < diffLength ) {
                    matchInPatternWeight = matchInPatternWeight - 3;
                }
                else {
                    matchInPatternWeight = matchInPatternWeight - 5;
                }
                typoMatchesWeight = 1;
                break;
            }
            default: throw new IllegalStateException("Length ratio type is not defined!");
        }
        int matchInPatternWeakWeight = matchInPatternWeight - 5;

        if ( STRICT ) {
            if ( lengthRatioType == LENGTHS_APPROX_EQUAL ) {
                if ( found < matchLength && firstWcInPatternIndex != 0 ) {
//                    firstCharMissed++;
                    mismatches++;
                }
            }

            if ( notFound == 1 && matchFull < 3 && matchType == PATTERN_IS_WORD_FRAGMENT ) {
                mismatches++;
            }
        }

        boolean straightSingleCharsWithLittleDiff =
                found >=3
                && wordChar_0_Missed == 0
                && typoMatches == 0
                && order == found
                && backwardMatches == 0
                && longestDiffInWord == 1
                && diffInWordSum > 0
                && matchFull == 0
                && matchInPatternWeak > matchInPatternStrong
                && (diffInWordSum - longestDiffInWord) < (found - 1)
                && diffInPatternSum == 0;

        int rate;
        if ( matchFull > 2 || (matchFull > 0 && matchInPatternStrong > matchFull) || matchInPatternWeak == 0 ) {
            rate = matchFull * 10
                    + matchInPattern * matchInPatternWeight
                    + matchInPatternStrengthBonus * 6
                    + backwardMatches * 6
                    + found * 5
                    + typoMatches * typoMatchesWeight
                    - wordChar_0_Missed * 80
                    - wordChar_1_Missed * mismatchesMinorWeight;
        }
        else if ( straightSingleCharsWithLittleDiff && matchFull == 0 ) {
            rate = found * 8
                    + matchInPattern * matchInPatternWeight
                    + found * 5;
        }
        else {
            rate = matchFull * 10
                    + matchInPatternWeight + matchInPatternWeight * matchInPatternStrong
                    + matchInPatternStrengthBonus * 6
                    + matchInPatternWeakWeight * matchInPatternWeak
                    + backwardMatches * 6
                    + found * 5
                    + typoMatches * typoMatchesWeight
                    - wordChar_0_Missed * 80
                    - wordChar_1_Missed * mismatchesMinorWeight;
        }
        logln("     base rate: " + rate);

        if ( wordChar_1_Missed == 1 && wordChar_2_Missed == 1 && wordChar_3_Missed == 1 ) {
            rate = rate - 60;
            logln("    -50 WORD[1,2,3] are missed! ");
        }

        if ( matchInPatternWeakTooMuchNoStrong && backwardMatches == 0 && typoMatches == 0 ) {
            rate = rate - 30;
            logln("    -30 to much weak ");
        }

        if ( diffInWordSum == longestDiffInWord ) {
            if ( diffInWordSum > 3 ) {
                rate = rate - 10;
                logln("    -10 single long word diff");
            }
        }

        if ( found > 3 &&
                found == order &&
                found == matchFull &&
                matchFull == matchInPattern &&
                matchInPatternStrong == found-1 &&
                matchInPatternWeak == 0 ) {
            if ( wordChar_0_Missed == 0 ) {
                rate = rate + 50;
                logln("    +50 [zlxj]");
            }
            else if ( found > notFound ) {
                if ( notFound == 1 ) {
                    if ( found > 4 ) {
                        rate = rate + 50;
                        logln("    +50 word is essentially a precise substring with i:0 char missed");
                    }
                    else {
                        logln("    +40 word is essentially a precise substring with i:0 char missed");
                    }
                }
                else {
                    rate = rate + 30;
                    logln("    +30 [ocyr]");
                }
            }

        }

        if ( matchFull > 3 && wordChar_0_Missed == 0 && wordChar_1_Missed == 0 ) {
            if ( found == order && found == matchFull ) {
                rate = rate + 100;
                logln("    +100 found substring");
            }
            else if ( matchInPattern - matchFull < 3 ) {
                rate = rate + 60;
                logln("    +60 full match > 3 && a little weak matches");
            }
        }

        if ( order > found ) {
            logln("ORDER > FOUND");
        }

        int wordFoundLength = wordLastFoundCharIndex - wordFirstFoundCharIndex + 1;
        int wordNotFoundLength = wordLength - wordFoundLength;
        int matchSpanInPattern = wcPrevInPattern - firstFoundWcInPatternIndex + 1;


        float wordFoundRatio;
        if ( wordFoundLength == wordLength ) {
            wordFoundRatio = 100f;
        }
        else {
            wordFoundRatio = (wordFoundLength * 100f) / wordLength;
        }

        boolean lastFoundCharIsLastInWord = iPrev == lastInWord;

        boolean embraceStartEnd = false;
        boolean embraceStartEndTooMuchDiff = false;
        boolean embraceEnd = false;
        boolean embraceStartHigh = false;
        boolean embraceStartLow = false;

        int diffInWordAndPatternSum = diffInWordSum + diffInPatternSum;

        if ( patternStartsWithWord
                && (lastFoundCharIsLastInWord || wordFoundRatio > 83.0) ) {
            if ( wcPrevInPattern == lastInPattern ) {
                if ( ( diffInWordSum + (diffInPatternSum * 2) ) < found) {
                    logln(" embrace [start-end]");
                    embraceStartEnd = true;
                    if ( lengthRatioType == LENGTHS_APPROX_EQUAL ) {

                    }
                    else {

                    }
                }
                else if ( diffInWordSum > 0 && diffInPatternSum > 0 && longestDiffInWord > 1 ) {
                    logln("   -40 : embrace [start-end] with a lot of diff [1]");
                    rate = rate - 40;
                    embraceStartEndTooMuchDiff = true;
                }
                else if ( longestDiffInWord >= found && (longestDiffInWord != diffInWordSum || longestDiffInWord > 3) ) {
                    logln("   -40 : embrace [start-end] with a lot of diff [2]");
                    rate = rate - 40;
                    embraceStartEndTooMuchDiff = true;
                }
            }
            else if ( wordFoundRatio == 100f ) {
                if ( (diffInWordSum + (diffInPatternSum * 2) ) < found) {
                    logln(" embrace [start-end]");
                    embraceStartEnd = true;
                    if ( lengthRatioType == LENGTHS_APPROX_EQUAL ) {

                    }
                    else {

                    }
                }
            }
        }

        if ( lastFoundCharIsLastInWord
                && wcPrevInPattern == lastInPattern
                && order ==  found
                && found > 3
                && matchFull >= 2
                && !embraceStartEndTooMuchDiff) {
            if ( (diffInWordSum + (diffInPatternSum * 2) ) > 3) {
                logln(" embrace [end] failed");
                rate = rate - 10;
                embraceEnd = false;
            }
            else {
                logln(" embrace [end]");
                embraceEnd = true;
            }
        }

        if ( patternStartsWithWord ) {
            if ( diffInPatternSum == 0 && !embraceStartEndTooMuchDiff ) {
                if ( found > 3 &&
                        matchFull >= 2 &&
                        (wordChar_1_Missed == 0 || diffInWordSum <= 2) ) {
                    logln(" embrace [start-high]");
                    embraceStartHigh = true;
                    embraceStartLow = true;
                }
                else if ( longestDiffInWord == 1 ) {
                    embraceStartLow = true;
                }
            }
        }


        if ( patternLength < wordLength && patternLength < 5 ) {
            if ( found < patternLength ) {
                if ( !embraceStartHigh && !embraceStartEnd ) {
                    logln("   pattern is too short to accept missed chars!");
                    return -1;
                }
            }
        }

        if ( embraceStartEnd ) {
            logln("   +5 : embraceStartEnd");
            rate = rate + 5;
        }

        if ( embraceEnd ) {
            logln("   +10 : embraceEnd");
            rate = rate + 10;
            if ( matchInPatternStrengthBonus > 0 ) {
                logln("   +5 : embraceEnd && strength bonus > 0");
                rate = rate + 5;
            }
            if ( notFound < 4 ) {
                logln("   +5 : not found < 4");
                rate = rate + 5;
            }
        }

        if ( embraceStartHigh ) {
            logln("   +10 : embrace start-high");
            rate = rate + 10;
        }

        if ( (embraceStartEnd || embraceStartHigh) && Ab_c_pattern ) {
            logln("   +10 : embrace start-high||start-end + ab_c");
            rate = rate + 10;
        }

        if ( embraceStartEnd && embraceEnd ) {
            logln("   +15 + embrace startEnd+end");
            rate = rate + 15;
        }

        boolean anyEmbrace = (embraceStartEnd || embraceEnd || embraceStartHigh) && !embraceStartEndTooMuchDiff;

        if ( found < 4 && mismatchWordChars > 0 ) {
            if ( matchLength > 3 ) {
                rate = rate - 5;
                logln("   other word -5");
                int wordRemnant = wordLength - (found + mismatchWordChars);
                if ( wordRemnant == 1 ) {
                    rate = rate - 15;
                    logln("   other word -15");
                }
                else if ( wordRemnant == 2 ) {
                    rate = rate - 5;
                    logln("   other word -5");
                }
            }
        }

        if ( patternLength < 7 && firstFoundWcInPatternIndex != 0 ) {
            int patternRemnant = patternLength - firstFoundWcInPatternIndex - wordInPatternLength;
            if ( patternRemnant == 1 ) {
                logln("    -10 : single char remains of pattern");
                rate = rate - 10;
            }
        }

        if ( matchFull == 0 ) {
            if ( diffInWordSum >= found ) {
                logln("   -20 full match == 0 && word diff >= found");
                rate = rate - 20;
            }
            if ( matchInPattern < found ) {
                logln("   -30 pattern match < found");
                rate = rate - 30;
            }
        }

        boolean diffTotalTooHigh = false;

        if ( wordChar_1_Missed  == 0 ) {
            if ( order < found ) {
                if ( found < 6 ) {
                    if ( diffInPatternSum > 0 && diffInWordAndPatternSum > found ) {
                        logln("   -30 diff total too high [0]");
                        rate = rate - 30;
                        diffTotalTooHigh = true;
                    }
//                    else if ( diffInPatternSum > 0 && diffInWordAndPatternSum >= found && longestDiffInWord > 2 ) {
//                        logln("   -20 diff total too high [3]");
//                        rate = rate - 20;
//                        diffTotalTooHigh = true;
//                    }
                }
            }
            else {
                if ( matchFull == 0 && found > 4 ) {
                    if ( diffInPatternSum > 1 && diffInWordSum > 1 && diffInWordAndPatternSum > found && longestDiffInWord > 1) {
                        logln("   -40 diff total too high [1]");
                        rate = rate - 40;
                        diffTotalTooHigh = true;
                    }
                }
            }
        }
        else {
            if ( diffInPatternSum > 0 && diffInWordAndPatternSum >= found ) {
                logln("   -40 diff total too high [2]");
                rate = rate - 40;
                diffTotalTooHigh = true;
            }
        }

        if ( embraceStartEnd ) {
            if ( diffInPatternSum == 0 && longestDiffInWord == 1 && matchInPatternStrong > 0 ) {
                if ( diffInWordSum < 3 ) {
                    rate = rate + 35;
                    logln("   +35 longest word diff = 1 and embrace start-end");
                }
                else if ( diffInWordSum < 6 ) {
                    rate = rate + 15;
                    logln("   +15 longest word diff = 1 and embrace start-end");
                }
            }
        }
        else if ( found < 4 ) {
            if ( diffInPatternSum == 0 && diffInWordSum < 3 && longestDiffInWord == 1 && matchInPatternStrong > 0 ) {
                if ( embraceStartHigh ) {
                    rate = rate + 25;
                    logln("   +25 longest word diff = 1 and embrace start-high");
                }
                else if ( embraceStartLow ) {
                    rate = rate + 10;
                    logln("   +10 longest word diff = 1 and embrace start-low");
                    if ( wordFoundRatio > 70 ) {
                        rate = rate + 12;
                        logln("      +12 word found ratio > 70%");
                    }
                }
            }
        }
        else {
            if ( diffInPatternSum == 0 && longestDiffInWord == 1 && matchInPatternStrong > 0 ) {
                rate = rate + 15;
                logln("   +15 longest word diff = 1 and found > 3");
            }
        }

        if ( found == 2 && wordLength == 3 ) {
            if ( patternStartsWithWord ) {
                if ( diffInWordSum == 1 ) {
                    rate = rate + 10;
                    logln("   +10 found=2 wordL=3");
                }
            }
        }

        if ( found == 3 && order == found && backwardMatches == 0 ) {
            if ( matchSpanInPattern < 6 && longestDiffInWord == 1 && patternStartsWithWord && diffInPatternSum < 2 ) {
                if ( wordNotFoundLength < 3 ) {
                    rate = rate + 30;
                    logln("   +30 special rule [1.1] - [PATTERN]ace : [WORD]AbCdE...<3");
                }
                else {

                }
            }

            if ( matchFull == 2 && matchInPattern == 3 ) {
                if ( wordChar_0_Missed == 0 && wordChar_1_Missed == 0 && wordChar_2_Missed == 1 ) {
                    if ( diffInPatternSum == 0 && diffInWordSum == 1 ) {
                        char char0 = word.charAt(0);
                        char char2 = word.charAt(2);
                        if ( char0 == char2 ) {
                            rate = rate + 30;
                            logln("   +30 special rule [1.2] - [PATTERN]abc : [WORD]ABaC...");
                        }
                    }
                }
            }

            if ( matchInPatternStrong == 2 || strongWordEnd ) {
                if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                    logln("     -5 found=order=3 backward=0 strong=2 || stong-end && diffs > 0");
                    rate = rate - 5;
                    if ( diffInPatternSum > 1 && diffInWordSum > 1 ) {
                        logln("     -15 found=order=3 backward=0 strong=2 || stong-end && both diffs > 1");
                        rate = rate - 15;
                    }
                }

                if ( diffInWordAndPatternSum > 3 ) {
                    logln("     -20 found=order=3 backward=0 strong=2 || stong-end && diffs sum > 3");
                    rate = rate - 20;
                }

                if ( diffInPatternSum == 0 ) {
                    if ( diffInWordSum == 0 ) {
                        logln("     +30 : word diff == 0 ");
                        rate = rate + 30;
                    }
                    else if ( diffInWordSum < 3 ) {
                        if ( longestDiffInWord == 2 && wordChar_1_Missed > 0 && wordChar_2_Missed > 0 && (wordLength < 7 || patternLength < 6) ) {

                        }
                        else {
                            logln("     +20 : word diff < 3");
                            rate = rate + 20;
                        }
                    }
                    else {
                        logln("     -20 found=order=3 backward=0 strong=2 && word diff > 2");
                        rate = rate - 20;
                    }
                }

                if ( typoMatches > 0 ) {
                    logln("     -5");
                    rate = rate - 5;
                }
            }
            else if ( matchInPatternStrong == 1 ) {
                if ( diffInPatternSum > 1 && diffInWordSum > 1 ) {
                    logln("     -30 strong = 1 && diff pattern > 0 && diff word > 0");
                    rate = rate - 30;
                }

                if ( patternStartsWithWord ) {
                    if ( diffInWordAndPatternSum > 2 ) {
                        logln("     -10 diffInWordSum + diffInPatternSum > 2");
                        rate = rate - 10;
                    }
                }
                else {
                    if ( diffInWordAndPatternSum > 1 ) {
                        if ( lastFoundCharIsLastInWord &&
                                diffInWordSum > 0 &&
                                diffInPatternSum == 0 &&
                                diffInWordSum == longestDiffInWord &&
                                wordChar_0_Missed == 0 &&
                                wordChar_1_Missed == 0 &&
                                longestDiffInWord < 4 ) {
                            if ( lengthRatioType == LENGTHS_DIFF_SUBSTANTIAL ) {
                                logln("     +5 single diff-in-word, but having start and and of word found");
                                rate = rate + 5;
                            }
                            else if ( lengthRatioType == LENGTHS_DIFF_INSIGNIFICANT ){

                            }
                            else if ( lengthRatioType == LENGTHS_APPROX_EQUAL) {
                                logln("     -10 another word");
                                rate = rate - 10;
                            }
                        }
                        else {
                            logln("     -10 diffInWordSum + diffInPatternSum > 1");
                            rate = rate - 10;
                        }
                    }
                }
            }
            else if ( matchInPatternStrong == 0 ) {
                if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                    logln("     -30 matchInPatternStrong == 0 && diffInPatternSum > 0 && diffInWordSum > 0");
                    rate = rate - 30;
                }
                if ( patternStartsWithWord ) {
                    if ( diffInWordAndPatternSum > 2 ) {
                        logln("     -10 matchInPatternStrong == 0 && patternStartsWithWord && diffInWordSum + diffInPatternSum > 2");
                        rate = rate - 10;
                    }
                }
                else {
                    if ( lastFoundCharIsLastInWord && wordLength < 7 ) {
                        if ( diffInPatternSum == 0 && longestDiffInWord == 1 ) {
                            logln("     +5 matchInPatternStrong == 0 && diffInPatternSum == 0 && longestDiffInWord == 1");
                            rate = rate + 5;
                        }
                        else if ( diffInWordAndPatternSum > 1 ) {
                            logln("     -10 matchInPatternStrong == 0 && diffInWordSum + diffInPatternSum > 1");
                            rate = rate - 10;
                        }
                    }
                    else if ( diffInWordAndPatternSum > 1 ) {
                        if ( diffInPatternSum == 0 ) {

                        }
                        else {
                            logln("     -10 wordLength > 6 word last char is not last found char, diffInWordSum + diffInPatternSum > 1");
                            rate = rate - 10;
                        }
                    }
                }
            }
        }

        if ( found == 4 ) {
            if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                if ( diffInPatternSum > diffInWordSum ) {
                    int penalty = (diffInWordSum + (diffInPatternSum*2)) * 5;
                    rate = rate - penalty;
                    logln("     -penalty[found=4] " + penalty);
                }

                if ( diffInWordAndPatternSum > found && (typoMatches > 0 || backwardMatches > 0) ) {
                    rate = rate - 30;
                    logln("     -30 patternDiff > 0 wordDiff > 0 back||type > 0 diffSum > found");
                }
            }
            else if ( diffInPatternSum == 0 && !embraceStartEndTooMuchDiff ) {
                if ( wordChar_1_Missed == 0 || diffInWordSum <= 2 || anyEmbrace) {
                    if ( matchInPatternStrong == 3 ) {
                        logln("     +30 : pattern diff == 0 word diff <= 2 ");
                        rate = rate + 30;
                    }
                    else if ( matchInPatternStrong == 2 ) {
                        logln("     +20 : strong matches == 2");
                        rate = rate + 20;
                    }
                    else if ( matchInPatternStrong == 1 ) {
//                        logln("     +15 : strong matches == 1");
//                        rate = rate + 15;
                    }
                    else if ( matchInPatternStrong == 0 ) {

                    }
                }
            }

            if ( order == found && backwardMatches == 0 && typoMatches == 0 ) {
                if ( wordChar_0_Missed == 0 && wordChar_1_Missed == 0 && wordChar_2_Missed == 1 ) {
                    char char0 = word.charAt(0);
                    char char2 = word.charAt(2);
                    if ( char0 == char2 ) {
                        rate = rate + 30;
                        logln("   +30 special rule [1.3] - [PATTERN]abcx : [WORD]ABaC.X..");
                    }
                }
            }
        }

        if ( found > 4 ) {
            if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                if ( order < found ) {
                    if ( diffInPatternSum > diffInWordSum ) {
                        int penalty = (diffInWordSum + (diffInPatternSum * 2)) * 5;
                        rate = rate - penalty;
                        logln("     -penalty[found>4] " + penalty);
                    }
                }

                if ( diffInWordAndPatternSum >= found && longestDiffInWord > 2 ) {
                    if ( typoMatches > 0 ) {
                        rate = rate - 40;
                        logln("     -40 typo, wordDiff > 0, patternDiff > 0, longest diff > 2");
                    }
                    else {
                        rate = rate - 30;
                        logln("     -30 wordDiff > 0, patternDiff > 0, longest diff > 2");
                    }

                }

                if ( matchType == WORD_IS_PATTERN_FRAGMENT && ( patternLength - wordInPatternLength ) <= 3 ) {
                    if ( diffInWordAndPatternSum >= found ) {
                        rate = rate - 10;
                        logln("      -10 too much diffs penalty");
                    }
                }
            }
            else if ( diffInWordAndPatternSum > 0 ) {
                if ( longestDiffInPattern > 1 && diffInPatternSum != longestDiffInPattern ) {
                    if ( matchFull < 3 ) {
                        rate = rate - 5;
                        logln("      -5 too much pattern diffs for long match");
                    }
                }
            }

            if ( order == found && backwardMatches == 0 && typoMatches == 0 ) {
                if ( wordChar_0_Missed == 0 && wordChar_1_Missed == 0 && wordChar_2_Missed == 1 ) {
                    char char0 = word.charAt(0);
                    char char2 = word.charAt(2);
                    if ( char0 == char2 ) {
                        rate = rate + 30;
                        logln("   +30 special rule [1.4] - [PATTERN]abcxy : [WORD]ABaC..X..Y..");
                    }
                }
            }
        }

        if ( mismatches == 0 && found > 2 ) {
            if ( found == matchLength ) {
                if ( matchInPattern == matchLength ) {
                    rate = rate + matchLength;
                }
                if ( gaps == 0 ) {
                    int totalIndirectMatches = matchInPattern + backwardMatches;
                    if ( totalIndirectMatches > found ) {
                        rate = rate + 10;
                    }
                }
            }

            if ( found == matchInPattern &&
                    order == found &&
                    backwardMatches == 0 &&
                    matchInPatternStrong > matchInPatternWeak ) {
                if ( matchFull >= (found / 2) ) {
//                    if ( found > 3 && (diffInPatternSum + diffInWordSum) <= (found-matchFull)) {
//                        rate = rate + 20;
//                    }
//                    else {
//                        rate = rate + 10;
//                    }

                    if ( found > 3 ) {
                        int patternMatchesOnly = found - matchFull;
                        if ( diffInPatternSum == 0 && diffInWordSum <= patternMatchesOnly ) {
                            rate = rate + 30;
                            logln("     +30 [zdbg]");
                        }
                        else if ( diffInWordAndPatternSum <= patternMatchesOnly ) {
                            rate = rate + 20;
                            logln("     +20 [prxu]");
                        }
                        else {
                            if ( wordChar_1_Missed == 0 ) {
                                rate = rate + 10;
                                logln("     +10 : ... found > 3 and second char not missed ");
                            }
                            else {
                                if ( diffInWordSum >= found ) {
                                    rate = rate - 30;
                                    logln("     -30 diffInWordSum >= found");
                                }
                                else {
                                    rate = rate - 10;
                                    logln("     -10 diffInWordSum < found");
                                }
                            }
                        }
                    }
                }
                else {
                    rate = rate + matchInPattern;
                }
            }
            else if ( matchInPattern > 2 ) {
                int patternMatchLength = wcPrevInPattern /* == last found in pattern */  - firstWcInPatternIndex + 1;
                if ( patternMatchLength == found ) {
                    if ( backwardMatches > 0 && !diffTotalTooHigh ) {
                        int backwardMatchesL = backwardMatches - 1;
                        if ( found == (matchInPattern + backwardMatchesL ) && order == found - backwardMatchesL ) {
                            int newRate = found * 10
                                    + found * matchInPatternWeight
                                    + found * 5
                                    + typoMatches * 3;
                            logln("    fix no-full-match, rate %s -> %s", rate, newRate);
                            rate = newRate;
                        }
                    }
                    else {

                    }
                } else if ( patternMatchLength > found ) {
                    int accepted;
                    switch ( found ) {
                        case 1:
                        case 2:
                        case 3:
                        case 4: accepted = 0; break;
                        case 5:
                        case 6: accepted = 1; break;
                        case 7:
                        case 8: accepted = 2; break;
                        default: accepted = 3;
                    }

                    if ( diffInWordSum + diffInPattern > found ) {
                        logln("   too much diffs");
                    }
                    else if ( patternMatchLength - found <= accepted && !diffTotalTooHigh ) {
                        boolean tooWeak =
                                matchFull > 0 && matchFull < 3 && diffInPatternSum > 1;
                        if ( ! embraceStartEndTooMuchDiff && ! tooWeak ) {
                            matchInPatternWeight = 6;
                            int newRate = found * 10
                                    + found * matchInPatternWeight
                                    + found * 5
                                    + typoMatches * 3;
                            logln("   reassign rate %s -> %s", rate, newRate);
                            rate = newRate;
                        }
                    }
                }
            }

            if ( found == matchFull && found == matchInPattern ) {
                rate = rate + matchFull * 2;
            }
        }

        logln("  order:%s, found:%s, pattern-span:%s, word-span:%s, full-matches:%s, pattern-matches:%s (s:%s w:%s), back-matches:%s, typo-matches:%s, w-diff:%s (longest:%s), p-diff:%s (longest:%s), 0-ch-miss:%s, 1-ch-miss:%s, 2-ch-miss:%s", order, found, matchSpanInPattern, matchSpanInWord, matchFull, matchInPattern, matchInPatternStrong, matchInPatternWeak, backwardMatches, typoMatches, diffInWordSum, longestDiffInWord, diffInPatternSum, longestDiffInPattern, wordChar_0_Missed, wordChar_1_Missed, wordChar_2_Missed);
        logln("  pattern: ");
        logln("     word-in-pattern length: " + wordInPatternLength);
        logln("  word: ");
        logln("     indexes : %s %s ", wordFirstFoundCharIndex, wordLastFoundCharIndex);
        logln("     length  : %s ", wordFoundLength);
        logln("     ratio   : %s%% ", wordFoundRatio);

        int matchFullVsPatternDiff = matchInPattern - matchFull;

        if ( matchFull == 2 && backwardMatches == 0 ) {
            if ( matchFullVsPatternDiff > 1 && matchInPatternWeak > 0 ) {
                if ( matchType == PATTERN_IS_WORD_FRAGMENT || matchInPatternStrengthBonus == 0 ) {
                    if ( diffInPatternSum > 0 || wordChar_1_Missed == 1 ) {
                        if ( diffInWordSum <= found/2 ) {
                            if ( found > 5 ) {
                                rate = rate + 20;
                                logln("    +20 ??: ");
                            }
                        }
                        else {
                            int penaltyConditions =
                                    (found < 5              ? 1 : 0) +
                                    (wordFoundRatio < 70    ? 1 : 0) +
                                    (order < found          ? 1 : 0);

                            if ( penaltyConditions > 1 ) {
                                int penalty = matchInPatternWeight * matchInPatternWeak;
                                rate = rate - penalty;
                                logln("    penalty[2]: " + penalty);
                            }
                        }
                    }
                }
            }
            else if ( matchFullVsPatternDiff == 1 && matchType == WORD_IS_PATTERN_FRAGMENT && diffInWordSum < 3 ) {
                if ( ! STRICT ) {
                }
            }
        }

//        if ( matchIndex+1 >= matchLength/2 ) {
//            logln("secondary word");
//            if ( anyEmbrace ) {
//                logln("good word");
//            }
//            else {
//                if ( wordFoundRatio <= 40 ) {
//                    logln("[1]ratio <= 40%");
//                }
//                logln("secondary no embrace");
//                if ( matchLength > 6 )  {
//
////                    if ( wordFoundRatio < 45 || wordFoundLength <=3 ) {
////                        rate = rate - 30;
////                        logln("    -30 ratio<50% and wordFoundLength <=3 ");
////                    }
//
//                    if ( found < 4 ) {
//                        if ( wordFoundRatio <= 40 ) {
//                            logln("secondary no embrace, bad, ratio <= 40%");
//                        }
//                        else {
//                            logln("secondary no embrace, bad");
//                        }
//                    }
//                    else if ( found == 4 ) {
//                        if ( wordFoundRatio <= 40 ) {
//                            logln("secondary no embrace, moderate, ratio <= 40%");
//                        }
//                        else {
//                            logln("secondary no embrace, moderate");
//                        }
//                    }
//                    else {
//                        if ( wordFoundRatio <= 40 ) {
//                            logln("secondary no embrace, good, ratio <= 40%");
//                        }
//                        else {
//                            logln("secondary no embrace, good");
//                        }
//                    }
//                }
//            }
//        }

        if ( matchInPatternStrong + matchInPatternWeak >= found ) {
            throw new IllegalStateException();
        }

        int requiredFullMatches;
        int requiredPatternMatches;
        int thresholdStrictBonus = 0;

        switch ( matchLength ) {
            case 1:
            case 2:
            case 3:
                requiredFullMatches = 2;
                requiredPatternMatches = 2;
                break;
            case 4:
                requiredFullMatches = 2;
                requiredPatternMatches = 3;
                break;
            case 5:
                if ( found >= 4 || matchType == PATTERN_IS_WORD_FRAGMENT || STRICT ) {
                    if ( (lengthRatioType == LENGTHS_APPROX_EQUAL || lengthRatioType == LENGTHS_DIFF_INSIGNIFICANT) && matchFullVsPatternDiff > 1 ) {
                        logln("  threshold +5");
                        thresholdStrictBonus = 5;
                    }
                    requiredFullMatches = 3;
                    requiredPatternMatches = 3;
                }
                else {
                    requiredFullMatches = 2;
                    requiredPatternMatches = 3;
                }
                break;
            case 6:
                if ( found >= 5 || matchType == PATTERN_IS_WORD_FRAGMENT) {
                    requiredFullMatches = 3;
                    requiredPatternMatches = 3;
                }
                else {
                    requiredFullMatches = 2;
                    requiredPatternMatches = 3;
                }
                break;
            case 7:
                requiredFullMatches = 3;
                requiredPatternMatches = 4;
                break;
            case 8:
                requiredFullMatches = 4;
                requiredPatternMatches = 4;
                break;
            case 9:
            default:
                requiredFullMatches = 4;
                requiredPatternMatches = 5;
        }

        int threshold = requiredFullMatches*10 + requiredPatternMatches*7 + found*5 + thresholdStrictBonus;
        logln("  threshold:%s, total:%s", threshold, rate);
        if ( rate >= threshold ) {

            long code = CODE_V2_BASE;

            code = code + patternLength                 * 100000000000L;
            code = code + rate                          * 100000000L;
            code = code + wordLength                    * 1000000L;
            code = code + firstFoundWcInPatternIndex    * 10000L;
            code = code + matchSpanInPattern            * 100L;
            code = code + found;

            return code;
        }
        else {
            return -1;
        }
    }
    /* script */

    private static String higlightChar(String s, int i) {
        char[] chars = s.toCharArray();
        char c = chars[i];
        char C = Character.toUpperCase(c);
        chars[i] = C;

        String ss = s.substring(0, i) + '(' + C + ')' + s.substring(i + 1);
        return ss;
    }
}
