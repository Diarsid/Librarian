package diarsid.search.impl.model;

import java.util.function.BiPredicate;

import diarsid.search.api.model.Entry;

class LabelConditionBindable extends RealLabel implements Entry.Label.ConditionBindable {

    private final BiPredicate<Entry, Entry.Label> condition;
    private final Entry.Label original;

    LabelConditionBindable(Entry.Label label, BiPredicate<Entry, Entry.Label> condition) {
        super(label.uuid(), label.createdAt(), label.userUuid(), label.name());
        this.original = label;
        this.condition = condition;
    }

    @Override
    public boolean canBeBoundTo(Entry entry) {
        return this.condition.test(entry, this.original);
    }

    @Override
    public Entry.Label origin() {
        return this.original;
    }
}
