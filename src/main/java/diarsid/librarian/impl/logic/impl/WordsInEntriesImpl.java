package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.rows.collectors.RowsCollectorOneIdToManyMap;
import diarsid.librarian.api.Behavior;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.api.WordsInEntries;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.Word;
import diarsid.librarian.impl.model.WordInEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
import static diarsid.librarian.api.Behavior.Feature.INCLUDE_CAMEL_CASED_WORD_ORIGINAL;
import static diarsid.librarian.api.Behavior.Feature.JOIN_SINGLE_CHARS_TO_NEXT_WORD;
import static diarsid.librarian.api.Behavior.Feature.USE_CAMEL_CASE_WORDS_DECOMPOSITION;
import static diarsid.librarian.api.model.Entry.Type.WORD;
import static diarsid.librarian.impl.logic.impl.StringTransformations.CaseConversion.CASE_TO_LOWER;
import static diarsid.librarian.impl.logic.impl.StringTransformations.toSimplifiedWords;
import static diarsid.librarian.impl.model.WordInEntry.Position.FIRST;
import static diarsid.librarian.impl.model.WordInEntry.Position.LAST;
import static diarsid.librarian.impl.model.WordInEntry.Position.MIDDLE;
import static diarsid.librarian.impl.model.WordInEntry.Position.SINGLE;
import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.model.Unique.uuidsOf;
import static diarsid.support.strings.StringUtils.containsTextSeparator;
import static diarsid.support.strings.StringUtils.splitByAnySeparators;

