package diarsid.librarian.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;

public interface Words {

    Optional<Entry.Word> findBy(UUID uuid);

    Optional<Entry.Word> findBy(User user, String word);

    Map<String, Optional<Entry.Word>> findAllBy(User user, List<String> words);

}
