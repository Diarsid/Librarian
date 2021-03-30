package diarsid.search.impl.model;

import java.time.LocalDateTime;

import diarsid.search.api.model.Entry;
import diarsid.support.model.UniqueCreatedAtJoined;
import diarsid.support.objects.references.Present;

import static java.util.UUID.randomUUID;

import static diarsid.support.model.Storable.State.NON_STORED;
import static diarsid.support.objects.references.References.simplePresentOf;

public class EntryJoinLabel extends UniqueCreatedAtJoined<Entry, Entry.Label> implements Entry.Labeled {

    private final Present<State> state;

    public EntryJoinLabel(LocalDateTime createdAt, Entry left, Entry.Label right) {
        super(randomUUID(), createdAt, left, right);
        this.state = simplePresentOf(NON_STORED);
    }

    @Override
    public State state() {
        return this.state.get();
    }

    @Override
    public State setState(State newState) {
        return this.state.resetTo(newState);
    }
}
