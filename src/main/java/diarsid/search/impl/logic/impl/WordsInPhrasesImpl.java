package diarsid.search.impl.logic.impl;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.impl.logic.api.WordsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.Phrase;
import diarsid.search.impl.model.Word;

import static java.util.UUID.randomUUID;

public class WordsInPhrasesImpl extends ThreadTransactional implements WordsInPhrases {

    public WordsInPhrasesImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public void save(Phrase phrase) {
        int inserted;
        for ( Word word : phrase.words() ) {
            inserted = super.currentTransaction()
                    .doUpdate(
                            "INSERT INTO words_in_phrases(uuid, word_uuid, phrase_uuid) " +
                            "VALUES(?, ?, ?)",
                            randomUUID(), word.uuid(), phrase.uuid());

            if ( inserted != 1 ) {
                throw new IllegalStateException();
            }
        }
    }
}
