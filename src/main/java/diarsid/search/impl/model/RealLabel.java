package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.search.api.model.Entry;
import diarsid.jdbc.api.rows.Row;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class RealLabel extends AbstractIdentifiableUserScoped implements Entry.Label {

    private final String name;

    public RealLabel(UUID uuid, LocalDateTime time, UUID userUuid, String name) {
        super(uuid, time, userUuid);
        this.name = name;
    }

    public RealLabel(Row row) {
        super(
                row.get("uuid", UUID.class),
                row.get("time", LocalDateTime.class),
                row.get("user_uuid", UUID.class),
                STORED);
        this.name = row.get("name", String.class);
    }

    public RealLabel(String columnPrefix, Row row) {
        super(
                row.get(columnPrefix + "uuid", UUID.class),
                row.get(columnPrefix + "time", LocalDateTime.class),
                row.get(columnPrefix + "user_uuid", UUID.class),
                STORED);
        this.name = row.get(columnPrefix + "name", String.class);
    }

    @Override
    public String name() {
        return name;
    }
}
