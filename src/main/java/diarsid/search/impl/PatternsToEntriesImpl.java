package diarsid.search.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.impl.api.internal.PatternsToEntries;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.Params;

import static java.util.stream.Collectors.toList;

import static diarsid.search.api.model.Storable.checkMustBeStored;
import static diarsid.search.impl.util.SqlUtil.placeholdersFor;
import static diarsid.jdbc.api.Params.params;

public class PatternsToEntriesImpl extends ThreadTransactional implements PatternsToEntries {

    private final StringsComparisonAlgorithm algorithm;

    public PatternsToEntriesImpl(
            JdbcTransactionThreadBindings transactionThreadBindings, StringsComparisonAlgorithm algorithm) {
        super(transactionThreadBindings);
        this.algorithm = algorithm;
    }

    @Override
    public List<PatternToEntry> findBy(Pattern pattern) {
        checkMustBeStored(pattern);

        RowCollectorForPatternToEntry relationsCollector = new RowCollectorForPatternToEntry();

        super.currentTransaction()
                .doQuery(
                        relationsCollector,
                        "SELECT * FROM entries " +
                        "   JOIN patterns_to_entries relations " +
                        "       ON relations.entry_uuid = entries.uuid " +
                        "   JOIN patterns " +
                        "       ON relations.pattern_uuid = patterns.uuid " +
                        "WHERE patterns.uuid = ? ",
                        pattern.uuid());

        return relationsCollector.relations();
    }

    @Override
    public List<PatternToEntry> findBy(Pattern pattern, List<Entry> entries) {
        checkMustBeStored(pattern);
        checkMustBeStored(entries);

        UUID userUuid = pattern.userUuid();
        List<Entry> otherEntries = entries
                .stream()
                .filter(entry -> entry.doesNotHaveUserUuid(userUuid))
                .collect(toList());

        if ( ! otherEntries.isEmpty() ) {
            throw new IllegalArgumentException();
        }

        RowCollectorForPatternToEntry relationsCollector = new RowCollectorForPatternToEntry();

        List<UUID> uuids = entries
                .stream()
                .map(Entry::uuid)
                .collect(toList());

        super.currentTransaction()
                .doQuery(
                        relationsCollector,
                        "SELECT * FROM entries " +
                        "   JOIN patterns_to_entries relations " +
                        "       ON relations.entry_uuid = entries.uuid " +
                        "   JOIN patterns " +
                        "       ON relations.pattern_uuid = patterns.uuid " +
                        "WHERE " +
                        "   patterns.uuid = ? AND " +
                        "   entries.uuid IN (:in)".replace(":in", placeholdersFor(uuids.size())),
                        pattern.uuid(), uuids);

        return relationsCollector.relations();
    }

    @Override
    public List<PatternToEntry> findBy(Entry entry) {
        checkMustBeStored(entry);

        RowCollectorForPatternToEntry relationsCollector = new RowCollectorForPatternToEntry();

        super.currentTransaction()
                .doQuery(
                        relationsCollector,
                        "SELECT * FROM entries " +
                        "   JOIN patterns_to_entries relations " +
                        "       ON relations.entry_uuid = entries.uuid " +
                        "   JOIN patterns " +
                        "       ON relations.pattern_uuid = patterns.uuid " +
                        "WHERE entries.uuid = ? ",
                        entry.uuid());

        return relationsCollector.relations();
    }

    @Override
    public int removeAllBy(Entry entry) {
        checkMustBeStored(entry);

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM patterns_to_entries " +
                        "WHERE patterns_to_entries.entry_uuid = ? ",
                        entry.uuid());

        return removed;
    }

    @Override
    public int remove(List<PatternToEntry> relations) {
        checkMustBeStored(relations);

        Set<UUID> userUuids = new HashSet<>();
        List<UUID> relationUuids = new ArrayList<>();

        for ( PatternToEntry relation : relations ) {
            relationUuids.add(relation.uuid());
            userUuids.add(relation.entry().userUuid());
        }

        if ( userUuids.size() > 1 ) {
            throw new IllegalArgumentException();
        }

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM patterns_to_entries " +
                        "WHERE patterns_to_entries.uuid IN (:in) ".replace(
                                ":in", placeholdersFor(relationUuids.size())),
                        relationUuids);

        return removed;
    }

    @Override
    public void save(List<PatternToEntry> relations) {
        List<Params> params = relations
                .stream()
                .map(relation -> params(
                        relation.uuid(),
                        relation.time(),
                        relation.pattern().uuid(),
                        relation.entry().userUuid()))
                .collect(toList());

        int[] inserted = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO patterns_to_entries (" +
                        "   uuid, " +
                        "   time, " +
                        "   pattern_uuid, " +
                        "   entry_uuid) " +
                        "VALUES (?, ?, ?, ?) ",
                        params);

        if ( inserted.length != relations.size() ) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void analyzeAgainAllRelationsOf(Entry entry) {
        checkMustBeStored(entry);

        List<PatternToEntry> relations = this.findBy(entry);
        this.removeAllBy(entry);

        List<PatternToEntry> newRelations = new ArrayList<>();

        Pattern pattern;
        PatternToEntry newRelation;
        for ( PatternToEntry oldRelation : relations ) {
            pattern = oldRelation.pattern();
            newRelation = this.algorithm.analyze(pattern, entry);
            newRelations.add(newRelation);
        }

        this.save(newRelations);
    }
}
