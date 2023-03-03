package diarsid.librarian.impl.logic.impl.search;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public class NoResultsFiltration implements ResultsFiltration {

    private static final Function<UuidAndAggregationCode, UUID> TO_UUID = uuidAndAggregationCode -> uuidAndAggregationCode.uuid;

    @Override
    public List<UUID> apply(List<UuidAndAggregationCode> uuidAndAggregationCodes, Consumer<List<UUID>> rejected) {
        return uuidAndAggregationCodes
                .stream()
                .map(TO_UUID)
                .collect(toList());
    }
}
