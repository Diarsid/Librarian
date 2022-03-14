package diarsid.librarian.api;

import java.util.List;
import java.util.Optional;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.User;

import static java.util.Arrays.asList;

import static diarsid.librarian.api.model.Entry.Label.Matching.ANY_OF;

public interface Search {

    interface Observer {

        default void patternFound(Pattern pattern) {
        }

        default void relationsFound(List<PatternToEntry> relations) {
        }

        default void entriesFound(List<Entry> entries) {
        }

        default void entriesAssessed(List<Entry> entries, List<PatternToEntry> relations) {
        }
    }

    List<PatternToEntry> findAllBy(
            User user,
            String pattern,
            Observer observer);

    List<PatternToEntry> findAllBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels,
            Observer observer);

    default List<PatternToEntry> findAllBy(
            User user,
            String pattern) {
        return this.findAllBy(user, pattern, (Observer) null);
    }

    default List<PatternToEntry> findAllBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels) {
        return this.findAllBy(user, pattern, matching, labels, (Observer) null);
    }

    default List<PatternToEntry> findAllBy(
            User user,
            String pattern,
            Entry.Label label) {
        return this.findAllBy(user, pattern, ANY_OF, List.of(label), (Observer) null);
    }

    default List<PatternToEntry> findAllBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            Entry.Label... labels) {
        return this.findAllBy(user, pattern, matching, asList(labels), (Observer) null);
    }

    Optional<PatternToEntry> findSingleBy(
            User user,
            String pattern);

    Optional<PatternToEntry> findSingleBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels);

    default Optional<PatternToEntry> findSingleBy(
            User user,
            String pattern,
            Entry.Label label) {
        return this.findSingleBy(user, pattern, ANY_OF, List.of(label));
    }

    default Optional<PatternToEntry> findSingleBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            Entry.Label... labels) {
        return this.findSingleBy(user, pattern, matching, asList(labels));
    }
}
