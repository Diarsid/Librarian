package diarsid.search.impl.logic.api;

import java.time.LocalDateTime;
import java.util.List;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.impl.search.TimeDirection;

public interface EntriesSearchByPattern {

    List<Entry> findBy(
            User user,
            String pattern);

    List<Entry> findBy(
            User user,
            String pattern,
            TimeDirection timeDirection,
            LocalDateTime time);

    List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels);

    List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels,
            TimeDirection timeDirection,
            LocalDateTime time);
}
