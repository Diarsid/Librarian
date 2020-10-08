package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.User;
import diarsid.jdbc.api.rows.Row;

import static diarsid.search.api.model.Storable.State.STORED;

public class RealUser extends AbstractIdentifiable implements User {

    private final String name;

    public RealUser(String name) {
        super();
        this.name = name;
    }

    public RealUser(UUID uuid, String name, LocalDateTime time) {
        super(uuid, time);
        this.name = name;
    }

    public RealUser(UUID uuid, String name, LocalDateTime time, State state) {
        super(uuid, time, state);
        this.name = name;
    }

    public RealUser(Row row) {
        super(
                row.get("uuid", UUID.class),
                row.get("time", LocalDateTime.class),
                STORED);
        this.name = row.get("name", String.class);
    }

    @Override
    public String name() {
        return this.name;
    }
}
