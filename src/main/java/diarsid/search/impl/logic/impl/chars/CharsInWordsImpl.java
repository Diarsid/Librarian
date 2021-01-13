package diarsid.search.impl.logic.impl.chars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.impl.logic.api.chars.CharsInWords;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.Word;
import diarsid.support.strings.CharactersCount;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;

public class CharsInWordsImpl extends ThreadBoundTransactional implements CharsInWords {

    public CharsInWordsImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public List<UUID> save(Word word) {
        List<UUID> charsRecordUuids = new ArrayList<>();

        CharactersCount counter = new CharactersCount();
        counter.calculateIn(word.string());

        List<List> params = new ArrayList<>();
        counter.forEach((c, qty) -> {
            UUID uuid = randomUUID();
            charsRecordUuids.add(uuid);
            params.add(asList(
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
                        "   user_uuid) \n" +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                        params);

        if ( insertedChars.length != counter.uniqueCharsQty() ) {
            throw new IllegalStateException();
        }

        return charsRecordUuids;
    }

}
