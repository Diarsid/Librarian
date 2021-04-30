package diarsid.librarian.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.api.model.User;

import static diarsid.support.model.Storable.State.STORED;


public class RealUser extends AbstractCreatedAt implements User {

    private final String name;

    public RealUser(UUID uuid, String name) {
        super(uuid);
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
