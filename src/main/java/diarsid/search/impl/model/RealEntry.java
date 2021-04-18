package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.impl.StringTransformations;

import static java.time.LocalDateTime.now;

import static diarsid.search.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.support.model.Storable.State.STORED;

public class RealEntry extends AbstractUpdatableUserScoped implements Entry {

    private final String stringOrigin;
    private final String stringLower;
    private final Entry.Type type;

    public RealEntry(String string, UUID userUuid) {
        super(userUuid);
        string = string.trim();
        this.stringOrigin = string;
        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
        this.stringLower = StringTransformations.simplify(this.stringOrigin, CASE_TO_LOWER, this.type);
    }

//    public RealEntry(String string, List<Label> labels, UUID userUuid) {
//        super(userUuid);
//        string = string.trim();
//        this.stringOrigin = string;
//        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
//        this.stringLower = unifyOriginalString(this.stringOrigin, this.type);
//        this.labels = labels;
//    }

    private RealEntry(RealEntry previous, String newString, LocalDateTime time) {
        super(previous.uuid(), time, now(), previous.userUuid(), previous.state());
        this.stringOrigin = newString.trim();
        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
        this.stringLower = StringTransformations.simplify(this.stringOrigin, CASE_TO_LOWER, this.type);
    }

    public RealEntry(Row row, LocalDateTime actualAt) {
        super(
                row.get("uuid", UUID.class),
                row.get("time", LocalDateTime.class),
                actualAt,
                row.get("user_uuid", UUID.class),
                STORED);
        this.stringOrigin = row.get("string_origin", String.class);
        this.stringLower = row.get("string_lower", String.class);
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    public RealEntry(String columnPrefix, Row row, LocalDateTime actualAt) {
        super(
                row.get(columnPrefix + "uuid", UUID.class),
                row.get(columnPrefix + "time", LocalDateTime.class),
                actualAt,
                row.get(columnPrefix + "user_uuid", UUID.class),
                STORED);
        this.stringOrigin = row.get(columnPrefix + "string_origin", String.class);
        this.stringLower = row.get(columnPrefix + "string_lower", String.class);
        this.type = Entry.Type.defineTypeOf(this.stringLower);
    }

    public RealEntry newEntryWith(String otherString) {
        return new RealEntry(otherString, super.userUuid());
    }

    @Override
    public String string() {
        return stringOrigin;
    }

    public String stringLower() {
        return stringLower;
    }

    public RealEntry changeTo(LocalDateTime time, String newString) {
        RealEntry changed = new RealEntry(this, newString, time);
        return changed;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "'" + super.uuid() + '\'' +
                ", '" + stringOrigin + '\'' +
                ", '" + super.createdAt() + '\'' +
                '}';
    }
}
