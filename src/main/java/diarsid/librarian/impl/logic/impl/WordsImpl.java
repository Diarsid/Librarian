package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.Word;
import diarsid.support.objects.PooledReusable;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.util.stream.Collectors.toList;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
import static diarsid.librarian.impl.logic.impl.search.charscan.CharSort.transform;
import static diarsid.support.model.Storable.State.STORED;

public class WordsImpl extends ThreadBoundTransactional implements Words {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectWords;

    public WordsImpl(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);

        this.sqlSelectWords = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT * \n" +
                "FROM words \n" +
                "WHERE \n" +
                "   words.string IN ( \n",
                "       ?", ", \n",
                "   ) AND \n" +
                "   words.user_uuid = ?"
        );
    }

    @Override
    public Word getOrSave(UUID userUuid, String string, LocalDateTime time) {
        Optional<Word> existingWord = this.findBy(userUuid, string);

        if ( existingWord.isEmpty() ) {
            Word word = new Word(super.nextRandomUuid(), string, time, userUuid);
            this.save(word);
            return word;
        }
        else {
            return existingWord.get();
        }
    }

    @Override
    public List<Word> getOrSave(UUID userUuid, List<String> strings, LocalDateTime time) {
        List<Word> existingWords = super.currentTransaction()
                .doQueryAndStream(
                        Word::new,
                        this.sqlSelectWords.getFor(strings),
                        strings, userUuid)
                .collect(toList());

        var stringsCopy = new ArrayList<>(strings);

        var existingStrings = existingWords
                .stream()
                .map(Word::string)
                .collect(toList());

        stringsCopy.removeAll(existingStrings);

        List<Word> newWords = stringsCopy
                .stream()
                .map(string -> new Word(super.nextRandomUuid(), string, time, userUuid))
                .collect(toList());

        List<List> args = newWords
                .stream()
                .map(word -> List.of(
                        word.uuid(),
                        word.string(),
                        transform(word.string()),
                        word.string().length(),
                        word.userUuid(),
                        word.createdAt()))
                .collect(toList());

        int[] changed = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO words (uuid, string, string_sort, word_size, user_uuid, time) \n" +
                        "VALUES(?, ?, ?, ?, ?, ?)",
                        args);

        mustAllBe(1, changed);

        newWords.forEach(word -> word.setState(STORED));

        List<Word> allWords = new ArrayList<>();
        allWords.addAll(existingWords);
        allWords.addAll(newWords);

        Map<String, Word> wordsByStrings = new HashMap<>();
        for ( Word word : allWords ) {
            wordsByStrings.put(word.string(), word);
        }
        allWords.clear();

        for ( String wordString : strings ) {
            allWords.add(wordsByStrings.get(wordString));
        }

        return allWords;
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
