package diarsid.librarian.impl.logic.impl.search;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static diarsid.librarian.impl.logic.impl.jdbc.h2.extensions.AggregationCodeV2.COMPARATOR_BY_FACTORS;

public class ResultsLoggingWrapper implements ResultsFiltration {

    private final ResultsFiltration wrapped;

    public ResultsLoggingWrapper(ResultsFiltration wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public List<UUID> apply(List<UuidAndAggregationCode> uuidAndAggregationCodes, Consumer<List<UUID>> rejected) {
        uuidAndAggregationCodes.sort(COMPARATOR_BY_FACTORS);
        UUID uuid;
        long code;
        for ( UuidAndAggregationCode uuidAndAggregationCode : uuidAndAggregationCodes ) {
            uuid = uuidAndAggregationCode.uuid;
            code = uuidAndAggregationCode.code;
            System.out.println("[RESULTS FILTER] " + uuid + " - " + code + uuidAndAggregationCode.describe());
        }

        return wrapped.apply(uuidAndAggregationCodes, rejected);
    }
}
