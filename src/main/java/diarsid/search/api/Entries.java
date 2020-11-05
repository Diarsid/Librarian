package diarsid.search.api;

import java.util.List;
import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.support.objects.CommonEnum;

public interface Entries {

    enum RelatedPatternsAction implements CommonEnum<RelatedPatternsAction> {
        REMOVE,
        ANALYZE_AGAIN
    }

    Entry save(User user, String entry);

    Entry save(User user, String entry, Entry.Label... labels);

    Optional<Entry> findBy(User user, String entry);

    List<Entry> findAllBy(User user, Entry.Label... labels);

    Entry replace(User user, String oldEntry, String newEntry, RelatedPatternsAction action);

    Entry replace(Entry entry, String newEntry, RelatedPatternsAction action);

    boolean remove(User user, String entry);

    boolean remove(Entry entry);

    Entry addLabels(User user, String entry, Entry.Label... labels);

    boolean addLabels(Entry entry, Entry.Label... labels);
}
