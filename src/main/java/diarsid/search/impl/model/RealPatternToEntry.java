package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.required.StringsComparisonAlgorithm;

public class RealPatternToEntry extends AbstractIdentifiable implements PatternToEntry {

    private final Entry entry;
    private final Pattern pattern;
    private final String algorithmName;
    private final float weight;

    public RealPatternToEntry(Entry entry, Pattern pattern, String columnPrefix, Row row) {
        super(
                row.uuidOf(columnPrefix + "uuid"),
                row.timeOf(columnPrefix + "time"));
        this.entry = entry;
        this.pattern = pattern;
        this.algorithmName = row.stringOf(columnPrefix + "algorithm");
        this.weight = row.floatOf(columnPrefix + "weight");
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
        return "RealPatternToEntry{" +
                "uuid='" + super.uuid() + '\'' +
                ", entry=" + entry +
                ", pattern=" + pattern +
                ", algorithmName='" + algorithmName + '\'' +
                ", weight=" + weight +
                '}';
    }
}
