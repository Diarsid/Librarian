package diarsid.librarian.impl.logic.api;

import java.util.List;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;

public interface PatternsToEntries {

    List<PatternToEntry> findBy(Pattern pattern);

    List<PatternToEntry> findBy(Pattern pattern, List<Entry> entries);

    List<PatternToEntry> findBy(Entry entry);

    int removeAllBy(Entry entry);

    int remove(List<PatternToEntry> relations);

    int removeBy(Entry entry, List<Pattern> patterns);

    void save(List<PatternToEntry> relations);

    void analyzeAgainAllRelationsOf(Entry entry);
}
