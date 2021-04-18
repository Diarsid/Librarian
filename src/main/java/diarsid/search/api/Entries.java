package diarsid.search.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.User;
import diarsid.support.model.Joined;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public interface Entries {

    interface OnUpdate {

        interface RemovingLabels {

            RemovingLabels ALL = (assignedLabels) -> assignedLabels;
            RemovingLabels NOTHING = (assignedLabels) -> emptyList();

            List<Entry.Label> toRemoveFrom(List<Entry.Label> entryLabelJoins);
        }

        interface RemovingPatterns {

            RemovingPatterns ALL = (assignedPatterns) -> assignedPatterns;
            RemovingPatterns NOTHING = (assignedPatterns) -> emptyList();

            List<Pattern> toRemoveFrom(List<Pattern> patternEntryJoins);
        }
    }

    Entry save(User user, String entry);

    Entry reload(Entry entry);

    List<Entry> reload(List<Entry> entries);

    Entry getBy(User user, UUID entryUuid);

    List<Entry> getBy(User user, List<UUID> uuids);

    Optional<Entry> findBy(User user, String entry);

    boolean doesExistBy(User user, String entry);

    default boolean doesNotExistBy(User user, String entry) {
        return ! this.doesExistBy(user, entry);
    }

    Entry update(
            Entry entry,
            String newEntry,
            OnUpdate.RemovingLabels removingLabels,
            OnUpdate.RemovingPatterns removingPatterns);

    boolean remove(User user, String entry);

    boolean remove(Entry entry);

    long countEntriesOf(User user);

    void checkMustExist(Entry entry) throws NotFoundException;

    void checkMustExist(List<Entry> entries) throws NotFoundException;

    default void checkMustExist(Entry... entries) throws NotFoundException {
        this.checkMustExist(asList(entries));
    }

}
