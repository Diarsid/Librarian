package diarsid.librarian.impl.logic.impl.search;

import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.AggregationCodeV2;

public final class UuidAndAggregationCode extends AggregationCodeV2 {

    public final UUID uuid;

    public UuidAndAggregationCode(UUID uuid, long code) {
        super(code);
        this.uuid = uuid;
    }

    public UuidAndAggregationCode(Row row) {
        super(row.longOf("r_code"));
        this.uuid = row.uuidOf("uuid");
    }

}
