package diarsid.search;

import java.util.List;

import diarsid.search.impl.logic.impl.WordsInEntriesImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WordInEntriesTest {

    @Test
    public void test_joinSingleCharsToNextWord() {
        List<String> given = List.of("A", "Book", "Name", "written", "by", "D", "J", "Doe");
        List<String> result = WordsInEntriesImpl.joinSingleCharsToNextWord(given);
        assertThat(result).isEqualTo(List.of("ABook", "Name", "written", "by", "DJDoe"));
    }

    @Test
    public void test_joinSingleCharsToNextWord_noClosingWord() {
        List<String> given = List.of("A", "Book", "Name", "written", "by", "Doe", "D", "J");
        List<String> result = WordsInEntriesImpl.joinSingleCharsToNextWord(given);
        assertThat(result).isEqualTo(List.of("ABook", "Name", "written", "by", "Doe", "DJ"));
    }

    @Test
    public void test_joinSingleCharsToNextWord_onlySingleChars() {
        List<String> given = List.of("A", "B", "C");
        List<String> result = WordsInEntriesImpl.joinSingleCharsToNextWord(given);
        assertThat(result).isEqualTo(List.of("ABC"));
    }

}
