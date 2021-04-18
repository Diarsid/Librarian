package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.impl.logic.api.Words;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.Word;

import static diarsid.search.impl.logic.impl.search.CharSort.transform;
import static diarsid.support.model.Storable.State.STORED;

public class WordsImpl extends ThreadBoundTransactional implements Words {

    public WordsImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public Word getOrSave(UUID userUuid, String string, LocalDateTime time) {
        Optional<Word> existingWord = this.findBy(userUuid, string);

        if ( existingWord.isEmpty() ) {
            Word word = new Word(string, time, userUuid);
            this.save(word);
            return word;
        }
        else {
            return existingWord.get();
        }
    }

    @Override
    public Optional<Word> findBy(UUID userUuid, String string) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        Word::new,
                        "SELECT * \n" +
                        "FROM words \n" +
                        "WHERE \n" +
                        "   words.string = ? AND \n" +
                        "   words.user_uuid = ?",
                        string, userUuid);
    }

    private void save(Word word) {
        String string = word.string();

        int updated = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO words (uuid, string, string_sort, word_size, user_uuid, time) \n" +
                        "VALUES(?, ?, ?, ?, ?, ?)",
                        word.uuid(), string, transform(string), string.length(), word.userUuid(), word.createdAt());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        word.setState(STORED);
    }
}
