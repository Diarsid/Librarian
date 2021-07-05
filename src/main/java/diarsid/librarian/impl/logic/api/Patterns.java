package diarsid.librarian.impl.logic.api;

import java.util.List;
import java.util.Optional;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.User;

public interface Patterns {

    Optional<Pattern> findBy(User user, String pattern);

    Optional<Pattern> findBy(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels);

    Pattern save(User user, String pattern);
}
