package diarsid.search.impl.logic.impl.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.Params;
import diarsid.search.impl.logic.api.chars.CharsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.Phrase;
import diarsid.support.strings.CharactersCount;

import static java.util.UUID.randomUUID;

import static diarsid.jdbc.api.Params.params;

public class CharsInPhrasesImpl extends ThreadTransactional implements CharsInPhrases {

    public CharsInPhrasesImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public List<UUID> save(Phrase phrase) {
        List<UUID> charsRecordUuids = new ArrayList<>();

        CharactersCount counter = new CharactersCount();
        counter.calculateIn(phrase.string());

        List<Params> params = new ArrayList<>();
        counter.forEach((c, qty) -> {
            UUID uuid = randomUUID();
            charsRecordUuids.add(uuid);
            params.add(params(
                    uuid,
                    c,
                    qty,
                    phrase.string().length(),
                    phrase.uuid(),
                    phrase.userUuid()));
        });

        int[] insertedChars =  super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO chars_in_phrases (" +
                        "   uuid, " +
                        "   ch, " +
                        "   qty, " +
                        "   phrase_size," +
                        "   phrase_uuid," +
                        "   user_uuid) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        params);

        if ( insertedChars.length != counter.uniqueCharsQty() ) {
            throw new IllegalStateException();
        }

        return charsRecordUuids;
    }

}
