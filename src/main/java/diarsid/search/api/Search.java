package diarsid.search.api;

import java.util.List;
import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.User;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public interface Search {

    List<PatternToEntry> findAllBy(User user, String pattern, List<Entry.Label> labels);

    default List<PatternToEntry> findAllBy(User user, String pattern) {
        return this.findAllBy(user, pattern, emptyList());
    }

    default List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label... labels) {
        return this.findAllBy(user, pattern, asList(labels));
    }

    Optional<PatternToEntry> findSingleBy(User user, String pattern, List<Entry.Label> labels);

    default Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label... labels) {
        return this.findSingleBy(user, pattern, asList(labels));
    }

    default Optional<PatternToEntry> findSingleBy(User user, String pattern) {
        return this.findSingleBy(user, pattern, emptyList());
    }
}
