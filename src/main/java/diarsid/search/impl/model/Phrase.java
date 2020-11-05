package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.jdbc.api.rows.Row;

import static java.util.UUID.randomUUID;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class Phrase extends AbstractIdentifiableUserScoped {

    private final String string;
    private final List<Word> words;

    public Phrase(UUID userUuid, List<Word> words, LocalDateTime time) {
        super(randomUUID(), time, userUuid);
        this.words = words;
        this.words.sort(Word.COMPARE_WORDS_BY_STRING);
        this.string = Word.join(this.words);
    }

    public Phrase(Row row) {
        super(
                ColumnGetter.uuidOf("uuid").getFrom(row),
                ColumnGetter.timeOf("time").getFrom(row),
                ColumnGetter.uuidOf("user_uuid").getFrom(row),
                STORED);
        this.string = ColumnGetter.stringOf("string").getFrom(row);
        this.words = new ArrayList<>();
    }

    public List<Word> words() {
        return words;
    }

    public String string() {
        return string;
    }
}
