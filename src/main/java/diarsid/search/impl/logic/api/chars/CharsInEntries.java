package diarsid.search.impl.logic.api.chars;

import java.util.List;
import java.util.UUID;

import diarsid.search.impl.model.RealEntry;

public interface CharsInEntries {

    List<UUID> save(RealEntry entry);

}
