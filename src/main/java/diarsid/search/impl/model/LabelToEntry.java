package diarsid.search.impl.model;

import java.time.LocalDateTime;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;
import diarsid.support.model.UniqueCreatedAtJoined;
import diarsid.support.objects.references.Present;

import static diarsid.support.model.Storable.State.NON_STORED;
import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.objects.references.References.simplePresentOf;

public class LabelToEntry extends UniqueCreatedAtJoined<Entry, Entry.Label> implements Entry.Labeled {

    private final Present<State> state;

    public LabelToEntry(Entry left, Entry.Label right) {
        super(left, right);
        this.state = simplePresentOf(NON_STORED);
    }

    public LabelToEntry(
            LocalDateTime actualAt,
            Row row,
            String joinColumnPrefix,
            String entryColumnPrefix,
            String labelColumnPrefix) {
        super(
                row.uuidOf(joinColumnPrefix + "uuid"),
                row.timeOf(joinColumnPrefix + "time"),
                new RealEntry(entryColumnPrefix, row, actualAt),
                new RealLabel(labelColumnPrefix, row));
        this.state = simplePresentOf(STORED);
    }

    @Override
    public State state() {
        return this.state.get();
    }

    @Override
    public State setState(State newState) {
        return this.state.resetTo(newState);
    }

    @Override
    public String toString() {
        return "LabelToEntry{" +
                "'" + super.uuid() + '\'' +
                ", '" + super.createdAt() + '\'' +
                ", entry='" + entry().string() + '\'' +
                ", label='" + label().name() + '\'' +
                '}';
    }
}
