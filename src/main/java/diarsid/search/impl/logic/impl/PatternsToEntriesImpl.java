package diarsid.search.impl.logic.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.required.StringsComparisonAlgorithm;
import diarsid.search.impl.logic.api.PatternsToEntries;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForPatternToEntryAndLabels;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.support.objects.GuardedPool;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.util.stream.Collectors.toList;

import static diarsid.support.model.Storable.checkMustBeStored;

public class PatternsToEntriesImpl extends ThreadBoundTransactional implements PatternsToEntries {

    private final StringsComparisonAlgorithm algorithm;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectPatternToEntryAndLabelsByEntries;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlDeleterPatternToEntryByEntries;
    private final GuardedPool<RowCollectorForPatternToEntryAndLabels> rowCollectorsPool;

    public PatternsToEntriesImpl(
            Jdbc jdbc,
            StringsComparisonAlgorithm algorithm,
            GuardedPool<RowCollectorForPatternToEntryAndLabels> rowCollectorsPool) {
        super(jdbc);
        this.algorithm = algorithm;
        this.rowCollectorsPool = rowCollectorsPool;
        this.sqlSelectPatternToEntryAndLabelsByEntries = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT p.*, e.*, l.*, pe.* \n" +
                "FROM entries e \n" +
                "   JOIN patterns_to_entries pe \n" +
                "       ON pe.entry_uuid = e.uuid \n" +
                "   JOIN patterns p \n" +
                "       ON pe.pattern_uuid = p.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN label l \n" +
                "       ON le.label_uuid = l.uuid \n" +
                "WHERE \n" +
                "   p.uuid = ? AND \n" +
                "   e.uuid IN ( \n",
                "       ?", ", \n",
                "   )");

        this.sqlDeleterPatternToEntryByEntries = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "DELETE FROM patterns_to_entries \n" +
                "WHERE patterns_to_entries.uuid IN ( \n",
                "   ?", ", \n",
                ") ");

    }

    @Override
    public List<PatternToEntry> findBy(Pattern pattern) {
        checkMustBeStored(pattern);

        try (RowCollectorForPatternToEntryAndLabels relationsCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction()
                    .doQuery(
                            relationsCollector,
                            "SELECT p.*, e.*, l.*, pe.* \n" +
                            "FROM entries e \n" +
                            "   JOIN patterns_to_entries pe \n" +
                            "       ON pe.entry_uuid = e.uuid \n" +
                            "   JOIN patterns p \n" +
                            "       ON pe.pattern_uuid = p.uuid \n" +
                            "   LEFT JOIN labels_to_entries le \n" +
                            "       ON e.uuid = le.entry_uuid \n" +
                            "   LEFT JOIN label l \n" +
                            "       ON le.label_uuid = l.uuid \n" +
                            "WHERE patterns.uuid = ? \n",
                            pattern.uuid());

            return relationsCollector.relations();
        }
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

        List<UUID> uuids = entries
                .stream()
                .map(Entry::uuid)
                .collect(toList());

        try (RowCollectorForPatternToEntryAndLabels relationsCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction()
                    .doQuery(
                            relationsCollector,
                            this.sqlSelectPatternToEntryAndLabelsByEntries.getFor(uuids),
                            pattern.uuid(), uuids);

            return relationsCollector.relations();
        }
    }

    @Override
    public List<PatternToEntry> findBy(Entry entry) {
        checkMustBeStored(entry);

        try (RowCollectorForPatternToEntryAndLabels relationsCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction()
                    .doQuery(
                            relationsCollector,
                            "SELECT * \n" +
                            "FROM entries e \n" +
                            "   JOIN patterns_to_entries pe \n" +
                            "       ON pe.entry_uuid = e.uuid \n" +
                            "   JOIN patterns p \n" +
                            "       ON pe.pattern_uuid = p.uuid " +
                            "   LEFT JOIN labels_to_entries le \n" +
                            "       ON e.uuid = le.entry_uuid \n" +
                            "   LEFT JOIN label l \n" +
                            "       ON le.label_uuid = l.uuid \n" +
                            "WHERE e.uuid = ? ",
                            entry.uuid());

            return relationsCollector.relations();
        }
    }

    @Override
    public int removeAllBy(Entry entry) {
        checkMustBeStored(entry);

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM patterns_to_entries \n" +
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
                        this.sqlDeleterPatternToEntryByEntries.getFor(relationUuids),
                        relationUuids);

        return removed;
    }

    @Override
    public void save(List<PatternToEntry> relations) {
        List<List> params = relations
                .stream()
                .map(relation -> List.of(
                        relation.uuid(),
                        relation.createdAt(),
                        relation.pattern().uuid(),
                        relation.entry().userUuid()))
                .collect(toList());

        int[] inserted = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO patterns_to_entries ( \n" +
                        "   uuid, \n" +
                        "   time, \n" +
                        "   pattern_uuid, \n" +
                        "   entry_uuid) \n" +
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
