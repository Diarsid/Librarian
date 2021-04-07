package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;
import diarsid.support.objects.CommonEnum;
import diarsid.support.strings.PathUtils;
import diarsid.support.strings.StringUtils;

import static java.time.LocalDateTime.now;

import static diarsid.search.api.model.Entry.Type.PATH;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_ORIGINAL;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_TO_LOWER;
import static diarsid.support.model.Storable.State.STORED;

public class RealEntry extends AbstractUpdatableUserScoped implements Entry {

    public enum CaseConversion implements CommonEnum<CaseConversion> {
        CASE_TO_LOWER,
        CASE_ORIGINAL
    }

    private final String stringOrigin;
    private final String stringLower;
    private final Entry.Type type;

    public RealEntry(String string, UUID userUuid) {
        super(userUuid);
        string = string.trim();
        this.stringOrigin = string;
        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
        this.stringLower = unifyOriginalString(this.stringOrigin, this.type);
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
        this.stringLower = unifyOriginalString(this.stringOrigin, this.type);
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

    public static String unifyOriginalString(String original, CaseConversion caseConversion) {
        String unified = original.trim().strip();;

        if ( caseConversion.equalTo(CASE_TO_LOWER) ) {
            unified = unified.toLowerCase();
        }

        unified = StringUtils.normalizeSpaces(unified);
        unified = StringUtils.normalizeDashes(unified);

        if ( StringUtils.containsPathSeparator(unified) ) {
            unified = PathUtils.normalizeSeparators(unified);
        }

        unified = unified.replace('-', ' ');
        unified = unified.replace('#', 'N');
        unified = StringUtils.removeSpecialCharsFrom(unified, ' ', '/');

        return unified;
    }

    public static String unifyOriginalString(String original, Entry.Type type) {
        String unified;

        unified = original.toLowerCase().trim().strip();
        unified = StringUtils.normalizeSpaces(unified);
        unified = StringUtils.normalizeDashes(unified);

        if ( type.equalTo(PATH) ) {
            unified = PathUtils.normalizeSeparators(unified);
        }

        unified = unified.replace('-', ' ');
        unified = unified.replace('#', 'N');
        unified = StringUtils.removeSpecialCharsFrom(unified, ' ', '/');

        return unified;
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
