package diarsid.search.impl.logic.api;

import java.util.List;

import diarsid.search.impl.model.PhraseInEntry;
import diarsid.search.impl.model.WordInEntry;

public interface PhrasesInEntries {

    List<PhraseInEntry> save(List<WordInEntry> entryWords);
}
