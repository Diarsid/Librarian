package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.jdbc.api.rows.Row;

public class RealPatternToEntry extends AbstractIdentifiable implements PatternToEntry {

    private final Entry entry;
    private final Pattern pattern;
    private final String algorithmName;
    private final double weight;

    public RealPatternToEntry(Entry entry, Pattern pattern, String columnPrefix, Row row) {
        super(
                ColumnGetter.uuidOf(columnPrefix + "uuid").getFrom(row),
                ColumnGetter.timeOf(columnPrefix + "time").getFrom(row));
        this.entry = entry;
        this.pattern = pattern;
        this.algorithmName = ColumnGetter.stringOf(columnPrefix + "algorithm").getFrom(row);
        this.weight = ColumnGetter.doubleOf(columnPrefix + "weight").getFrom(row);
    }

    public RealPatternToEntry(
            Entry entry, Pattern pattern, Row row) {
        super(
                ColumnGetter.uuidOf("uuid").getFrom(row),
                ColumnGetter.timeOf("time").getFrom(row));
        this.entry = entry;
        this.pattern = pattern;
        this.algorithmName = ColumnGetter.stringOf("algorithm").getFrom(row);
        this.weight = ColumnGetter.doubleOf("weight").getFrom(row);
    }

    public RealPatternToEntry(
            UUID uuid,
            LocalDateTime time,
            RealEntry entry,
            RealPattern pattern,
            StringsComparisonAlgorithm algorithm,
            double weight) {
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
    public double weight() {
        return weight;
    }
}
