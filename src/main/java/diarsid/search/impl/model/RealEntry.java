package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.rows.Row;
import diarsid.search.api.model.Entry;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class RealEntry extends AbstractIdentifiableUserScoped implements Entry {

    private final List<Label> labels;
    private String stringOrigin;
    private String stringLower;
    private Entry.Type type;

    public RealEntry(String string, UUID userUuid) {
        super(userUuid);
        string = string.trim();
        this.stringOrigin = string;
        this.stringLower = string.toLowerCase();
        this.labels = new ArrayList<>();
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    public RealEntry(String string, List<Label> labels, UUID userUuid) {
        super(userUuid);
        string = string.trim();
        this.stringOrigin = string;
        this.stringLower = string.toLowerCase();
        this.labels = labels;
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    public RealEntry(
            UUID uuid,
            String string,
            LocalDateTime time,
            List<Label> labels,
            UUID userUuid,
            State state) {
        super(uuid, time, userUuid, state);
        string = string.trim();
        this.stringOrigin = string;
        this.stringLower = string.toLowerCase();
        this.labels = labels;
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    public RealEntry(Row row) {
        super(
                row.get("uuid", UUID.class),
                row.get("time", LocalDateTime.class),
                row.get("user_uuid", UUID.class),
                STORED);
        this.stringOrigin = row.get("string_origin", String.class);
        this.stringLower = row.get("string_lower", String.class);
        this.labels = new ArrayList<>();
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    public RealEntry(String columnPrefix, Row row) {
        super(
                row.get(columnPrefix + "uuid", UUID.class),
                row.get(columnPrefix + "time", LocalDateTime.class),
                row.get(columnPrefix + "user_uuid", UUID.class),
                STORED);
        this.stringOrigin = row.get(columnPrefix + "string_origin", String.class);
        this.stringLower = row.get(columnPrefix + "string_lower", String.class);
        this.labels = new ArrayList<>();
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    @Override
    public String string() {
        return stringOrigin;
    }

    public String stringLower() {
        return stringLower;
    }

    public void changeTo(String newString) {
        newString = newString.trim();
        this.stringOrigin = newString;
        this.stringLower = newString.toLowerCase();
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    @Override
    public List<Label> labels() {
        return labels;
    }

    @Override
    public Type type() {
        return type;
    }

}
