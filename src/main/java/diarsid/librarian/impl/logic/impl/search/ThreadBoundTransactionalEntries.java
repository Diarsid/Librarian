package diarsid.librarian.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

abstract class ThreadBoundTransactionalEntries extends ThreadBoundTransactional {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByUuids;

    public ThreadBoundTransactionalEntries(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);

        this.sqlSelectEntriesByUuids = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT DISTINCT * \n" +
                "FROM entries e \n" +
                "WHERE e.uuid IN ( \n",
                "    ?", ", \n", ")");
    }

    protected List<Entry> getEntriesBy(List<UUID> entryUuids) {
        if ( entryUuids.isEmpty() ) {
            return emptyList();
        }

        LocalDateTime actualAt = now();
        List<Entry> entries = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealEntry(row, actualAt),
                        this.sqlSelectEntriesByUuids.getFor(entryUuids),
                        entryUuids)
                .collect(toList());

        return entries;
    }

}
