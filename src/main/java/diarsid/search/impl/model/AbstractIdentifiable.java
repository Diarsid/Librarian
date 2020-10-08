package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import diarsid.search.api.model.Identifiable;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

import static diarsid.search.api.model.Storable.State.NON_STORED;

public abstract class AbstractIdentifiable implements Identifiable {

    private final UUID uuid;
    private final LocalDateTime time;
    private final AtomicReference<State> state;

    public AbstractIdentifiable() {
        this.uuid = randomUUID();
        this.time = now();
        this.state = new AtomicReference<>(NON_STORED);
    }

    public AbstractIdentifiable(UUID uuid, LocalDateTime time) {
        this.uuid = uuid;
        this.time = time;
        this.state = new AtomicReference<>(NON_STORED);
    }

    public AbstractIdentifiable(UUID uuid, LocalDateTime time, State state) {
        this.uuid = uuid;
        this.time = time;
        this.state = new AtomicReference<>(state);
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public LocalDateTime time() {
        return this.time;
    }

    @Override
    public State state() {
        return this.state.get();
    }

    @Override
    public State setState(State newState) {
        return this.state.getAndSet(newState);
    }
}
