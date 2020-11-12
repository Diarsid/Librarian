package diarsid.search.api;

import java.util.List;
import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.support.objects.CommonEnum;

import static java.util.Arrays.asList;

public interface Entries {

    enum RelatedPatternsAction implements CommonEnum<RelatedPatternsAction> {
        REMOVE,
        ANALYZE_AGAIN
    }

    Entry save(User user, String entry);

    Entry save(User user, String entry, List<Entry.Label> labels);

    Optional<Entry> findBy(User user, String entry);

    List<Entry> findAllBy(User user, List<Entry.Label> labels);

    Entry replace(User user, String oldEntry, String newEntry, RelatedPatternsAction action);

    Entry replace(Entry entry, String newEntry, RelatedPatternsAction action);

    boolean remove(User user, String entry);

    boolean remove(Entry entry);

    Entry addLabels(User user, String entry, List<Entry.Label> labels);

    boolean addLabels(Entry entry, List<Entry.Label> labels);

    default Entry save(User user, String entry, Entry.Label... labels) {
        return this.save(user, entry, asList(labels));
    }

    default List<Entry> findAllBy(User user, Entry.Label... labels) {
        return this.findAllBy(user, asList(labels));
    }

    default Entry addLabels(User user, String entry, Entry.Label... labels) {
        return this.addLabels(user, entry, asList(labels));
    }

    default boolean addLabels(Entry entry, Entry.Label... labels) {
        return this.addLabels(entry, asList(labels));
    }

    default Entry save(User user, String entry, Entry.Label label) {
        return this.save(user, entry, List.of(label));
    }

    default List<Entry> findAllBy(User user, Entry.Label label) {
        return this.findAllBy(user, List.of(label));
    }

    default Entry addLabels(User user, String entry, Entry.Label label) {
        return this.addLabels(user, entry, List.of(label));
    }

    default boolean addLabels(Entry entry, Entry.Label label) {
        return this.addLabels(entry, List.of(label));
    }
}
