package diarsid.search.api;

import java.util.List;
import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.User;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import static diarsid.search.api.model.Entry.Label.Matching.ANY_OF;

public interface Search {

    List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels);

    default List<PatternToEntry> findAllBy(User user, String pattern) {
        return this.findAllBy(user, pattern, null, emptyList());
    }

    default List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label label) {
        return this.findAllBy(user, pattern, ANY_OF, List.of(label));
    }

    default List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label.Matching matching, Entry.Label... labels) {
        return this.findAllBy(user, pattern, matching, asList(labels));
    }

    Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels);

    default Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label label) {
        return this.findSingleBy(user, pattern, ANY_OF, List.of(label));
    }

    default Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label.Matching matching, Entry.Label... labels) {
        return this.findSingleBy(user, pattern, matching, asList(labels));
    }

    default Optional<PatternToEntry> findSingleBy(User user, String pattern) {
        return this.findSingleBy(user, pattern, null, emptyList());
    }
}
