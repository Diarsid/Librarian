package diarsid.search.impl.logic.impl;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.impl.logic.api.WordsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.Phrase;
import diarsid.search.impl.model.Word;

import static java.util.UUID.randomUUID;

public class WordsInPhrasesImpl extends ThreadBoundTransactional implements WordsInPhrases {

    public WordsInPhrasesImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public void save(Phrase phrase) {
        int inserted;
        for ( Word word : phrase.words() ) {
            inserted = super.currentTransaction()
                    .doUpdate(
                            "INSERT INTO words_in_phrases(uuid, word_uuid, phrase_uuid) \n" +
                            "VALUES(?, ?, ?)",
                            randomUUID(), word.uuid(), phrase.uuid());

            if ( inserted != 1 ) {
                throw new IllegalStateException();
            }
        }
    }
}
