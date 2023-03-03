package diarsid.librarian.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.SqlHistory;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.EntriesSearchByPattern;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.librarian.impl.logic.impl.search.CharSort.transform;
import static diarsid.support.model.Unique.uuidsOf;

public class EntriesSearchByCharScan extends ThreadBoundTransactionalEntries implements EntriesSearchByPattern {

    private final ResultsFiltration resultsFiltration;
    private final Consumer<List<UUID>> rejected;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAnyOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByNoneOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByNoneOfLabelsBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByNoneOfLabelsAfterOrEqualTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAllOfLabelsBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAllOfLabelsAfterOrEqualTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAnyOfLabelsBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesUuidsByAnyOfLabelsAfterOrEqualTime;

    public EntriesSearchByCharScan(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);
        this.resultsFiltration = new ResultsLoggingWrapper(new NoResultsFiltration());
        this.rejected = (entries) -> {
            List<String> ignoreWithMissed = entries
                    .stream()
                    .map(uuidCode -> "ignore entry '" + uuidCode + "' due to missed")
                    .collect(toList());
            SqlHistory history = this.currentTransaction().sqlHistory();
            if ( nonNull(history) ) {
                history.comment(ignoreWithMissed);
            }
        };

        this.sqlSelectEntriesUuidsByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByNoneOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByNoneOfLabelsBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = we.entry_uuid AND e.time < ? \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByNoneOfLabelsAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = we.entry_uuid AND e.time >= ? \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByAllOfLabelsBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = we.entry_uuid AND e.time < ? \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByAllOfLabelsAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = we.entry_uuid AND e.time >= ? \n" +
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
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByAnyOfLabelsBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = we.entry_uuid AND e.time < ? \n" +
                "		JOIN ( \n" +
                "			    SELECT entry_uuid \n" +
                "			    FROM labels_to_entries \n" +
                "			    WHERE label_uuid IN ( \n",
                "					 ?", ", \n",
                "                   ) \n" +
                "			) le \n" +
                "			ON we.entry_uuid = le.entry_uuid \n" +
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");

