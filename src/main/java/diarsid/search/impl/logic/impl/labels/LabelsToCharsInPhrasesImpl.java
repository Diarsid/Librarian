package diarsid.search.impl.logic.impl.labels;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.support.time.Timer;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

public class LabelsToCharsInPhrasesImpl extends ThreadBoundTransactional implements LabelsToCharsInPhrases {

    public LabelsToCharsInPhrasesImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public void join(Entry entry, Entry.Label label, LocalDateTime joiningTime) {
        ThreadBoundJdbcTransaction transaction = super.currentTransaction();

        Timer timer = new Timer();

        timer.start("select");
        List<UUID> charsUuids = transaction
                .doQueryAndStream(
                        ColumnGetter.uuidOf("char_uuid"),
                        "SELECT " +
                        "   cp.uuid AS char_uuid, " +
                        "FROM chars_in_phrases cp " +
                        "   JOIN phrases_in_entries pe " +
                        "       ON pe.phrase_uuid = cp.phrase_uuid " +
                        "WHERE " +
                        "   pe.entry_uuid = ? AND " +
                        "   NOT EXISTS " +
                        "       ( " +
                        "       SELECT * " +
                        "       FROM labels_to_chars_in_phrases lcp " +
                        "       WHERE " +
                        "           lcp.chars_uuid = cp.uuid AND " +
                        "           lcp.label_uuid = ? " +
                        "       ) ",
                        entry.uuid(), label.uuid())
                .collect(toList());
        timer.stop();

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

        timer.start("insert");
        int[] inserted = transaction
                .doBatchUpdate(
                        "INSERT INTO labels_to_chars_in_phrases(" +
                        "   uuid, " +
                        "   chars_uuid, " +
                        "   label_uuid, " +
                        "   time) " +
                        "VALUES(?, ?, ?, ?)",
                        params);
        timer.stop();

        timer.timings().forEach(timing -> {
            System.out.println("    " + timing.name() + " : " + timing.millis());
        });
        System.out.println("    inserted : " + inserted.length);

        if ( inserted.length != params.size() ) {
            throw new IllegalStateException();
        }
    }
}
