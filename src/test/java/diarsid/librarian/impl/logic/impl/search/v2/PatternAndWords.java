package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.AggregationCodeV2;
import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV32;
import diarsid.librarian.impl.logic.impl.search.charscan.count.CountCharMatchesV1;
import diarsid.librarian.impl.logic.impl.search.charscan.count.CountCharMatchesV2;
import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching;
import diarsid.librarian.tests.model.WordMatchingCode;
import diarsid.support.strings.MultilineMessage;

import static diarsid.librarian.impl.logic.impl.search.CharSort.transform;

public class PatternAndWords {

    public final H2AggregateFunctionForAnalyzeV32 aggregator;

    public final String pattern;
    public final List<String> words;
    public final List<WordMatchingCode> wordCodes = new ArrayList<>();

    public final long resultCode;
    public final AggregationCodeV2 aggregationCode;
    public final MultilineMessage report;

    public PatternAndWords(
            PatternToWordMatching matching,
            CountCharMatchesV2 countCharMatches,
            String pattern,
            List<String> words) {
        this.aggregator = new H2AggregateFunctionForAnalyzeV32();
        this.pattern = pattern;
        this.words = words;

        this.report = new MultilineMessage("", "   ");
        report.newLine().add("pattern : ").add(pattern);

        for ( String word : words ) {
            long code = matching.evaluate(pattern, word);
            int lengthMatch = countCharMatches.evaluate(
                    pattern,
                    word,
                    transform(pattern),
                    transform(word),
                    60);
            wordCodes.add(new WordMatchingCode(word, code));
            report.newLine().indent(2).add(word).add(" : lengthMatch:").add(lengthMatch).add(" code:").add(code).add(" : ").add(matching.describe(code).toString());
        }

        for ( WordMatchingCode wordCode : wordCodes) {
            if ( wordCode.code > 0 ) {
                try {
                    aggregator.add(wordCode.code);
                }
                catch (SQLException e) {
                    // ignore, not expected here
                }
            }
        }

        resultCode = aggregator.getResult();
        if ( resultCode > -1 ) {
            aggregationCode = new AggregationCodeV2(resultCode);
        }
        else {
            aggregationCode = null;
            var reason = H2AggregateFunctionForAnalyzeV32
                    .RejectionReason
                    .findByValue((int) resultCode)
                    .map(Enum::name)
                    .orElse("UNKNOWN");

            report.newLine().add("rejection reason: ").add(reason);
        }

        report.newLine().add("aggregation: ").add(aggregator.report());
    }
}
