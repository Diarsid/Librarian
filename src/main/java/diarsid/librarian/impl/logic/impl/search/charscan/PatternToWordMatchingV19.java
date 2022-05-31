package diarsid.librarian.impl.logic.impl.search.charscan;

public final class PatternToWordMatchingV19 extends LoggingInScript implements PatternToWordMatchingCodeV1 {

    @Override
    public int version() {
        return 19;
    }

    @Override
    public long evaluate(String pattern, String word) {
        logln("p:%s --- w:%s", pattern, word);

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
                int total = wordLength*10 + wordLength*7 + wordLength*5;
                logln("   " + total);
                long code = 100000000000L;

                code = code + patternLength * 1000000000L;
                code = code + total * 1000000L;
                code = code + wordLength * 10000L;
                code = code + wordInPatternIndex * 100L;
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
                int total = patternLength*10 + patternLength*7 + patternLength*5;
                logln("   " + total);
                long code = 100000000000L;

                code = code + patternLength * 1000000000L;
                code = code + total * 1000000L;
                code = code + wordLength * 10000L;
                code = code + patternInWordIndex * 100L;
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

        diffLength = rangeLength - matchLength;

        if ( diffLength < matchLength / 2 ) {
            lengthRatioType = LENGTHS_APPROX_EQUAL;
        }
        else if ( diffLength >= matchLength / 2 && diffLength < matchLength - 1 ) {
            lengthRatioType = LENGTHS_DIFF_INSIGNIFICANT;
        }
        else {
            lengthRatioType = LENGTHS_DIFF_SUBSTANTIAL;
        }

        int mismatchesMajor = 0;
        int mismatchesMinor = 0;
        int mismatchesOnlyWord = 0;
        int mismatches = 0;
        int found = 0;
        int fullMatch = 0;
        int backwardMatches = 0;
        int matchInPattern = 0;
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

        for (int i = 0; i < wordLength; i++) {
            wc = word.charAt(i);
            wcInPattern = pattern.indexOf(wc, wcPrevInPattern + 1);
            diffInPattern = -1;
            diffInWord = -1;

            if ( wcInPattern < 0 ) {
                if ( i == 0 ) {
                    logln("   w:%s not found! ", wc, wcPrev);
                    mismatchesMajor++;
                    mismatches++;
                }
                else if ( i == 1 ) {
                    logln("   w:%s not found! ", wc, wcPrev);
//                    mismatchesMinor++;
//                    mismatches++;
                    if ( mismatchesMajor > 0 ) {
                        logln("      mismatches major!");
                        return -1;
                    }
                }
                else {
                    logln("   w:%s not found after w:%s[p:%s]", wc, wcPrev, wcPrevInPattern);
                    int wcInPatternWeak = pattern.indexOf(wc, firstWcInPattern);
                    if ( wcInPatternWeak < 0 ) {
                        logln("      w:%s not found from word-in-pattern beginning", wc);
                    }
                    else {
                        if ( wcPrevInPattern > -1 ) {
                            int distanceFromItoWordEnd = lastInWord - i;
                            int distanceFromCPrevToC = wcPrevInPattern - wcInPatternWeak;
                            if ( distanceFromCPrevToC > distanceFromItoWordEnd ) {
                                logln("      w:%s[p:%s] too far [1]!", wc, wcInPatternWeak);
                                break;
                            }
                            else {
                                if ( i != lastInWord ) {
                                    if ( wcInPatternWeak != firstWcInPattern ) {
                                        if ( wcInPatternWeak == wcPrevInPattern - 1 ) {
                                            if ( i == iPrev + 1 ) {
                                                if ( gaps > 0 ) {
                                                    logln("      w:%s[p:%s] backward match from word-in-pattern beginning before w:%s[p:%s]",  wc, wcInPatternWeak, wcPrev, wcPrevInPattern);
                                                    backwardMatches++;
                                                    found++;
                                                    gaps--;
                                                }
                                            }
                                            else {
                                                mismatchesOnlyWord++;
                                            }
//                                            if ( gaps > 0 ) {
//                                                logln("      w:%s[p:%s] backward match from word-in-pattern beginning before w:%s[p:%s]",  wc, wcInPatternWeak, wcPrev, wcPrevInPattern);
//                                                backwardMatches++;
//                                                found++;
//                                                gaps--;
//                                            }
                                        }
                                        else {
                                            typoMatches++;
                                            logln("      w:%s[p:%s] typo match from word-in-pattern beginning", wc, wcInPatternWeak);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
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
                        int distanceFromItoWordEnd = wordLength - i;
                        if ( distanceFromCPrevToC > 2 && distanceFromCPrevToC >= matchInPattern) {

                            if ( fullMatch < 1 ) {
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
                                            logln("      w:%s[p:%s] -> w:%s[p:%s]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            firstWcInPattern = wcPrevInPatternNprev;
                                            wcPrevInPattern = wcPrevInPatternNprev;
                                        }
                                    }
                                    else {
                                        int distanceChange = wcPrevInPatternNprev - wcPrevInPattern;
                                        if ( distanceChange > 0 && distanceChange < distanceFromItoWordEnd && wcPrevInPatternNprev < wcInPattern ) {
                                            logln("      w:%s[p:%s] -> w:%s[p:%s]", wcPrev, wcPrevInPattern, wcPrev, wcPrevInPatternNprev);
                                            wcPrevInPattern = wcPrevInPatternNprev;
                                            if ( diffInPatternSum > 0 ) {
                                                diffInPatternSum = diffInPatternSum - distanceChange - 1;
                                            }
                                            distanceFromCPrevToC = wcInPattern - wcPrevInPattern;
                                            if ( distanceFromCPrevToC > (distanceFromItoWordEnd + 2) ) {
                                                logln("   w:%s not found on reasonable length after w:%s[p:%s], found at p:%s", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                                continue;
                                            }
                                        }
                                        else {
                                            logln("   w:%s not found on reasonable length after w:%s[p:%s], found at p:%s", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                            continue;
                                        }
                                    }
                                }
                            }
                            else {
                                logln("   w:%s not found on reasonable length after w:%s[p:%s], found at p:%s", wc, wcPrev, wcPrevInPattern, wcInPattern);
                                if ( distanceFromCPrevToC > found ) {
                                    char i2wc;
                                    int i2FirstWcInPattern = -1;
                                    int i2wcInPattern = -1;
                                    int i2wcInPatternPrev = wcPrevInPattern;
                                    int i2FullMatch = 0;
                                    int i2MatchInPattern = 0;
                                    int i2 = 0;
                                    int i2Order = 0;
                                    int allowed = Math.max(fullMatch+1, found);

                                    duplicateSearch: for (; i2 < allowed; i2++) {
                                        i2wc = word.charAt(i2);
                                        i2wcInPattern = pattern.indexOf(i2wc, i2wcInPatternPrev + 1);
                                        if ( i2wcInPattern > -1 ) {
                                            if ( i2 == 0 ) {
                                                i2FirstWcInPattern = i2wcInPattern;
                                            }

                                            if ( i2wcInPattern < wcInPattern ) {
                                                logln("     duplication search: w:%s[p:%s]", i2wc, i2wcInPattern);
                                                if ( i2 > 0 ) {
                                                    if ( i2wcInPatternPrev + 1 == i2wcInPattern ) {
                                                        i2FullMatch++;
                                                        i2MatchInPattern++;
                                                    } else {
                                                        i2MatchInPattern++;
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
                                            if ( i2FullMatch >= fullMatch && allowed < i ) {
                                                allowed++;
                                            }
                                        }
                                    }

                                    boolean change =
                                            i2FullMatch > fullMatch ||
                                                    (i2FullMatch == fullMatch && i2MatchInPattern >= matchInPattern);

                                    if ( change ) {
                                        logln("     duplication search: fullMatches %s starting from %s", i2FullMatch, i2FirstWcInPattern);
                                        i = i2 - 1;
                                        order = i2Order;
                                        firstWcInPattern = i2FirstWcInPattern;
                                        wcPrevInPattern = i2wcInPatternPrev;
                                        iPrev = i;
                                        continue;
                                    }
                                    else {
                                        break;
                                    }
                                }
                                else {
                                    logln("   w:%s[p:%s] too far [3]!", wc, wcInPattern);
                                    break;
                                }
                            }
                        }
                    }
                    else if ( distanceFromCPrevToC > 0 ) {
                        order++;
                    }
                }

                logln("   w:%s[p:%s]", wc, wcInPattern);
                if ( wcPrevInPattern > -1 ) {
                    diffInPattern = wcInPattern - wcPrevInPattern;
                    diffInWord = i - iPrev;
                    if ( diffInPattern == 1 ) {
                        logln("m");
                        matchInPattern++;
                        if ( diffInWord == 1 ) {
                            fullMatch++;
                            logln("fm");
                        }
                    }
                    else {
                        boolean diffMoreThanOne = diffInPattern > 1;
                        boolean cPrevIndexBeforeCIndex = wcPrevInPattern < wcInPattern;
                        if ( cPrevIndexBeforeCIndex ) {
                            matchInPattern++;
                            logln("m");
                        }
                        int wcPrevInPatternN = wcPrevInPattern;
                        while ( diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                            gaps++;
                            logln("      gap between char w:%s[p:%s] and previous char w:%s[p:%s]!", wc, wcInPattern, wcPrev, wcPrevInPattern);

                            wcPrev = word.charAt(i - 1);
                            wcPrevInPatternN = pattern.indexOf(wcPrev, wcPrevInPatternN + 1);
                            if ( wcPrevInPatternN < 0 ) {
                                logln("      w:%s not found after p:%s", wcPrev, wcPrevInPattern);
                                int gap = diffInPattern - 1;
                                if ( fullMatch == 0 && gap > found ) {
                                    logln("         no full-matches, gap[%s] > found[%s]", gap, found);
                                    return -1;
                                }
                                break;
                            }
                            if ( wcPrevInPatternN > wcInPattern ) {
                                logln("      w:%s found only after current w:%s[p:%s]", wcPrev, wc, wcInPattern);
                                break;
                            }
                            if ( wcPrevInPatternN == wcInPattern) {
                                break;
                            }
                            diffInPattern = wcInPattern - wcPrevInPatternN;

                            if ( (diffInPattern + i) > wordLength) {
                                logln("         p:%s to far", wcPrevInPatternN);
                                break;
                            }

                            diffMoreThanOne = diffInPattern > 1;
                            cPrevIndexBeforeCIndex = wcPrevInPatternN < wcInPattern;

                            if ( ! diffMoreThanOne && cPrevIndexBeforeCIndex ) {
                                wcPrevInPattern = wcPrevInPatternN;
                                logln("      gap fixed char w:%s[p:%s] and previous char w:%s[p:%s] - p:%s!", wc, wcInPattern, wcPrev, wcPrevInPattern, wcPrevInPatternN);
                                gaps--;
                                logln("m");
                                if ( diffInWord == 1 ) {
                                    fullMatch++;
                                    logln("fm");
                                }
                            }
                        }

                        // exp
                        diffInPattern = wcInPattern - wcPrevInPattern;
                        if ( diffInPattern > 1 ) {
                            char pc;
                            int pcInWord;
                            for (int iPattern = wcPrevInPattern+1; iPattern < wcInPattern; iPattern++) {
                                pc = pattern.charAt(iPattern);
                                pcInWord = word.indexOf(pc, iPrev + 1);
                                if ( pcInWord < 0 ) {
                                    logln("      search p:%s[%s] in word - not found!", pc, iPattern);
//                                    mismatchesMinor++;
//                                    mismatches++;
                                }
                                else if ( pcInWord > i + 3 ) {
                                    logln("      search p:%s[%s] in word - found at w[%s]", pc, iPattern, i);
//                                    mismatchesMinor++;
//                                    mismatches++;
                                }
                            }
                        }
                        // exp
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
                    break;
                }

                if ( diffInWord > -1 ) {
                    diffInWordSum = diffInWordSum + diffInWord - 1;
                    diffInPatternSum = diffInPatternSum + diffInPattern - 1;
                }

                wcPrevInPattern = wcInPattern;
                iPrev = i;

                found++;

                if ( wcInPattern == lastInPattern ) {
                    logln("      end of pattern reached");
                    break;
                }
            }
        }

        if ( fullMatch > 0 ) {
            fullMatch++;
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

        if ( found == 0 || order == 0 || fullMatch + matchInPattern == 0 ) {
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

        if ( matchType == PATTERN_IS_WORD_FRAGMENT ) {
            if ( firstWcInPattern != 0 ) {
                mismatchesMajor++;
            }
            mismatchesMinor = mismatchesMinor + mismatchesOnlyWord;
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
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                }
                else {
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                }
                typoMatchesWeight = 3;
                break;
            }
            case LENGTHS_DIFF_SUBSTANTIAL: {
                if ( diffInWordSum <= matchLength ) {

                }
                else if ( diffInWordSum <= diffLength / 2 ) {
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                }
                else if ( diffInWordSum < diffLength ) {
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                }
                else {
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                    matchInPatternWeight--;
                }
                typoMatchesWeight = 1;
                break;
            }
            default: throw new IllegalStateException("Length ratio type is not defined!");
        }

        int total = fullMatch * 10
                + matchInPattern * matchInPatternWeight
                + backwardMatches * 6
                + found * 5
                + typoMatches * typoMatchesWeight
                - mismatchesMajor * 35
                - mismatchesMinor * mismatchesMinorWeight;

        if ( mismatches == 0 && found > 2 ) {
            if ( found == matchLength ) {
                if ( matchInPattern == matchLength ) {
                    total = total + matchLength;
                }
                if ( gaps == 0 ) {
                    int totalIndirectMatches = matchInPattern + backwardMatches;
                    if ( totalIndirectMatches > found ) {
                        total = total + 10;
                    }
                }
            }

            if ( found == matchInPattern && order == found && backwardMatches == 0 ) {
                if ( fullMatch >= (found / 2) ) {
                    total = total + 10;
                }
                else {
                    total = total + matchInPattern;
                }
            }
            else if ( matchInPattern > 2 ) {
                int patternMatchLength = wcPrevInPattern /* == last found in pattern */  - firstWcInPattern + 1;
                int backwardMatchesL = backwardMatches - 1;
                if ( patternMatchLength == found ) {
                    if ( found == (matchInPattern + backwardMatchesL ) && order == found - backwardMatchesL ) {
                        total = found * 10
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
                        total = found * 10
                                + found * matchInPatternWeight
                                + found * 5
                                + typoMatches * 3;
                    }
                }
            }

            if ( found == fullMatch && found == matchInPattern ) {
                total = total + fullMatch * 2;
            }
        }

        logln("  order:%s, found:%s, full-matches:%s, pattern-matches:%s, back-matches:%s, typo-matches:%s, w-diff:%s, p-diff:%s, M-mismatches:%s, m-mismatches:%s", order, found, fullMatch, matchInPattern, backwardMatches, typoMatches, diffInWordSum, diffInPatternSum, mismatchesMajor, mismatchesMinor);

        int requiredFullMatches;
        int requiredPatternMatches;

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
                if ( found >= 4 || matchType == PATTERN_IS_WORD_FRAGMENT ) {
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


        int threshold = requiredFullMatches*10 + requiredPatternMatches*7 + found*5;
        logln("  threshold:%s, total:%s", threshold, total);
        if ( total >= threshold ) {

            long code = 100000000000L;

            code = code + patternLength * 1000000000L;
            code = code + total * 1000000L;
            code = code + wordLength * 10000L;
            if ( firstWcInPattern > -1 ) {
                code = code + firstWcInPattern * 100L;
            }
            else {
                code = code + firstFoundWcInPattern * 100L;
            }
            code = code + found;

            return code;
        }
        else {
            return -1;
        }
    }

}
