package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV23;
import diarsid.librarian.impl.logic.impl.search.UuidAndAggregationCode;
import diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching;
import diarsid.librarian.tests.model.WordMatchingCode;
import diarsid.support.strings.MultilineMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.UUID.randomUUID;

import static diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching.CURRENT_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class AggregationTest {

    static Logger log = LoggerFactory.getLogger(AggregationTest.class);

    H2AggregateFunctionForAnalyzeV23 aggregator = new H2AggregateFunctionForAnalyzeV23();
    PatternToWordMatching matching = CURRENT_VERSION;
    String pattern;
    Boolean expectOk;
    List<String> words;
    List<WordMatchingCode> wordCodes = new ArrayList<>();
    long resultCode;
    UuidAndAggregationCode uuidAndAggregationCode;

    void analyze() throws SQLException {
        MultilineMessage report = new MultilineMessage("", "   ");
        report.newLine().add("pattern : ").add(pattern);

        for ( String word : words ) {
            long code = matching.evaluate(pattern, word);
            wordCodes.add(new WordMatchingCode(word, code));
            report.newLine().indent(2).add(word).add(" : ").add(code).add(" : ").add(matching.describe(code).toString());
        }

        for ( WordMatchingCode wordCode : wordCodes) {
            if ( wordCode.code > 0 ) {
                aggregator.add(wordCode.code);
            }
        }

        resultCode = aggregator.getResult();
        if ( resultCode > -1 ) {
            uuidAndAggregationCode = new UuidAndAggregationCode(randomUUID(), resultCode);
            assertThat(uuidAndAggregationCode.missed).isEqualTo(aggregator.missed());
            assertThat(uuidAndAggregationCode.overlaps).isEqualTo(aggregator.overlaps());
            assertThat(uuidAndAggregationCode.rateSum).isEqualTo(aggregator.rateSum());
        }
        else {
            var reason = H2AggregateFunctionForAnalyzeV23
                    .RejectionReason
                    .findByValue((int) resultCode)
                    .map(Enum::name)
                    .orElse("UNKOWN");

            report.newLine().add("rejection reason: ").add(reason);
        }

        report.newLine().add("result  : ").add(aggregator.report());
        log.info(report.compose());

        boolean expectOkButFail = expectOk && resultCode < 0;
        boolean expectFailButOk = !expectOk && resultCode > 0;

        if ( expectOkButFail || expectFailButOk ) {
            fail();
        }
    }

//    public String report("") {
//        MultilineMessage message = new MultilineMessage();
//        message.add("result       :").add(result).newLine();
//        message.add("positions    :").add(String.valueOf(positionsRow).replace('8', '_')).newLine();
//        message.add("rate sum     :").add(rateSum).newLine();
//        message.add("missed       :").add(missed).newLine();
//        message.add("overlaps     :").add(overlaps).newLine();
//        message.add("words count  :").add(words).newLine();
//        message.add("words length :").add(wordsLengthSum).newLine();
//        return message.toString();
//    }

    @Test
    public void test() throws Exception {
        pattern = "lorofrngbyjrrtolk";
        words = List.of(
                "by",
                "jrr",
                "lord",
                "of",
                "rings",
                "tolkien");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_1() throws Exception {
        pattern = "lorofrngbyjrrtolk";
        words = List.of(
                "by",
                "of",
                "or",
                "to",
                "too");
        expectOk = false;
        analyze();
    }

    @Test
    public void test_2() throws Exception {
        pattern = "lorofrngbyjrrtolk";
        words = List.of(
                "by",
                "jerry",
                "lore",
                "of",
                "old");
        expectOk = false;
        analyze();
    }



    @Test
    public void test_byjrrtolk() throws Exception {
        pattern = "byjrrtolk";
        words = List.of(
                "by",
                "jrr",
                "tolkien");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_3() throws Exception {
        pattern = "3toolssevrirtl";
        words = List.of(
                "tools",
                "3tools",
                "servers",
                "virtualization");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_4() throws Exception {
        pattern = "3toolssevrirtl";
        words = List.of(
                "toss",
                "to",
                "rinker",
                "lrinker");
        expectOk = false;
        analyze();
    }

    @Test
    public void test_5() throws Exception {
        pattern = "toolssevrirtl";
        words = List.of(
                "tools",
                "3tools",
                "servers",
                "virtualization");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_6() throws Exception {
        pattern = "toolssevrirtl";
        words = List.of("seven", "to", "robert");
        expectOk = false;
        analyze();
    }

    @Test
    public void test_7() throws Exception {
        pattern = "jrtolkguide";
        words = List.of("jrr", "tolkien", "guide");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_8() throws Exception {
        pattern = "whltwhtmn";
        words = List.of("walt", "whitman's");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_8_1() throws Exception {
        pattern = "whltwhtmn";
        words = List.of("walt", "whitman");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_9() throws Exception {
        pattern = "tolos";
        words = List.of("to", "london");
        expectOk = false;
        analyze();
    }

    @Test
    public void test_10() throws Exception {
        pattern = "tolos";
        words = List.of("to", "toddlers");
        expectOk = false;
        analyze();
    }

    @Test
    public void test_11() throws Exception {
        pattern = "lorofrng";
        words = List.of("frog", "lorrie");
        expectOk = false;
        analyze();
    }

    @Test
    public void test_12() throws Exception {
        pattern = "yasnrkawbata";
        words = List.of("yasunari", "kawabata");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_13() throws Exception {
        pattern = "yasnkawbata";
        words = List.of("yasunari", "kawabata");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_drklalver() throws Exception {
        pattern = "drklalver";
        words = List.of("dracula", "lover");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_drklalver_1() throws Exception {
        pattern = "drklalver";
        words = List.of("draculas", "lover");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_drklalver_2() throws Exception {
        pattern = "drklalver";
        words = List.of("draculas", "dracula", "lover");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_romeries() throws Exception {
        pattern = "romeries";
        words = List.of("rome", "rise");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_lororng() throws Exception {
        pattern = "lororng";
        words = List.of(
                "lord",
                "of",
                "rings");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_lororng_2() throws Exception {
        pattern = "lororng";
        words = List.of(
                "the",
                "lord",
                "of",
                "rings", "by", "rob", "inglis", "and", "jrr", "tolkien", "alan", "lee");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_tolosvirtl() throws Exception {
        pattern = "tolosvirtl";
        words = List.of(
                "tools",
                "lvirtualization");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_kwisahaderh() throws Exception {
        pattern = "kwisahaderh";
        words = List.of(
                "kwisatz",
                "haderach",
                "is",
                "desire",
                "had",
                "aharkonnen",
                "have",
                "her",
                "adaughter",
                "desert",
                "another",
                "has",
                "ha",
                "hebrew",
                "derekh");

        expectOk = true;
        analyze();
    }

//    too much 7
//    P:
//        4 : The Last Hours of Ancient Sunlight: The Fate of the World and What We Can Do Before It's Too Late by Neale Donald Walsch and Thom Hartmann, and Joseph Chilton Pearce
//    result:80015402012000403 pos:122117777 rateSum:154 missed:0 span-missed:4 overlaps:3 words:2 wordsLength:12
//     : 10909208010804 : pattern_L=9, word_L=8, found=4, match_Ix=1, match_Span=8, rate=92
//     : 10906204000403 : pattern_L=9, word_L=4, found=3, match_Ix=0, match_Span=4, rate=62
    @Test
    public void test_whltwhtmn() throws Exception {
        pattern = "whltwhtmn";
        words = List.of(
                "hartmann",
                "what");
        expectOk = true;
        analyze();
    }

//    incorrect positions row:
//            0 : The Sacred Art of Lovingkindness: Preparing to Practice (The Art of Spiritual Living) by Rami M. Shapiro
//    result:80020703017000101 pos:1111211117 rateSum:207 missed:0 span-missed:1 overlaps:1 words:3 wordsLength:17
//    spiritual : 11010109040605 : pattern_L=10, word_L=9, found=5, match_Ix=4, match_Span=6, rate=101
//    living : 11006206020503 : pattern_L=10, word_L=6, found=3, match_Ix=2, match_Span=5, rate=62
//    to : 11004402000202 : pattern_L=10, word_L=2, found=2, match_Ix=0, match_Span=2, rate=44
    @Test
    public void test________() throws Exception {
        pattern = "????????";
        words = List.of(
                "spiritual",
                "living",
                "to");
        expectOk = true;
        analyze();
    }

    // TODO test
    @Test
    public void test_tolknlororing() throws Exception {
        pattern = "tolknlororing";
        words = List.of(
                "lord",
                "of",
                "rings",
                "tolkien");
        expectOk = true;
        analyze();
    }

}
