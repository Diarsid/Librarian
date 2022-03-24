package diarsid.librarian.api;

import java.util.List;

import diarsid.librarian.api.model.Entry;

import static java.util.Arrays.asList;

import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;

public interface LabeledEntries {

    List<Entry.Labeled> findAllBy(Entry.Label.Matching matching, List<Entry.Label> labels);

    List<Entry.Labeled> findAllBy(Entry entry);

    List<Entry.Labeled> findAllBy(List<Entry> entries);

    List<Entry.Labeled> bind(Entry entry, List<Entry.Label> labels);

    List<Entry.Labeled> bind(List<Entry> entries, Entry.Label label);

    List<Entry.Labeled> bind(List<Entry> entries, List<Entry.Label> labels);

    Entry.Labeled bind(Entry entry, Entry.Label label);

    boolean unbind(Entry entry, List<Entry.Label> labels);

    boolean unbind(Entry entry, Entry.Label label);

    long countEntriesBy(Entry.Label label);

    long countEntriesBy(Entry.Label.Matching matching, List<Entry.Label> labels);

    default List<Entry.Labeled> findAllBy(Entry... entries) {
        return this.findAllBy(List.of(entries));
    }

    default List<Entry.Labeled> findAllBy(Entry.Label label) {
        return this.findAllBy(ANY_OF, List.of(label));
    }

    default List<Entry.Labeled> findAllBy(Entry.Label.Matching matching, Entry.Label... labels) {
        return this.findAllBy(matching, asList(labels));
    }

    default List<Entry.Labeled> bind(Entry entry, Entry.Label... labels) {
        return this.bind(entry, asList(labels));
    }

    default boolean unbind(Entry entry, Entry.Label... labels) {
        return this.unbind(entry, asList(labels));
    }

    default long countEntriesBy(Entry.Label.Matching matching, Entry.Label... labels) {
        return this.countEntriesBy(matching, asList(labels));
    }
}
