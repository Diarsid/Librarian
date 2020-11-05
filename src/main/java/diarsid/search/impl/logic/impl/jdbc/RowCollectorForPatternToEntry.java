package diarsid.search.impl.logic.impl.jdbc;

import java.util.UUID;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealPattern;
import diarsid.search.impl.model.RealPatternToEntry;
import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.jdbc.api.rows.OneToManyRelationsListCollector;

public class RowCollectorForPatternToEntry
        extends OneToManyRelationsListCollector<PatternToEntry, Entry, UUID, Pattern, UUID> {

    public RowCollectorForPatternToEntry() {
        super(
                ColumnGetter.uuidOf("entries.uuid"),
                ColumnGetter.uuidOf("patterns.uuid"),
                row -> new RealEntry("entries.", row),
                row -> new RealPattern("patterns.", row),
                (entry, pattern, row) -> new RealPatternToEntry(entry, pattern, row));
    }
}
