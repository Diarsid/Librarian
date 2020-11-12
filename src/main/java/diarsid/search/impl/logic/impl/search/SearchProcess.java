package diarsid.search.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

        public LocalDateTime time() {
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
    private final Possible<List<Entry.Label>> labels;
    private final CharactersCount charactersCount;
    private final List<Object> queryArguments;
    private final CriteriaSteps criteriaSteps;

    private final CharactersCount.CountConsumer fillArgumentsForPreciseQuery;
    private final CharactersCount.CountConsumer fillArgumentsForNonPreciseQuery;

    private final Possible<List<Entry>> result;

    SearchProcess() {
        this.status = simplePresentOf(IDLE);
        this.user = simplePossibleButEmpty();
        this.pattern = simplePossibleButEmpty();
        this.labels = simplePossibleButEmpty();
        this.time = simplePossibleButEmpty();
        this.result = simplePossibleButEmpty();
        this.charactersCount = new CharactersCount();
        this.queryArguments = new ArrayList<>();
        this.criteriaSteps = new CriteriaSteps();

        this.fillArgumentsForPreciseQuery = (ch, count) -> {
            this.queryArguments.add(ch);
            this.queryArguments.add(count);
            this.queryArguments.add(pattern.orThrow().length());
            this.queryArguments.add(user.orThrow().uuid());
        };

        this.fillArgumentsForNonPreciseQuery = (ch, count) -> {
            if ( count == 1 ) {
                this.queryArguments.add(ch);
                this.queryArguments.add(count);
                this.queryArguments.add(this.pattern.orThrow().length());
                this.queryArguments.add(this.user.orThrow().uuid());
            }
            else { // > 1
                for (int i = 1; i <= count; i++) {
                    this.queryArguments.add(ch);
                    this.queryArguments.add(i);
                    this.queryArguments.add(this.pattern.orThrow().length());
                    this.queryArguments.add(this.user.orThrow().uuid());
                }
            }
        };
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
        this.queryArguments.clear();
        this.criteriaSteps.clear();
    }

    private void fillUserPatternLabels(User user, String pattern, List<Entry.Label> labels) {
        this.status.resetTo(STARTED);
        this.user.resetTo(user);
        this.pattern.resetTo(pattern);
        this.charactersCount.calculateIn(pattern);
        this.labels.resetTo(labels);
        this.criteriaSteps.setMaxSteps(this.defineMaxDecreasingSteps());
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

    void fill(User user, String pattern, List<Entry.Label> labels) {
        this.fillUserPatternLabels(user, pattern, labels);
    }

    void fill(User user, String pattern, List<Entry.Label> labels, TimeDirection direction, LocalDateTime time) {
        this.fillUserPatternLabels(user, pattern, labels);
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

    public void composeQueryArguments() {
        if ( this.labels.orThrow().isEmpty() ) {
            if ( this.time.isPresent() ) {

            }
            else {
                if ( this.criteriaSteps.decreasedCriteriaSteps() == 0 ) { // precise query
                    if ( this.queryArguments.isEmpty() ) {
                        this.charactersCount.forEach(this.fillArgumentsForPreciseQuery);
                    }
                }
                else { // non precise query
                    int decreasedRate = this.pattern.orThrow().length() - this.criteriaSteps.decreasedCriteriaSteps();

                    if ( this.criteriaSteps.decreasedCriteriaSteps() == 1 ) {
                        this.queryArguments.clear(); // clear argument filled for precise query
                        this.charactersCount.forEach(this.fillArgumentsForNonPreciseQuery);
                        this.queryArguments.add(decreasedRate);
                    }
                    else { // > 1
                        this.queryArguments.set(queryArguments.size() - 1, decreasedRate); // change dercreased rate argument
                    }
                }
            }
        }
        else {
            if ( this.time.isPresent() ) {

            }
            else {

            }
        }

    }

    public List<Object> queryArguments() {
        return this.queryArguments;
    }
}
