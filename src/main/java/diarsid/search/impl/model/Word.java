package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class Word extends AbstractIdentifiableUserScoped {

    public final static String SEPARATOR = "_";

    public final static Comparator<Word> COMPARE_WORDS_BY_STRING = Comparator.comparing(word -> word.string);

    private final String string;

    public Word(String string, LocalDateTime time, UUID userUuid) {
        super(randomUUID(), time, userUuid);
        this.string = string;
    }

    public Word(Row row) {
        super(
                row.uuidOf("uuid"),
                row.timeOf("time"),
                row.uuidOf("user_uuid"),
                STORED);
        this.string = row.stringOf("string");
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
