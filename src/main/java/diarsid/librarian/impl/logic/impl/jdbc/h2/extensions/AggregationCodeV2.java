package diarsid.librarian.impl.logic.impl.jdbc.h2.extensions;

import diarsid.jdbc.api.sqltable.rows.Row;

public class AggregationCodeV2 extends AggregationCode {

    /*
     * template '10 aa 0 bb 0 cc 0 dd' where:
     * aa - pattern length
     * bb - missed
     * cc - missed in word span
     * dd - overlaps
     */
    public final long rateSum;
    public final long words;
    public final long wordsLengthSum;
    public final long missed;
    public final long wordSpaces;
    public final long overlaps;

    public AggregationCodeV2(long code) {
        super(code);

        if ( code > -1 ) {
            this.overlaps = (int) (code % 100);

            code = code / 100;

            this.wordSpaces = (int) (code % 100);

            code = code / 100;

            this.missed = (int) (code % 100);

            code = code / 100;

            this.wordsLengthSum = (int) (code % 1000);

            code = code / 1000;

            this.words = (int) (code % 100);

            code = code / 100;

            this.rateSum = (int) (code % 10000);
        }
        else {
            this.rateSum = NOT_APPLICABLE;
            this.words = NOT_APPLICABLE;
            this.wordsLengthSum = NOT_APPLICABLE;
            this.missed = NOT_APPLICABLE;
            this.wordSpaces = NOT_APPLICABLE;
            this.overlaps = NOT_APPLICABLE;
        }
    }

    public AggregationCodeV2(Row row) {
        super(row.longOf("r_code"));

        long code = super.code;

        if ( super.code > -1 ) {
            this.overlaps = (int) (code % 100);

            code = code / 100;

            this.wordSpaces = (int) (code % 100);

            code = code / 100;

            this.missed = (int) (code % 100);

            code = code / 100;

            this.wordsLengthSum = (int) (code % 1000);

            code = code / 1000;

            this.words = (int) (code % 100);

            code = code / 100;

            this.rateSum = (int) (code % 10000);
        }
        else {
            this.rateSum = NOT_APPLICABLE;
            this.words = NOT_APPLICABLE;
            this.wordsLengthSum = NOT_APPLICABLE;
            this.missed = NOT_APPLICABLE;
            this.wordSpaces = NOT_APPLICABLE;
            this.overlaps = NOT_APPLICABLE;
        }
    }

    public boolean hasMissed() {
        return missed > 0;
    }
}
