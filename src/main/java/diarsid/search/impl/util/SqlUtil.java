package diarsid.search.impl.util;

public class SqlUtil {

    public static String placeholdersFor(int qty) {
        if ( qty <= 0 ) {
            throw new IllegalArgumentException();
        }
        else if ( qty == 1 ) {
            return " ? ";
        }
        else {
            return " ?, ".repeat(qty - 1) + "? ";
        }
    }
}
