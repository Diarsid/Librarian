package diarsid.librarian.impl.logic.impl.jdbc.h2.extensions;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.h2.api.AggregateFunction;

import static java.sql.Types.BIGINT;

import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.NO_WORDS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.PATTERN_START_NOT_FOUND_AND_MANY_WORDS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.PATTERN_START_NOT_FOUND_AND_ONE_WORD;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.TOO_MUCH_MISSED;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.TOO_MUCH_SPACES_AND_MISSED_IN_WORDS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.TOO_MUCH_SPACES_IN_WORDS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV25.RejectionReason.WORDS_TOTAL_LENGTH_SUM_TOO_SMALL;

public class H2AggregateFunctionForAnalyzeV25 implements AggregateFunction {

    /*
     * template '10 xx 0 yy 0 zz' where:
     * xx - pattern length
     * yy - missed
     * zz - overlaps
     */
    public static final long BASE_AGGREGATION_CODE = 80000000000000000L;

    private static final int USUAL_MATCH_TYPE = 1;
    private static final int FISRT_CHAR_MATCH_TYPE = 2;

    private static final int UNASSIGNED = -1;
    private static final int RATE_NOT_APPLICABLE = -2;
    private static final int NON_FILLED_POSITION = 8;
    private static final int WORD_SPACE_POSITION = 7;

    public enum RejectionReason {

        NO_WORDS(-4),
        TOO_MUCH_MISSED(-5),
        WORDS_TOTAL_LENGTH_SUM_TOO_SMALL(-6),
        WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL(-7),
        WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL(-8),
        WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL(-9),
        PATTERN_START_NOT_FOUND_AND_ONE_WORD(-10),
        PATTERN_START_NOT_FOUND_AND_MANY_WORDS(-11),
        PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS(-12),
        TOO_MUCH_SPACES_IN_WORDS(-13),
        TOO_MUCH_SPACES_AND_MISSED_IN_WORDS(-14);

        private final int value;

        RejectionReason(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Optional<RejectionReason> findByValue(int value) {
            for ( RejectionReason reason : RejectionReason.values() ) {
                if ( reason.value() == value ) {
                    return Optional.of(reason);
                }
            }

            return Optional.empty();
        }
    }

    private long positionsRow = UNASSIGNED;
    private int patternLength = UNASSIGNED;
    private int words = 0;
    private int wordsDuplicated = 0;
    private int wordsLengthSum = 0;
    private int wordsLengthSumDuplicated = 0;
    private int overlaps = 0;
    private int overlapsDuplicated = 0;
    private boolean patternStartFound = false;
    private int firstIndexInPattern = UNASSIGNED;
    private int missed = 0;
    private int wordSpaces = 0;
    private int rateSum = 0;
    private long code = 0;

    public static long exemplarAggregate(int length) {
        switch ( length ) {
            case 1:  return 8L;
            case 2:  return 88L;
            case 3:  return 888L;
            case 4:  return 8888L;
            case 5:  return 88888L;
            case 6:  return 888888L;
            case 7:  return 8888888L;
            case 8:  return 88888888L;
            case 9:  return 888888888L;
            case 10: return 8888888888L;
            case 11: return 88888888888L;
            case 12: return 888888888888L;
            case 13: return 8888888888888L;
            case 14: return 88888888888888L;
            case 15: return 888888888888888L;
            case 16: return 8888888888888888L;
            case 17: return 88888888888888888L;
            case 18: return 888888888888888888L;
            case 19: return 8888888888888888888L;
            default: throw new IllegalArgumentException();
        }
    }

    public static int positionAt(long positionsRow, long indexInRow) {
        long remnant = positionsRow % (indexInRow * 10);
        return (int) (remnant / indexInRow);
    }

    @Override
    public void init(Connection conn) throws SQLException {

    }

    @Override
    public int getType(int[] inputTypes) throws SQLException {
        return BIGINT;
    }

