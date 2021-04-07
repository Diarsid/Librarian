package diarsid.search.impl.logic.impl.search.v2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.search.TimeDirection;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.RealEntry;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static diarsid.search.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.search.impl.logic.impl.search.v2.CharSort.transform;
import static diarsid.support.model.Unique.uuidsOf;

public class SearchByCharsImpl extends ThreadBoundTransactional implements SearchByChars {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByUuids;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAnyOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByNoneOfLabels;

    public SearchByCharsImpl(Jdbc jdbc) {
        super(jdbc);

        this.sqlSelectEntriesByUuids = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT DISTINCT * \n" +
                "FROM entries e \n" +
                "WHERE e.uuid IN ( \n",
                "    ?", ", \n", ")");

        this.sqlSelectEntriesUuidsByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		MY_MATCHING_18(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "		JOIN ( \n" +
                "			    SELECT entry_uuid \n" +
                "			    FROM labels_to_entries \n" +
                "			    WHERE label_uuid IN ( \n",
                "					 ?", ", \n",
                "                   ) \n" +
                "			) le \n" +
                "			ON we.entry_uuid = le.entry_uuid \n" +
                "	WHERE MYLENGTH_4(?, w.string_sort, 60) > -1 \n" +
                ") \n" +
                "SELECT entry_uuid \n" +
                "FROM labeled_words_scan_raw \n" +
                "WHERE word_code >-1 \n" +
                "GROUP BY entry_uuid \n" +
                "HAVING CODE_ANALYZE_17(word_code) > -1 ");

        this.sqlSelectEntriesUuidsByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		MY_MATCHING_18(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "		JOIN ( \n" +
                "			    SELECT entry_uuid \n" +
                "			    FROM labels_to_entries \n" +
                "			    WHERE label_uuid IN ( \n",
                "                   ?", ", \n",
                "                   ) \n" +
                "			    GROUP BY (entry_uuid) \n" +
                "			    HAVING COUNT(label_uuid) = ? \n" +
                "			) le \n" +
                "			ON we.entry_uuid = le.entry_uuid \n" +
                "	WHERE MYLENGTH_4(?, w.string_sort, 60) > -1	\n" +
                ") \n" +
                "SELECT entry_uuid \n" +
                "FROM labeled_words_scan_raw \n" +
                "WHERE word_code >-1 \n" +
                "GROUP BY entry_uuid \n" +
                "HAVING CODE_ANALYZE_17(word_code) > -1 ");

