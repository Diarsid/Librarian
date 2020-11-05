package diarsid.search.impl.logic.impl.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.Params;
import diarsid.search.impl.logic.api.chars.CharsInWords;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.Word;
import diarsid.support.strings.CharactersCount;

import static java.util.UUID.randomUUID;

import static diarsid.jdbc.api.Params.params;

public class CharsInWordsImpl extends ThreadTransactional implements CharsInWords {

    public CharsInWordsImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public List<UUID> save(Word word) {
        List<UUID> charsRecordUuids = new ArrayList<>();

        CharactersCount counter = new CharactersCount();
        counter.calculateIn(word.string());

        List<Params> params = new ArrayList<>();
        counter.forEach((c, qty) -> {
            UUID uuid = randomUUID();
            charsRecordUuids.add(uuid);
            params.add(params(
                    uuid,
                    c,
                    qty,
                    word.string().length(),
                    word.uuid(),
                    word.userUuid()));
        });

        int[] insertedChars =  super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO chars_in_words (" +
                        "   uuid, " +
                        "   ch, " +
                        "   qty, " +
                        "   word_size, " +
                        "   word_uuid, " +
                        "   user_uuid) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        params);

        if ( insertedChars.length != counter.uniqueCharsQty() ) {
            throw new IllegalStateException();
        }

        return charsRecordUuids;
    }

}
