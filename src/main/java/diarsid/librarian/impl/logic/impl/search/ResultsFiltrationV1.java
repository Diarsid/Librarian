package diarsid.librarian.impl.logic.impl.search;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static diarsid.support.objects.collections.CollectionUtils.isNotEmpty;

public class ResultsFiltrationV1 implements ResultsFiltration {

    @Override
    public List<UUID> apply(List<UuidAndAggregationCode> uuidAndAggregationCodes, Consumer<List<UUID>> rejected) {
        List<UUID> entriesWithoutMissed = new ArrayList<>();
        List<UUID> entriesWithMissed = new ArrayList<>();

        UUID uuid;
        for ( UuidAndAggregationCode uuidAndAggregationCode : uuidAndAggregationCodes ) {
            uuid = uuidAndAggregationCode.uuid;
            if ( uuidAndAggregationCode.hasMissed() ) {
                entriesWithMissed.add(uuid);
            }
            else {
                entriesWithoutMissed.add(uuid);
            }
        }

        if ( entriesWithoutMissed.isEmpty() ) {
            return entriesWithMissed;
        }

        if ( isNotEmpty(entriesWithMissed) ) {
            rejected.accept(entriesWithMissed);
        }

        return entriesWithoutMissed;
    }
}
