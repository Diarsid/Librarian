package diarsid.librarian.impl.model;

import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.api.model.Entry;
import diarsid.support.objects.CommonEnum;

public class WordInEntry extends AbstractUniqueStorable {

    public enum Position implements CommonEnum<Position> {
        FIRST, MIDDLE, LAST, SINGLE
    }

    private final UUID entryUuid;
    private final UUID wordUuid;
    private final Position position;
    private final int index;

    public WordInEntry(UUID uuid, RealEntry entry, Entry.Word word, Position position, int index) {
        super(uuid);
        this.entryUuid = entry.uuid();
        this.wordUuid = word.uuid();
        this.position = position;
        this.index = index;
    }

    public WordInEntry(Row row) {
        super(row.get("uuid", UUID.class));
        this.entryUuid = row.get("entry_uuid", UUID.class);
        this.wordUuid = row.get("word_uuid", UUID.class);
        this.position = row.get("position", Position.class);
        this.index = row.get("index", Integer.class);
    }

    public UUID entryUuid() {
        return entryUuid;
    }

    public UUID wordUuid() {
        return wordUuid;
    }

    public Position position() {
        return position;
    }

    public int index() {
        return index;
    }

    @Override
    public String toString() {
        return "WordInEntry{" +
                "entry=" + entryUuid +
                ", word=" + wordUuid +
                ", position=" + position +
                ", index=" + index +
                '}';
    }
}
