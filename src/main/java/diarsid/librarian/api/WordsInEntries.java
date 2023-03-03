package diarsid.librarian.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import diarsid.librarian.api.model.Entry;

public interface WordsInEntries {

    List<Entry> findEntriesBy(Entry.Word word);

    List<Entry> findEntriesBy(List<Entry.Word> words);

    List<Entry.Word> findWordsBy(UUID entryUuid);

    default List<Entry.Word> findWordsBy(Entry entry) {
        return this.findWordsBy(entry.uuid());
    }

    Map<Entry, List<Entry.Word>> findAllWordsInEveryEntryBy(List<Entry> entries);

    Map<Entry.Word, List<Entry>> findUniqueWordsInAll(List<Entry> entries);

}
