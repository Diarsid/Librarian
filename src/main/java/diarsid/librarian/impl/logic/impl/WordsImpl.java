package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.Word;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
import static diarsid.librarian.impl.logic.impl.search.CharSort.transform;
import static diarsid.support.model.Storable.State.STORED;

public class WordsImpl extends ThreadBoundTransactional implements
        Words,
        diarsid.librarian.api.Words {

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
    public Entry.Word getOrSave(UUID userUuid, String string, LocalDateTime time) {
        Optional<Entry.Word> existingWord = this.findBy(userUuid, string);

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
    public List<Entry.Word> getOrSave(UUID userUuid, List<String> strings, LocalDateTime time) {
        List<Word> existingWords = this
                .streamWordsBy(userUuid, strings)
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

        int[] changed = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO words (uuid, string, string_sort, word_size, user_uuid, time) \n" +
                        "VALUES(?, ?, ?, ?, ?, ?)",
                        (word, params) -> {
                            params.addNext(word.uuid());
                            params.addNext(word.string());
                            params.addNext(transform(word.string()));
                            params.addNext(word.string().length());
                            params.addNext(word.userUuid());
                            params.addNext(word.createdAt());
                        },
                        newWords);

        mustAllBe(1, changed);

        newWords.forEach(word -> word.setState(STORED));

        List<Entry.Word> allWords = new ArrayList<>();
        allWords.addAll(existingWords);
        allWords.addAll(newWords);

        Map<String, Entry.Word> wordsByStrings = new HashMap<>();
        for ( Entry.Word word : allWords ) {
            wordsByStrings.put(word.string(), word);
        }
        allWords.clear();

        for ( String wordString : strings ) {
            allWords.add(wordsByStrings.get(wordString));
        }

        return allWords;
    }

    private Stream<Word> streamWordsBy(UUID userUuid, List<String> strings) {
        var wordsStream = super.currentTransaction()
                .doQueryAndStream(
                        Word::new,
                        this.sqlSelectWords.getFor(strings),
                        strings, userUuid);
        return wordsStream;
    }

    @Override
    public Optional<Entry.Word> findBy(UUID userUuid, String word) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        Word::new,
                        "SELECT * \n" +
                        "FROM words \n" +
                        "WHERE \n" +
                        "   words.string = ? AND \n" +
                        "   words.user_uuid = ?",
                        word, userUuid);
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

    @Override
    public Optional<Entry.Word> findBy(UUID uuid) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        Word::new,
                        "SELECT * \n" +
                        "FROM words \n" +
                        "WHERE words.uuid = ?",
                        uuid);
    }

    @Override
    public Map<String, Optional<Entry.Word>> findAllBy(User user, List<String> wordsStrings) {
        Map<String, Word> wordsByStrings = this
                .streamWordsBy(user.uuid(), wordsStrings)
                .collect(toMap(
                        (word) -> word.string(),
                        (word) -> word));

        Map<String, Optional<Entry.Word>> optionalWordsByStrings = new HashMap<>();

        wordsStrings.forEach(wordString -> {
            var word = wordsByStrings.get(wordString);
            optionalWordsByStrings.put(wordString, Optional.ofNullable(word));
        });

        return optionalWordsByStrings;
    }

    @Override
    public Optional<Entry.Word> findBy(User user, String string) {
        return this.findBy(user.uuid(), string);
    }
}
