package diarsid.librarian.api;

import java.util.List;
import java.util.Optional;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;

import static java.util.Arrays.asList;

import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;

public interface Search {

    List<PatternToEntry> findAllBy(User user, String pattern);

    List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels);

    default List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label label) {
        return this.findAllBy(user, pattern, ANY_OF, List.of(label));
    }

    default List<PatternToEntry> findAllBy(User user, String pattern, Entry.Label.Matching matching, Entry.Label... labels) {
        return this.findAllBy(user, pattern, matching, asList(labels));
    }

    Optional<PatternToEntry> findSingleBy(User user, String pattern);

    Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels);

    default Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label label) {
        return this.findSingleBy(user, pattern, ANY_OF, List.of(label));
    }

    default Optional<PatternToEntry> findSingleBy(User user, String pattern, Entry.Label.Matching matching, Entry.Label... labels) {
        return this.findSingleBy(user, pattern, matching, asList(labels));
    }
}
