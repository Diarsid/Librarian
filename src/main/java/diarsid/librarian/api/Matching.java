package diarsid.librarian.api;

import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatching;
import diarsid.librarian.impl.logic.impl.search.charscan.matching.PatternToWordMatchingCodeV2;

public interface Matching {

    public static final Matching INSTANCE = (PatternToWordMatchingCodeV2) PatternToWordMatching.currentVersion();

    interface Match {

        int rate();

        int index();

        int length();

        int charsFound();
    }

    Match find(String pattern, String word);
}
