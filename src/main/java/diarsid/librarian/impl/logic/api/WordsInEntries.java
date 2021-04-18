package diarsid.librarian.impl.logic.api;

import java.util.List;

import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.WordInEntry;

public interface WordsInEntries {

    List<WordInEntry> save(User user, RealEntry entry);

}
