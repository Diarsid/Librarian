package diarsid.librarian;

import java.util.List;

import org.junit.jupiter.api.Test;

import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.librarian.impl.logic.impl.StringTransformations.toSimplifiedWords;
import static org.assertj.core.api.Assertions.assertThat;

public class StringTransformationsTest {

    @Test
    public void test() {
        String name = "John D.";
        List<String> expected = List.of("john", "d");

        assertThat(expected).isEqualTo(toSimplifiedWords(name, CASE_TO_LOWER, true, true, true, true));
    }
}
