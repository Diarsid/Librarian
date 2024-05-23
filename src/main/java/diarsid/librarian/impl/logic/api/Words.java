package diarsid.librarian.impl.logic.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.librarian.api.model.Entry;

public interface Words extends diarsid.librarian.api.Words {

    Entry.Word getOrSave(UUID userUuid, String string, LocalDateTime time);

    List<Entry.Word> getOrSave(UUID userUuid, List<String> string, LocalDateTime time);

    Optional<Entry.Word> findBy(UUID userUuid, String word);
}
