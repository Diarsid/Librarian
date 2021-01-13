package diarsid.search.impl.logic.impl.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.impl.logic.api.chars.CharsInEntries;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.RealEntry;
import diarsid.support.strings.CharactersCount;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

public class CharsInEntriesImpl extends ThreadBoundTransactional implements CharsInEntries {

    public CharsInEntriesImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public List<UUID> save(RealEntry entry) {
        List<UUID> charsRecordsUuids = new ArrayList<>();

        CharactersCount charactersCount = new CharactersCount();
        charactersCount.calculateIn(entry.stringLower());

        UUID entryUuid = entry.uuid();
        List<List> params = new ArrayList<>();
        charactersCount.forEach((c, qty) -> {
            UUID charsRecordUuid = randomUUID();
            charsRecordsUuids.add(charsRecordUuid);
            params.add(asList(
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
