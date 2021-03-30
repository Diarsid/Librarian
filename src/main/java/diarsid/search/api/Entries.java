package diarsid.search.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.support.objects.CommonEnum;

import static java.util.Arrays.asList;

import static diarsid.search.api.model.Entry.Label.Matching.ANY_OF;

public interface Entries {

    enum RelatedPatternsAction implements CommonEnum<RelatedPatternsAction> {
        REMOVE,
        ANALYZE_AGAIN
    }

    Entry save(User user, String entry);

    Entry save(User user, String entry, List<Entry.Label> labels);

    Entry reload(Entry entry);

    boolean doesNotRequireReload(Entry entry);

    List<Entry> reload(List<Entry> entries);

    Entry getBy(User user, UUID entryUuid);

    List<Entry> getBy(List<UUID> uuids);

    Optional<Entry> findBy(User user, String entry);

    List<Entry> findAllBy(User user, Entry.Label.Matching matching, List<Entry.Label> labels);

    Entry replace(User user, String oldEntry, String newEntry, RelatedPatternsAction action);

    Entry replace(Entry entry, String newEntry, RelatedPatternsAction action);

    boolean remove(User user, String entry);

    boolean remove(Entry entry);

    Entry addLabels(User user, String entry, List<Entry.Label> labels);

    boolean addLabels(Entry entry, List<Entry.Label> labels);

    Entry removeLabels(User user, String entry, List<Entry.Label> labels);

    boolean removeLabels(Entry entry, List<Entry.Label> labels);

    long countEntriesOf(User user);

    long countEntriesBy(User user, String label);

    long countEntriesBy(Entry.Label label);

    long countEntriesBy(User user, Entry.Label.Matching matching, List<String> labels);

    long countEntriesBy(Entry.Label.Matching matching, List<Entry.Label> labels);

    default Entry save(User user, String entry, Entry.Label label) {
        return this.save(user, entry, List.of(label));
    }

    default Entry save(User user, String entry, Entry.Label... labels) {
        return this.save(user, entry, asList(labels));
    }

    default boolean doesRequireReload(Entry entry) {
        return this.doesNotRequireReload(entry);
    }

    default List<Entry> findAllBy(User user, Entry.Label label) {
        return this.findAllBy(user, ANY_OF, List.of(label));
    }

    default List<Entry> findAllBy(User user, Entry.Label.Matching matching, Entry.Label... labels) {
        return this.findAllBy(user, matching, asList(labels));
    }

    default Entry addLabels(User user, String entry, Entry.Label... labels) {
        return this.addLabels(user, entry, asList(labels));
    }

    default boolean addLabels(Entry entry, Entry.Label... labels) {
        return this.addLabels(entry, asList(labels));
    }

    default Entry removeLabels(User user, String entry, Entry.Label... labels) {
        return this.removeLabels(user, entry, asList(labels));
    }

    default boolean removeLabels(Entry entry, Entry.Label... labels) {
        return this.removeLabels(entry, asList(labels));
    }

    default Entry addLabel(User user, String entry, Entry.Label label) {
        return this.addLabels(user, entry, List.of(label));
    }

    default boolean addLabel(Entry entry, Entry.Label label) {
        return this.addLabels(entry, List.of(label));
    }

    default long countEntriesBy(User user, Entry.Label.Matching matching, String... labels) {
        return this.countEntriesBy(user, matching, asList(labels));
    }

    default long countEntriesBy(Entry.Label.Matching matching, Entry.Label... labels) {
        return this.countEntriesBy(matching, asList(labels));
    }
}
