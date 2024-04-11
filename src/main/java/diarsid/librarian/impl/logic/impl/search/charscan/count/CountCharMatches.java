package diarsid.librarian.impl.logic.impl.search.charscan.count;

import diarsid.librarian.impl.logic.impl.search.charscan.NamedAndVersionedByClassName;

public interface CountCharMatches extends NamedAndVersionedByClassName {

    CountCharMatches CURRENT_VERSION = new CountCharMatchesV9();

    @Override
    default String name() {
        return "EVAL_LENGTH";
    }

    int evaluate(String string1, String string2, int requiredRatio /* 1 - 100 */);

}
