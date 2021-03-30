package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.impl.logic.impl.support.TransactionalScoped;
import diarsid.support.model.Updatable;

public abstract class AbstractIdentifiableTransactionalUserScopedMutable
        extends AbstractIdentifiableUserScoped
        implements Updatable, TransactionalScoped {

    private final LocalDateTime actualAt;
    private final UUID transactionUuid;

    public AbstractIdentifiableTransactionalUserScopedMutable(UUID userUuid, UUID transactionUuid) {
        super(userUuid);
        this.actualAt = super.createdAt();
        this.transactionUuid = transactionUuid;
    }

    public AbstractIdentifiableTransactionalUserScopedMutable(
            UUID uuid, LocalDateTime time, UUID userUuid, LocalDateTime actualAt, UUID transactionUuid) {
        super(uuid, time, userUuid);
        this.actualAt = actualAt;
        this.transactionUuid = transactionUuid;
    }

    public AbstractIdentifiableTransactionalUserScopedMutable(
            UUID uuid, LocalDateTime time, UUID userUuid, State state, LocalDateTime actualAt, UUID transactionUuid) {
        super(uuid, time, userUuid, state);
        this.actualAt = actualAt;
        this.transactionUuid = transactionUuid;
    }

    @Override
    public final LocalDateTime actualAt() {
        return actualAt;
    }

    @Override
    public final UUID transactionUuid() {
        return transactionUuid;
    }
}
