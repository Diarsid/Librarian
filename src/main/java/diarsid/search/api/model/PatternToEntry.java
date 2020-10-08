package diarsid.search.api.model;

public interface PatternToEntry extends Identifiable  {

    Entry entry();

    Pattern pattern();

    String algorithmCanonicalName();

    double weight();

    default String entryString() {
        return this.entry().string();
    }

    default String patternString() {
        return this.pattern().string();
    }
}
