package diarsid.search.impl.logic.impl.labels;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.JdbcTransaction;
import diarsid.jdbc.api.Params;
import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.api.labels.LabelsToCharsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import static diarsid.jdbc.api.Params.params;

public class LabelsToCharsInPhrasesImpl extends ThreadTransactional implements LabelsToCharsInPhrases {

    public LabelsToCharsInPhrasesImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public void join(Entry entry, Entry.Label label, LocalDateTime joiningTime) {
        JdbcTransaction transaction = super.currentTransaction();

        List<UUID> charsUuids = transaction
                .doQueryAndStream(
                        ColumnGetter.uuidOf("char_uuid"),
                        "SELECT " +
                        "   cp.uuid AS char_uuid, " +
                        "   p.uuid AS phrase_uuid " +
                        "FROM chars_in_phrases cp " +
                        "   JOIN phrases p " +
                        "       ON cp.phrase_uuid = p.uuid " +
                        "   JOIN phrases_in_entries pe " +
                        "       ON pe.phrase_uuid = p.uuid " +
                        "WHERE pe.entry_uuid = ?",
                        entry.uuid())
                .collect(toList());

        List<Params> params = charsUuids
                .stream()
                .map(charUuid -> params(
                        randomUUID(),
                        charUuid,
                        label.uuid(),
                        joiningTime))
                .collect(toList());

        int[] inserted = transaction
                .doBatchUpdate(
                        "INSERT INTO labels_to_chars_in_phrases(" +
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
