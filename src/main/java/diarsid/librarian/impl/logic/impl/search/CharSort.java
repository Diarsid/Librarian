package diarsid.librarian.impl.logic.impl.search;

import java.util.Arrays;

public final class CharSort {

    private CharSort() {}

    public static String transform(String origin) {
        char[] chars = origin.toCharArray();
        Arrays.sort(chars);
        String sorted = String.copyValueOf(chars);
        return sorted;
    }
}
