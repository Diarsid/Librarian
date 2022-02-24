package diarsid.librarian.impl.logic.impl.jdbc.h2.extensions;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.h2.api.AggregateFunction;

import static java.sql.Types.INTEGER;

import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.NO_WORDS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.PATTERN_START_NOT_FOUND_AND_MANY_WORDS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.PATTERN_START_NOT_FOUND_AND_ONE_WORD;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.TOO_MUCH_MISSED;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL;
import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV22.RejectionReason.WORDS_TOTAL_LENGTH_SUM_TOO_SMALL;

public class H2AggregateFunctionForAnalyzeV22 implements AggregateFunction {

    /*
     * template '9 xx 0 yy 0 zz' where:
     * xx - pattern length
     * yy - missed
     * zz - overlaps
     */
    public static final long BASE_RESULT_TEMPLATE = 10_000_000_000L;
    private static final int UNASSIGNED = -1;
    private static final int NON_FILLED_POSITION = 8;
    private static final int NON_FOUND_POSITION = 7;

    public enum RejectionReason {

        NO_WORDS(-4),
        TOO_MUCH_MISSED(-5),
        WORDS_TOTAL_LENGTH_SUM_TOO_SMALL(-6),
        WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL(-7),
        WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL(-8),
        WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL(-9),
        PATTERN_START_NOT_FOUND_AND_ONE_WORD(-10),
        PATTERN_START_NOT_FOUND_AND_MANY_WORDS(-11),
        PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS(-12);

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
    private int missedInSpan = 0;
    private int rateSum = 0;
    private long result = 0;

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
        return INTEGER;
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

        int argFound = (int) (code % 100);

        code = code / 100;

        int argMatchSpan = (int) (code % 100);

        code = code / 100;

        int argMatchIndex = (int) (code % 100);

        code = code / 100;

        int argWordLength = (int) (code % 100);

        code = code / 100;

        int argRate = (int) (code % 1000);

        code = code / 1000;

        int argPatternLength = (int) (code % 100);

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
                if ( position == NON_FILLED_POSITION) {
                    positionsRow = positionsRow - positionIndexInRow * 7;
                }
                else {
                    if ( position < 7 ) {
                        positionsRow = positionsRow + positionIndexInRow;
                    }
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

                if ( position == NON_FILLED_POSITION ) {
                    if ( i < matchFoundEnd ) {
                        positionsRow = positionsRow - positionIndexInRow * 7;
                    }
                    else {
                        positionsRow = positionsRow - positionIndexInRow;
                    }
                }
                else {
                    if ( position < NON_FILLED_POSITION ) {
                        positionsRow = positionsRow + positionIndexInRow;
                    }
                    overlapsInWord++;
                    if ( i == argMatchIndex ) {
                        matchStartsWithOverlap = true;
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
            result = NO_WORDS.value;
            return result;
        }

        if ( ! patternStartFound ) {
            if ( words == 1 ) {
                result = PATTERN_START_NOT_FOUND_AND_ONE_WORD.value;
                return result;
            }
            else {
                result = PATTERN_START_NOT_FOUND_AND_MANY_WORDS.value;
                return result;
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
                if ( position == NON_FOUND_POSITION ) {
                    missedInSpan++;
                }
                positionsDecoding = positionsDecoding / 10;
            }
//            return format("words:%s rate%s overlap:%s missed:%s positions:%s", wordsCount, rateSum, overlap, missed, aggregate);
        }
        else {
//            return format("words:%s rate%s overlap:%s missed:%s", wordsCount, rateSum, overlap, missed);
        }

        if ( patternLength < 6 && missed > 0 ) {
            result = PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS.value;
            return result;
        }

        if ( wordsDuplicated > 0 ) {
            int deduplicatedWordsLength = wordsLengthSum - wordsLengthSumDuplicated;
            int deduplicatedOverlaps = overlaps - overlapsDuplicated;
            if ( deduplicatedWordsLength < patternLength ) {
                result = WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL.value;
                return result;
            }
            else {
                int realMeaningfulWordsLength = deduplicatedWordsLength - deduplicatedOverlaps;
                if ( realMeaningfulWordsLength < patternLength ) {
                    result = WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL.value;
                    return result;
                }
            }
        }
        else {
            if ( wordsLengthSum < patternLength) {
                result = WORDS_TOTAL_LENGTH_SUM_TOO_SMALL.value;
                return result;
            }
            else {
                int meaningfulWordsLength = wordsLengthSum - overlaps;
                if ( meaningfulWordsLength < patternLength ) {
                    result = WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL.value;
                    return result;
                }
            }
        }

        if ( missed == 0 ) {
            result = encodeResult();
            return result;
        }

        int totalMissed = 0;
        if ( missed == 0 ) {
            totalMissed = 0;
        }
        else {
            totalMissed = missed + missedInSpan;
        }

        int missedThreshold = patternLength / 4;
        if ( missedThreshold == 0 ) {
            missedThreshold = 1;
        }

        if ( totalMissed >= missedThreshold ) {
            result = TOO_MUCH_MISSED.value;
            return result;
        }
        else {
            result = encodeResult();
            return result;
        }
    }

    public long encodeResult() {
        long result = BASE_RESULT_TEMPLATE;
        result = result + overlaps;
        result = result + missedInSpan * 1_00;
        result = result + missed * 1_00_00;
        result = result + patternLength * 1_00_00_00;
        return result;
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
        return missedInSpan;
    }

    public long result() {
        return result;
    }

    public int rateSum() {
        return rateSum;
    }

    public String report() {
        return String.format("result:%s pos:%s rateSum:%s missed:%s span-missed:%s overlaps:%s words:%s wordsLength:%s", result, positionsRow, rateSum, missed, missedInSpan, overlaps, words, wordsLengthSum);
    }
}
