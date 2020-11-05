package diarsid.search.impl.model;

public class PhraseInEntry extends AbstractUnique {

    private final RealEntry entry;
    private final Phrase phrase;

    public PhraseInEntry(RealEntry entry, Phrase phrase) {
        super();
        this.entry = entry;
        this.phrase = phrase;
    }

    public RealEntry entry() {
        return entry;
    }

    public Phrase phrase() {
        return phrase;
    }
}
