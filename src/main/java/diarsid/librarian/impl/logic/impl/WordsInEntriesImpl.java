package diarsid.librarian.impl.logic.impl;

import java.util.ArrayList;
import java.util.List;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.JdbcOperations;
import diarsid.librarian.api.Behavior;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.logic.api.WordsInEntries;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealEntry;
import diarsid.librarian.impl.model.Word;
import diarsid.librarian.impl.model.WordInEntry;

import static java.util.stream.Collectors.toList;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
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
import static diarsid.support.strings.StringUtils.containsTextSeparator;
import static diarsid.support.strings.StringUtils.splitByAnySeparators;

public class WordsInEntriesImpl extends ThreadBoundTransactional implements WordsInEntries {

    private final Words words;
    private final Behavior behavior;

    public WordsInEntriesImpl(Jdbc jdbc, UuidSupplier uuidSupplier, Words words, Behavior behavior) {
        super(jdbc, uuidSupplier);
        this.words = words;
        this.behavior = behavior;
    }

    @Override
    public List<WordInEntry> save(User user, RealEntry entry) {
        List<WordInEntry> wordInEntries = new ArrayList<>();

        Word word;
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

            List<Word> words = this.words.getOrSave(user.uuid(), wordStrings, entry.createdAt());

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
        boolean useCamelCase = this.behavior.isEnabled(user, USE_CAMEL_CASE_WORDS_DECOMPOSITION);
        boolean useSingleCharJoining = this.behavior.isEnabled(user, JOIN_SINGLE_CHARS_TO_NEXT_WORD);

        return toSimplifiedWords(entry.string(), CASE_TO_LOWER, useCamelCase, useSingleCharJoining, false);
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
                        wordInEntry.word().uuid(),
                        wordInEntry.entry().uuid(),
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
                            params.addNext(wordInEntry.word().uuid());
                            params.addNext(wordInEntry.entry().uuid());
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
}
