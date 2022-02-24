package diarsid.librarian.impl.logic.impl.search.charscan;

public abstract class MatchingCode {

    public static enum Version {
        V1, V2
    }

    public final long code;

    public MatchingCode(long code) {
        this.code = code;
    }

    public abstract Version version();

}
