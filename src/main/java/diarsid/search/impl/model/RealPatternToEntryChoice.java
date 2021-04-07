package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;

import static java.time.LocalDateTime.now;

import static diarsid.support.model.Storable.State.NON_STORED;

public class RealPatternToEntryChoice extends AbstractCreatedAt implements PatternToEntryChoice {

    private final PatternToEntry patternToEntry;
    private LocalDateTime actual;

    public RealPatternToEntryChoice(PatternToEntry patternToEntry) {
        super();
        this.patternToEntry = patternToEntry;
        this.actual = super.createdAt();
    }

    private RealPatternToEntryChoice(PatternToEntryChoice oldChoice, PatternToEntry patternToEntry) {
        super(oldChoice.uuid(), oldChoice.createdAt());
        this.patternToEntry = patternToEntry;
        this.actualize();
    }

    public RealPatternToEntryChoice(UUID uuid, LocalDateTime time, PatternToEntry patternToEntry) {
        super(uuid, time);
        this.patternToEntry = patternToEntry;
        this.actual = super.createdAt();
    }

    public RealPatternToEntryChoice(UUID uuid, LocalDateTime time, State state, PatternToEntry patternToEntry) {
        super(uuid, time, state);
        this.patternToEntry = patternToEntry;
        this.actual = super.createdAt();
    }

    public RealPatternToEntryChoice(PatternToEntry patternToEntry, Row row) {
        super(
                row.uuidOf("uuid"),
                row.timeOf("time"));
        this.patternToEntry = patternToEntry;
        this.actual = row.timeOf("time_actual");
    }

    public RealPatternToEntryChoice(PatternToEntry patternToEntry, String prefix, Row row) {
        super(
                row.uuidOf(prefix + "uuid"),
                row.timeOf(prefix + "time"));
        this.patternToEntry = patternToEntry;
        this.actual = row.timeOf(prefix + "time_actual");
    }

    public RealPatternToEntryChoice(
            Row row,
            String joinColumnPrefix,
            String patternColumnPrefix,
            String entryColumnPrefix,
            String patternToEntryColumnPrefix,
            LocalDateTime entryActualAt) {
        super(
                row.uuidOf(joinColumnPrefix + "uuid"),
                row.timeOf(joinColumnPrefix + "time"));
        this.patternToEntry = new RealPatternToEntry(
                row, patternToEntryColumnPrefix,
                entryColumnPrefix,
                patternColumnPrefix,
                entryActualAt);
        this.actual = row.timeOf(joinColumnPrefix + "time_actual");
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

    @Override
    public String toString() {
        return "PatternToEntryChoice{" +
                "'" + super.uuid() + '\'' +
                ", patternToEntry=" + patternToEntry +
                ", actual=" + actual +
                '}';
    }
}
