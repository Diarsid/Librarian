package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.meta.Identifiable;

import static java.time.LocalDateTime.now;

public abstract class AbstractIdentifiable extends AbstractUnique implements Identifiable {

    private final LocalDateTime time;

    public AbstractIdentifiable() {
        super();
        this.time = now();
    }

    public AbstractIdentifiable(UUID uuid, LocalDateTime time) {
        super(uuid);
        this.time = time;
    }

    public AbstractIdentifiable(UUID uuid, LocalDateTime time, State state) {
        super(uuid, state);
        this.time = time;
    }

    @Override
    public LocalDateTime time() {
        return this.time;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "uuid=" + super.uuid() +
                ", state=" + super.state() +
                ", time=" + time +
                '}';
    }
}