    @Override
    public void add(Object arg) throws SQLException {
        long code;

        if ( arg instanceof Integer ) {
            int intArg = (int) arg;
            code = intArg;
        }
        else if ( arg instanceof Long ) {
            code = (long) arg;
        }
        else {
            throw new SQLException("Unexpected data type: " + arg.getClass().getSimpleName());
        }

        long codeSource = code;

        int matchType;
        int argFound;
        int argMatchSpan;
        int argMatchIndex;
        int argWordLength;
        int argRate;
        int argPatternLength;

        if ( code / 1000000 == FISRT_CHAR_MATCH_TYPE ) {
            matchType = FISRT_CHAR_MATCH_TYPE;

            argWordLength = (int) (code % 100);
            code = code / 100;
            argPatternLength = (int) (code % 100);

            argFound = 1;
            argMatchSpan = 1;
            argMatchIndex = 0;
            argRate = RATE_NOT_APPLICABLE;
        }
        else {
            matchType = USUAL_MATCH_TYPE;

            argFound = (int) (code % 100);
            code = code / 100;
            argMatchSpan = (int) (code % 100);
            code = code / 100;
            argMatchIndex = (int) (code % 100);
            code = code / 100;
            argWordLength = (int) (code % 100);
            code = code / 100;
            argRate = (int) (code % 1000);
            code = code / 1000;
            argPatternLength = (int) (code % 100);
        }

        if ( patternLength == UNASSIGNED ) {
            patternLength = argPatternLength;
        }

        if ( positionsRow == UNASSIGNED ) {
            positionsRow = exemplarAggregate(argPatternLength);
        }

        if ( patternStartFound || argMatchIndex == 0 ) {
            patternStartFound = true;
            firstIndexInPattern = 0;
        }
        else {
            if ( firstIndexInPattern == UNASSIGNED ) {
                firstIndexInPattern = argMatchIndex;
            }
            else if ( argMatchIndex < firstIndexInPattern ) {
                firstIndexInPattern = argMatchIndex;
            }
        }

        long positionsBeforeWordApplying = positionsRow;

        long positionIndexInRow;
        int position;
        int overlapsInWord = 0;
        boolean matchStartsWithOverlap = false;

        if ( argMatchSpan == argFound ) {
            int matchEnd = argMatchIndex + argFound;
            for ( int i = argMatchIndex; i < matchEnd; i++) {
                positionIndexInRow = (long) Math.pow(10, (patternLength - i) - 1);
                if ( positionIndexInRow == 0 ) {
                    break;
                }
                position = positionAt(positionsRow, positionIndexInRow);
                if ( position == NON_FILLED_POSITION ) {
                    positionsRow = positionsRow - positionIndexInRow * (NON_FILLED_POSITION-1);
                }
                else if ( position == WORD_SPACE_POSITION ) {
                    positionsRow = positionsRow - positionIndexInRow * (WORD_SPACE_POSITION-1);
                }
                else if ( position == WORD_SPACE_POSITION-1 ) {
                    overlapsInWord++;
                    if ( i == argMatchIndex ) {
                        matchStartsWithOverlap = true;
                    }
                } else if ( position < WORD_SPACE_POSITION-1 ) {
                    positionsRow = positionsRow + positionIndexInRow;
                    overlapsInWord++;
                    if ( i == argMatchIndex ) {
                        matchStartsWithOverlap = true;
                    }
                }
            }
        }
        else {
            int matchSpanEnd = argMatchIndex + argMatchSpan;
            int matchFoundEnd = argMatchIndex + argFound;
            for ( int i = argMatchIndex; i < matchSpanEnd; i++) {
                positionIndexInRow = (long) Math.pow(10, (patternLength - i) - 1);
                if ( positionIndexInRow == 0 ) {
                    break;
                }
                position = positionAt(positionsRow, positionIndexInRow);

                boolean isPositionFull = i < matchFoundEnd;
                if ( position == NON_FILLED_POSITION ) {
                    if ( isPositionFull ) {
                        positionsRow = positionsRow - positionIndexInRow * 7;
                    }
                    else {
                        positionsRow = positionsRow - positionIndexInRow;
                    }
                }
                else if ( position == WORD_SPACE_POSITION ) {
                    if ( isPositionFull ) {
                        positionsRow = positionsRow - positionIndexInRow * (WORD_SPACE_POSITION-1);
                        overlapsInWord++;
                        if ( i == argMatchIndex ) {
                            matchStartsWithOverlap = true;
                        }
                    }
                }
                else if ( position == WORD_SPACE_POSITION-1 ) {
                    if ( isPositionFull ) {
                        overlapsInWord++;
                        if ( i == argMatchIndex ) {
                            matchStartsWithOverlap = true;
                        }
                    }
                }
                else if ( position < WORD_SPACE_POSITION-1 ) {
                    if ( isPositionFull ) {
                        positionsRow = positionsRow + positionIndexInRow;
                        overlapsInWord++;
                        if ( i == argMatchIndex ) {
                            matchStartsWithOverlap = true;
                        }
                    }
                }
            }
        }

        boolean rollbackWordApplying = false;

        if ( matchStartsWithOverlap && overlapsInWord == 1 && argMatchSpan == 2 ) {
            rollbackWordApplying = true;
        }

        if ( rollbackWordApplying ) {
            positionsRow = positionsBeforeWordApplying;
        }
        else {
            overlaps = overlaps + overlapsInWord;
            words++;
            wordsLengthSum = wordsLengthSum + argWordLength;
            rateSum = rateSum + argRate;
            if ( overlapsInWord == argMatchSpan ) {
                wordsDuplicated++;
                wordsLengthSumDuplicated = wordsLengthSumDuplicated + argWordLength;
                overlapsDuplicated = overlapsDuplicated + overlapsInWord;
            }
        }
    }

