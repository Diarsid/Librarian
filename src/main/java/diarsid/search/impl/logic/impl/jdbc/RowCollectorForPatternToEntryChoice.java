package diarsid.search.impl.logic.impl.jdbc;

import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.sqltable.rows.Row;
import diarsid.jdbc.api.sqltable.rows.RowOperation;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;
import diarsid.search.api.model.meta.Unique;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.RealLabel;
import diarsid.search.impl.model.RealPattern;
import diarsid.search.impl.model.RealPatternToEntry;
import diarsid.search.impl.model.RealPatternToEntryChoice;
import diarsid.support.objects.PooledReusable;

import static java.lang.String.format;
import static java.util.Objects.nonNull;

public class RowCollectorForPatternToEntryChoice extends PooledReusable implements RowOperation {

    private final String patternPrefix;
    private final String entryPrefix;
    private final String labelPrefix;
    private final String patternToEntryPrefix;
    private final String choicePrefix;

    private final String patternUuidColumn;
    private final String entryUuidColumn;
    private final String labelUuidColumn;
    private final String patternToEntryUuidColumn;
    private final String choiceUuidColumn;

    private Pattern pattern;
    private Entry entry;
    private PatternToEntry patternToEntry;
    private PatternToEntryChoice choice;

    private boolean firstRowProcessed;

    public RowCollectorForPatternToEntryChoice(
            String patternPrefix,
            String entryPrefix,
            String labelPrefix,
            String patternToEntryPrefix,
            String choicePrefix) {
        this.patternPrefix = patternPrefix;
        this.entryPrefix = entryPrefix;
        this.labelPrefix = labelPrefix;
        this.patternToEntryPrefix = patternToEntryPrefix;
        this.choicePrefix = choicePrefix;

        this.patternUuidColumn = patternPrefix + "uuid";
        this.entryUuidColumn = entryPrefix + "uuid";
        this.labelUuidColumn = labelPrefix + "uuid";
        this.patternToEntryUuidColumn = patternToEntryPrefix + "uuid";
        this.choiceUuidColumn = choicePrefix + "uuid";
    }

    @Override
    public void process(Row row) {
        if ( ! firstRowProcessed ) {
            entry = new RealEntry(entryPrefix, row);
            pattern = new RealPattern(patternPrefix, row);
            patternToEntry = new RealPatternToEntry(entry, pattern, patternToEntryPrefix, row);
            choice = new RealPatternToEntryChoice(patternToEntry, choicePrefix, row);

            UUID labelUuid = row.uuidOf(labelUuidColumn);
            if ( nonNull(labelUuid) ) {
                Entry.Label label = new RealLabel(labelPrefix, row);
                entry.labels().add(label);
            }

            firstRowProcessed = true;
        }
        else {
            UUID choiceUuid = row.uuidOf(choiceUuidColumn);
            UUID entryUuid = row.uuidOf(entryUuidColumn);
            UUID patternUuid = row.uuidOf(patternUuidColumn);
            UUID patternToEntryUuid = row.uuidOf(patternToEntryUuidColumn);

            Entry.Label label = new RealLabel(labelPrefix, row);
            entry.labels().add(label);

            mustHaveUuid(entry, entryUuid);
            mustHaveUuid(choice, choiceUuid);
            mustHaveUuid(pattern, patternUuid);
            mustHaveUuid(patternToEntry, patternToEntryUuid);
        }

    }

    public Optional<PatternToEntryChoice> patternToEntryChoice() {
        return Optional.ofNullable(choice);
    }

    @Override
    protected void clearForReuse() {
        firstRowProcessed = false;
        entry = null;
        choice = null;
        pattern = null;
        patternToEntry = null;
    }

    private static void mustHaveUuid(Unique unique, UUID uuid) {
        if ( ! unique.uuid().equals(uuid) ) {
            throw new IllegalStateException(format("%s has uuid %s but expected %s",
                    unique.getClass().getSimpleName(),
                    unique.uuid(),
                    uuid));
        }
    }
}
