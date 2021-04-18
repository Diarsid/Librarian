package diarsid.librarian.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.support.model.Updatable;

public abstract class AbstractUpdatableUserScoped extends AbstractIdentifiableUserScoped implements Updatable {

    private final LocalDateTime actualAt;

    public AbstractUpdatableUserScoped(UUID userUuid) {
        super(userUuid);
        this.actualAt = super.createdAt();
    }

    public AbstractUpdatableUserScoped(UUID uuid, LocalDateTime time, LocalDateTime actualAt, UUID userUuid) {
        super(uuid, time, userUuid);
        this.actualAt = actualAt;
    }

    public AbstractUpdatableUserScoped(UUID uuid, LocalDateTime time, LocalDateTime actualAt, UUID userUuid, State state) {
        super(uuid, time, userUuid, state);
        this.actualAt = actualAt;
    }

    @Override
    public LocalDateTime actualAt() {
        return actualAt;
    }
}
