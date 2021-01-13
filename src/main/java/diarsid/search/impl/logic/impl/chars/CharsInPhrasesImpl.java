package diarsid.search.impl.logic.impl.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.impl.logic.api.chars.CharsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.Phrase;
import diarsid.support.strings.CharactersCount;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

public class CharsInPhrasesImpl extends ThreadBoundTransactional implements CharsInPhrases {

    public CharsInPhrasesImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public List<UUID> save(Phrase phrase) {
        List<UUID> charsRecordUuids = new ArrayList<>();

        CharactersCount counter = new CharactersCount();
        counter.calculateIn(phrase.string());

        List<List> params = new ArrayList<>();
        counter.forEach((c, qty) -> {
            UUID uuid = randomUUID();
            charsRecordUuids.add(uuid);
            params.add(asList(
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
                        "   user_uuid) \n" +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        params);

        if ( insertedChars.length != counter.uniqueCharsQty() ) {
            throw new IllegalStateException();
        }

        return charsRecordUuids;
    }

}
