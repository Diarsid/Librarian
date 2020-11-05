package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.jdbc.api.rows.Row;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class Word extends AbstractIdentifiableUserScoped {

    public final static String SEPARATOR = "_";

    public final static Comparator<Word> COMPARE_WORDS_BY_STRING = (word1, word2) -> {
        return word1.string.compareTo(word2.string);
    };

    private final String string;

    public Word(String string, LocalDateTime time, UUID userUuid) {
        super(randomUUID(), time, userUuid);
        this.string = string;
    }

    public Word(Row row) {
        super(
                ColumnGetter.uuidOf("uuid").getFrom(row),
                ColumnGetter.timeOf("time").getFrom(row),
                ColumnGetter.uuidOf("user_uuid").getFrom(row),
                STORED);
        this.string = ColumnGetter.stringOf("string").getFrom(row);
    }

    public String string() {
        return string;
    }

    public static String join(List<Word> words) {
        return words
                .stream()
                .sorted(COMPARE_WORDS_BY_STRING)
                .map(Word::string)
                .collect(joining(SEPARATOR));
    }
}