        this.sqlSelectEntriesUuidsByNoneOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		MY_MATCHING_18(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "		JOIN ( \n" +
                "			    SELECT uuid AS entry_uuid \n" +
                "				FROM entries \n" +
                "				WHERE uuid NOT IN ( \n" +
                "					SELECT entry_uuid \n" +
                "				    FROM labels_to_entries \n" +
                "				    WHERE label_uuid IN ( \n",
                "						  ?", ", \n",
                "					) \n" +
                "				) \n" +
                "				AND \n" +
                "				user_uuid = ? \n" +
                "			) le \n" +
                "			ON we.entry_uuid = le.entry_uuid \n" +
                "	WHERE MYLENGTH_4(?, w.string_sort, 60) > -1 \n" +
                ") \n" +
                "SELECT entry_uuid \n" +
                "FROM labeled_words_scan_raw \n" +
                "WHERE word_code >-1 \n" +
                "GROUP BY entry_uuid \n" +
                "HAVING CODE_ANALYZE_17(word_code) > -1 ");
    }

    @Override
    public List<Entry> findBy(User user, String pattern) {
        return this.searchBy(user, pattern);
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return this.searchBy(user, pattern);
        }
        else if ( labels.size() == 1 ) {
            Entry.Label label = labels.get(0);
            if ( matching.equalTo(NONE_OF) ) {
                return this.searchByNotLabel(pattern, label);
            }
            else {
                return this.searchByLabel(pattern, label);
            }

        }
        else {
            switch ( matching ) {
                case ANY_OF: return this.searchByAnyOfLabels(pattern, labels);
                case ALL_OF: return this.searchByAllOfLabels(pattern, labels);
                case NONE_OF: return this.searchByNoneOfLabels(pattern, labels);
                default: throw matching.unsupported();
            }
        }
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels,
            TimeDirection timeDirection,
            LocalDateTime time) {
        if ( labels.isEmpty() ) {
            switch ( timeDirection ) {
                case BEFORE: return this.searchByBefore(user, pattern, time);
                case AFTER_OR_EQUAL: return this.searchByAfterOrEqual(user, pattern, time);
                default: throw timeDirection.unsupported();
            }
        }
        else if ( labels.size() == 1 ) {
            Entry.Label label = labels.get(0);
            if ( matching.equalTo(NONE_OF) ) {
                switch ( timeDirection ) {
                    case BEFORE: return this.searchByNotLabelBefore(pattern, label, time);
                    case AFTER_OR_EQUAL: return this.searchByNotLabelAfterOrEqual(pattern, label, time);
                    default: throw timeDirection.unsupported();
                }
            }
            else {
                switch ( timeDirection ) {
                    case BEFORE: return this.searchByLabelBefore(pattern, label, time);
                    case AFTER_OR_EQUAL: return this.searchByLabelAfterOrEqual(pattern, label, time);
                    default: throw timeDirection.unsupported();
                }
            }
        }
        else {
            switch ( matching ) {
                case ANY_OF:
                    switch ( timeDirection ) {
                        case BEFORE: return this.searchByAnyOfLabelsBefore(pattern, labels, time);
                        case AFTER_OR_EQUAL: return this.searchByAnyOfLabelsAfterOrEqual(pattern, labels, time);
                        default: throw timeDirection.unsupported();
                    }
                case ALL_OF:
                    switch ( timeDirection ) {
                        case BEFORE: return this.searchByAllOfLabelsBefore(pattern, labels, time);
                        case AFTER_OR_EQUAL: return this.searchByAllOfLabelsAfterOrEqual(pattern, labels, time);
                        default: throw timeDirection.unsupported();
                    }
                case NONE_OF:
                    switch ( timeDirection ) {
                        case BEFORE: return this.searchByNoneOfLabelsBefore(pattern, labels, time);
                        case AFTER_OR_EQUAL: return this.searchByNoneOfLabelsAfterOrEqual(pattern, labels, time);
                        default: throw timeDirection.unsupported();
                    }
                default:
                    throw matching.unsupported();
            }
        }
    }

    private List<Entry> getEntriesBy(List<UUID> entryUuids) {
        if ( entryUuids.isEmpty() ) {
            return emptyList();
        }

        LocalDateTime actualAt = now();
        List<Entry> entries = super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealEntry(row, actualAt),
                        this.sqlSelectEntriesByUuids.getFor(entryUuids),
                        entryUuids)
                .collect(toList());

        return entries;
    }

    private List<Entry> searchBy(User user, String pattern) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("uuid"),
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "	SELECT uuid, string, MY_MATCHING_18(?, string) AS w_code \n" +
                        "	FROM words \n" +
                        "	WHERE \n" +
                        "       MYLENGTH_4(?, string_sort, 60) > -1 AND \n" +
                        "       user_uuid = ? \n" +
                        "), \n" +
                        "words_scan AS ( \n" +
                        "	SELECT * \n" +
                        "	FROM words_scan_raw \n" +
                        "	WHERE w_code > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "	SELECT we.entry_uuid, CODE_ANALYZE_17(ws.w_code) r_code \n" +
                        "	FROM words_scan ws \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.word_uuid = ws.uuid \n" +
                        "	GROUP BY we.entry_uuid \n" +
                        ") \n" +
                        "SELECT e.uuid \n" +
                        "FROM entries_scan es \n" +
                        "	JOIN entries e \n" +
                        "		ON e.uuid = es.entry_uuid \n" +
                        "WHERE es.r_code > 0 ; \n",
                        pattern, transform(pattern), user.uuid())
                .collect(toList());

        return this.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByLabel(String pattern, Entry.Label label) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("entry_uuid"),
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		MY_MATCHING_18(?, w.string) AS word_code \n" +
                        "	FROM labels_to_entries le \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.entry_uuid = le.entry_uuid \n" +
                        "		JOIN words w \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "	WHERE \n" +
                        "		le.label_uuid = ? AND \n" +
                        "		MYLENGTH_4(?, w.string_sort, 60) > -1 \n" +
                        ") \n" +
                        "SELECT entry_uuid \n" +
                        "FROM labeled_words_scan_raw \n" +
                        "WHERE word_code >-1 \n" +
                        "GROUP BY entry_uuid \n" +
                        "HAVING CODE_ANALYZE_17(word_code) > -1 ",
                        pattern, label.uuid(), transform(pattern))
                .collect(toList());

        return this.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAnyOfLabels(String pattern, List<Entry.Label> labels) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("entry_uuid"),
                        this.sqlSelectEntriesUuidsByAnyOfLabels.getFor(labels),
                        pattern, uuidsOf(labels), transform(pattern))
                .collect(toList());

        return this.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAllOfLabels(String pattern, List<Entry.Label> labels) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("entry_uuid"),
                        this.sqlSelectEntriesUuidsByAllOfLabels.getFor(labels),
                        pattern, uuidsOf(labels), labels.size(), transform(pattern))
                .collect(toList());

        return this.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByBefore(User user, String pattern, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByAfterOrEqual(User user, String pattern, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByLabelBefore(String pattern, Entry.Label label, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByLabelAfterOrEqual(String pattern, Entry.Label label, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByAnyOfLabelsBefore(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByAnyOfLabelsAfterOrEqual(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByAllOfLabelsBefore(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByAllOfLabelsAfterOrEqual(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByNotLabel(String pattern, Entry.Label label) {
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("entry_uuid"),
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		MY_MATCHING_18(?, string) AS word_code \n" +
                        "	FROM words w \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "		JOIN ( \n" +
                        "			    SELECT uuid AS entry_uuid \n" +
                        "				FROM entries \n" +
                        "				WHERE uuid NOT IN ( \n" +
                        "					SELECT entry_uuid \n" +
                        "				    FROM labels_to_entries \n" +
                        "				    WHERE label_uuid = ? \n" +
                        "				) \n" +
                        "				AND \n" +
                        "				user_uuid = ? \n" +
                        "			) le \n" +
                        "			ON we.entry_uuid = le.entry_uuid \n" +
                        "	WHERE MYLENGTH_4(?, w.string_sort, 60) > -1 \n" +
                        ") \n" +
                        "SELECT entry_uuid \n" +
                        "FROM labeled_words_scan_raw \n" +
                        "WHERE word_code >-1 \n" +
                        "GROUP BY entry_uuid \n" +
                        "HAVING CODE_ANALYZE_17(word_code) > -1 ",
                        pattern, label.uuid(), label.userUuid(), transform(pattern))
                .collect(toList());

        return this.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNoneOfLabels(String pattern, List<Entry.Label> labels) {
        UUID userUuid = labels.get(0).userUuid();
        List<UUID> entryUuids = super.currentTransaction()
                .doQueryAndStream(
                        ColumnGetter.uuidOf("entry_uuid"),
                        this.sqlSelectEntriesUuidsByNoneOfLabels.getFor(labels),
                        pattern, uuidsOf(labels), userUuid, transform(pattern))
                .collect(toList());

        return this.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNotLabelBefore(String pattern, Entry.Label label, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByNotLabelAfterOrEqual(String pattern, Entry.Label label, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByNoneOfLabelsBefore(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }

    private List<Entry> searchByNoneOfLabelsAfterOrEqual(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        throw new UnsupportedOperationException();
    }
}
