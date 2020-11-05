package diarsid.search.impl.logic.api.search;

import java.time.LocalDateTime;
import java.util.List;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.impl.search.TimeDirection;

public interface SearchByChars {

    List<Entry> findByChars(
            User user,
            String pattern,
            List<Entry.Label> labels);

    List<Entry> findByChars(
            User user,
            String pattern,
            List<Entry.Label> labels,
            TimeDirection timeDirection,
            LocalDateTime time);
}
