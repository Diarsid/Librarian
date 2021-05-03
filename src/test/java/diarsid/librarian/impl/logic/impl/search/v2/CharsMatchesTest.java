package diarsid.librarian.impl.logic.impl.search.v2;

import org.junit.jupiter.api.Test;

import static diarsid.librarian.impl.logic.impl.search.charscan.CharSort.transform;
import static diarsid.librarian.impl.logic.impl.search.charscan.CountCharMatches.evaluate;
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
