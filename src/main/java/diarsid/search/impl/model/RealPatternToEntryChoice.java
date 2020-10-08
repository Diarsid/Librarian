package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;
import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.jdbc.api.rows.Row;

import static java.time.LocalDateTime.now;

import static diarsid.search.api.model.Storable.State.NON_STORED;

public class RealPatternToEntryChoice extends AbstractIdentifiable implements PatternToEntryChoice {

    private final PatternToEntry patternToEntry;
    private LocalDateTime actual;

    public RealPatternToEntryChoice(PatternToEntry patternToEntry) {
        super();
        this.patternToEntry = patternToEntry;
        this.actual = super.time();
    }

    private RealPatternToEntryChoice(PatternToEntryChoice oldChoice, PatternToEntry patternToEntry) {
        super(oldChoice.uuid(), oldChoice.time());
        this.patternToEntry = patternToEntry;
        this.actualize();
    }

    public RealPatternToEntryChoice(UUID uuid, LocalDateTime time, PatternToEntry patternToEntry) {
        super(uuid, time);
        this.patternToEntry = patternToEntry;
        this.actual = super.time();
    }

    public RealPatternToEntryChoice(UUID uuid, LocalDateTime time, State state, PatternToEntry patternToEntry) {
        super(uuid, time, state);
        this.patternToEntry = patternToEntry;
        this.actual = super.time();
    }

    public RealPatternToEntryChoice(Row row) {
        super(
                ColumnGetter.uuidOf("uuid").getFrom(row),
                ColumnGetter.timeOf("time").getFrom(row));
        Entry entry = new RealEntry("entries.", row);
        Pattern pattern = new RealPattern("pattern.", row);
        this.patternToEntry = new RealPatternToEntry(entry, pattern, "relations.", row);
        this.actual = ColumnGetter.timeOf("time_actual").getFrom(row);
    }

    @Override
    public PatternToEntry patternToEntry() {
        return patternToEntry;
    }

    @Override
    public PatternToEntryChoice changeTo(PatternToEntry newRelation) {
        return new RealPatternToEntryChoice(this, newRelation);
    }

    @Override
    public void actualize() {
        actual = now();
        super.setState(NON_STORED);
    }

    @Override
    public LocalDateTime actualTime() {
        return actual;
    }
}
