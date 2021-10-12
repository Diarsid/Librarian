package diarsid.librarian.impl.logic.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.model.Word;

public interface Words {

    Word getOrSave(UUID userUuid, String string, LocalDateTime time);

    List<Word> getOrSave(UUID userUuid, List<String> string, LocalDateTime time);

    Optional<Word> findBy(UUID userUuid, String string);

    default Optional<Word> findBy(User user, String string) {
        return this.findBy(user.uuid(), string);
    }
}
