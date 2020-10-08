package diarsid.search.impl.interaction;

import diarsid.search.api.interaction.UserChoice;

public class RealUserChoice implements UserChoice {

    private final int index;
    private final Result result;

    public RealUserChoice(Result result, int index) {
        this.index = index;
        this.result = result;
    }

    @Override
    public Result result() {
        return this.result;
    }

    @Override
    public int chosenVariantIndex() {
        return this.index;
    }
}
