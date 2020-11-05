package diarsid.search.impl.logic.impl;

import java.util.ArrayList;
import java.util.List;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.impl.logic.api.Words;
import diarsid.search.impl.logic.api.WordsInEntries;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.Word;
import diarsid.search.impl.model.WordInEntry;

import static diarsid.search.api.model.Entry.Type.WORD;
import static diarsid.search.api.model.meta.Storable.State.STORED;
import static diarsid.search.impl.model.WordInEntry.Position.FIRST;
import static diarsid.search.impl.model.WordInEntry.Position.LAST;
import static diarsid.search.impl.model.WordInEntry.Position.MIDDLE;
import static diarsid.search.impl.model.WordInEntry.Position.SINGLE;
import static diarsid.support.strings.StringUtils.containsTextSeparator;
import static diarsid.support.strings.StringUtils.splitByAnySeparators;

public class WordsInEntriesImpl extends ThreadTransactional implements WordsInEntries {

    private final Words words;

    public WordsInEntriesImpl(
            JdbcTransactionThreadBindings transactionThreadBindings,
            Words words) {
        super(transactionThreadBindings);
        this.words = words;
    }

    @Override
    public List<WordInEntry> save(RealEntry entry) {
        List<WordInEntry> wordInEntries = new ArrayList<>();

        Word word;
        WordInEntry wordInEntry;
        if ( entry.type().equalTo(WORD) ) {
            word = this.words.getOrSave(entry.userUuid(), entry.stringLower(), entry.time());
            wordInEntry = new WordInEntry(entry, word, SINGLE, 0);
            this.save(wordInEntry);
            wordInEntries.add(wordInEntry);
        }
        else {
            List<String> wordStrings = splitByAnySeparators(entry.stringLower());

            WordInEntry.Position wordPosition;
            String wordString;
            int last = wordStrings.size() - 1;
            int wordsActualCounter = 0;

            for (int i = 0; i < wordStrings.size(); i++) {
                wordString = wordStrings.get(i);

                if ( containsTextSeparator(wordString) ) {
                    continue;
                }

                word = this.words.getOrSave(entry.userUuid(), wordString, entry.time());
                wordPosition = definePosition(i, last);

                wordInEntry = new WordInEntry(entry, word, wordPosition, wordsActualCounter);
                this.save(wordInEntry);
                wordInEntries.add(wordInEntry);

                wordsActualCounter++;
            }
        }

        return wordInEntries;
    }

    private void save(WordInEntry wordInEntry) {
        int updated = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO words_in_entries(" +
                        "   uuid, " +
                        "   word_uuid, " +
                        "   entry_uuid, " +
                        "   position, " +
                        "   index) " +
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
}
