package diarsid.librarian.impl.logic.impl.search.v2;

import diarsid.jdbc.api.sqltable.rows.Row;

class WordCode {

    final String string;
    final long code;

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
