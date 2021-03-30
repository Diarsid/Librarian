package diarsid.search.impl.logic.impl.search.v2;

import java.util.Arrays;

public final class CharSort {

    private CharSort() {}

    public static String transform(String origin) {
        char[] chars = origin.toCharArray();
        Arrays.sort(chars);
        String sorted = String.copyValueOf(chars);
        return sorted;
    }

    public static void main(String[] args) {
        System.out.println(transform("tolos"));
    }
}