        this.sqlSelectEntriesUuidsByAnyOfLabelsAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "labeled_words_scan_raw AS ( \n" +
                "	SELECT \n" +
                "		we.entry_uuid, \n" +
                "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                "	FROM words w \n" +
                "		JOIN words_in_entries we \n" +
                "			ON w.uuid = we.word_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = we.entry_uuid AND e.time >= ? \n" +
                "		JOIN ( \n" +
                "			    SELECT entry_uuid \n" +
                "			    FROM labels_to_entries \n" +
                "			    WHERE label_uuid IN ( \n",
                "					 ?", ", \n",
                "                   ) \n" +
                "			) le \n" +
                "			ON we.entry_uuid = le.entry_uuid \n" +
                "	WHERE \n" +
                "       w.user_uuid = ? AND \n" +
                "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                "), \n" +
                "entries_scan AS ( \n" +
                "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                "   FROM labeled_words_scan_raw \n" +
                "   WHERE word_code >-1 \n" +
                "   GROUP BY entry_uuid \n" +
                ") \n" +
                "SELECT uuid, r_code \n" +
                "FROM entries_scan \n" +
                "WHERE r_code > -1 ");
    }

    @Override
    public List<Entry> findBy(User user, String pattern) {
        return this.searchBy(user, pattern);
    }

    @Override
    public List<Entry> findBy(User user, String pattern, TimeDirection timeDirection, LocalDateTime time) {
        switch ( timeDirection ) {
            case BEFORE: return this.searchByBefore(user, pattern, time);
            case AFTER_OR_EQUAL: return this.searchByAfterOrEqual(user, pattern, time);
            default: throw timeDirection.unsupported();
        }
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

    private List<Entry> searchBy(User user, String pattern) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "	SELECT uuid, string, EVAL_MATCHING_V46(?, string) AS w_code \n" +
                        "	FROM words \n" +
                        "	WHERE \n" +
                        "       EVAL_LENGTH_V5(?, string_sort, 60) > -1 AND \n" +
                        "       user_uuid = ? \n" +
                        "), \n" +
                        "words_scan AS ( \n" +
                        "	SELECT * \n" +
                        "	FROM words_scan_raw \n" +
                        "	WHERE w_code > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "	SELECT we.entry_uuid, EVAL_CODES_V24(ws.w_code) r_code \n" +
                        "	FROM words_scan ws \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.word_uuid = ws.uuid \n" +
                        "	GROUP BY we.entry_uuid \n" +
                        ") \n" +
                        "SELECT e.uuid, r_code \n" +
                        "FROM entries_scan es \n" +
                        "	JOIN entries e \n" +
                        "		ON e.uuid = es.entry_uuid \n" +
                        "WHERE es.r_code > 0 ; \n",
                        pattern, transform(pattern), user.uuid())
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByLabel(String pattern, Entry.Label label) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		EVAL_MATCHING_V46(?, w.string) AS word_code \n" +
                        "	FROM labels_to_entries le \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.entry_uuid = le.entry_uuid \n" +
                        "		JOIN words w \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "	WHERE \n" +
                        "		le.label_uuid = ? AND \n" +
                        "       w.user_uuid = ? AND \n" +
                        "		EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                        "   FROM labeled_words_scan_raw \n" +
                        "   WHERE word_code >-1 \n" +
                        "   GROUP BY entry_uuid \n" +
                        ") \n" +
                        "SELECT uuid, r_code \n" +
                        "FROM entries_scan \n" +
                        "WHERE r_code > -1 ",
                        pattern, label.uuid(), label.userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAnyOfLabels(String pattern, List<Entry.Label> labels) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByAnyOfLabels.getFor(labels),
                        pattern, uuidsOf(labels), labels.get(0).userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAllOfLabels(String pattern, List<Entry.Label> labels) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByAllOfLabels.getFor(labels),
                        pattern, uuidsOf(labels), labels.size(), labels.get(0).userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByBefore(User user, String pattern, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "	SELECT uuid, string, EVAL_MATCHING_V46(?, string) AS w_code \n" +
                        "	FROM words \n" +
                        "	WHERE \n" +
                        "       EVAL_LENGTH_V5(?, string_sort, 60) > -1 AND \n" +
                        "       user_uuid = ? \n" +
                        "), \n" +
                        "words_scan AS ( \n" +
                        "	SELECT * \n" +
                        "	FROM words_scan_raw \n" +
                        "	WHERE w_code > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "	SELECT we.entry_uuid, EVAL_CODES_V24(ws.w_code) r_code \n" +
                        "	FROM words_scan ws \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.word_uuid = ws.uuid \n" +
                        "	GROUP BY we.entry_uuid \n" +
                        ") \n" +
                        "SELECT e.uuid, r_code \n" +
                        "FROM entries_scan es \n" +
                        "	JOIN entries e \n" +
                        "		ON e.uuid = es.entry_uuid \n" +
                        "WHERE \n" +
                        "   es.r_code > 0 AND \n" +
                        "   e.time < ?; \n",
                        pattern, transform(pattern), user.uuid(), time)
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAfterOrEqual(User user, String pattern, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "words_scan_raw AS ( \n" +
                        "	SELECT uuid, string, EVAL_MATCHING_V46(?, string) AS w_code \n" +
                        "	FROM words \n" +
                        "	WHERE \n" +
                        "       EVAL_LENGTH_V5(?, string_sort, 60) > -1 AND \n" +
                        "       user_uuid = ? \n" +
                        "), \n" +
                        "words_scan AS ( \n" +
                        "	SELECT * \n" +
                        "	FROM words_scan_raw \n" +
                        "	WHERE w_code > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "	SELECT we.entry_uuid, EVAL_CODES_V24(ws.w_code) r_code \n" +
                        "	FROM words_scan ws \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.word_uuid = ws.uuid \n" +
                        "	GROUP BY we.entry_uuid \n" +
                        ") \n" +
                        "SELECT e.uuid, r_code \n" +
                        "FROM entries_scan es \n" +
                        "	JOIN entries e \n" +
                        "		ON e.uuid = es.entry_uuid \n" +
                        "WHERE \n" +
                        "   es.r_code > 0 AND \n" +
                        "   e.time >= ?; \n",
                        pattern, transform(pattern), user.uuid(), time)
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByLabelBefore(String pattern, Entry.Label label, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		EVAL_MATCHING_V46(?, w.string) AS word_code \n" +
                        "	FROM labels_to_entries le \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.entry_uuid = le.entry_uuid \n" +
                        "		JOIN words w \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "	WHERE \n" +
                        "		le.label_uuid = ? AND \n" +
                        "       w.user_uuid = ? AND \n" +
                        "		EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                        "), \n" +
                        "entries_scan AS (" +
                        "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                        "   FROM labeled_words_scan_raw \n" +
                        "      JOIN entries e \n" +
                        "          ON e.uuid = entry_uuid AND e.time < ? \n" +
                        "   WHERE word_code >-1 \n" +
                        "   GROUP BY entry_uuid \n" +
                        ") \n" +
                        "SELECT uuid, r_code \n" +
                        "FROM entries_scan \n" +
                        "WHERE r_code > -1 ",
                        pattern, label.uuid(), label.userUuid(), transform(pattern), time)
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByLabelAfterOrEqual(String pattern, Entry.Label label, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		EVAL_MATCHING_V46(?, w.string) AS word_code \n" +
                        "	FROM labels_to_entries le \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON we.entry_uuid = le.entry_uuid \n" +
                        "		JOIN words w \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "	WHERE \n" +
                        "		le.label_uuid = ? AND \n" +
                        "       w.user_uuid = ? AND \n" +
                        "		EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                        "), \n" +
                        "entries_scan AS (" +
                        "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                        "   FROM labeled_words_scan_raw \n" +
                        "      JOIN entries e \n" +
                        "          ON e.uuid = entry_uuid AND e.time >= ? \n" +
                        "   WHERE word_code >-1 \n" +
                        "   GROUP BY entry_uuid \n" +
                        ") \n" +
                        "SELECT uuid, r_code \n" +
                        "FROM entries_scan \n" +
                        "WHERE r_code > -1 ",
                        pattern, label.uuid(), label.userUuid(), transform(pattern), time)
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAnyOfLabelsBefore(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByAnyOfLabelsBeforeTime.getFor(labels),
                        pattern, time, uuidsOf(labels), labels.get(0).userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAnyOfLabelsAfterOrEqual(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByAnyOfLabelsAfterOrEqualTime.getFor(labels),
                        pattern, time, uuidsOf(labels), labels.get(0).userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAllOfLabelsBefore(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByAllOfLabelsBeforeTime.getFor(labels),
                        pattern, time, uuidsOf(labels), labels.size(), labels.get(0).userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByAllOfLabelsAfterOrEqual(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByAllOfLabelsAfterOrEqualTime.getFor(labels),
                        pattern, time, uuidsOf(labels), labels.size(), labels.get(0).userUuid(), transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNotLabel(String pattern, Entry.Label label) {
        UUID userUuid = label.userUuid();
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
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
                        "	WHERE \n" +
                        "       w.user_uuid = ? AND \n" +
                        "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                        "   FROM labeled_words_scan_raw \n" +
                        "   WHERE word_code >-1 \n" +
                        "   GROUP BY entry_uuid \n" +
                        ") \n" +
                        "SELECT uuid, r_code \n" +
                        "FROM entries_scan \n" +
                        "WHERE r_code > -1 ",
                        pattern, label.uuid(), userUuid, userUuid, transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNoneOfLabels(String pattern, List<Entry.Label> labels) {
        UUID userUuid = labels.get(0).userUuid();

        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByNoneOfLabels.getFor(labels),
                        pattern, uuidsOf(labels), userUuid, userUuid, transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNotLabelBefore(String pattern, Entry.Label label, LocalDateTime time) {
        UUID userUuid = label.userUuid();
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                        "	FROM words w \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "       JOIN entries e \n" +
                        "           ON e.uuid = we.entry_uuid AND e.time < ? \n" +
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
                        "	WHERE \n" +
                        "       w.user_uuid = ? AND \n" +
                        "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                        "   FROM labeled_words_scan_raw \n" +
                        "   WHERE word_code >-1 \n" +
                        "   GROUP BY entry_uuid \n" +
                        ") \n" +
                        "SELECT uuid, r_code \n" +
                        "FROM entries_scan \n" +
                        "WHERE r_code > -1 ",
                        pattern, time, label.uuid(), userUuid, userUuid, transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNotLabelAfterOrEqual(String pattern, Entry.Label label, LocalDateTime time) {
        UUID userUuid = label.userUuid();
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        "WITH \n" +
                        "labeled_words_scan_raw AS ( \n" +
                        "	SELECT \n" +
                        "		we.entry_uuid, \n" +
                        "		EVAL_MATCHING_V46(?, string) AS word_code \n" +
                        "	FROM words w \n" +
                        "		JOIN words_in_entries we \n" +
                        "			ON w.uuid = we.word_uuid \n" +
                        "       JOIN entries e \n" +
                        "           ON e.uuid = we.entry_uuid AND e.time >= ? \n" +
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
                        "	WHERE \n" +
                        "       w.user_uuid = ? AND \n" +
                        "       EVAL_LENGTH_V5(?, w.string_sort, 60) > -1 \n" +
                        "), \n" +
                        "entries_scan AS ( \n" +
                        "   SELECT entry_uuid AS uuid, EVAL_CODES_V24(word_code) AS r_code \n" +
                        "   FROM labeled_words_scan_raw \n" +
                        "   WHERE word_code >-1 \n" +
                        "   GROUP BY entry_uuid \n" +
                        ") \n" +
                        "SELECT uuid, r_code \n" +
                        "FROM entries_scan \n" +
                        "WHERE r_code > -1 ",
                        pattern, time, label.uuid(), userUuid, userUuid, transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNoneOfLabelsBefore(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        UUID userUuid = labels.get(0).userUuid();
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByNoneOfLabelsBeforeTime.getFor(labels),
                        pattern, time, uuidsOf(labels), userUuid, userUuid, transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }

    private List<Entry> searchByNoneOfLabelsAfterOrEqual(String pattern, List<Entry.Label> labels, LocalDateTime time) {
        UUID userUuid = labels.get(0).userUuid();
        List<UuidAndAggregationCode> entryUuidsCodes = super.currentTransaction()
                .doQueryAndStream(
                        UuidAndAggregationCode::new,
                        this.sqlSelectEntriesUuidsByNoneOfLabelsAfterOrEqualTime.getFor(labels),
                        pattern, time, uuidsOf(labels), userUuid, userUuid, transform(pattern))
                .collect(toList());

        List<UUID> entryUuids = this.resultsFiltration.apply(entryUuidsCodes, this.rejected);

        return super.getEntriesBy(entryUuids);
    }
}
