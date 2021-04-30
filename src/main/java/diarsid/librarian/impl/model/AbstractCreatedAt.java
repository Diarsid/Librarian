package diarsid.librarian.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.support.model.CreatedAt;

import static java.time.LocalDateTime.now;

public abstract class AbstractCreatedAt extends AbstractUniqueStorable implements CreatedAt {

    private final LocalDateTime time;

    public AbstractCreatedAt(UUID uuid) {
        super(uuid);
        this.time = now();
    }

    public AbstractCreatedAt(UUID uuid, LocalDateTime time) {
        super(uuid);
        this.time = time;
    }

    public AbstractCreatedAt(UUID uuid, LocalDateTime time, State state) {
        super(uuid, state);
        this.time = time;
    }

    @Override
    public LocalDateTime createdAt() {
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
