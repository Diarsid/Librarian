package diarsid.search.impl.logic.impl.jdbc;

import java.util.UUID;

import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.jdbc.api.rows.Row;
import diarsid.support.objects.Pair;

public class TwoUuids extends Pair<UUID, UUID> {

    public TwoUuids(UUID uuid1, UUID uuid2) {
        super(uuid1, uuid2);
    }

    public TwoUuids(Row row, String firstName, String secondName) {
        super(
                ColumnGetter.uuidOf(firstName).getFrom(row),
                ColumnGetter.uuidOf(secondName).getFrom(row));
    }
}
