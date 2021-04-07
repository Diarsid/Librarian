package diarsid.search.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.support.objects.CommonEnum;

import static java.util.Arrays.asList;

public interface Entries {

    enum PatternsTodoOnEntryReplace implements CommonEnum<PatternsTodoOnEntryReplace> {
        REMOVE_RELATED_PATTERN,
        ANALYZE_AGAIN_RELATED_PATTERN
    }

    interface LabelsTodoOnEntryReplace extends BiPredicate<Entry, Entry.Label> {

        boolean isToReassign(Entry entry, Entry.Label label);

        default boolean test(Entry entry, Entry.Label label) {
            return this.isToReassign(entry, label);
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

    Entry replace(User user, String oldEntry, String newEntry, PatternsTodoOnEntryReplace action);

    Entry replace(Entry entry, String newEntry, PatternsTodoOnEntryReplace action);

    Entry replace(User user, String oldEntry, String newEntry, PatternsTodoOnEntryReplace action, LabelsTodoOnEntryReplace reassign);

    Entry replace(Entry entry, String newEntry, PatternsTodoOnEntryReplace action, LabelsTodoOnEntryReplace reassign);

    boolean remove(User user, String entry);

    boolean remove(Entry entry);

    long countEntriesOf(User user);

    void checkMustExist(Entry entry) throws NotFoundException;

    void checkMustExist(List<Entry> entries) throws NotFoundException;

    default void checkMustExist(Entry... entries) throws NotFoundException {
        this.checkMustExist(asList(entries));
    }

}
