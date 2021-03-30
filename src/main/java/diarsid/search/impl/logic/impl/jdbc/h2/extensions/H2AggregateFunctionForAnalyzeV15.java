package diarsid.search.impl.logic.impl.jdbc.h2.extensions;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.h2.api.AggregateFunction;

import static java.sql.Types.INTEGER;

import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.NO_WORDS;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.ONE_WORD_AND_PATTERN_START_NOT_FOUND;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.TOO_MUCH_MISSED;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL;
import static diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15.RejectionReason.WORDS_TOTAL_LENGTH_SUM_TOO_SMALL;

public class H2AggregateFunctionForAnalyzeV15 implements AggregateFunction {

    private static final int UNASSIGNED = -1;
    private static final int NON_LOCATED_POSITION = 8;

    public enum RejectionReason {

        NO_WORDS(-4),
        TOO_MUCH_MISSED(-5),
        WORDS_TOTAL_LENGTH_SUM_TOO_SMALL(-6),
        WORDS_MEANINGFUL_LENGTH_SUM_TOO_SMALL(-7),
        WORDS_DEDUPLICATED_TOTAL_LENGTH_SUM_TOO_SMALL(-8),
        WORDS_DEDUPLICATED_MEANINGFUL_LENGTH_SUM_TOO_SMALL(-9),
        ONE_WORD_AND_PATTERN_START_NOT_FOUND(-10),
        PATTERN_TOO_SMALL_TO_HAVE_MISSED_CHARS(-11);

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
    private int rateSum = 0;
    private int result = 0;

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

    public static int numberAtPosition(long where, long factor) {
        long remnant = where % (factor*10);
        return (int) (remnant / factor);
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

        int matchLength = (int) (code % 100);

        code = code / 100;

        int matchIndex = (int) (code % 100);

        code = code / 100;

        int wordLength = (int) (code % 100);

        code = code / 100;

        int rate = (int) (code % 1000);

        code = code / 1000;

        int totalLength = (int) (code % 100);

        if ( patternLength == UNASSIGNED ) {
            patternLength = totalLength;
        }

        if ( positionsRow == UNASSIGNED ) {
            positionsRow = exemplarAggregate(totalLength);
        }

        if ( patternStartFound || matchIndex == 0 ) {
            patternStartFound = true;
            firstIndexInPattern = 0;
        }
        else {
            if ( firstIndexInPattern == UNASSIGNED ) {
                firstIndexInPattern = matchIndex;
            }
            else if ( matchIndex < firstIndexInPattern ) {
                firstIndexInPattern = matchIndex;
            }
        }

        long position;
        int number;
        int overlapsInWord = 0;
        for ( int i = matchIndex; i < matchIndex + matchLength; i++) {
            position = (long) Math.pow(10, (totalLength - i) - 1);
            if ( position == 0 ) {
                break;
            }
            number = numberAtPosition(positionsRow, position);
            if ( number == NON_LOCATED_POSITION ) {
                positionsRow = positionsRow - position * 7;
            }
            else {
                if ( number < 7 ) {
                    positionsRow = positionsRow + position;
                }
                overlapsInWord++;
            }
        }

        overlaps = overlaps + overlapsInWord;
        words++;
        wordsLengthSum = wordsLengthSum + wordLength;
        rateSum = rateSum + rate;
        if ( overlapsInWord == matchLength ) {
            wordsDuplicated++;
            wordsLengthSumDuplicated = wordsLengthSumDuplicated + wordLength;
            overlapsDuplicated = overlapsDuplicated + overlapsInWord;
        }
    }

    @Override
    public Integer getResult() {
        if ( words == 0 ) {
            result = NO_WORDS.value;
            return result;
        }

        if ( ! patternStartFound && words == 1 ) {
            result = ONE_WORD_AND_PATTERN_START_NOT_FOUND.value;
            return result;
        }

        if ( positionsRow > 0 ) {
            long aggregateRemnant = positionsRow;
            long aggregateRemnantOrder;
            while ( aggregateRemnant > 0 ) {
                aggregateRemnantOrder = aggregateRemnant % 10;
                if ( aggregateRemnantOrder == 8 ) {
                    missed++;
                }
                aggregateRemnant = aggregateRemnant / 10;
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
            result = rateSum;
            return rateSum;
        }

        int threshold = patternLength / 4;
        if ( threshold == 0 ) {
            threshold = 1;
        }

        if ( missed > threshold ) {
            result = TOO_MUCH_MISSED.value;
            return result;
        }
        else {
            int x = rateSum / patternLength;
            result = rateSum - (x * missed);
            return result;
        }
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

    public int result() {
        return result;
    }

    public int rateSum() {
        return rateSum;
    }

    public String report() {
        return String.format("result:%s pos:%s rateSum:%s missed:%s overlaps:%s words:%s wordsLength:%s", result, positionsRow, rateSum, missed, overlaps, words, wordsLengthSum);
    }
}
