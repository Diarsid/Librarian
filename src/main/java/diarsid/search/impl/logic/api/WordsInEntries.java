package diarsid.search.impl.logic.api;

import java.util.List;

import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.WordInEntry;

public interface WordsInEntries {

    List<WordInEntry> save(RealEntry entry);

}
