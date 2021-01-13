package diarsid.search.impl.logic.impl.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.jdbc.api.sqltable.rows.RowOperation;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealLabel;
import diarsid.search.impl.model.RealPattern;
import diarsid.search.impl.model.RealPatternToEntry;
import diarsid.support.objects.PooledReusable;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class RowCollectorForPatternToEntryAndLabels extends PooledReusable implements RowOperation {

    private final Map<UUID, Pattern> patternsByUuids;
    private final Map<UUID, Entry> entriesByUuids;
    private final Map<UUID, Entry.Label> labelsByUuids;

    private final List<PatternToEntry> patternToEntries;

    private final String patternUuidColumn;
    private final String entryUuidColumn;
    private final String labelUuidColumn;

    private final String patternPrefix;
    private final String entryPrefix;
    private final String labelPrefix;
    private final String patternToEntryPrefix;

    public RowCollectorForPatternToEntryAndLabels(
            String patternPrefix,
            String entryPrefix,
            String labelPrefix,
            String patternToEntryPrefix) {
        this.patternPrefix = patternPrefix;
        this.entryPrefix = entryPrefix;
        this.labelPrefix = labelPrefix;
        this.patternToEntryPrefix = patternToEntryPrefix;

        this.patternUuidColumn = patternPrefix + "uuid";
        this.entryUuidColumn = entryPrefix + "uuid";
        this.labelUuidColumn = labelPrefix + "uuid";

        this.patternToEntries = new ArrayList<>();
        this.patternsByUuids = new HashMap<>();
        this.entriesByUuids = new HashMap<>();
        this.labelsByUuids = new HashMap<>();
    }

    @Override
    protected void clearForReuse() {
        this.patternToEntries.clear();
        this.patternsByUuids.clear();
        this.entriesByUuids.clear();
        this.labelsByUuids.clear();
    }

    @Override
    public void process(Row row) {
        UUID patternUuid = row.uuidOf(patternUuidColumn);

        Pattern pattern = patternsByUuids.get(patternUuid);

        if ( isNull(pattern) ) {
            pattern = new RealPattern(patternPrefix, row);
            patternsByUuids.put(patternUuid, pattern);
        }

        UUID entryUuid = row.uuidOf(entryUuidColumn);

        if ( nonNull(entryUuid) ) {
            Entry entry = entriesByUuids.get(entryUuid);

            if ( isNull(entry) ) {
                entry = new RealEntry(entryPrefix, row);
                entriesByUuids.put(entryUuid, entry);
            }

            PatternToEntry patternToEntry = new RealPatternToEntry(entry, pattern, patternToEntryPrefix, row);
            patternToEntries.add(patternToEntry);

            UUID labelUuid = row.uuidOf(labelUuidColumn);

            if ( nonNull(labelUuid) ) {
                Entry.Label label = labelsByUuids.get(labelUuid);

                if ( isNull(label) ) {
                    label = new RealLabel(labelPrefix, row);
                    labelsByUuids.put(labelUuid, label);
                }

                entry.labels().add(label);
            }
        }
        else {
            throw new IllegalStateException();
        }
    }

    public List<PatternToEntry> relations() {
        return new ArrayList<>(patternToEntries);
    }
}
