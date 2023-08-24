package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import static diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching.currentVersion;

public class AggregationTest {

    static Logger log = LoggerFactory.getLogger(AggregationTest.class);

    static final PatternToWordMatching MATCHING = currentVersion();
    static {
        MATCHING.setLoggingEnabled(false);
    }

    String pattern;
    Boolean expectOk;
    List<String> words;
    long resultCode;

    void analyze() throws SQLException {
        PatternAndWords patternAndWords = new PatternAndWords(MATCHING, pattern, words);

        if ( patternAndWords.aggregationCode != null ) {
            assertThat(patternAndWords.aggregationCode.missed).isEqualTo(patternAndWords.aggregator.missed());
            assertThat(patternAndWords.aggregationCode.overlaps).isEqualTo(patternAndWords.aggregator.overlaps());
            assertThat(patternAndWords.aggregationCode.rateSum).isEqualTo(patternAndWords.aggregator.rateSum());
        }

        log.info(patternAndWords.report.compose());

        resultCode = patternAndWords.resultCode;

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

    @Test
    public void test_kwistzhadrch() throws Exception {
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
        expectOk = false;
        analyze();
    }

    @Disabled
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

    @Test
    public void test_javengins() throws Exception {
        pattern = "javengins";
        words = List.of(
                "java",
                "engines");
        expectOk = true;
        analyze();
    }

    @Disabled
    @Test
    public void test_XXX() throws Exception {
        pattern = "xabc";
        words = List.of(
                "xyyy",
                "aoboco");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_upostarch() throws Exception {
        pattern = "upostarch";
        words = List.of(
                "poshta",
                "archive",
                "ukr");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_upshstnoftftclnt() throws Exception {
        pattern = "upshstnoftftclnt";
        words = List.of(
                "ukr",
                "poshta",
                "status",
                "notification",
                "client",
                "ua");
        expectOk = true;
        analyze();
    }

    @Test
    public void test_virtualization_virtlzn() throws Exception {
        pattern = "virtlzn";
        words = List.of(
                "virtualization"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_hofmaninctant() throws Exception {
        pattern = "hofmaninctant";
        words = List.of(
                "incantation",
                "hoffman",
                "by",
                "alice"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_projupsth() throws Exception {
        pattern = "projupsth";
        words = List.of(
                "1projects",
                "ddev",
                "dev",
                "poshta",
                "projects",
                "ukr"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_projsupsth() throws Exception {
        pattern = "projsupsth";
        words = List.of(
                "1projects",
                "ddev",
                "dev",
                "poshta",
                "projects",
                "ukr"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_progsloclscl() throws Exception {
        pattern = "progsloclscl";
        words = List.of(
                "social",
                "locally",
                "programs"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_rddragnhanballectr() throws Exception {
        pattern = "rddragnhanballectr";
        words = List.of(
                "red",
                "dragon",
                "hannibal",
                "lecter"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_witwidsmwinstchucrl() throws Exception {
        pattern = "witwidsmwinstchucrl";
        words = List.of(
                "wit",
                "wisdom",
                "winston",
                "churchill"
        );
        expectOk = true;
        analyze();
    }

    @Test
    public void test_conhelth() throws Exception {
        pattern = "conhelth";
        words = List.of(
                "the",
                "coaches"
        );
        expectOk = false;
        analyze();
    }

    @Test
    public void test_rrhgtphilpsy() throws Exception {
        pattern = "rrhgtphilpsy";
        words = List.of(
                "philosophy",
                "right"
        );
        expectOk = true;
        analyze();
    }
}
