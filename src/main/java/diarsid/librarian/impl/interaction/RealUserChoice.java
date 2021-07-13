package diarsid.librarian.impl.interaction;

import diarsid.librarian.api.interaction.UserChoice;

public class RealUserChoice implements UserChoice {

    public static final int UNRESOLVED_INDEX = Integer.MIN_VALUE;

    private final int index;
    private final Decision decision;

    public RealUserChoice(Decision decision, int index) {
        this.index = index;
        this.decision = decision;
    }

    public RealUserChoice(Decision decision) {
        this.decision = decision;
        if ( decision.hasIndex ) {
            throw new IllegalArgumentException();
        }
        this.index = UNRESOLVED_INDEX;
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
