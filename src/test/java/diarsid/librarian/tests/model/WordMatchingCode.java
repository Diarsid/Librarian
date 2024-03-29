package diarsid.librarian.tests.model;

import java.util.Comparator;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.librarian.impl.logic.impl.search.charscan.matching.MatchingCodeV2;

public class WordMatchingCode extends MatchingCodeV2 {

    public static final Comparator <WordMatchingCode> RATE_COMPARATOR = (code1, code2) -> {
        return Integer.compare(code1.rate, code2.rate) * -1;
    };

    public final String string;
    public final String description;

    public WordMatchingCode(Row row, String codeColumnName, String wordColumnName) {
        super(row.longOf(codeColumnName));
        this.string = row.stringOf(wordColumnName);
        this.description = super.toString();
    }

    public WordMatchingCode(String string, long code) {
        super(code);
        this.string = string;
        this.description = super.toString();
    }

    @Override
    public String toString() {
        return "WordCode{" +
                "'" + this.string + '\'' +
                ", " + this.description +
                '}';
    }
}
