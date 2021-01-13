package diarsid.search.impl.interaction;

import diarsid.search.api.interaction.UserChoice;

public class RealUserChoice implements UserChoice {

    private final int index;
    private final Decision decision;

    public RealUserChoice(Decision decision, int index) {
        this.index = index;
        this.decision = decision;
    }

    @Override
    public Decision decision() {
        return this.decision;
    }

    @Override
    public int chosenVariantIndex() {
        return this.index;
    }
}
