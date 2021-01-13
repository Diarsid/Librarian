package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.impl.logic.api.Phrases;
import diarsid.search.impl.logic.api.WordsInPhrases;
import diarsid.search.impl.logic.api.chars.CharsInPhrases;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.Phrase;
import diarsid.search.impl.model.Word;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class PhrasesImpl extends ThreadBoundTransactional implements Phrases {

    private final WordsInPhrases wordsInPhrases;
    private final CharsInPhrases charsInPhrases;

    public PhrasesImpl(
            Jdbc jdbc,
            WordsInPhrases wordsInPhrases,
            CharsInPhrases charsInPhrases) {
        super(jdbc);
        this.wordsInPhrases = wordsInPhrases;
        this.charsInPhrases = charsInPhrases;
    }

    @Override
    public Phrase getOrSave(UUID userUuid, List<Word> words, LocalDateTime time) {
        Optional<Phrase> existingPhrase = this.findBy(words, userUuid);

        if ( existingPhrase.isEmpty() ) {
            Phrase phrase = new Phrase(userUuid, words, time);
            this.save(phrase);
            return phrase;
        }
        else {
            return existingPhrase.get();
        }
    }

    private Optional<Phrase> findBy(List<Word> words, UUID userUuid) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        Phrase::new,
                        "SELECT * \n" +
                        "FROM phrases p \n" +
                        "WHERE \n" +
                        "   p.string = ? AND \n" +
                        "   p.user_uuid = ? ",
                        Word.join(words), userUuid);
    }

    private void save(Phrase phrase) {
        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO phrases (uuid, string, phrase_size, user_uuid, time) \n" +
                        "VALUES(?, ?, ?, ?, ?)",
                        phrase.uuid(), phrase.string(), phrase.string().length(), phrase.userUuid(), phrase.time());

        if ( inserted != 1 ) {
            throw new IllegalStateException();
        }

        wordsInPhrases.save(phrase);
        charsInPhrases.save(phrase);

        phrase.setState(STORED);
    }
}
