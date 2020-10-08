package diarsid.search.impl.api.internal;

import java.util.List;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;

public interface PatternsToEntries {

    List<PatternToEntry> findBy(Pattern pattern);

    List<PatternToEntry> findBy(Pattern pattern, List<Entry> entries);

    List<PatternToEntry> findBy(Entry entry);

    int removeAllBy(Entry entry);

    int remove(List<PatternToEntry> relations);

    void save(List<PatternToEntry> relations);

    void analyzeAgainAllRelationsOf(Entry entry);
}
