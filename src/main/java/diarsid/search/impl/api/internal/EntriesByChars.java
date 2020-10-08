package diarsid.search.impl.api.internal;

import java.time.LocalDateTime;
import java.util.List;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;

public interface EntriesByChars {

    List<Entry> findByChars(User user, String pattern);

    List<Entry> findByCharsAfterOrEqualTime(User user, String pattern, LocalDateTime time);

    List<Entry> findByCharsBeforeTime(User user, String pattern, LocalDateTime time);
}
