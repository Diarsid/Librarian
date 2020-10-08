package diarsid.search.api;

import java.util.List;
import java.util.Optional;

import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.User;

public interface Search {

    List<PatternToEntry> findAllBy(User user, String pattern);

    Optional<PatternToEntry> findSingleBy(User user, String pattern);
}
