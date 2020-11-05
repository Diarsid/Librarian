package diarsid.search.impl.logic.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.search.impl.model.Phrase;
import diarsid.search.impl.model.Word;

public interface Phrases {

    Phrase getOrSave(UUID userUuid, List<Word> words, LocalDateTime time);
}
