package diarsid.search.impl.logic.impl.search.v2;

import org.junit.jupiter.api.Test;

import static diarsid.search.impl.logic.impl.search.v2.CharSort.transform;
import static diarsid.search.impl.logic.impl.search.v2.CountCharMatches.evaluate;
import static org.junit.jupiter.api.Assertions.fail;

public class CharsMatchesTest {

    @Test
    public void test_1() {
        String s1 = transform("abcefg");
        String s2 = transform("xabc");

        int matches = evaluate(s1, s2, 60);

        if ( matches != 3 ) {
            fail();
        }
    }
}