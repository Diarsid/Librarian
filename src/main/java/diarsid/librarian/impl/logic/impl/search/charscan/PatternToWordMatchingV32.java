package diarsid.librarian.impl.logic.impl.search.charscan;

public final class PatternToWordMatchingV32 extends LoggingInScript implements PatternToWordMatchingCodeV2 {

    @Override
    public int version() {
        return 32;
    }

    @Override
    /* script */
    public long evaluate(String pattern, String word) {
        logln("%s : P:%s --- W:%s", this.nameAndVersion(), pattern, word);

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
                    logln("   W:%s not found! ", wc, wcPrev);
                    firstCharMissed++;
                    mismatches++;
                }
                else if ( i == 1 ) {
                    logln("   W:%s not found! ", wc, wcPrev);
                    secondCharMissed++;
//                    mismatches++;
                    if ( firstCharMissed > 0 ) {
                        logln("      mismatches major!");
                        return -1;
                    }
                }
                else {
                    logln("   W:%s not found after W:%s[P:%s]", wc, wcPrev, wcPrevInPattern);
                    int wcInPatternWeak = pattern.indexOf(wc, firstWcInPattern);
                    if ( wcInPatternWeak < 0 ) {
                        logln("      W:%s not found from word-in-pattern beginning [P:%s]", wc, firstWcInPattern);
                    }
                    else {
                        if ( wcPrevInPattern > -1 ) {
                            int distanceFromItoWordEnd = lastInWord - i;
                            int distanceFromCPrevToC = wcPrevInPattern - wcInPatternWeak;
                            if ( distanceFromCPrevToC > distanceFromItoWordEnd ) {
                                logln("      W:%s[P:%s] too far [1]!", wc, wcInPatternWeak);
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
                                                    logln("         W:%s[P:%s] backward match [+1] prohibited!",  wc, wcInPatternWeak);
                                                }
                                                else {
                                                    if ( gaps > 0 ) {
                                                        logln("      W:%s[P:%s] backward match [+1] from word-in-pattern beginning [P:%s] before W:%s[P:%s]",  wc, wcInPatternWeak, firstWcInPattern, wcPrev, wcPrevInPattern);
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
                                                            prevCharMatchStrength = PREV_CHAR_STRONG;
                                                            prevCharResult = PREV_CHAR_MATCH_FULL;
                                                        }

                                                        diffInPatternSum = diffInPatternSum - (wcPrevInPattern - wcInPatternWeak) - 1;
                                                        wcPrevInPattern = wcInPatternWeak;
                                                    }
                                                }
                                            }
                                            else if ( i == iPrev + 2 ) {
                                                boolean backwardMatch2Prohibited =
                                                        (i+1)/2 > matchInPattern+1 ||
                                                        matchFull > 0 ||
                                                        secondCharMissed > 0;
                                                if ( backwardMatch2Prohibited ) {
                                                    mismatchesOnlyWord++;
                                                }
                                                else {
                                                    if ( gaps > 0 ) {
                                                        logln("      W:%s[P:%s] backward match [+2] from word-in-pattern beginning [P:%s] before W:%s[P:%s]",  wc, wcInPatternWeak, firstWcInPattern, wcPrev, wcPrevInPattern);
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
                                            logln("      W:%s[P:%s] typo match from word-in-pattern beginning", wc, wcInPatternWeak);
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
                                            logln("      W:%s[P:%s] -> W:%s[P:%s]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            firstWcInPattern = wcPrevInPatternNprev;
                                            wcPrevInPattern = wcPrevInPatternNprev;
                                        }
                                    }
                                    else {
                                        int distanceChange = wcPrevInPatternNprev - wcPrevInPattern;
                                        if ( distanceChange > 0 && distanceChange < distanceFromItoWordEnd && wcPrevInPatternNprev < wcInPattern ) {
                                            logln("      W:%s[P:%s] -> W:%s[P:%s]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            wcPrevInPattern = wcPrevInPatternNprev;
                                            if ( diffInPatternSum > 0 ) {
                                                diffInPatternSum = diffInPatternSum - distanceChange - 1;
                                            }
                                            distanceFromCPrevToC = wcInPattern - wcPrevInPattern;
                                            if ( distanceFromCPrevToC > (distanceFromItoWordEnd + 2) ) {
                                                logln("   W:%s not found on reasonable length [1] after W:%s[P:%s], found at [P:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                                prevCharResult = PREV_CHAR_NOT_FOUND;
                                                continue wordCharsIterating;
                                            }
                                        }
                                        else {
                                            logln("   W:%s not found on reasonable length [2] after W:%s[P:%s], found at [P:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                            prevCharResult = PREV_CHAR_NOT_FOUND;
                                            continue wordCharsIterating;
                                        }
                                    }
                                }
                            }
                            else /* matchFull >= 1 */ {
                                logln("   W:%s not found on reasonable length after [3] W:%s[P:%s], found at [P:%s]", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                boolean foundInGap = false;
                                if ( gaps > 0 ) {
                                    int from = firstWcInPattern > -1 ? firstWcInPattern : firstFoundWcInPattern;
                                    if ( from > -1 ) {
                                        from++;
                                        char pc;
                                        logln("      scanning gaps from [P:%s] to [P:%s]", from, wcPrevInPattern-1);
                                        for ( int i2 = from; i2 < wcPrevInPattern; i2++ ) {
                                            pc = pattern.charAt(i2);
                                            if ( wc == pc ) {
                                                logln("        found at [P:%s]", i2);
                                                foundInGap = true;
                                                backwardMatches++;
                                                found++;
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
                                                    logln("        duplication search: W:%s[P:%s]", i2wc, i2wcInPattern);
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
                                        }

                                        continue wordCharsIterating;
                                    }
                                    else {
                                        logln("      W:%s[P:%s] too far [3]!", wc, wcInPattern);
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

                logln("   W:%s[P:%s]", wc, wcInPattern);
                if ( wcPrevInPattern > -1 ) {
                    diffInPattern = wcInPattern - wcPrevInPattern;
                    diffInWord = i - iPrev;
                    if ( diffInPattern == 1 ) {
                        logln("      PATTERN MATCH");
                        matchInPattern++;
                        if ( diffInWord == 1 ) {
                            logln("      FULL MATCH");
                            matchFull++;
                            if ( prevCharResult == PREV_CHAR_MATCH_PATTERN ) {
                                if ( matchInPatternWeak > 0 ) {
                                    matchInPatternWeak--;
                                    logln("        weak--");
                                    matchInPatternStrong++;
                                    logln("        strong, prev match is full {1}");
                                    prevCharMatchStrength = PREV_CHAR_STRONG;
                                    if ( mismatches == 0 && diffInWordSum <= 3 ) {
                                        logln("        strong bonus");
                                        matchInPatternStrengthBonus++;
                                    }
                                }
                            }
                            matchInPatternStrong++;
                            logln("        strong, full match");
                            prevCharMatchStrength = PREV_CHAR_STRONG;
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
                                        logln("        weak");
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
                                    logln("        weak");
                                    prevCharMatchStrength = PREV_CHAR_WEAK;
                                }
                            }
                            else if ( prevCharResult == PREV_CHAR_NOT_FOUND
                                    || prevCharResult == PREV_CHAR_FOUND) {
                                matchInPatternWeak++;
                                logln("        weak");
                                prevCharMatchStrength = PREV_CHAR_WEAK;
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
                                logln("      W:%s[P:%s] too far [4], ignore and continue!", wc, wcInPattern);
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
                            else if ( prevCharResult == PREV_CHAR_MATCH_PATTERN || prevCharResult == PREV_CHAR_FOUND ) {
                                matchInPatternWeak++;
                                logln("        weak");
                                prevCharMatchStrength = PREV_CHAR_WEAK;
                            }
                            prevCharResult = PREV_CHAR_MATCH_PATTERN;
                        }

                        boolean diffMoreThanOne = diffInPattern > 1;
                        int wcPrevInPatternN = wcPrevInPattern;
                        gapFixing: while ( diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                            gaps++;
                            logln("      gap between char W:%s[P:%s] and previous char W:%s[P:%s]!", wc, wcInPattern, wcPrev, wcPrevInPattern);

                            wcPrev = word.charAt(i - 1);
                            wcPrevInPatternN = pattern.indexOf(wcPrev, wcPrevInPatternN + 1);
                            if ( wcPrevInPatternN < 0 ) {
                                logln("      W:%s not found after P:%s", wcPrev, wcPrevInPattern);
                                gap = diffInPattern - 1;
                                if ( matchFull == 0 && gap > found ) {
                                    logln("         no full-matches, gap[%s] > found[%s]", gap, found);
                                    return -1;
                                }
                                break gapFixing;
                            }
                            if ( wcPrevInPatternN > wcInPattern ) {
                                logln("      W:%s found only after current W:%s[P:%s]", wcPrev, wc, wcInPattern);
                                break gapFixing;
                            }
                            if ( wcPrevInPatternN == wcInPattern) {
                                break gapFixing;
                            }
                            diffInPattern = wcInPattern - wcPrevInPatternN;

                            if ( (diffInPattern + i) > wordLength) {
                                logln("         P:%s to far", wcPrevInPatternN);
                                break gapFixing;
                            }

                            diffMoreThanOne = diffInPattern > 1;
                            cPrevIndexBeforeCIndex = wcPrevInPatternN < wcInPattern;

                            if ( ! diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                                wcPrevInPattern = wcPrevInPatternN;
                                logln("      gap fixed char W:%s[P:%s] and previous char W:%s[P:%s] - P:%s!", wc, wcInPattern, wcPrev, wcPrevInPattern, wcPrevInPatternN);
                                gaps--;
                                logln("      MATCH, gap fixed");
                                if ( diffInWord == 1 ) {
                                    logln("      FULL MATCH");
                                    matchFull++;
                                    matchInPattern++;
                                    if ( prevCharResult == PREV_CHAR_MATCH_PATTERN ) {
                                        if ( matchInPatternWeak > 0 ) {
                                            matchInPatternWeak--;
                                            logln("        weak--");
                                            matchInPatternStrong++;
                                            logln("        strong");
                                            prevCharMatchStrength = PREV_CHAR_STRONG;
                                            if ( mismatches == 0 && diffInWordSum < 2 ) {
                                                logln("        strong bonus");
                                                matchInPatternStrengthBonus++;
                                            }
                                        }
                                    }
                                    matchInPatternStrong++;
                                    logln("        strong");
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
                        break wordCharsIterating;
                    }
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

        if ( found == 3 && order == found && backwardMatches == 0 && matchInPatternStrong == 2 ) {
            if ( diffInWordSum == 0 && diffInPatternSum > 0 ) {
                rate = rate - 20;
            }

            if ( diffInPatternSum > 0 && diffInWordSum > 0 ) {
                rate = rate - 10;
            }

            if ( diffInPatternSum == 0 && diffInWordSum >= 0 && diffInWordSum <=2 ) {
                rate = rate + 5;
            }

            if ( typoMatches > 0 ) {
                rate = rate - 5;
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

            if ( found == matchInPattern && order == found && backwardMatches == 0 && matchInPatternStrong > matchInPatternWeak ) {
                if ( matchFull >= (found / 2) ) {
                    if ( found > 3 && (diffInPatternSum + diffInWordSum) <= (found-matchFull)) {
                        rate = rate + 20;
                    }
                    else {
                        rate = rate + 10;
                    }
                }
                else {
                    rate = rate + matchInPattern;
                }
            }
            else if ( matchInPattern > 2 ) {
                int patternMatchLength = wcPrevInPattern /* == last found in pattern */  - firstWcInPattern + 1;
                int backwardMatchesL = backwardMatches - 1;
                if ( patternMatchLength == found ) {
                    if ( found == (matchInPattern + backwardMatchesL ) && order == found - backwardMatchesL ) {
                        rate = found * 10
                                + found * matchInPatternWeight
                                + found * 5
                                + typoMatches * 3;
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
                    } else if ( patternMatchLength - found <= accepted ) {
                        matchInPatternWeight = 6;
                        rate = found * 10
                                + found * matchInPatternWeight
                                + found * 5
                                + typoMatches * 3;
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
                    rate = rate - matchInPatternWeight * matchInPatternWeak;
                }
            }
            else if ( matchFullVsPatternDiff == 1 && matchType == WORD_IS_PATTERN_FRAGMENT && diffInWordSum < 3 ) {
                if ( ! STRICT ) {
//                    System.out.println("???");
//                    total = total + matchInPatternWeight; // CHANGED
                }
            }
        }

//        if ( matchFull > 0 && matchFull < 3 && matchFullVsPatternDiff > 1 && backwardMatches == 0 && matchInPatternWeak > 0 ) {
//            total = total - matchInPatternWeight * matchInPatternWeak;
//        }

        logln("  order:%s, found:%s, full-matches:%s, pattern-matches:%s (s:%s w:%s), back-matches:%s, typo-matches:%s, w-diff:%s, p-diff:%s, 0-ch-miss:%s, 1-ch-miss:%s", order, found, matchFull, matchInPattern, matchInPatternStrong, matchInPatternWeak, backwardMatches, typoMatches, diffInWordSum, diffInPatternSum, firstCharMissed, secondCharMissed);

        /* V.25.0 VALIDITY CHECK */
//        if ( matchInPatternStrong + matchInPatternWeak != matchInPattern - 1 ) {
//            throw new IllegalStateException();
//        }

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

            int matchIndex;
            if ( firstWcInPattern > -1 ) {
                matchIndex = firstWcInPattern;
            }
            else {
                matchIndex = firstFoundWcInPattern;
            }
            int matchSpan = wcPrevInPattern - matchIndex + 1;

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
}
