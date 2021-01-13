package diarsid.search.impl.model;

import java.util.Objects;
import java.util.UUID;

import diarsid.search.api.model.meta.Unique;
import diarsid.support.objects.references.Present;
import diarsid.support.objects.references.References;

import static java.util.UUID.randomUUID;

import static diarsid.search.api.model.meta.Storable.State.NON_STORED;

public abstract class AbstractUnique implements Unique {

    private final UUID uuid;
    private final Present<State> state;

    public AbstractUnique() {
        this.uuid = randomUUID();
        this.state = References.simplePresentOf(NON_STORED);
    }

    public AbstractUnique(UUID uuid) {
        this.uuid = uuid;
        this.state = References.simplePresentOf(NON_STORED);
    }

    public AbstractUnique(UUID uuid, State state) {
        this.uuid = uuid;
        this.state = References.simplePresentOf(state);
    }

    @Override
    public UUID uuid() {
        return this.uuid;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractUnique)) return false;
        AbstractUnique that = (AbstractUnique) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "uuid=" + uuid +
                ", state=" + state +
                '}';
    }
}
