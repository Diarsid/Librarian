package diarsid.search.impl.logic.impl.search.v1;

import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;

import static java.util.stream.Collectors.toList;

public class WordUuidString {

    private final UUID uuid;
    private final String string;

    public WordUuidString(Row row) {
        this.uuid = row.uuidOf("uuid");
        this.string = row.stringOf("string");
    }

    public UUID uuid() {
        return uuid;
    }

    public String string() {
        return string;
    }

    @Override
    public String toString() {
        return "WordString{" +
                "uuid=" + uuid +
                ", string='" + string + '\'' +
                '}';
    }

    public static List uuidsOf(List<WordUuidString> words) {
        return words
                .stream()
                .map(WordUuidString::uuid)
                .collect(toList());
    }
}