public class WordsInEntriesImpl extends ThreadBoundTransactional implements
        WordsInEntries,
        diarsid.librarian.api.WordsInEntries {

    private final Words words;
    private final Behavior behavior;

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesHavingWordsIn;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectWordsHavingEntriesIn;

    public WordsInEntriesImpl(Jdbc jdbc, UuidSupplier uuidSupplier, Words words, Behavior behavior) {
        super(jdbc, uuidSupplier);
        this.words = words;
        this.behavior = behavior;


        this.sqlSelectEntriesHavingWordsIn = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH entries_of_words AS ( \n" +
                "   SELECT DISTINCT we.entry_uuid \n" +
                "   FROM words_in_entries we \n" +
                "   WHERE we.word_uuid IN ( \n",
                "       ?", ", \n",
                "   ) \n" +
                "   GROUP BY we.entry_uuid HAVING COUNT(we.word_uuid) = ? \n" +
                ") \n" +
                "SELECT e.* \n" +
                "FROM entries e \n" +
                "   JOIN entries_of_words \n" +
                "       ON e.uuid = entries_of_words.entry_uuid"
        );

        this.sqlSelectWordsHavingEntriesIn = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT w.*, we.entry_uuid \n" +
                "FROM words w \n" +
                "   JOIN words_in_entries we \n" +
                "       ON w.uuid = we.word_uuid \n" +
                "WHERE we.entry_uuid = IN ( \n",
                "       ?", ", \n",
                "   ) \n" +
                "ORDER BY we.entry_uuid, w.index ASC"
        );
    }

    @Override
    public List<WordInEntry> save(User user, RealEntry entry) {
        List<WordInEntry> wordInEntries = new ArrayList<>();

        Entry.Word word;
        WordInEntry wordInEntry;
        if ( entry.type().equalTo(WORD) ) {
            word = this.words.getOrSave(entry.userUuid(), entry.stringLower(), entry.createdAt());
            wordInEntry = new WordInEntry(super.nextRandomUuid(), entry, word, SINGLE, 0);
            this.save(wordInEntry);
            wordInEntries.add(wordInEntry);
        }
        else {
            List<String> wordStrings = splitEntryToWords(user, entry);

            wordStrings = wordStrings
                    .stream()
                    .filter(wordString -> ! containsTextSeparator(wordString))
                    .collect(toList());

            List<Entry.Word> words = this.words.getOrSave(user.uuid(), wordStrings, entry.createdAt());

            WordInEntry.Position wordPosition;
            int last = words.size() - 1;
            for (int i = 0; i < words.size(); i++) {
                word = words.get(i);
                wordPosition = definePosition(i, last);
                wordInEntry = new WordInEntry(super.nextRandomUuid(), entry, word, wordPosition, i);
                wordInEntries.add(wordInEntry);
            }

            this.save(wordInEntries);
        }

        return wordInEntries;
    }

    private List<String> splitEntryToWords(User user, RealEntry entry) {
        Map<Behavior.Feature, Boolean> features = this.behavior.get(
                user,
                USE_CAMEL_CASE_WORDS_DECOMPOSITION,
                JOIN_SINGLE_CHARS_TO_NEXT_WORD,
                INCLUDE_CAMEL_CASED_WORD_ORIGINAL);

        return toSimplifiedWords(
                entry.string(),
                CASE_TO_LOWER,
                features.get(USE_CAMEL_CASE_WORDS_DECOMPOSITION),
                features.get(INCLUDE_CAMEL_CASED_WORD_ORIGINAL),
                features.get(JOIN_SINGLE_CHARS_TO_NEXT_WORD),
                false);
    }

    private void save(WordInEntry wordInEntry) {
        int updated = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO words_in_entries( \n" +
                        "   uuid, \n" +
                        "   word_uuid, \n" +
                        "   entry_uuid, \n" +
                        "   position, \n" +
                        "   index) \n" +
                        "VALUES(?, ?, ?, ?, ?)",
                        wordInEntry.uuid(),
                        wordInEntry.wordUuid(),
                        wordInEntry.entryUuid(),
                        wordInEntry.position(),
                        wordInEntry.index());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        wordInEntry.setState(STORED);
    }

    private void save(List<WordInEntry> wordInEntries) {
        int[] updated = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO words_in_entries( \n" +
                        "   uuid, \n" +
                        "   word_uuid, \n" +
                        "   entry_uuid, \n" +
                        "   position, \n" +
                        "   index) \n" +
                        "VALUES(?, ?, ?, ?, ?)",
                        (wordInEntry, params) -> {
                            params.addNext(wordInEntry.uuid());
                            params.addNext(wordInEntry.wordUuid());
                            params.addNext(wordInEntry.entryUuid());
                            params.addNext(wordInEntry.position());
                            params.addNext(wordInEntry.index());
                        },
                        wordInEntries);

        mustAllBe(1, updated);

        wordInEntries.forEach(wordInEntry -> wordInEntry.setState(STORED));
    }

    private static WordInEntry.Position definePosition(int i, int last) {
        if ( i == 0 ) {
            if ( i == last ) {
                 return SINGLE;
            }
            else {
                 return FIRST;
            }
        }
        else if ( i == last ) {
             return LAST;
        }
        else {
             return MIDDLE;
        }
    }

    public static List<String> joinSingleCharsToNextWord(List<String> words) {
        List<String> result = new ArrayList<>();
        StringBuilder wordBuilder = new StringBuilder();
        String word;
        String newWord;
        int last = words.size() - 1;

        for (int i = 0; i < words.size(); i++) {
            word = words.get(i);

            if ( word.length() == 1 ) {
                wordBuilder.append(word);

                if ( i == last ) {
                    result.add(wordBuilder.toString());
                }
            }
            else if ( wordBuilder.length() > 0 ) {
                wordBuilder.append(word);
                newWord = wordBuilder.toString();
                wordBuilder.delete(0, wordBuilder.length());
                result.add(word);
                result.add(newWord);
            }
            else {
                result.add(word);
            }
        }

        return result;
    }

    public static List<String> splitToWords(String string) {
        return splitByAnySeparators(string)
                .stream()
                .distinct()
                .collect(toList());
    }

    @Override
    public List<Entry> findEntriesBy(Entry.Word word) {
        LocalDateTime actualAt = now();

        List<Entry> entries = super.currentTransaction()
                .doQueryAndStream(
                        (row) -> new RealEntry(row, actualAt),
                        "SELECT e.* \n" +
                        "FROM entries e \n" +
                        "   JOIN words_in_entries we \n" +
                        "       ON e.uuid = we.entry_uuid \n" +
                        "WHERE we.word_uuid = ?",
                        word.uuid())
                .collect(toList());

        return entries;
    }

    @Override
    public List<Entry> findEntriesBy(List<Entry.Word> words) {
        LocalDateTime actualAt = now();

        List<UUID> wordUuids = uuidsOf(words);

        List<Entry> entries = super.currentTransaction()
                .doQueryAndStream(
                        (row) -> new RealEntry(row, actualAt),
                        this.sqlSelectEntriesHavingWordsIn.getFor(words),
                        wordUuids, words.size())
                .collect(toList());

        return entries;
    }

    @Override
    public List<Entry.Word> findWordsBy(UUID entryUuid) {
        List<Entry.Word> words = super.currentTransaction()
                .doQueryAndStream(
                        Word::new,
                        "SELECT w.* \n" +
                        "FROM words w \n" +
                        "   JOIN words_in_entries we \n" +
                        "       ON w.uuid = we.word_uuid \n" +
                        "WHERE we.entry_uuid = ?",
                        entryUuid)
                .collect(toList());

        return words;
    }

    @Override
    public Map<Entry, List<Entry.Word>> findAllWordsInEveryEntryBy(List<Entry> entries) {
        RowsCollectorOneIdToManyMap<UUID, Entry.Word, UUID> collector = wordsByEntriesUuidsCollector(entries);

        Map<UUID, List<Entry.Word>> wordsByEntriesUuids = collector.oneIdToMany();

        Map<Entry, List<Entry.Word>> wordsByEntries = entries
                .stream()
                .collect(toMap(
                        entry -> entry,
                        entry -> wordsByEntriesUuids.get(entry.uuid())));

        return wordsByEntries;
    }

    @Override
    public Map<Entry.Word, List<Entry>> findUniqueWordsInAll(List<Entry> entries) {
        RowsCollectorOneIdToManyMap<UUID, Entry.Word, UUID> collector = wordsByEntriesUuidsCollector(entries);

        Map<UUID, List<Entry.Word>> wordsByEntriesUuids = collector.oneIdToMany();
        Map<UUID, Entry> entriesByUuids = entries
                .stream()
                .collect(toMap(
                        entry -> entry.uuid(),
                        entry -> entry));

        Map<Entry.Word, List<Entry>> result = new HashMap<>();

        wordsByEntriesUuids.forEach((entryUuid, words) -> {
            words.forEach(word -> {
                Entry entry = entriesByUuids.get(entryUuid);

                List<Entry> entriesOfWord = result.get(word);

                if ( isNull(entriesOfWord) ) {
                    entriesOfWord = new ArrayList<>();
                    result.put(word, entriesOfWord);
                }

                entriesOfWord.add(entry);
            });
        });

        return result;
    }

    private RowsCollectorOneIdToManyMap<UUID, Entry.Word, UUID> wordsByEntriesUuidsCollector(List<Entry> entries) {
        List<UUID> entriesUuids = uuidsOf(entries);

        RowsCollectorOneIdToManyMap<UUID, Entry.Word, UUID> wordsByEntriesUuidsCollector = new RowsCollectorOneIdToManyMap<>(
                row -> row.uuidOf("we.entry_uuid"),
                row -> row.uuidOf("w.uuid"),
                row -> new Word(row));

        super.currentTransaction()
                .doQuery(
                        wordsByEntriesUuidsCollector,
                        sqlSelectWordsHavingEntriesIn.getFor(entries),
                        entriesUuids);

        return wordsByEntriesUuidsCollector;
    }
}
