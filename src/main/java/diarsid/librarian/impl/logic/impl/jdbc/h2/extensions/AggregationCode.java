package diarsid.librarian.impl.logic.impl.jdbc.h2.extensions;

public abstract class AggregationCode {

    public static final int NOT_APPLICABLE = -9;

    public final long code;

    public AggregationCode(long code) {
        this.code = code;
    }
}
