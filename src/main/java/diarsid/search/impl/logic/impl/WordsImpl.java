package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.impl.logic.api.Words;
import diarsid.search.impl.logic.api.chars.CharsInWords;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.Word;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class WordsImpl extends ThreadTransactional implements Words {

    private final CharsInWords charsInWords;

    public WordsImpl(JdbcTransactionThreadBindings transactionThreadBindings, CharsInWords charsInWords) {
        super(transactionThreadBindings);
        this.charsInWords = charsInWords;
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

    private Optional<Word> findBy(UUID userUuid, String string) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        Word::new,
                        "SELECT * " +
                        "FROM words " +
                        "WHERE " +
                        "   words.string = ? AND " +
                        "   words.user_uuid = ?",
                        string, userUuid);
    }

    private void save(Word word) {
        int updated = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO words (uuid, string, word_size, user_uuid, time) " +
                        "VALUES(?, ?, ?, ?, ?)",
                        word.uuid(), word.string(), word.string().length(), word.userUuid(), word.time());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        charsInWords.save(word);

        word.setState(STORED);
    }
}
