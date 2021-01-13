package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.User;

import static diarsid.search.api.model.meta.Storable.State.STORED;

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

    @Override
    public String toString() {
        return "RealUser{" +
                "uuid='" + super.uuid() + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
