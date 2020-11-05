package diarsid.search.impl.model;

import diarsid.support.objects.CommonEnum;

public class WordInEntry extends AbstractUnique {

    public enum Position implements CommonEnum<Position> {
        FIRST, MIDDLE, LAST, SINGLE
    }

    private final RealEntry entry;
    private final Word word;
    private final Position position;
    private final int index;

    public WordInEntry(RealEntry entry, Word word, Position position, int index) {
        super();
        this.entry = entry;
        this.word = word;
        this.position = position;
        this.index = index;
    }

    public RealEntry entry() {
        return entry;
    }

    public Word word() {
        return word;
    }

    public Position position() {
        return position;
    }

    public int index() {
        return index;
    }
}
