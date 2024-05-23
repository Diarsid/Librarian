package diarsid.librarian.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;
import diarsid.librarian.impl.logic.api.Words;
import diarsid.librarian.impl.model.Word;

public class EntriesSearchByPatternImpl implements EntriesSearchByPattern {

    private final Words words;
    private final EntriesSearchByCharScan searchByCharScan;
    private final EntriesSearchByWord searchByWords;

    public EntriesSearchByPatternImpl(Words words, EntriesSearchByCharScan searchByCharScan, EntriesSearchByWord searchByWords) {
        this.words = words;
        this.searchByCharScan = searchByCharScan;
        this.searchByWords = searchByWords;
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern) {
        Optional<Entry.Word> word = words.findBy(user, pattern);

        List<Entry> entries;
        if ( word.isPresent() ) {
            entries = searchByWords.findBy(word.get());
        }
        else {
            entries = searchByCharScan.findBy(user, pattern);
        }

        return entries;
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern,
            TimeDirection timeDirection,
            LocalDateTime time) {
        Optional<Entry.Word> word = words.findBy(user, pattern);

        List<Entry> entries;
        if ( word.isPresent() ) {
            entries = searchByWords.findBy(word.get(), timeDirection, time);
        }
        else {
            entries = searchByCharScan.findBy(user, pattern, timeDirection, time);
        }

        return entries;
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels) {
        Optional<Entry.Word> word = words.findBy(user, pattern);

        List<Entry> entries;
        if ( word.isPresent() ) {
            entries = searchByWords.findBy(word.get(), matching, labels);
        }
        else {
            entries = searchByCharScan.findBy(user, pattern, matching, labels);
        }

        return entries;
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels,
            TimeDirection timeDirection,
            LocalDateTime time) {
        Optional<Entry.Word> word = words.findBy(user, pattern);

        List<Entry> entries;
        if ( word.isPresent() ) {
            entries = searchByWords.findBy(word.get(), matching, labels, timeDirection, time);
        }
        else {
            entries = searchByCharScan.findBy(user, pattern, matching, labels, timeDirection, time);
        }

        return entries;
    }
}
