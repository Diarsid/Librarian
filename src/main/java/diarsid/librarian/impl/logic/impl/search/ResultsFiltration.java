package diarsid.librarian.impl.logic.impl.search;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public interface ResultsFiltration {

    List<UUID> apply(List<UuidAndAggregationCode> uuidAndAggregationCodes, Consumer<List<UUID>> rejected);
}
