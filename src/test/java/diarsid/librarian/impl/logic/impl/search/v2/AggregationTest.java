package diarsid.librarian.impl.logic.impl.search.v2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV17;
import diarsid.librarian.impl.logic.impl.search.PatternToWordMatchingCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class AggregationTest {

    H2AggregateFunctionForAnalyzeV17 aggregator = new H2AggregateFunctionForAnalyzeV17();
    String pattern;
    Boolean expectOk;
    List<String> words;
    List<WordCode> wordCodes = new ArrayList<>();
    int resultCode;

    void analyze() throws SQLException {
        boolean isFailed = false;
        for ( String word : words ) {
            long code = PatternToWordMatchingCode.evaluate(pattern, word);
            if ( code < 0 ) {
                isFailed = true;
            }
            wordCodes.add(new WordCode(word, code));
        }

        if ( isFailed && expectOk ) {
            fail();
        }

        for ( WordCode wordCode : wordCodes) {
            aggregator.add(wordCode.code);
        }

        resultCode = aggregator.getResult();

        System.out.println(aggregator.report());

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
        PatternToWordMatchingCode.logEnabled.resetTo(false);
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

}
