package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;
import diarsid.support.objects.CommonEnum;
import diarsid.support.strings.PathUtils;
import diarsid.support.strings.StringUtils;

import static diarsid.search.api.model.Entry.Type.PATH;
import static diarsid.search.api.model.meta.Storable.State.STORED;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_ORIGINAL;
import static diarsid.search.impl.model.RealEntry.CaseConversion.CASE_TO_LOWER;

public class RealEntry extends AbstractIdentifiableUserScoped implements Entry {

    public enum CaseConversion implements CommonEnum<CaseConversion> {
        CASE_TO_LOWER,
        CASE_ORIGINAL
    }

    private final List<Label> labels;
    private String stringOrigin;
    private String stringLower;
    private Entry.Type type;

    public RealEntry(String string, UUID userUuid) {
        super(userUuid);
        string = string.trim();
        this.stringOrigin = string;
        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
        this.stringLower = unifyOriginalString(this.stringOrigin, this.type);
        this.labels = new ArrayList<>();
    }

    public RealEntry(String string, List<Label> labels, UUID userUuid) {
        super(userUuid);
        string = string.trim();
        this.stringOrigin = string;
        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
        this.stringLower = unifyOriginalString(this.stringOrigin, this.type);
        this.labels = labels;
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
        this.type = Entry.Type.defineTypeOf(this.stringOrigin);
        this.stringLower = unifyOriginalString(this.stringOrigin, this.type);
        this.labels = labels;
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

    public void changeTo(String newString, String unifiedNewString) {
        newString = newString.trim();
        this.stringOrigin = newString;
        this.stringLower = unifiedNewString;
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

        return unified;
    }

    public static void main(String[] args) {
        String s = "aaa-bbbb";
        String s1 = unifyOriginalString(s, CASE_ORIGINAL);
        int a =5;
    }

    @Override
    public String toString() {
        return "RealEntry{" +
                "uuid='" + super.uuid() + '\'' +
                ", origin='" + stringOrigin + '\'' +
                ", type=" + type +
                '}';
    }
}
