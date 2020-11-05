package diarsid.search.impl.logic.impl.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.Params;
import diarsid.search.impl.logic.api.chars.CharsInEntries;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.RealEntry;
import diarsid.support.strings.CharactersCount;

import static java.util.UUID.randomUUID;

import static diarsid.jdbc.api.Params.params;

public class CharsInEntriesImpl extends ThreadTransactional implements CharsInEntries {

    public CharsInEntriesImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public List<UUID> save(RealEntry entry) {
        List<UUID> charsRecordsUuids = new ArrayList<>();

        CharactersCount charactersCount = new CharactersCount();
        charactersCount.calculateIn(entry.stringLower());

        UUID entryUuid = entry.uuid();
        List<Params> params = new ArrayList<>();
        charactersCount.forEach((c, qty) -> {
            UUID charsRecordUuid = randomUUID();
            charsRecordsUuids.add(charsRecordUuid);
            params.add(params(
                    charsRecordUuid,
                    c,
                    qty,
                    entry.stringLower().length(),
                    entryUuid,
                    entry.userUuid()));
        });

        int[] insertedChars =  super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO chars_in_entries (" +
                        "   uuid, " +
                        "   ch, " +
                        "   qty, " +
                        "   entry_size, " +
                        "   entry_uuid," +
                        "   user_uuid) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        params);

        if ( insertedChars.length != charactersCount.uniqueCharsQty() ) {
            throw new IllegalStateException();
        }

        return charsRecordsUuids;
    }
}
