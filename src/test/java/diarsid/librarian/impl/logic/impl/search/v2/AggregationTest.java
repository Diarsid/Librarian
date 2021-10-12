package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV19;
import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV20;
import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV21;
import diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching;
import diarsid.librarian.impl.logic.impl.search.charscan.UuidAndResultCode;
import diarsid.librarian.tests.model.WordCode;
import diarsid.support.strings.MultilineMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.UUID.randomUUID;

import static diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching.CURRENT_VERSION;
import static diarsid.librarian.impl.logic.impl.search.charscan.PatternToWordMatching.describe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class AggregationTest {

    static Logger log = LoggerFactory.getLogger(AggregationTest.class);

    H2AggregateFunctionForAnalyzeV21 aggregator = new H2AggregateFunctionForAnalyzeV21();
    String pattern;
    Boolean expectOk;
    List<String> words;
    List<WordCode> wordCodes = new ArrayList<>();
    long resultCode;
    UuidAndResultCode uuidAndResultCode;

    void analyze() throws SQLException {
        MultilineMessage report = new MultilineMessage("", "   ");
        report.newLine().add("pattern : ").add(pattern);

        for ( String word : words ) {
            long code = CURRENT_VERSION.evaluate(pattern, word);
            wordCodes.add(new WordCode(word, code));
            report.newLine().indent(2).add(word).add(" : ").add(code).add(" : ").add(describe(code));
        }

        for ( WordCode wordCode : wordCodes) {
            if ( wordCode.code > 0 ) {
                aggregator.add(wordCode.code);
            }
        }

        resultCode = aggregator.getResult();
        if ( resultCode > -1 ) {
            uuidAndResultCode = new UuidAndResultCode(randomUUID(), resultCode);
            assertThat(uuidAndResultCode.missed).isEqualTo(aggregator.missed());
            assertThat(uuidAndResultCode.overlaps).isEqualTo(aggregator.overlaps());
            assertThat(uuidAndResultCode.patternLength).isEqualTo(aggregator.patternLength());
        }
        else {
            var reason = H2AggregateFunctionForAnalyzeV19
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

    @BeforeAll
    public static void setUp() throws Exception {
        PatternToWordMatching.logEnabled.resetTo(false);
    }

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
    public void test_lororng() throws Exception {
        pattern = "lororng";
        words = List.of(
                "the",
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
}
