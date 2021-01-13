package diarsid.search.impl.model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiPredicate;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.search.api.model.Entry;

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

    @Override
    public Entry.Label.ConditionBindable bindableIf(BiPredicate<Entry, Entry.Label> condition) {
        LabelConditionBindable bindableLabel = new LabelConditionBindable(this, condition);
        bindableLabel.setState(this.state());
        return bindableLabel;
    }

    @Override
    public String toString() {
        return "RealLabel{" +
                "uuid='" + super.uuid() + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
