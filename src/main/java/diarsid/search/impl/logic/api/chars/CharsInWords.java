package diarsid.search.impl.logic.api.chars;

import java.util.List;
import java.util.UUID;

import diarsid.search.impl.model.Word;

public interface CharsInWords {

    List<UUID> save(Word word);

}
