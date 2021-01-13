package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.model.Entry;
import diarsid.search.impl.logic.api.Phrases;
import diarsid.search.impl.logic.api.PhrasesInEntries;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.Phrase;
import diarsid.search.impl.model.PhraseInEntry;
import diarsid.search.impl.model.RealEntry;
import diarsid.search.impl.model.Word;
import diarsid.search.impl.model.WordInEntry;

import static java.util.stream.Collectors.toList;

import static diarsid.search.impl.model.CartesianStringComposition.composeFrom;

public class PhrasesInEntriesImpl extends ThreadBoundTransactional implements PhrasesInEntries {

    private final Phrases phrases;
    private final Function<WordInEntry, String> wordInEntryString;

    public PhrasesInEntriesImpl(
            Jdbc jdbc,
            Phrases phrases) {
        super(jdbc);
        this.phrases = phrases;
        this.wordInEntryString = (wordInEntry) -> wordInEntry.word().string();
    }

    @Override
    public List<PhraseInEntry> save(List<WordInEntry> entryWords) {
        List<PhraseInEntry> phraseInEntries = new ArrayList<>();

        Entry entry = entryWords.get(0).entry();
        UUID userUuid = entry.userUuid();
        LocalDateTime time = entry.time();

        List<List<WordInEntry>> cartesian = composeFrom(entryWords, this.wordInEntryString, 3);

        Phrase phrase;
        List<Word> words;
        for ( List<WordInEntry> composedWordsInEntry : cartesian ) {
            words = composedWordsInEntry
                    .stream()
                    .map(WordInEntry::word)
                    .collect(toList());

            phrase = this.phrases.getOrSave(userUuid, words, time);

            PhraseInEntry phraseInEntry = this.save(entry, phrase);

            phraseInEntries.add(phraseInEntry);
//            phraseString = words
//                    .stream()
//                    .map(Word::string)
//                    .collect(joining("_"));
        }

        return phraseInEntries;
    }

    private PhraseInEntry save(Entry entry, Phrase phrase) {
        PhraseInEntry phraseInEntry = new PhraseInEntry((RealEntry) entry, phrase);

        int updated = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO phrases_in_entries( \n" +
                        "   uuid, \n" +
                        "   phrase_uuid, \n" +
                        "   entry_uuid) \n" +
                        "VALUES(?, ?, ?)",
                        phraseInEntry.uuid(),
                        phrase.uuid(),
                        entry.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        return phraseInEntry;
    }
}
