package diarsid.librarian.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.required.StringsComparisonAlgorithm;

public class RealPatternToEntry extends AbstractCreatedAt implements PatternToEntry {

    private final Entry entry;
    private final Pattern pattern;
    private final String algorithmName;
    private final float weight;

//    public RealPatternToEntry(Entry entry, Pattern pattern, String columnPrefix, Row row) {
//        super(
//                row.uuidOf(columnPrefix + "uuid"),
//                row.timeOf(columnPrefix + "time"));
//        this.entry = entry;
//        this.pattern = pattern;
//        this.algorithmName = row.stringOf(columnPrefix + "algorithm");
//        this.weight = row.floatOf(columnPrefix + "weight");
//    }

    public RealPatternToEntry(
            Pattern pattern,
            Row row,
            String joinColumnPrefix,
            String entryColumnPrefix,
            LocalDateTime entryActualAt) {
        super(
                row.uuidOf(joinColumnPrefix + "uuid"),
                row.timeOf(joinColumnPrefix + "time"));
        this.entry = new RealEntry(entryColumnPrefix, row, entryActualAt);
        this.pattern = pattern;
        this.algorithmName = row.stringOf(joinColumnPrefix + "algorithm");
        this.weight = row.floatOf(joinColumnPrefix + "weight");
    }

    public RealPatternToEntry(
            Entry entry,
            Row row,
            String joinColumnPrefix,
            String patternColumnPrefix) {
        super(
                row.uuidOf(joinColumnPrefix + "uuid"),
                row.timeOf(joinColumnPrefix + "time"));
        this.entry = entry;
        this.pattern = new RealPattern(patternColumnPrefix, row);
        this.algorithmName = row.stringOf(joinColumnPrefix + "algorithm");
        this.weight = row.floatOf(joinColumnPrefix + "weight");
    }

    public RealPatternToEntry(
            Row row,
            String joinColumnPrefix,
            String entryColumnPrefix,
            String patternColumnPrefix,
            LocalDateTime entryActualAt) {
        super(
                row.uuidOf(joinColumnPrefix + "uuid"),
                row.timeOf(joinColumnPrefix + "time"));
        this.entry = new RealEntry(entryColumnPrefix, row, entryActualAt);
        this.pattern = new RealPattern(patternColumnPrefix, row);
        this.algorithmName = row.stringOf(joinColumnPrefix + "algorithm");
        this.weight = row.floatOf(joinColumnPrefix + "weight");
    }

    public RealPatternToEntry(
            Entry entry, Pattern pattern, Row row) {
        super(
                row.uuidOf("uuid"),
                row.timeOf("time"));
        this.entry = entry;
        this.pattern = pattern;
        this.algorithmName = row.stringOf("algorithm");
        this.weight = row.floatOf("weight");
    }

    public RealPatternToEntry(
            UUID uuid,
            LocalDateTime time,
            RealEntry entry,
            RealPattern pattern,
            StringsComparisonAlgorithm algorithm,
            float weight) {
        super(uuid, time);
        this.entry = entry;
        this.pattern = pattern;
        this.algorithmName = algorithm.canonicalName();
        this.weight = weight;
    }

    @Override
    public Entry entry() {
        return entry;
    }

    @Override
    public Pattern pattern() {
        return pattern;
    }

    @Override
    public String algorithmCanonicalName() {
        return algorithmName;
    }

    @Override
    public float weight() {
        return weight;
    }

    @Override
    public String toString() {
        return "PatternToEntry{" +
                "'" + super.uuid() + '\'' +
                ", entry='" + entry.string() + '\'' +
                ", pattern='" + pattern.string() + '\'' +
                ", algorithm='" + algorithmName + '\'' +
                ", weight=" + weight +
                '}';
    }
}
