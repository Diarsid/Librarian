package diarsid.search.impl.logic.impl.search;

import java.util.concurrent.atomic.AtomicInteger;

import diarsid.support.objects.StatefulClearable;
import diarsid.support.objects.references.Possible;

import static diarsid.support.objects.references.References.simplePossibleButEmpty;

public class CriteriaSteps implements StatefulClearable {

    private final AtomicInteger criteriaSteps;
    private final Possible<Integer> maxSteps;

    public CriteriaSteps() {
        this.criteriaSteps = new AtomicInteger(0);
        this.maxSteps = simplePossibleButEmpty();
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps.resetTo(maxSteps);
    }

    @Override
    public void clear() {
        this.criteriaSteps.set(0);
        this.maxSteps.nullify();
    }

    public boolean ifCanDecreaseCriteria() {
        return this.criteriaSteps.get() < this.maxSteps.orThrow();
    }

    public void decreaseCriteria() {
        if ( ! this.ifCanDecreaseCriteria() ) {
            throw new IllegalStateException();
        }

        this.criteriaSteps.incrementAndGet();
    }

    public int decreasedCriteriaSteps() {
        return this.criteriaSteps.get();
    }
}
