package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.meta.UserScoped;

public abstract class AbstractIdentifiableUserScoped extends AbstractIdentifiable implements UserScoped {

    private final UUID userUuid;

    public AbstractIdentifiableUserScoped(UUID userUuid) {
        super();
        this.userUuid = userUuid;
    }

    public AbstractIdentifiableUserScoped(UUID uuid, LocalDateTime time, UUID userUuid) {
        super(uuid, time);
        this.userUuid = userUuid;
    }

    public AbstractIdentifiableUserScoped(UUID uuid, LocalDateTime time, UUID userUuid, State state) {
        super(uuid, time, state);
        this.userUuid = userUuid;
    }

    @Override
    public UUID userUuid() {
        return this.userUuid;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "uuid=" + super.uuid() +
                ", state=" + super.state() +
                ", time=" + super.time() +
                ", userUuid=" + userUuid +
                '}';
    }
}
