package diarsid.librarian.impl.logic.impl.search.charscan.matching;

public final class PatternToWordMatchingV34 extends PatternToWordMatchingCodeV2 {

    @Override
    /* script */
    public long evaluate(String pattern, String word) {
        logln("%s : PATTERN:%s  <--->  WORD:%s", this.nameAndVersion(), pattern, word);

        final long CODE_BASE = 10000000000000L;

        int wordLength = word.length();
        int patternLength = pattern.length();
        int bothLength = wordLength + patternLength;
        int matchLength;
        int rangeLength;
        int diffLength;
        int lengthRatioType;

        final int LENGTHS_APPROX_EQUAL = 1;
        final int LENGTHS_DIFF_INSIGNIFICANT = 2;
        final int LENGTHS_DIFF_SUBSTANTIAL = 3;

        if ( patternLength >= wordLength) {
            matchLength = wordLength;
            rangeLength = patternLength;

            int wordInPatternIndex = pattern.indexOf(word);
            if ( wordInPatternIndex > -1 ) {
                int rate = wordLength*10 + wordLength*7 + wordLength*5;
                logln("   " + rate);
                long code = CODE_BASE;

                code = code + patternLength         * 100000000000L;
                code = code + rate                  * 100000000L;
                code = code + wordLength            * 1000000L;
                code = code + wordInPatternIndex    * 10000L;
                code = code + wordLength            * 100L;
                code = code + wordLength;

                return code;
            }
            else if ( wordLength == 2 ) {
                logln("  [c]");
                return -1;
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
                logln("   " + rate);
                long code = CODE_BASE;

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

        int firstCharMissed = 0;
        int secondCharMissed = 0;
        int mismatchesOnlyWord = 0;
        int mismatches = 0;
        int found = 0;
        int matchFull = 0;
        int matchInPattern = 0;
        int matchInPatternWeak = 0;
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

        char wc;
        char wcPrev = '_';
        int wcInPattern;
        int wcPrevInPattern = -1;
        int iPrev = -1;
        int firstWcInPattern = -1;
        int firstFoundWcInPattern = -1;
        int wordInPatternLength;
        int diffInPattern = -1;
        int diffInWord = -1;

        int longestDiffInWord = -1;

        final int PREV_CHAR_UNINITIALIZED = -1;
        final int PREV_CHAR_UNKNOWN = 0;
        final int PREV_CHAR_NOT_FOUND = 1;
        final int PREV_CHAR_FOUND = 2;
        final int PREV_CHAR_MATCH_PATTERN = 3;
        final int PREV_CHAR_MATCH_FULL = 4;

        final String PREV_CHAR_WEAK = "WEAK";
        final String PREV_CHAR_STRONG = "STRONG";

        int prevCharResult = PREV_CHAR_UNINITIALIZED;
        String prevCharMatchStrength = null;

        wordCharsIterating: for (int i = 0; i < wordLength; i++) {
            wc = word.charAt(i);
            wcInPattern = pattern.indexOf(wc, wcPrevInPattern + 1);
            diffInPattern = -1;
            diffInWord = -1;

            if ( wcInPattern < 0 ) {
                if ( i == 0 ) {
                    logln("   WORD:%s not found! ", wc, wcPrev);
                    firstCharMissed++;
                    mismatches++;
                }
                else if ( i == 1 ) {
                    logln("   WORD:%s not found! ", wc, wcPrev);
                    secondCharMissed++;
                    if ( firstCharMissed > 0 ) {
                        logln("      mismatches major!");
                        return -1;
                    }
                }
                else {
                    logln("   WORD:%s not found after WORD:%s[PATTERN:%s] - %s : %s", wc, wcPrev, wcPrevInPattern, higlightChar(word, i), higlightChar(pattern, wcPrevInPattern));
                    int wcInPatternWeak = pattern.indexOf(wc, firstWcInPattern);
                    if ( wcInPatternWeak < 0 ) {
                        logln("      WORD:%s not found from word-in-pattern beginning [PATTERN:%s]", wc, firstWcInPattern);
                        prevCharResult = PREV_CHAR_NOT_FOUND;
                    }
                    else {
                        if ( wcPrevInPattern > -1 ) {
                            int distanceFromItoWordEnd = lastInWord - i;
                            int distanceFromCPrevToC = wcPrevInPattern - wcInPatternWeak;
                            if ( distanceFromCPrevToC > distanceFromItoWordEnd ) {
                                logln("      WORD:%s[PATTERN:%s] too far [1]!", wc, wcInPatternWeak);
                                break wordCharsIterating;
                            }
                            else {
                                if ( i != lastInWord ) {
                                    if ( wcInPatternWeak != firstWcInPattern ) {
                                        if ( wcInPatternWeak == wcPrevInPattern - 1 ) {
                                            if ( i == iPrev + 1 ) {
                                                boolean backwardMatchProhibited =
                                                        (i+1)/2 > matchInPattern+1 ||
                                                        secondCharMissed > 0;
                                                if ( backwardMatchProhibited ) {
                                                    logln("         WORD:%s[PATTERN:%s] backward match [+1] prohibited!",  wc, wcInPatternWeak);
                                                }
                                                else {
                                                    if ( gaps > 0 ) {
                                                        logln("      WORD:%s[PATTERN:%s] backward match [+1] from word-in-pattern beginning [PATTERN:%s] before WORD:%s[PATTERN:%s]",  wc, wcInPatternWeak, firstWcInPattern, wcPrev, wcPrevInPattern);
                                                        backwardMatches++;
                                                        found++;
                                                        gaps--;

                                                        boolean firstCharsTypo =
                                                                wcInPatternWeak == firstWcInPattern + 1 &&
                                                                (prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND);
                                                        if ( firstCharsTypo ) {
                                                            logln("        abc-acb typo!");
                                                            order++;
                                                            matchInPattern++;
                                                            matchFull++;
                                                            matchFull++;
                                                            matchInPatternStrong++;
                                                            matchInPatternStrong++;
                                                            matchInPatternWeak--;
                                                            diffInWordSum--;
                                                            diffInPatternSum--;
                                                            prevCharMatchStrength = PREV_CHAR_STRONG;
                                                            prevCharResult = PREV_CHAR_MATCH_FULL;
                                                        }

                                                        diffInPatternSum = diffInPatternSum - (wcPrevInPattern - wcInPatternWeak) + 1;
                                                        if ( i != lastInWord && wcPrevInPattern+2 < patternLength-1 ) {
                                                            char wcNext = word.charAt(i+1);
                                                            if ( wcPrev != wcNext ) {
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
                                                        secondCharMissed > 0;
                                                if ( backwardMatch2Prohibited ) {
                                                    logln("         WORD:%s[PATTERN:%s] backward match [+2] prohibited!",  wc, wcInPatternWeak);
                                                    mismatchesOnlyWord++;
                                                }
                                                else {
                                                    if ( gaps > 0 ) {
                                                        logln("      WORD:%s[PATTERN:%s] backward match [+2] from word-in-pattern beginning [PATTERN:%s] before WORD:%s[PATTERN:%s]",  wc, wcInPatternWeak, firstWcInPattern, wcPrev, wcPrevInPattern);
                                                        wcPrevInPattern = wcInPatternWeak;
                                                        backwardMatches++;
                                                        found++;
                                                        gaps--;
                                                    }
                                                }
                                            }
                                            else {
                                                mismatchesOnlyWord++;
                                            }
                                        }
                                        else {
                                            typoMatches++;
                                            logln("      WORD:%s[PATTERN:%s] typo match from word-in-pattern beginning", wc, wcInPatternWeak);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else /* wcInPattern >= 0 */ {
                if ( i == 0 ) {
                    firstWcInPattern = wcInPattern;
                }
                else if ( firstWcInPattern < 0 && firstFoundWcInPattern < 0 ) {
                    firstFoundWcInPattern = wcInPattern;
                }

                if ( wcPrevInPattern > -1 ) {
                    int distanceFromCPrevToC = wcInPattern - wcPrevInPattern;
                    if ( distanceFromCPrevToC > 1 ) {
                        order++;
                        logln("   order+ [2]");
                        int distanceFromItoWordEnd = wordLength - i;
                        if ( distanceFromCPrevToC > 2 && distanceFromCPrevToC >= matchInPattern) {

                            if ( matchFull < 1 ) {
                                // no full matches - can loop to find stronger matching of prev char
                                int wcPrevInPatternN = wcPrevInPattern;
                                int wcPrevInPatternNprev = -1;
                                boolean goFurther = true;
                                while ( goFurther ) {
                                    wcPrevInPatternNprev = wcPrevInPatternN;
                                    wcPrevInPatternN = pattern.indexOf(wcPrev, wcPrevInPatternN + 1);
                                    goFurther = wcPrevInPatternN > -1 && wcPrevInPatternN < wcInPattern;
                                }
                                if ( wcPrevInPatternNprev != wcPrevInPattern ) {
                                    if ( firstWcInPattern == wcPrevInPattern ) {
                                        if ( wcPrevInPatternNprev > wcPrevInPattern && wcPrevInPatternNprev < wcInPattern ) {
                                            logln("      WORD:%s[PATTERN:%s] -> WORD:%s[PATTERN:%s]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            firstWcInPattern = wcPrevInPatternNprev;
                                            wcPrevInPattern = wcPrevInPatternNprev;
                                        }
                                    }
                                    else {
                                        int distanceChange = wcPrevInPatternNprev - wcPrevInPattern;
                                        if ( distanceChange > 0 && distanceChange < distanceFromItoWordEnd && wcPrevInPatternNprev < wcInPattern ) {
                                            logln("      WORD:%s[PATTERN:%s] -> WORD:%s[PATTERN:%s]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            wcPrevInPattern = wcPrevInPatternNprev;
                                            if ( diffInPatternSum > 0 ) {
                                                diffInPatternSum = diffInPatternSum + distanceChange - 1;
                                            }
                                            distanceFromCPrevToC = wcInPattern - wcPrevInPattern;
                                            if ( distanceFromCPrevToC > (distanceFromItoWordEnd + 2) ) {
                                                logln("   WORD:%s not found on reasonable length [1] after WORD:%s[PATTERN:%s], found at [PATTERN:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                                order--;
                                                logln("   order-- [2][1]");
                                                prevCharResult = PREV_CHAR_NOT_FOUND;
                                                continue wordCharsIterating;
                                            }
                                        }
                                        else {
                                            logln("   WORD:%s not found on reasonable length [2] after WORD:%s[PATTERN:%s], found at [PATTERN:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                            prevCharResult = PREV_CHAR_NOT_FOUND;
                                            order--;
                                            logln("   order-- [2][2]");
                                            continue wordCharsIterating;
                                        }
                                    }
                                }
                            }
                            else /* matchFull >= 1 */ {
                                logln("   WORD:%s not found on reasonable length after [3] WORD:%s[PATTERN:%s], found at [PATTERN:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                if ( prevCharResult != PREV_CHAR_NOT_FOUND ) {
                                    mismatchWordChars++;
                                }

                                boolean foundInGap = false;
                                if ( gaps > 0 ) {
                                    int from = firstWcInPattern > -1 ? firstWcInPattern : firstFoundWcInPattern;
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
                                                found++;
                                                if ( prevCharResult != PREV_CHAR_NOT_FOUND ) {
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
                                            firstWcInPattern = i2FirstWcInPattern;
                                            wcPrevInPattern = i2wcInPatternPrev;
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
                        logln("   order+ [1]");
                        order++;
                    }
                }

                logln("   WORD:%s[PATTERN:%s]", wc, wcInPattern);
                if ( wcPrevInPattern > -1 ) {
                    diffInPattern = wcInPattern - wcPrevInPattern;
                    diffInWord = i - iPrev;

                    if ( longestDiffInWord < 0 || diffInWord-1 > longestDiffInWord ) {
                        longestDiffInWord = diffInWord-1;
                    }

                    if ( diffInPattern == 1 ) {
                        logln("      PATTERN MATCH");
                        matchInPattern++;
                        if ( diffInWord == 1 ) {
                            logln("      FULL MATCH [1]");
                            matchFull++;
                            if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                                matchInPatternStrong++;
                                logln("        strong, prev match is full {1}");
                                prevCharMatchStrength = PREV_CHAR_STRONG;
                            }
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND ) {
                                if ( matchInPatternWeak > 0 ) {
                                    if ( diffInWordSum >= 3 && matchFull < 2 ) {
                                        logln("        weak++");
                                        matchInPatternWeak++;
                                        prevCharMatchStrength = PREV_CHAR_WEAK;
                                    }
                                    else {
                                        if ( diffInWordSum == 1 ) {
                                            logln("        weak-- strong++");
                                            matchInPatternStrong++;
                                            matchInPatternWeak--;
                                        }
                                        matchInPatternStrong++;
                                        logln("        strong, prev match is pattern or found {1}");
                                        prevCharMatchStrength = PREV_CHAR_STRONG;
                                        if ( mismatches == 0 && diffInWordSum <= 3 ) {
                                            logln("        strong bonus");
                                            matchInPatternStrengthBonus++;
                                        }
                                    }
                                }
                                else {
                                    matchInPatternStrong++;
                                    prevCharMatchStrength = PREV_CHAR_STRONG;
                                    logln("        strong, full match");
                                }
                            }
                            prevCharResult = PREV_CHAR_MATCH_FULL;
                        }
                        else {
                            if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                                if ( i == lastInWord ) {
                                    matchInPatternStrong++;
                                    logln("        strong, prev match is full and char finishes the word");
                                    prevCharMatchStrength = PREV_CHAR_STRONG;
                                }
                                else {
                                    if ( matchInPattern > 2 && matchFull > 1 ) {
                                        matchInPatternStrong++;
                                        logln("        strong, prev match is full {2}");
                                        prevCharMatchStrength = PREV_CHAR_STRONG;
                                    }
                                    else {
                                        matchInPatternWeak++;
                                        logln("        weak [1]");
                                        prevCharMatchStrength = PREV_CHAR_WEAK;
                                    }
                                }
                            }
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN ) {
                                if ( i == lastInWord ) {
                                    matchInPatternStrong++;
                                    logln("        strong, prev match is pattern but char finishes the word");
                                    if ( prevCharResult == PREV_CHAR_MATCH_PATTERN && prevCharMatchStrength == PREV_CHAR_WEAK ) {
                                        matchInPatternWeak--;
                                        matchInPatternStrong++;
                                    }
                                    prevCharMatchStrength = PREV_CHAR_STRONG;
                                }
                                else {
                                    matchInPatternWeak++;
                                    logln("        weak [2]");
                                    prevCharMatchStrength = PREV_CHAR_WEAK;
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
                                    logln("        strong?");
                                    prevCharMatchStrength = PREV_CHAR_STRONG;
                                }
                                else {
                                    matchInPatternWeak++;
                                    logln("        weak [3]");
                                    prevCharMatchStrength = PREV_CHAR_WEAK;
                                }
                            }
                            prevCharResult = PREV_CHAR_MATCH_PATTERN;
                        }
                    }
                    else {
                        int gap = diffInPattern - 1;
                        if ( i < gap ) {
                            boolean gapToBigToAccept =
                                    gap > 2
                                            || diffInPattern > (patternLength / 2);
                            if ( gapToBigToAccept ) {
                                logln("      WORD:%s[PATTERN:%s] too far [4], ignore and continue!", wc, wcInPattern);
                                order--;
                                logln("      order--");
                                continue wordCharsIterating;
                            }
                        }

                        boolean cPrevIndexBeforeCIndex = wcPrevInPattern < wcInPattern;
                        if ( cPrevIndexBeforeCIndex ) {
                            matchInPattern++;
                            logln("      PATTERN MATCH, prev before current");
                            if ( prevCharResult == PREV_CHAR_MATCH_FULL ) {
                                matchInPatternStrong++;
                                logln("        strong, prev match is full {3}");
                                prevCharMatchStrength = PREV_CHAR_STRONG;
                            }
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN
                                    || prevCharResult == PREV_CHAR_FOUND
                                    || prevCharResult == PREV_CHAR_NOT_FOUND ) {
                                matchInPatternWeak++;
                                logln("        weak [4]");
                                prevCharMatchStrength = PREV_CHAR_WEAK;
                            }
                            prevCharResult = PREV_CHAR_MATCH_PATTERN;
                        }

                        boolean diffMoreThanOne = diffInPattern > 1;
                        int wcPrevInPatternN = wcPrevInPattern;
                        gapFixing: while ( diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                            gaps++;
                            logln("      gap between char WORD:%s[PATTERN:%s] and previous char WORD:%s[PATTERN:%s]!", wc, wcInPattern, wcPrev, wcPrevInPattern);

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
                                wcPrevInPattern = wcPrevInPatternN;
                                logln("      gap fixed char WORD:%s[PATTERN:%s] and previous char WORD:%s[PATTERN:%s] - PATTERN:%s!", wc, wcInPattern, wcPrev, wcPrevInPattern, wcPrevInPatternN);
                                gaps--;
                                logln("      MATCH, gap fixed");
                                if ( diffInWord == 1 ) {
                                    logln("      FULL MATCH [2]");
                                    matchFull++;
                                    if ( prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND ) {
                                        if ( matchInPatternWeak > 0 ) {
                                            matchInPatternWeak--;
                                            logln("        weak--");
                                            matchInPatternStrong++;
                                            logln("        strong [1]");
                                            prevCharMatchStrength = PREV_CHAR_STRONG;
                                            if ( mismatches == 0 && diffInWordSum < 2 ) {
                                                logln("        strong bonus");
                                                matchInPatternStrengthBonus++;
                                            }
                                        }
                                        else {
                                            matchInPatternStrong++;
                                            logln("        strong [2]");
                                        }
                                    }
                                    prevCharMatchStrength = PREV_CHAR_STRONG;
                                    prevCharResult = PREV_CHAR_MATCH_FULL;
                                }
                            }
                        }
                    }
                }

                wcPrev = wc;
                if ( firstWcInPattern > -1 ) {
                    wordInPatternLength = wcInPattern - firstWcInPattern + 1;
                }
                else {
                    wordInPatternLength = wcInPattern - firstFoundWcInPattern + 1;
                }

                if ( wordInPatternLength > wordLength + 2 ) {
                    logln("      word-in-pattern-length is substantially longer than word");
                    break wordCharsIterating;
                }

                if ( diffInWord > -1 ) {
                    diffInWordSum = diffInWordSum + diffInWord - 1;
                    diffInPatternSum = diffInPatternSum + diffInPattern - 1;
                    logln("      diff++");
                }

                wcPrevInPattern = wcInPattern;
                iPrev = i;

                found++;
                if ( prevCharResult == PREV_CHAR_UNINITIALIZED ) {
                    prevCharResult = PREV_CHAR_FOUND;
                }

                if ( wcInPattern == lastInPattern ) {
                    if ( rangeLength < 6 && found < matchLength && gaps > 0 ) {
                        wcPrevInPattern = firstWcInPattern > -1 ? firstWcInPattern : firstFoundWcInPattern;
                        continue wordCharsIterating;
                    }
                    else {
                        logln("      end of pattern reached");
                        if ( i == lastInWord-1 ) {
                            boolean noDoubleChars = wc != word.charAt(lastInWord);
                            boolean longEnough = wordLength > 4;
                            boolean notGoodEnough = matchFull == 1;
                            boolean diffsTooHigh = diffInWordSum > 1 || diffInPatternSum > 1;
                            boolean moreWeakThanStrong = matchInPatternStrong <= matchInPatternWeak;
                            if ( noDoubleChars && longEnough && notGoodEnough && diffsTooHigh && moreWeakThanStrong ) {
                                logln("      only 1 char remain in word");
                                if ( prevCharMatchStrength == PREV_CHAR_STRONG ) {
                                    matchInPatternWeak++;
                                    logln("        weak++ strong--");
                                    matchInPatternStrong--;
                                    prevCharMatchStrength = PREV_CHAR_WEAK;
                                }
                            }
                        }
                        break wordCharsIterating;
                    }
                }
            }
        }

        int matchIndex;
        if ( firstWcInPattern > -1 ) {
            matchIndex = firstWcInPattern;
        }
        else {
            matchIndex = firstFoundWcInPattern;
        }

        strongWordEnd =
                prevCharResult == PREV_CHAR_MATCH_FULL
                && prevCharMatchStrength == PREV_CHAR_STRONG
                && diffInPatternSum == 0;

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

        if ( found == 0 || order == 0 || matchFull + matchInPattern == 0 ) {
            logln("   not found anything!");
            return -1;
        }

        if ( patternLength < wordLength && patternLength < 5 ) {
            if ( found < patternLength ) {
                logln("   pattern is too short to accept missed chars!");
                return -1;
            }
        }

        final int WORD_IS_PATTERN_FRAGMENT = 1;
        final int PATTERN_IS_WORD_FRAGMENT = 2;

        int matchType = WORD_IS_PATTERN_FRAGMENT;
        int notFound = matchLength - found;
        if ( patternLength <= wordLength ) {
            if ( found == matchLength ) {
                matchType = PATTERN_IS_WORD_FRAGMENT;
            }
            else {
                if ( notFound == 1 ) {
                    matchType = PATTERN_IS_WORD_FRAGMENT;
                }
                else if ( notFound == 2 && found > 3 ) {
                    matchType = PATTERN_IS_WORD_FRAGMENT;
                }
                else if ( notFound == 2 && found >= 2 && firstWcInPattern == 0) {
                    matchType = PATTERN_IS_WORD_FRAGMENT;
                }
            }
        }

        logln("match type: %s", matchType == WORD_IS_PATTERN_FRAGMENT ? "WORD_IS_PATTERN_FRAGMENT" : "PATTERN_IS_WORD_FRAGMENT");

        if ( matchType == PATTERN_IS_WORD_FRAGMENT ) {
            if ( firstWcInPattern != 0 ) {
                firstCharMissed++;
            }
            secondCharMissed = secondCharMissed + mismatchesOnlyWord;
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
                if ( found < matchLength && firstWcInPattern != 0 ) {
                    firstCharMissed++;
                    mismatches++;
                }
            }

            if ( notFound == 1 && matchFull < 3 && matchType == PATTERN_IS_WORD_FRAGMENT ) {
                mismatches++;
            }
        }

        int rate;
        if ( matchFull > 2 || matchInPatternStrong > matchFull || matchInPatternWeak == 0 ) {
            rate = matchFull * 10
                    + matchInPattern * matchInPatternWeight
                    + matchInPatternStrengthBonus * 6
                    + backwardMatches * 6
                    + found * 5
                    + typoMatches * typoMatchesWeight
                    - firstCharMissed * 35
                    - secondCharMissed * mismatchesMinorWeight;
        }
        else {
            rate = matchFull * 10
                    + matchInPatternWeight + matchInPatternWeight * matchInPatternStrong
                    + matchInPatternStrengthBonus * 6
                    + matchInPatternWeakWeight * matchInPatternWeak
                    + backwardMatches * 6
                    + found * 5
                    + typoMatches * typoMatchesWeight
                    - firstCharMissed * 35
                    - secondCharMissed * mismatchesMinorWeight;
        }

        if ( order > found ) {
            logln("ORDER > FOUND");
        }

        boolean embraceStartEnd = false;
        boolean embraceStartEndTooMuchDiff = false;
        boolean embraceEnd = false;
        boolean embraceStart = false;

        if ( matchIndex == 0
                && iPrev == lastInWord
                && wcPrevInPattern == lastInPattern ) {
            if ( (diffInWordSum + diffInPatternSum*2 ) < found) {
                logln(" embrace [start-end]");
                embraceStartEnd = true;
                if ( lengthRatioType == LENGTHS_APPROX_EQUAL ) {

                }
                else {

                }
            }
            else if ( (diffInWordSum > 0 && diffInPatternSum > 0) || longestDiffInWord >= found ) {
                logln("   -40 : embrace [start-end] with a lot of diff");
                rate = rate - 40;
                embraceStartEndTooMuchDiff = true;
            }
        }

        if ( iPrev == lastInWord
                && wcPrevInPattern == lastInPattern
                && order ==  found
                && found > 3
                && matchFull >= 2
                && (diffInWordSum + diffInPatternSum*2) < 4
                && !embraceStartEndTooMuchDiff) {
            logln(" embrace [end]");
            embraceEnd = true;
        }

        if ( matchIndex == 0 &&
                found > 3 &&
                matchFull >= 2 &&
                (secondCharMissed == 0 || diffInWordSum <= 2) &&
                diffInPatternSum == 0
                && !embraceStartEndTooMuchDiff) {
            logln(" embrace [start]");
            embraceStart = true;
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

        if ( embraceStart ) {
            logln("   +10 : embraceStart");
            rate = rate + 10;
        }

        if ( (embraceStartEnd || embraceStart) && Ab_c_pattern ) {
            logln("   +10 : embrace start|end + ab_c");
            rate = rate + 10;
        }

        if ( embraceStartEnd && embraceEnd ) {
            logln("   +15 + embrace startEnd+end");
            rate = rate + 15;
        }

        boolean anyEmbrace = (embraceStartEnd || embraceEnd || embraceStart) && !embraceStartEndTooMuchDiff;

        if ( found < 4 && mismatchWordChars > 0 ) {
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

        if ( matchFull == 0 ) {
            if ( diffInWordSum >= found ) {
                logln("   -20");
                rate = rate - 20;
            }
            if ( matchInPattern < found ) {
                logln("   -30");
                rate = rate - 30;
            }
        }

        boolean diffTotalTooHigh = false;
        int diffTotalSum = diffInWordSum + diffInPatternSum;
        if ( secondCharMissed  == 0 ) {
            if ( diffInPatternSum > 0 && diffTotalSum > found ) {
                logln("   -30");
                rate = rate - 30;
                diffTotalTooHigh = true;
            }
        }
        else {
            if ( diffInPatternSum > 0 && diffTotalSum >= found ) {
                logln("   -40");
                rate = rate - 40;
                diffTotalTooHigh = true;
            }
        }


        if ( found == 3 && order == found && backwardMatches == 0 ) {
            if ( matchInPatternStrong == 2 || strongWordEnd ) {
                if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                    logln("     -20");
                    rate = rate - 20;
                }

                if ( diffInPatternSum == 0 ) {
                    if ( diffInWordSum == 0 ) {
                        logln("     +30 : word diff == 0 ");
                        rate = rate + 30;
                    }
                    else if ( diffInWordSum < 3 ) {
                        logln("     +20 : word diff < 3");
                        rate = rate + 20;
                    }
                    else {
                        logln("     -20");
                        rate = rate - 20;
                    }
                }

                if ( diffInWordSum + diffInPatternSum > 3 ) {
                    logln("     -20");
                    rate = rate - 20;
                }

                if ( typoMatches > 0 ) {
                    logln("     -5");
                    rate = rate - 5;
                }
            }
            else if ( matchInPatternStrong == 1 ) {
                if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                    logln("     -30");
                    rate = rate - 30;
                }
                if ( matchIndex == 0 ) {
                    if ( diffInWordSum + diffInPatternSum > 2 ) {
                        logln("     -10");
                        rate = rate - 10;
                    }
                }
                else {
                    if ( diffInWordSum + diffInPatternSum > 1 ) {
                        logln("     -10");
                        rate = rate - 10;
                    }
                }
            }
            else if ( matchInPatternStrong == 0 ) {
                if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                    logln("     -30");
                    rate = rate - 30;
                }
                if ( matchIndex == 0 ) {
                    if ( diffInWordSum + diffInPatternSum > 2 ) {
                        logln("     -10");
                        rate = rate - 10;
                    }
                }
                else {
                    if ( diffInWordSum + diffInPatternSum > 1 ) {
                        logln("     -10");
                        rate = rate - 10;
                    }
                }
            }
        }

        if ( found == 4 ) {
            if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                if ( diffInPatternSum > diffInWordSum ) {
                    int penalty = (diffInWordSum + diffInPatternSum*2) * 5;
                    rate = rate - penalty;
                    logln("     -penalty[found=4] " + penalty);
                }
            }
            else if ( diffInPatternSum == 0 && !embraceStartEndTooMuchDiff ) {
                if ( secondCharMissed == 0 || diffInWordSum <= 2 || anyEmbrace) {
                    if ( matchInPatternStrong == 3 ) {
                        logln("     +30 : pattern diff == 0 word diff <= 2 ");
                        rate = rate + 30;
                    }
                    else if ( matchInPatternStrong == 2 ) {
                        logln("     +20 : strong matches == 2");
                        rate = rate + 20;
                    }
                    else if ( matchInPatternStrong == 1 ) {
                        logln("     +15 : strong matches == 1");
                        rate = rate + 15;
                    }
                    else if ( matchInPatternStrong == 0 ) {

                    }
                }
                else {

                }
            }
        }

        if ( found > 4 ) {
            if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                if ( diffInPatternSum > diffInWordSum ) {
                    int penalty = (diffInWordSum + diffInPatternSum*2) * 5;
                    rate = rate - penalty;
                    logln("     -penalty[found>4] " + penalty);
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
                        int patternMatchesOnly = found-matchFull;
                        if ( diffInPatternSum == 0 && diffInWordSum <= patternMatchesOnly ) {
                            rate = rate + 30;
                            logln("     +30");
                        }
                        else if ( (diffInPatternSum + diffInWordSum) <= patternMatchesOnly ) {
                            rate = rate + 20;
                            logln("     +20");
                        }
                        else {
                            if ( secondCharMissed == 0 ) {
                                rate = rate + 10;
                                logln("     +10 : ... found > 3 and second char not missed ");
                            }
                            else {
                                if ( diffInWordSum >= found ) {
                                    rate = rate - 30;
                                    logln("     -30");
                                }
                                else {
                                    rate = rate - 10;
                                    logln("     -10");
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
                int patternMatchLength = wcPrevInPattern /* == last found in pattern */  - firstWcInPattern + 1;
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
                    } else if ( patternMatchLength - found <= accepted && !diffTotalTooHigh ) {
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

            if ( found == matchFull && found == matchInPattern ) {
                rate = rate + matchFull * 2;
            }
        }

        int matchFullVsPatternDiff = matchInPattern - matchFull;

        if ( matchFull == 2 && backwardMatches == 0 ) {
            if ( matchFullVsPatternDiff > 1 && matchInPatternWeak > 0 ) {
                if ( matchType == PATTERN_IS_WORD_FRAGMENT || matchInPatternStrengthBonus == 0 ) {
                    if ( diffInPatternSum > 0 || secondCharMissed == 1) {
                        if ( diffInWordSum <= found/2 ) {
                            rate = rate + 10;
                            logln("    +10 ??: ");
                        }
                        else {
                            int penalty = matchInPatternWeight * matchInPatternWeak;
                            rate = rate - penalty;
                            logln("    penalty[2]: " + penalty);
                        }
                    }
                }
            }
            else if ( matchFullVsPatternDiff == 1 && matchType == WORD_IS_PATTERN_FRAGMENT && diffInWordSum < 3 ) {
                if ( ! STRICT ) {
                }
            }
        }

        int matchSpan = wcPrevInPattern - matchIndex + 1;

        logln("  order:%s, found:%s, match-span:%s, full-matches:%s, pattern-matches:%s (s:%s w:%s), back-matches:%s, typo-matches:%s, w-diff:%s (longest:%s), p-diff:%s, 0-ch-miss:%s, 1-ch-miss:%s", order, found, matchSpan, matchFull, matchInPattern, matchInPatternStrong, matchInPatternWeak, backwardMatches, typoMatches, diffInWordSum, longestDiffInWord, diffInPatternSum, firstCharMissed, secondCharMissed);

        if ( found == 3 && secondCharMissed == 1 && matchLength > 5 ) {
            if ( diffInWordSum > 2 ) {
                logln("EXP 1 ");
            }
            else {
                logln("EXP 2 ");
            }
        }

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

            long code = CODE_BASE;

            code = code + patternLength     * 100000000000L;
            code = code + rate              * 100000000L;
            code = code + wordLength        * 1000000L;
            code = code + matchIndex        * 10000L;
            code = code + matchSpan         * 100L;
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
