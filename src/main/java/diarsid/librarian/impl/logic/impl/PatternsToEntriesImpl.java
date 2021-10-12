package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.impl.logic.api.PatternsToEntries;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealPatternToEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
import static diarsid.support.model.Storable.checkMustBeStored;
import static diarsid.support.model.Unique.uuidsOf;

public class PatternsToEntriesImpl extends ThreadBoundTransactional implements PatternsToEntries {

    private final AlgorithmToModelAdapter algorithmAdapter;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectPatternToEntryByEntries;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlDeleterPatternToEntryByEntries;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlDeleterPatternToEntryByEntryAndPattern;

    public PatternsToEntriesImpl(
            Jdbc jdbc,
            UuidSupplier uuidSupplier,
            AlgorithmToModelAdapter algorithmAdapter) {
        super(jdbc, uuidSupplier);
        this.algorithmAdapter = algorithmAdapter;
        this.sqlSelectPatternToEntryByEntries = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT \n" +
                "   p.uuid          AS p_uuid, \n" +
                "   p.string        AS p_string, \n" +
                "   p.time          AS p_time, \n" +
                "   p.user_uuid     AS p_user_uuid, \n" +
                "   pe.uuid         AS pe_uuid, \n" +
                "   pe.time         AS pe_time, \n" +
                "   pe.algorithm    AS pe_algorithm, \n" +
                "   pe.weight       AS pe_weight \n" +
                "FROM entries e \n" +
                "   JOIN patterns_to_entries pe \n" +
                "       ON pe.entry_uuid = e.uuid \n" +
                "   JOIN patterns p \n" +
                "       ON pe.pattern_uuid = p.uuid \n" +
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

        this.sqlDeleterPatternToEntryByEntryAndPattern = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "DELETE FROM patterns_to_entries pe \n" +
                "WHERE \n" +
                "   pe.entry_uuid = ? AND \n" +
                "   pe.pattern_uuid IN ( \n",
                "       ?", ", \n",
                "   ) ");
    }

    @Override
    public List<PatternToEntry> findBy(Pattern pattern) {
        checkMustBeStored(pattern);
        LocalDateTime actualAt = now();

        List<PatternToEntry> patternsToEntries = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealPatternToEntry(pattern, row, "pe_", "e_", actualAt),
                        "SELECT \n" +
                        "   e.uuid          AS e_uuid, \n" +
                        "   e.string_origin AS e_string_origin, \n" +
                        "   e.string_lower  AS e_string_lower, \n" +
                        "   e.time          AS e_time, \n" +
                        "   e.user_uuid     AS e_user_uuid, \n" +
                        "   pe.uuid         AS pe_uuid, \n" +
                        "   pe.time         AS pe_time, \n" +
                        "   pe.algorithm    AS pe_algorithm, \n" +
                        "   pe.weight       AS pe_weight \n" +
                        "FROM entries e \n" +
                        "   JOIN patterns_to_entries pe \n" +
                        "       ON pe.entry_uuid = e.uuid \n" +
                        "   JOIN patterns p \n" +
                        "       ON pe.pattern_uuid = p.uuid \n" +
                        "WHERE p.uuid = ? ",
                        pattern.uuid())
                .collect(toList());

        return patternsToEntries;
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

        LocalDateTime entriesActualAt = now();

        List<PatternToEntry> relations = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealPatternToEntry(pattern, row, "pe_", "e_", entriesActualAt),
                        this.sqlSelectPatternToEntryByEntries.getFor(uuids),
                        pattern.uuid(), uuids)
                .collect(toList());

        return relations;
    }

    @Override
    public List<PatternToEntry> findBy(Entry entry) {
        checkMustBeStored(entry);

        List<PatternToEntry> relations = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealPatternToEntry(entry, row, "pe_", "p_"),
                        "SELECT \n" +
                        "   p.uuid          AS p_uuid, \n" +
                        "   p.string        AS p_string, \n" +
                        "   p.time          AS p_time, \n" +
                        "   p.user_uuid     AS p_user_uuid, \n" +
                        "   pe.uuid         AS pe_uuid, \n" +
                        "   pe.time         AS pe_time, \n" +
                        "   pe.algorithm    AS pe_algorithm, \n" +
                        "   pe.weight       AS pe_weight \n" +
                        "FROM entries e \n" +
                        "   JOIN patterns_to_entries pe \n" +
                        "       ON pe.entry_uuid = e.uuid \n" +
                        "   JOIN patterns p \n" +
                        "       ON pe.pattern_uuid = p.uuid \n" +
                        "WHERE e.uuid = ? ",
                        entry.uuid())
                .collect(toList());

        return relations;
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
    public int removeBy(Entry entry, List<Pattern> patterns) {
        checkMustBeStored(entry);
        checkMustBeStored(patterns);

        int removed = super.currentTransaction()
                .doUpdate(
                        this.sqlDeleterPatternToEntryByEntryAndPattern.getFor(patterns),
                        entry.uuid(), uuidsOf(patterns));

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
                        relation.entry().uuid(),
                        relation.weight(),
                        relation.algorithmCanonicalName()))
                .collect(toList());

        int[] inserted = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO patterns_to_entries ( \n" +
                        "   uuid, \n" +
                        "   time, \n" +
                        "   pattern_uuid, \n" +
                        "   entry_uuid, \n" +
                        "   weight, \n" +
                        "   algorithm) \n" +
                        "VALUES (?, ?, ?, ?, ?, ?) ",
                        params);

        mustAllBe(1, inserted);
        if ( inserted.length != relations.size() ) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void analyzeAgainAllRelationsOf(Entry entry) {
        checkMustBeStored(entry);

        List<PatternToEntry> relations = this.findBy(entry);
        List<PatternToEntry> newRelations = new ArrayList<>();

        Pattern pattern;
        PatternToEntry newRelation;
        LocalDateTime now = now();
        for ( PatternToEntry oldRelation : relations ) {
            pattern = oldRelation.pattern();
            newRelation = this.algorithmAdapter.analyze(pattern, entry, now);
            newRelations.add(newRelation);
        }

        this.removeAllBy(entry);
        this.save(newRelations);
    }
}
