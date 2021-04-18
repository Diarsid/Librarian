package diarsid.librarian.impl.model;

import java.util.Objects;
import java.util.UUID;

import diarsid.support.model.Storable;
import diarsid.support.model.Unique;
import diarsid.support.objects.references.Present;
import diarsid.support.objects.references.References;

import static java.util.UUID.randomUUID;

import static diarsid.support.model.Storable.State.NON_STORED;


public abstract class AbstractUniqueStorable implements Unique, Storable {

    private final UUID uuid;
    private final Present<State> state;

    public AbstractUniqueStorable() {
        this.uuid = randomUUID();
        this.state = References.simplePresentOf(NON_STORED);
    }

    public AbstractUniqueStorable(UUID uuid) {
        this.uuid = uuid;
        this.state = References.simplePresentOf(NON_STORED);
    }

    public AbstractUniqueStorable(UUID uuid, Storable.State state) {
        this.uuid = uuid;
        this.state = References.simplePresentOf(state);
    }

    @Override
    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public Storable.State state() {
        return this.state.get();
    }

    @Override
    public Storable.State setState(Storable.State newState) {
        return this.state.resetTo(newState);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractUniqueStorable)) return false;
        AbstractUniqueStorable that = (AbstractUniqueStorable) o;
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
