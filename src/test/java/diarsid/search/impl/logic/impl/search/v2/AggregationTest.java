package diarsid.search.impl.logic.impl.search.v2;

import java.util.ArrayList;
import java.util.List;

import diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV14;
import diarsid.search.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV15;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class AggregationTest {

    H2AggregateFunctionForAnalyzeV15 aggregator = new H2AggregateFunctionForAnalyzeV15();
    String pattern;
    List<String> words;
    List<Long> codes = new ArrayList<>();
    int resultCode;

    void analyze(String... words) throws Exception {
        for ( String word : words ) {
            long code = PatternToWordMatchingCode.evaluate(pattern, word);
            if ( code < 0 ) {
                fail();
            }
            codes.add(code);
        }

        for ( Long code : codes ) {
            aggregator.add(code);
        }

        aggregator.getResult();

        System.out.println(aggregator.report());
    }

    @Test
    public void test() throws Exception {
        pattern = "lorofrngbyjrrtolk";
        analyze(
                "by",
                "jrr",
                "lord",
                "of",
                "rings",
                "tolkien");

        int a = 5;
    }

    @Test
    public void test_1() throws Exception {
        pattern = "lorofrngbyjrrtolk";
        analyze(
                "by",
                "of",
                "or",
                "to",
                "too");

        int a = 5;
    }

    @Test
    public void test_2() throws Exception {
        pattern = "lorofrngbyjrrtolk";
        analyze(
                "by",
                "jerry",
                "lore",
                "of",
                "old");

        int a = 5;
    }

    @Test
    public void test_3() throws Exception {
        pattern = "3toolssevrirtl";
        analyze(
                "tools",
                "3tools",
                "servers",
                "virtualization");

        int a = 5;
    }

    @Test
    public void test_4() throws Exception {
        pattern = "3toolssevrirtl";
        analyze(
                "toss",
                "to",
                "rinker",
                "lrinker");

        int a = 5;
    }

    @Test
    public void test_5() throws Exception {
        pattern = "toolssevrirtl";
        analyze(
                "tools",
                "3tools",
                "servers",
                "virtualization");

        int a = 5;
    }

    @Test
    public void test_6() throws Exception {
        pattern = "toolssevrirtl";
        analyze(
                "seven",
                "to",
                "robert");

        int a = 5;
    }

    @Test
    public void test_7() throws Exception {
        pattern = "jrtolkguide";
        analyze(
                "JRR",
                "tolkien",
                "guide");

        int a = 5;
    }

    @Test
    public void test_8() throws Exception {
        pattern = "whltwhtmn";
        analyze(
                "walt",
                "whitman's");

        int a = 5;
    }

    @Test
    public void test_9() throws Exception {
        pattern = "tolos";
        analyze(
                "to",
                "london");

        int a = 5;
    }

    @Test
    public void test_10() throws Exception {
        pattern = "tolos";
        analyze(
                "to",
                "toddlers");

        int a = 5;
    }

}
