package diarsid.librarian.impl.logic.impl.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;

import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.H2AggregateFunctionForAnalyzeV19.BASE_RESULT_TEMPLATE;

public final class UuidAndResultCode {

    public static final int NOT_APPLICABLE = -9;

    public final UUID uuid;

    /*
     * template '9 xx 0 yy 0 zz' where:
     * xx - pattern length
     * yy - missed
     * zz - overlaps
     */
    public final long result;
    public final int patternLength;
    public final int missed;
    public final int overlaps;

    public UuidAndResultCode(UUID uuid, long result) {
        this.uuid = uuid;
        this.result = result;

        if ( result > -1 ) {
            long resultMod = result % BASE_RESULT_TEMPLATE;
            patternLength = (int) resultMod / 1_000_000;
            resultMod = resultMod % 1_000_000;
            missed = (int) resultMod / 1_000;
            resultMod = resultMod % 1_000;
            overlaps = (int) resultMod;
        }
        else {
            patternLength = NOT_APPLICABLE;
            missed = NOT_APPLICABLE;
            overlaps = NOT_APPLICABLE;
        }
    }

    public UuidAndResultCode(Row row) {
        this.uuid = row.uuidOf("uuid");
        this.result = row.longOf("r_code");

        if ( result > -1 ) {
            long resultMod = result % BASE_RESULT_TEMPLATE;
            patternLength = (int) resultMod / 1_000_000;
            resultMod = resultMod % 1_000_000;
            missed = (int) resultMod / 1_000;
            resultMod = resultMod % 1_000;
            overlaps = (int) resultMod;
        }
        else {
            patternLength = NOT_APPLICABLE;
            missed = NOT_APPLICABLE;
            overlaps = NOT_APPLICABLE;
        }
    }

    public boolean hasMissed() {
        return missed > 0;
    }

}
