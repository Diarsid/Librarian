package diarsid.librarian.tests.model;

import java.util.Comparator;

import diarsid.jdbc.api.sqltable.rows.Row;

public class WordCode {

    public static final Comparator <WordCode> RATE_COMPARATOR = (code1, code2) -> {
        return Integer.compare(code1.rate, code2.rate) * -1;
    };

    public final String string;
    public final long code;
    public final int matchLength;
    public final int matchIndex;
    public final int wordLength;
    public final int rate;
    public final int patternLength;
    public final String description;

    public WordCode(Row row) {
        this.string = row.stringOf("string");
        this.code = row.longOf("w_code");

        long codeMutable = code;

        this.matchLength = (int) (codeMutable % 100);

        codeMutable = codeMutable / 100;

        this.matchIndex = (int) (codeMutable % 100);

        codeMutable = codeMutable / 100;

        this.wordLength = (int) (codeMutable % 100);

        codeMutable = codeMutable / 100;

        this.rate = (int) (codeMutable % 1000);

        codeMutable = codeMutable / 1000;

        this.patternLength = (int) (codeMutable % 100);

        this.description =
                "patternL:" + patternLength +
                ", wordL:" + wordLength +
                ", matchL:" + matchLength +
                ", matchIx:" + matchIndex +
                ", rate:" + rate;
    }

    public WordCode(String string, long code) {
        this.string = string;
        this.code = code;

        long codeMutable = code;

        this.matchLength = (int) (codeMutable % 100);

        codeMutable = codeMutable / 100;

        this.matchIndex = (int) (codeMutable % 100);

        codeMutable = codeMutable / 100;

        this.wordLength = (int) (codeMutable % 100);

        codeMutable = codeMutable / 100;

        this.rate = (int) (codeMutable % 1000);

        codeMutable = codeMutable / 1000;

        this.patternLength = (int) (codeMutable % 100);

        this.description =
                "patternL:" + patternLength +
                ", wordL:" + wordLength +
                ", matchL:" + matchLength +
                ", matchIx:" + matchIndex +
                ", rate:" + rate;
    }

    @Override
    public String toString() {
        return "WordCode{" +
                "'" + string + '\'' +
                ", " + code +
                '}';
    }
}
