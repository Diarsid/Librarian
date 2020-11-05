package diarsid.search.impl.logic.api.chars;

import java.util.List;
import java.util.UUID;

import diarsid.search.impl.model.Phrase;

public interface CharsInPhrases {

    List<UUID> save(Phrase phrase);
}
