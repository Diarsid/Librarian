package diarsid.librarian.impl.logic.impl.search.charscan;

public class Main {

    public static void main(String[] args) {
        long code = 80000000000000000L;

        long rateSum = 1234;
        long words = 2;
        long wordsLengthSum = 11;
        long missed = 1;
        long wordSpaces = 2;
        long overlaps = 3;

        code = code + rateSum           * 100000000000L;
        code = code + words             * 1000000000L;
        code = code + wordsLengthSum    * 1000000L;
        code = code + missed            * 10000L;
        code = code + wordSpaces        * 100L;
        code = code + overlaps;

        System.out.println(code);
    }
}
