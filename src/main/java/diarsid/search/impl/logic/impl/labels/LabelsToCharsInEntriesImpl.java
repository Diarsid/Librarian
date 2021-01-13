package diarsid.search.impl.logic.impl.labels;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInEntries;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

public class LabelsToCharsInEntriesImpl extends ThreadBoundTransactional implements LabelsToCharsInEntries {

    public LabelsToCharsInEntriesImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public void join(Entry entry, Entry.Label label, LocalDateTime joiningTime) {
        ThreadBoundJdbcTransaction transaction = super.currentTransaction();

        List<UUID> charsUuids = transaction
                .doQueryAndStream(
                        ColumnGetter.uuidOf("char_uuid"),
                        "SELECT " +
                        "   ce.uuid AS char_uuid, " +
                        "FROM chars_in_entries ce " +
                        "WHERE " +
                        "   ce.entry_uuid = ? AND " +
                        "   NOT EXISTS " +
                        "       (" +
                        "       SELECT * " +
                        "       FROM labels_to_chars_in_entries lce " +
                        "       WHERE " +
                        "           lce.chars_uuid = ce.uuid AND " +
                        "           lce.label_uuid = ? " +
                        "       )",
                        entry.uuid(), label.uuid())
                .collect(toList());

        if ( charsUuids.isEmpty() ) {
            return;
        }

        List<List> params = charsUuids
                .stream()
                .map(charUuid -> asList(
                        randomUUID(),
                        charUuid,
                        label.uuid(),
                        joiningTime))
                .collect(toList());

        int[] inserted = transaction
                .doBatchUpdate(
                        "INSERT INTO labels_to_chars_in_entries(" +
                        "   uuid, " +
                        "   chars_uuid, " +
                        "   label_uuid, " +
                        "   time) " +
                        "VALUES(?, ?, ?, ?)",
                        params);

        if ( inserted.length != params.size() ) {
            throw new IllegalStateException();
        }
    }
}