    @Override
    public Long getResult() {
        if ( words == 0 ) {
            code = NO_WORDS.value;
            return code;
        }

        if ( ! patternStartFound ) {
            if ( words == 1 ) {
                code = PATTERN_START_NOT_FOUND_AND_ONE_WORD.value;
                return code;
            }
            else {
                code = PATTERN_START_NOT_FOUND_AND_MANY_WORDS.value;
                return code;
            }
        }

        if ( positionsRow > 0 ) {
            long positionsDecoding = positionsRow;
            long position;
            while ( positionsDecoding > 0 ) {
                position = positionsDecoding % 10;
                if ( position == NON_FILLED_POSITION ) {
                    missed++;
                }
                if ( position == WORD_SPACE_POSITION) {
                    wordSpaces++;
                }
                positionsDecoding = positionsDecoding / 10;
            }
//            return format("words:%s rate%s overlap:%s missed:%s positions:%s", wordsCount, rateSum, overlap, missed, aggregate);
        }
        else {
//            return format("words:%s rate%s overlap:%s missed:%s", wordsCount, rateSum, overlap, missed);
        }

        if ( patternLength < 6 && missed > 0 ) {
            code = PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS.value;
            return code;
        }

        if ( wordsDuplicated > 0 ) {
            int deduplicatedWordsLength = wordsLengthSum - wordsLengthSumDuplicated;
            int deduplicatedOverlaps = overlaps - overlapsDuplicated;
            if ( deduplicatedWordsLength < patternLength ) {
                code = WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL.value;
                return code;
            }
            else {
                int realMeaningfulWordsLength = deduplicatedWordsLength - deduplicatedOverlaps;
                if ( realMeaningfulWordsLength < patternLength ) {
                    code = WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL.value;
                    return code;
                }
            }
        }
        else {
            if ( wordsLengthSum < patternLength) {
                code = WORDS_TOTAL_LENGTH_SUM_TOO_SMALL.value;
                return code;
            }
            else {
                int meaningfulWordsLength = wordsLengthSum - overlaps;
                if ( meaningfulWordsLength < patternLength ) {
                    code = WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL.value;
                    return code;
                }
            }
        }

        if ( missed == 0 ) {
            if ( wordSpaces == 0 ) {
                code = encodeResult();
            }
            else {
                int wordSpacesThreshold = patternLength / 3;
                if ( wordSpacesThreshold == 0 ) {
                    wordSpacesThreshold = 1;
                }

                if ( missed > wordSpacesThreshold ) {
                    code = TOO_MUCH_SPACES_IN_WORDS.value;
                }
                else {
                    code = encodeResult();
                }
            }
        }
        else {
            if ( wordSpaces == 0 ) {
                int missedThreshold = patternLength / 4;
                if ( missedThreshold == 0 ) {
                    missedThreshold = 1;
                }

                if ( missed >= missedThreshold ) {
                    code = TOO_MUCH_MISSED.value;
                }
                else {
                    code = encodeResult();
                }
            }
            else {
                int missedAndSpacesThreshold = patternLength / 3;
                if ( missedAndSpacesThreshold == 0 ) {
                    missedAndSpacesThreshold = 1;
                }

                if ( (missed + wordSpaces) > missedAndSpacesThreshold ) {
                    code = TOO_MUCH_SPACES_AND_MISSED_IN_WORDS.value;
                }
                else {
                    code = encodeResult();
                }
            }
        }

        return code;
    }

    public long encodeResult() {
        long code = BASE_AGGREGATION_CODE;

        code = code + rateSum           * 100000000000L;
        code = code + words             * 1000000000L;
        code = code + wordsLengthSum    * 1000000L;
        code = code + missed            * 10000L;
        code = code + wordSpaces        * 100L;
        code = code + overlaps;

        return code;
    }

    public long positionsRow() {
        return positionsRow;
    }

    public int patternLength() {
        return patternLength;
    }

    public int wordsCount() {
        return words;
    }

    public int wordsLengthSum() {
        return wordsLengthSum;
    }

    public int overlaps() {
        return overlaps;
    }

    public int missed() {
        return missed;
    }

    public int missedInSpan() {
        return wordSpaces;
    }

    public long result() {
        return code;
    }

    public int rateSum() {
        return rateSum;
    }

    public String report() {
        return String.format("result:%s pos:%s rateSum:%s missed:%s span-missed:%s overlaps:%s words:%s wordsLength:%s", code, positionsRow, rateSum, missed, wordSpaces, overlaps, words, wordsLengthSum);
    }
}
