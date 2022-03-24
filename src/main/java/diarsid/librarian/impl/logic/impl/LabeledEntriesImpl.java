package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Behavior;
import diarsid.librarian.api.Entries;
import diarsid.librarian.api.LabeledEntries;
import diarsid.librarian.api.Labels;
import diarsid.librarian.api.exceptions.NotFoundException;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.EntriesLabelsJoinTable;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.LabelToEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static diarsid.jdbc.api.JdbcOperations.mustAllBe;
import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.api.model.meta.UserScoped.checkMustBelongToOneUser;
import static diarsid.support.model.Joined.distinctRightsOf;
import static diarsid.support.model.Joined.makeJoined;
import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.model.Storable.checkMustBeStored;
import static diarsid.support.model.Unique.uuidsOf;

public class LabeledEntriesImpl extends ThreadBoundTransactional implements LabeledEntries {

    private final Entries entries;
    private final Labels labels;
    private final Behavior behavior;
    private final EntriesLabelsJoinTable entriesLabelsJoinTable;
    private final BiFunction<Entry, Entry.Label, Entry.Labeled> entryLabelJoin;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAnyOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByNoneOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesAndLabelsByAllEntries;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlCountEntriesByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlCountEntriesByAnyOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlCountEntriesByNoneOfLabels;

    public LabeledEntriesImpl(
            Jdbc jdbc,
            UuidSupplier uuidSupplier,
            Entries entries,
            Labels labels,
            Behavior behavior,
            EntriesLabelsJoinTable entriesLabelsJoinTable) {
        super(jdbc, uuidSupplier);
        this.entries = entries;
        this.labels = labels;
        this.behavior = behavior;
        this.entriesLabelsJoinTable = entriesLabelsJoinTable;
        this.entryLabelJoin = (entry, label) -> new LabelToEntry(super.nextRandomUuid(), entry, label);

        this.sqlSelectEntriesAndLabelsByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       ?", ", \n", " ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(label_uuid) = ? \n" +
                "   ) \n" +
                "SELECT \n" +
                "   le.uuid         AS j_uuid, \n" +
                "   le.time         AS j_time, \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.time          AS e_time, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "       ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid \n" +
                "   JOIN labels l \n" +
                "       ON l.uuid = le.label_uuid ");

        this.sqlSelectEntriesAndLabelsByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       ?", ", \n", " ) \n" +
                "   ) \n" +
                "SELECT \n" +
                "   le.uuid         AS j_uuid, \n" +
                "   le.time         AS j_time, \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.time          AS e_time, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "       ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid \n" +
                "   JOIN labels l \n" +
                "       ON l.uuid = le.label_uuid ");

        this.sqlSelectEntriesAndLabelsByNoneOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT \n" +
                "   le.uuid         AS j_uuid, \n" +
                "   le.time         AS j_time, \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.time          AS e_time, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM entries e \n" +
                "   JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid AND e.user_uuid = ? \n" +
                "   JOIN labels l \n" +
                "       ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       ?", ", \n",
                "   ) \n" +
                ") " );

        this.sqlSelectEntriesAndLabelsByAllEntries = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT \n" +
                "   le.uuid         AS j_uuid, \n" +
                "   le.time         AS j_time, \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.time          AS e_time, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM entries e \n" +
                "   JOIN labels_to_entries le \n" +
                "       ON e.uuid = le.entry_uuid \n" +
                "   JOIN labels l \n" +
                "       ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid IN ( \n",
                "   ?", ", \n",
                " )");

        this.sqlCountEntriesByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT COUNT(*) AS qty \n" +
                "FROM ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       ?", ", \n", " ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(label_uuid) = ? \n" +
                ") t");

        this.sqlCountEntriesByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT COUNT(le.entry_uuid) AS qty \n" +
                "FROM labels_to_entries le \n" +
                "WHERE le.label_uuid IN ( \n",
                "    ?", ", \n",
                ") ");

        this.sqlCountEntriesByNoneOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT COUNT(DISTINCT le.entry_uuid) AS qty \n" +
                "FROM labels_to_entries le \n" +
                "   JOIN entries e \n" +
                "       ON e.uuid = le.entry_uuid AND e.user_uuid = ? \n" +
                "WHERE le.entry_uuid NOT IN ( \n" +
                "   SELECT DISTINCT le2.entry_uuid \n" +
                "   FROM labels_to_entries le2 \n" +
                "   WHERE le2.label_uuid IN ( \n",
                "       ?", ", \n",
                "   ) \n" +
                ") ");
    }

    @Override
    public List<Entry.Labeled> findAllBy(Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return emptyList();
        }

        checkMustBeStored(labels);
        checkMustBelongToOneUser(labels);
        this.labels.checkMustExist(labels);

        List<UUID> labelUuids = uuidsOf(labels);
        List<Entry.Labeled> labeled;
        LocalDateTime entryActualAt = now();

        if ( labels.size() == 1 ) {
            if ( matching.equalTo(NONE_OF) ) {
                labeled = super.currentTransaction()
                        .doQueryAndStream(
                                row -> new LabelToEntry(entryActualAt, row, "j_", "e_", "l_"),
                                "WITH \n" +
                                "labeled_entries \n" +
                                "AS (" +
                                "   SELECT DISTINCT le.entry_uuid \n" +
                                "   FROM labels_to_entries le \n" +
                                "   WHERE le.label_uuid != ? \n" +
                                ")   \n" +
                                "SELECT \n" +
                                "   le.uuid         AS j_uuid, \n" +
                                "   le.time         AS j_time, \n" +
                                "   e.uuid          AS e_uuid, \n" +
                                "   e.time          AS e_time, \n" +
                                "   e.string_origin AS e_string_origin, \n" +
                                "   e.string_lower  AS e_string_lower, \n" +
                                "   e.user_uuid     AS e_user_uuid, \n" +
                                "   l.uuid          AS l_uuid, \n" +
                                "   l.time          AS l_time, \n" +
                                "   l.name          AS l_name, \n" +
                                "   l.user_uuid     AS l_user_uuid \n" +
                                "FROM labeled_entries lbe \n" +
                                "   JOIN entries e \n" +
                                "       ON e.uuid = lbe.entry_uuid \n" +
                                "   JOIN labels_to_entries le \n" +
                                "       ON e.uuid = le.entry_uuid \n" +
                                "   JOIN labels l \n" +
                                "       ON l.uuid = le.label_uuid ",
                                labels.get(0).uuid())
                        .collect(toList());
            }
            else {
                labeled = super.currentTransaction()
                        .doQueryAndStream(
                                row -> new LabelToEntry(entryActualAt, row, "j_", "e_", "l_"),
                                "WITH \n" +
                                "labeled_entries \n" +
                                "AS (" +
                                "   SELECT DISTINCT le.entry_uuid \n" +
                                "   FROM labels_to_entries le \n" +
                                "   WHERE le.label_uuid = ? \n" +
                                ")   \n" +
                                "SELECT \n" +
                                "   le.uuid         AS j_uuid, \n" +
                                "   le.time         AS j_time, \n" +
                                "   e.uuid          AS e_uuid, \n" +
                                "   e.time          AS e_time, \n" +
                                "   e.string_origin AS e_string_origin, \n" +
                                "   e.string_lower  AS e_string_lower, \n" +
                                "   e.user_uuid     AS e_user_uuid, \n" +
                                "   l.uuid          AS l_uuid, \n" +
                                "   l.time          AS l_time, \n" +
                                "   l.name          AS l_name, \n" +
                                "   l.user_uuid     AS l_user_uuid \n" +
                                "FROM labeled_entries lbe \n" +
                                "   JOIN entries e \n" +
                                "       ON e.uuid = lbe.entry_uuid \n" +
                                "   JOIN labels_to_entries le \n" +
                                "       ON e.uuid = le.entry_uuid \n" +
                                "   JOIN labels l \n" +
                                "       ON l.uuid = le.label_uuid ",
                                labels.get(0).uuid())
                        .collect(toList());
            }
        }
        else {
            switch ( matching ) {
                case ANY_OF: {
                    labeled = super.currentTransaction()
                            .doQueryAndStream(
                                    row -> new LabelToEntry(entryActualAt, row, "j_", "e_", "l_"),
                                    this.sqlSelectEntriesAndLabelsByAnyOfLabels.getFor(labelUuids),
                                    labelUuids)
                            .collect(toList());
                    break;
                }
                case ALL_OF: {
                    labeled = super.currentTransaction()
                            .doQueryAndStream(
                                    row -> new LabelToEntry(entryActualAt, row, "j_", "e_", "l_"),
                                    this.sqlSelectEntriesAndLabelsByAllOfLabels.getFor(labelUuids),
                                    labelUuids, labels.size())
                            .collect(toList());
                    break;
                }
                case NONE_OF: {
                    labeled = super.currentTransaction()
                            .doQueryAndStream(
                                    row -> new LabelToEntry(entryActualAt, row, "j_", "e_", "l_"),
                                    this.sqlSelectEntriesAndLabelsByNoneOfLabels.getFor(labelUuids),
                                    labels.get(0).userUuid(), labelUuids)
                            .collect(toList());
                    break;
                }
                default:
                    throw matching.unsupported();
            }
        }

        return labeled;
    }

    @Override
    public List<Entry.Labeled> findAllBy(Entry entry) {
        checkMustBeStored(entry);
        this.entries.checkMustExist(entry);
        List<Entry.Labeled> joinedLabels = this.entriesLabelsJoinTable.getAllJoinedTo(entry);
        return joinedLabels;
    }

    @Override
    public List<Entry.Labeled> findAllBy(List<Entry> entries) {
        LocalDateTime entriesAtualAt = now();
        return super.currentTransaction()
                .doQueryAndStream(
                        row -> new LabelToEntry(entriesAtualAt, row, "j_", "e_", "l_"),
                        this.sqlSelectEntriesAndLabelsByAllEntries.getFor(entries),
                        uuidsOf(entries))
                .collect(toList());
    }

    private Entry getEntryBy(User user, String entryString) {
        Optional<Entry> foundEntry = this.entries.findBy(user, entryString);

        if ( foundEntry.isEmpty() ) {
            throw new NotFoundException();
        }

        return foundEntry.get();
    }

    @Override
    public List<Entry.Labeled> bind(Entry entry, List<Entry.Label> labels) {
        checkMustBeStored(entry);
        checkMustBeStored(labels);
        entry.checkAllHaveSameUser(labels);
        this.entries.checkMustExist(entry);
        this.labels.checkMustExist(labels);

        if ( labels.isEmpty() ) {
            return emptyList();
        }

        List<Entry.Label> labelsToJoin = new ArrayList<>();
        Entry.Label.ConditionBindable bindableLabel;
        for ( Entry.Label label : labels) {
            if ( label instanceof Entry.Label.ConditionBindable ) {
                bindableLabel = (Entry.Label.ConditionBindable) label;
                if ( bindableLabel.canNotBeBoundTo(entry) ) {
                    continue;
                }
            }

            labelsToJoin.add(label);
        }

        List<Entry.Labeled> alreadyLabeled = this.entriesLabelsJoinTable.getAllJoinedTo(entry);
        List<Entry.Label> alreadyJoinedLabels = distinctRightsOf(alreadyLabeled);

        labelsToJoin.removeAll(alreadyJoinedLabels);
        alreadyJoinedLabels.removeAll(labelsToJoin);

        if ( labelsToJoin.isEmpty() ) {
            return emptyList();
        }

        List<Entry.Labeled> labeledEntries = makeJoined(entry, labelsToJoin, this.entryLabelJoin);

        int[] changes = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO labels_to_entries (uuid, entry_uuid, label_uuid, time) \n" +
                        "VALUES (?, ?, ?, ?)",
                        labeled -> List.of(
                                labeled.uuid(),
                                labeled.entryUuid(),
                                labeled.labelUuid(),
                                labeled.createdAt()),
                        labeledEntries);

        labeledEntries.forEach(labeled -> labeled.setState(STORED));

        mustAllBe(1, changes);

        return labeledEntries;
    }

    @Override
    public List<Entry.Labeled> bind(List<Entry> entries, Entry.Label label) {
        List<Entry.Labeled> allLabeled = new ArrayList<>();
        Entry.Labeled labeled;

        for ( Entry entry : entries ) {
            labeled = this.bind(entry, label);
            allLabeled.add(labeled);
        }

        return allLabeled;
    }

    @Override
    public List<Entry.Labeled> bind(List<Entry> entries, List<Entry.Label> labels) {
        List<Entry.Labeled> allLabeled = new ArrayList<>();
        List<Entry.Labeled> labeled;

        for ( Entry entry : entries ) {
            labeled = this.bind(entry, labels);
            allLabeled.addAll(labeled);
        }

        return allLabeled;
    }

    @Override
    public Entry.Labeled bind(Entry entry, Entry.Label label) {
        checkMustBeStored(entry);
        checkMustBeStored(label);
        this.entries.checkMustExist(entry);
        entry.checkHasSameUser(label);

//        boolean decomposeEntryPathToDerivedEntries = behavior.isEnabled(user, DECOMPOSE_ENTRY_PATH);
//        boolean applyParentLabelsToDerivedEntries = behavior.isEnabled(user, APPLY_PARENT_LABELS_TO_DERIVED_ENTRIES);
//
//        if ( entry.type().equalTo(PATH) && decomposeEntryPathToDerivedEntries ) {
//            Optional<Entry> existingEntry;
//            RealEntry newDerivedEntry;
//            for ( String path : decomposePath(entry.string(), DONT_NORMALIZE, DONT_INCLUDE_ORIGINAL) ) {
//                existingEntry = this.findBy(user, path);
//
//                if ( existingEntry.isPresent() ) {
//                    if ( applyParentLabelsToDerivedEntries ) {
//                        this.addLabels(existingEntry.get(), labels);
//                    }
//                }
//                else {
//                    newDerivedEntry = entry.newEntryWith(path);
//                    this.saveInternally(user, newDerivedEntry);
//                    if ( applyParentLabelsToDerivedEntries ) {
//                        this.addLabels(newDerivedEntry, labels);
//                    }
//                }
//            }
//        }

        if ( label instanceof Entry.Label.ConditionBindable ) {
            Entry.Label.ConditionBindable bindableLabel = (Entry.Label.ConditionBindable) label;
            if ( bindableLabel.canNotBeBoundTo(entry) ) {
                throw new IllegalArgumentException();
            }
        }

        List<Entry.Labeled> alreadyJoinedRelations = this.entriesLabelsJoinTable.getAllJoinedTo(entry);

        Optional<Entry.Labeled> joined = alreadyJoinedRelations
                .stream()
                .filter(labeledEntry -> labeledEntry.hasRight(label))
                .findFirst();

        if ( joined.isPresent() ) {
            return joined.get();
        }

        Entry.Labeled newJoined = new LabelToEntry(super.nextRandomUuid(), entry, label);

        int changes = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO labels_to_entries (uuid, entry_uuid, label_uuid, time) \n" +
                        "VALUES (?, ?, ?, ?)",
                        newJoined.uuid(),
                        newJoined.entryUuid(),
                        newJoined.labelUuid(),
                        newJoined.createdAt());

        if ( changes != 1 ) {
            throw new IllegalStateException();
        }

        newJoined.setState(STORED);

        return newJoined;
    }

    @Override
    public boolean unbind(Entry entry, List<Entry.Label> labels) {
        return this.entriesLabelsJoinTable.removeBy(entry, labels);
    }

    @Override
    public boolean unbind(Entry entry, Entry.Label label) {
        return this.entriesLabelsJoinTable.removeBy(entry, label);
    }

    @Override
    public long countEntriesBy(Entry.Label label) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        row -> row.longOf("qty"),
                        "SELECT COUNT(DISTINCT le.entry_uuid) AS qty \n" +
                        "FROM labels_to_entries le \n" +
                        "WHERE le.label_uuid = ? ",
                        label.uuid())
                .orElseThrow();
    }

    @Override
    public long countEntriesBy(Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return 0;
        }

        if ( labels.size() == 1 ) {
            if ( matching.equalTo(NONE_OF) ) {
                Entry.Label label = labels.get(0);
                long count = super.currentTransaction()
                        .doQueryAndConvertFirstRow(
                                row -> row.longOf("qty"),
                                "SELECT COUNT(DISTINCT le.entry_uuid) AS qty \n" +
                                "FROM labels_to_entries le \n" +
                                "   JOIN entries e \n" +
                                "       ON e.uuid = le.entry_uuid AND e.user_uuid = ? \n" +
                                "WHERE le.entry_uuid NOT IN ( \n" +
                                "   SELECT DISTINCT le2.entry_uuid \n" +
                                "   FROM labels_to_entries le2 \n" +
                                "   WHERE le2.label_uuid = ? \n" +
                                ") ",
                                label.userUuid(), label.uuid())
                        .orElseThrow();
                return count;
            }
            else {
                return this.countEntriesBy(labels.get(0));
            }
        }

        long count;
        switch ( matching ) {
            case ANY_OF:
                count = super.currentTransaction()
                        .doQueryAndConvertFirstRow(
                                row -> row.longOf("qty"),
                                this.sqlCountEntriesByAnyOfLabels.getFor(labels),
                                uuidsOf(labels))
                        .orElseThrow();
                break;
            case ALL_OF:
                count = super.currentTransaction()
                        .doQueryAndConvertFirstRow(
                                row -> row.longOf("qty"),
                                this.sqlCountEntriesByAllOfLabels.getFor(labels),
                                uuidsOf(labels), labels.size())
                        .orElseThrow();
                break;
            case NONE_OF:
                count = super.currentTransaction()
                        .doQueryAndConvertFirstRow(
                                row -> row.longOf("qty"),
                                this.sqlCountEntriesByNoneOfLabels.getFor(labels),
                                labels.get(0).userUuid(), uuidsOf(labels))
                        .orElseThrow();
                break;
            default:
                throw matching.unsupported();
        }

        return count;
    }
}
