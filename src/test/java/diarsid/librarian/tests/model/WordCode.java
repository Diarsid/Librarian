package diarsid.librarian.tests.model;

import diarsid.jdbc.api.sqltable.rows.Row;

public class WordCode {

    public final String string;
    public final long code;

    public WordCode(Row row) {
        this.string = row.stringOf("string");
        this.code = row.longOf("w_code");
    }

    public WordCode(String string, long code) {
        this.string = string;
        this.code = code;
    }

    @Override
    public String toString() {
        return "WordCode{" +
                "'" + string + '\'' +
                ", " + code +
                '}';
    }
}
