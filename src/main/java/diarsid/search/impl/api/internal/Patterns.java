package diarsid.search.impl.api.internal;

import java.util.Optional;

import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.User;

public interface Patterns {

    Optional<Pattern> findBy(User user, String pattern);

    Pattern save(User user, String pattern);
}
