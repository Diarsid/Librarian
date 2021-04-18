package diarsid.librarian.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.librarian.api.exceptions.NotFoundException;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;

import static java.util.Arrays.asList;

public interface Labels {

    Entry.Label getBy(User user, UUID uuid);

    Entry.Label getOrSave(User user, String name);

    List<Entry.Label> getOrSave(User user, List<String> names);

    default List<Entry.Label> getOrSave(User user, String... names) {
        return this.getOrSave(user, asList(names));
    }

    Optional<Entry.Label> findBy(User user, String name);

    void checkMustExist(Entry.Label label) throws NotFoundException;

    void checkMustExist(List<Entry.Label> labels) throws NotFoundException;

    default void checkMustExist(Entry.Label... labels) throws NotFoundException {
        this.checkMustExist(asList(labels));
    }
}
