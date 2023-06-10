package diarsid.librarian.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.support.model.UpdatedAt;

public abstract class AbstractUpdatableUserScoped extends AbstractIdentifiableUserScoped implements UpdatedAt {

    private final LocalDateTime actualAt;

    public AbstractUpdatableUserScoped(UUID uuid, UUID userUuid) {
        super(uuid, userUuid);
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
