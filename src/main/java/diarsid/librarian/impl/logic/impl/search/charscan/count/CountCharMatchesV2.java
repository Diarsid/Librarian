package diarsid.librarian.impl.logic.impl.search.charscan.count;

import diarsid.librarian.impl.logic.impl.search.charscan.NamedAndVersionedByClassName;

public interface CountCharMatchesV2 extends NamedAndVersionedByClassName {

    CountCharMatchesV2 CURRENT_VERSION = new CountCharMatchesV11();

    @Override
    default String name() {
        return "EVAL_LENGTH";
    }

    int evaluate(
            String pattern,
            String word,
            String string1,
            String string2,
            int requiredRatio /* 1 - 100 */);

}
