package diarsid.search.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.support.objects.PooledReusable;
import diarsid.support.objects.references.Possible;
import diarsid.support.objects.references.Present;
import diarsid.support.strings.CharactersCount;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;

import static diarsid.search.impl.logic.impl.search.SearchProcess.Status.FINISHED;
import static diarsid.search.impl.logic.impl.search.SearchProcess.Status.IDLE;
import static diarsid.search.impl.logic.impl.search.SearchProcess.Status.STARTED;
import static diarsid.support.objects.references.References.simplePossibleButEmpty;
import static diarsid.support.objects.references.References.simplePresentOf;

class SearchProcess extends PooledReusable {

    enum Status {
        IDLE,
        STARTED,
        FINISHED
    }

    private static class Time {

        private final TimeDirection direction;
        private final LocalDateTime time;

        Time(TimeDirection direction, LocalDateTime time) {
            this.time = time;
            this.direction = direction;
        }

        public TimeDirection direction() {
            return direction;
        }

        public LocalDateTime time() {
            return time;
        }
    }

    private static final BiFunction<List<Entry>, List<Entry>, List<Entry>> ENTRIES_MERGING =
            (presentEntries, newEntries) -> {
                presentEntries.addAll(newEntries);
                return presentEntries;
            };

    // Request part
    private final Present<SearchProcess.Status> status;
    private final Possible<User> user;
    private final Possible<String> pattern;
    private final Possible<Time> time;
    private final Possible<List<Entry.Label>> labels;
    private final CharactersCount charactersCount;

    private final Possible<List<Entry>> result;

    SearchProcess() {
        this.status = simplePresentOf(IDLE);
        this.user = simplePossibleButEmpty();
        this.pattern = simplePossibleButEmpty();
        this.labels = simplePossibleButEmpty();
        this.time = simplePossibleButEmpty();
        this.result = simplePossibleButEmpty();
        this.charactersCount = new CharactersCount();
    }

    @Override
    protected void clearForReuse() {
        this.status.resetTo(IDLE);
        this.user.nullify();
        this.pattern.nullify();
        this.labels.nullify();
        this.time.nullify();
        this.result.nullify();
        this.charactersCount.clear();
    }

    private void fillUserPatternLabels(User user, String pattern, List<Entry.Label> labels) {
        this.status.resetTo(STARTED);
        this.user.resetTo(user);
        this.pattern.resetTo(pattern);
        this.charactersCount.calculateIn(pattern);
        this.labels.resetTo(labels);
    }

    void fill(User user, String pattern, List<Entry.Label> labels) {
        this.fillUserPatternLabels(user, pattern, labels);
    }

    void fill(User user, String pattern, List<Entry.Label> labels, TimeDirection direction, LocalDateTime time) {
        this.fillUserPatternLabels(user, pattern, labels);
        this.time.resetTo(new Time(direction, time));
    }

    boolean hasTime() {
        return nonNull(this.time);
    }

    boolean hasPatternLength(int length) {
        return this.pattern.orThrow().length() == length;
    }

    boolean hasResult() {
        return this.result.isPresent() && ! this.result.orThrow().isEmpty();
    }

    boolean hasNoResult() {
        return this.result.isNotPresent() || this.result.orThrow().isEmpty();
    }

    public User user() {
        return this.user.orThrow();
    }

    public String pattern() {
        return this.pattern.orThrow();
    }

    public CharactersCount charactersCount() {
        return this.charactersCount;
    }

    public List<Entry.Label> labels() {
        return this.labels.orThrow();
    }

    public Time time() {
        return this.time.orThrow();
    }

    public List<Entry> result() {
        return this.result.orThrow();
    }

    public boolean ifCanDecreaseWordsCriteria() {
        return false;
    }

    public void decreaseWordsCriteria() {

    }

    public boolean ifCanDecreasePhrasesCriteria() {
        return false;
    }

    public void decreasePhrasesCriteria() {

    }

    public boolean isReasonableSearchInPhrases() {
        return false;
    }

    public boolean isReasonableSearchInEntries() {
        return false;
    }

    public void finishedWith(List<Entry> entries) {
        this.status.resetTo(FINISHED);
        this.result.resetOrMerge(entries, ENTRIES_MERGING);
    }

    public void finishedEmpty() {
        this.status.resetTo(FINISHED);
        this.result.resetTo(emptyList());
    }
}
