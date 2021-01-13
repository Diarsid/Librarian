package diarsid.search.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.support.objects.PooledReusable;
import diarsid.support.objects.references.Possible;
import diarsid.support.objects.references.Present;
import diarsid.support.strings.CharactersCount;

import static java.util.Collections.emptyList;

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

    static class Time {

        private final TimeDirection direction;
        private final LocalDateTime value;

        Time(TimeDirection direction, LocalDateTime value) {
            this.value = value;
            this.direction = direction;
        }

        public TimeDirection direction() {
            return direction;
        }

        public LocalDateTime value() {
            return value;
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
    private final Possible<Entry.Label.Matching> labelsMatching;
    private final Possible<List<Entry.Label>> labels;
    private final CharactersCount charactersCount;
    private int charsClausesQtyInArguments;
    private final List<Object> charsQueryArguments;
    private final List<Object> extendedArguments;
    private final CriteriaSteps criteriaSteps;

    private final CharactersCount.CountConsumer fillArgumentsForPreciseQuery;
    private final CharactersCount.CountConsumer fillArgumentsForNonPreciseQuery;

    private final Possible<List<Entry>> result;

    SearchProcess() {
        this.status = simplePresentOf(IDLE);
        this.user = simplePossibleButEmpty();
        this.pattern = simplePossibleButEmpty();
        this.labelsMatching = simplePossibleButEmpty();
        this.labels = simplePossibleButEmpty();
        this.time = simplePossibleButEmpty();
        this.result = simplePossibleButEmpty();
        this.charactersCount = new CharactersCount();
        this.charsQueryArguments = new ArrayList<>();
        this.extendedArguments = new ArrayList<>();
        this.criteriaSteps = new CriteriaSteps();

        this.fillArgumentsForPreciseQuery = (ch, count) -> {
            this.charsQueryArguments.add(ch);
            this.charsQueryArguments.add(count);
            this.charsQueryArguments.add(pattern.orThrow().length());
            this.charsQueryArguments.add(user.orThrow().uuid());

            this.charsClausesQtyInArguments++;
        };

        this.fillArgumentsForNonPreciseQuery = (ch, count) -> {
            if ( count == 1 ) {
                this.charsQueryArguments.add(ch);
                this.charsQueryArguments.add(count);
                this.charsQueryArguments.add(this.pattern.orThrow().length());
                this.charsQueryArguments.add(this.user.orThrow().uuid());

                this.charsClausesQtyInArguments++;
            }
            else { // > 1
                int patternLength = this.pattern.orThrow().length();
                UUID userUuid = this.user.orThrow().uuid();
                for (int i = 1; i <= count; i++) {
                    this.charsQueryArguments.add(ch);
                    this.charsQueryArguments.add(i);
                    this.charsQueryArguments.add(patternLength);
                    this.charsQueryArguments.add(userUuid);

                    this.charsClausesQtyInArguments++;
                }
            }
        };
    }

    @Override
    protected void clearForReuse() {
        this.status.resetTo(IDLE);
        this.user.nullify();
        this.pattern.nullify();
        this.labelsMatching.nullify();
        this.labels.nullify();
        this.time.nullify();
        this.result.nullify();
        this.charactersCount.clear();
        this.clearQueryArguments();
        this.criteriaSteps.clear();
    }

    private void fillUserPattern(
            User user, String pattern) {
        this.status.resetTo(STARTED);
        this.user.resetTo(user);
        this.pattern.resetTo(pattern);
        this.charactersCount.calculateIn(pattern);
        this.criteriaSteps.setMaxSteps(this.defineMaxDecreasingSteps());
        this.labels.resetTo(emptyList());
    }

    private void fillUserPatternLabels(
            User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels) {
        this.fillUserPattern(user, pattern);
        this.labelsMatching.resetTo(matching);
        this.labels.resetTo(labels);
    }

    private int defineMaxDecreasingSteps() {
        int totalCharsQty = this.pattern.orThrow().length();

        if ( totalCharsQty < 4 ) {
            return 0;
        }

        int duplicatedCharsQty = totalCharsQty - this.charactersCount.uniqueCharsQty();

        int maxDecreasingSteps;

        if ( duplicatedCharsQty == 0 ) {
            maxDecreasingSteps = (int) Math.ceil(totalCharsQty * 0.2);
        }
        else {
            maxDecreasingSteps = (int) Math.ceil(totalCharsQty * 0.1);
            maxDecreasingSteps = maxDecreasingSteps + duplicatedCharsQty;
        }

        if ( totalCharsQty > 6 ) {
            maxDecreasingSteps = maxDecreasingSteps + (totalCharsQty / 6);
        }

        int maxDecreasingStepsLimit = (int) Math.ceil(totalCharsQty / 3f);

        if ( maxDecreasingSteps > maxDecreasingStepsLimit ) {
            maxDecreasingSteps = maxDecreasingStepsLimit;
        }

        return maxDecreasingSteps;
    }

    void fill(User user, String pattern) {
        this.fillUserPattern(user, pattern);
    }

    void fill(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels) {
        this.fillUserPatternLabels(user, pattern, matching, labels);
    }

    void fill(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels, TimeDirection direction, LocalDateTime time) {
        this.fillUserPatternLabels(user, pattern, matching, labels);
        this.time.resetTo(new Time(direction, time));
    }

    boolean hasTime() {
        return this.time.isPresent();
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

    public boolean ifCanDecreaseCriteria() {
        return this.criteriaSteps.ifCanDecreaseCriteria();
    }

    public void decreaseCriteria() {
        this.criteriaSteps.decreaseCriteria();
    }

    public int decreasedCriteriaSteps() {
        return this.criteriaSteps.decreasedCriteriaSteps();
    }

    public void finishedWith(List<Entry> entries) {
        this.status.resetTo(FINISHED);
        this.result.resetOrMerge(entries, ENTRIES_MERGING);
    }

    public void finishedEmpty() {
        this.status.resetTo(FINISHED);
        this.result.resetTo(emptyList());
    }

    void clearQueryArguments() {
        this.charsQueryArguments.clear();
        this.extendedArguments.clear();
        this.charsClausesQtyInArguments = 0;
    }

    public List<Object> charsQueryArguments() {
        return this.charsQueryArguments;
    }

    public List<Object> extendedArguments() {
        return this.extendedArguments;
    }

    public int charsQueryArgumentsClausesQty() {
        return charsClausesQtyInArguments;
    }

    public Entry.Label.Matching labelsMatching() {
        return this.labelsMatching.or(null);
    }

    void fillCharsArgumentsForPreciseQuery() {
        this.charactersCount().forEach(this.fillArgumentsForPreciseQuery);
        this.extendedArguments.addAll(this.charsQueryArguments);
    }

    void fillCharsArgumentsForNonPreciseQuery() {
        this.charactersCount().forEach(this.fillArgumentsForNonPreciseQuery);
        this.extendedArguments.addAll(this.charsQueryArguments);
    }
}
