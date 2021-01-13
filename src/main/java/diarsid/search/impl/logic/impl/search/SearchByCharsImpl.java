package diarsid.search.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.List;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.jdbc.PooledRowCollectorForEntriesAndLabels;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.strings.similarity.api.Similarity;
import diarsid.support.objects.GuardedPool;
import diarsid.support.strings.StringCacheFor2RepeatedSeparatedPrefixSuffix;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import static diarsid.search.api.model.Entry.Label.Matching.ALL_OF;
import static diarsid.search.api.model.Entry.Label.Matching.ANY_OF;
import static diarsid.search.impl.logic.impl.search.TimeDirection.BEFORE;
import static diarsid.support.configuration.Configuration.actualConfiguration;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchByCharsImpl extends ThreadBoundTransactional implements SearchByChars {

    private final Similarity similarity;
    private final GuardedPool<PooledRowCollectorForEntriesAndLabels> rowCollectorsPool;

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByWords;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndLabel;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndLabelAndBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndLabelAndAfterOrEqualTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAnyOfLabels;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAnyOfLabelsAndBeforeTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAnyOfLabelsAndAfterOrEqualTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAllOfLabels;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAllOfLabelsAndBeforeTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAllOfLabelsAndAfterOrEqualTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectEntriesByWordsAndAfterOrEqualTime;

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromWordsIntersect;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersect;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectAndBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectAndAfterOrEqualTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectWithAllOfLabels;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectWithAllOfLabelsAndBeforeTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectWithAllOfLabelsAndAfterOrEqualTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectWithAnyOfLabels;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectWithAnyOfLabelsAndBeforeTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersectWithAnyOfLabelsAndAfterOrEqualTime;

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromWordsUnionAllGroupByAndSum;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSum;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumAndBeforeTime;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumAndAfterOrEqualTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabels;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabelsAndBeforeTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabelsAndAfterOrEqualTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabels;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabelsAndBeforeTime;
    private final StringCacheFor2RepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabelsAndAfterOrEqualTime;

    private final GuardedPool<SearchProcess> searchProcessPool;

    public SearchByCharsImpl(Jdbc jdbc) {
        super(jdbc);

        this.similarity = Similarity.createInstance(actualConfiguration());

        this.rowCollectorsPool = new GuardedPool<>(() -> new PooledRowCollectorForEntriesAndLabels("e_", "l_"));

        this.sqlSelectEntriesByWords = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM words_result wr \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = wr.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) ");

        this.sqlSelectEntriesByWordsAndLabel = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid = ? \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe\n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries)");

        this.sqlSelectEntriesByWordsAndLabelAndBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid = ? \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe\n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) AND \n" +
                "   e.time < ? ");

        this.sqlSelectEntriesByWordsAndLabelAndAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid = ? \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe\n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) AND \n" +
                "   e.time >= ? ");

        this.sqlSelectEntriesByWordsAndAnyOfLabels = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid IN (", "?", ", ", ") \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries)");

        this.sqlSelectEntriesByWordsAndAnyOfLabelsAndBeforeTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid IN (", "?", ", ", ") \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) AND \n" +
                "   e.time < ? ");

        this.sqlSelectEntriesByWordsAndAnyOfLabelsAndAfterOrEqualTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid IN (", "?", ", ", ") \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) AND \n" +
                "   e.time >= ? ");

        this.sqlSelectEntriesByWordsAndAllOfLabels = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid \n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid IN (", "?", ", ", ") \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(label_uuid) = ? \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries)");

        this.sqlSelectEntriesByWordsAndAllOfLabelsAndBeforeTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid IN (", "?", ", ", ") \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(label_uuid) = ? \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) AND \n" +
                "   e.time < ? ");

        this.sqlSelectEntriesByWordsAndAllOfLabelsAndAfterOrEqualTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS ( \n" +
                "   SELECT DISTINCT le.entry_uuid\n" +
                "   FROM words_result wr \n" +
                "      JOIN labels_to_entries le \n" +
                "          ON wr.entry_uuid = le.entry_uuid \n" +
                "   WHERE le.label_uuid IN (", "?", ", ", ") \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(label_uuid) = ? \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM labeled_entries lbe \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = lbe.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM labeled_entries) AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) AND \n" +
                "   e.time >= ? ");

        this.sqlSelectEntriesByWordsAndBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM words_result wr \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = wr.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.time < ? AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) ");

        this.sqlSelectEntriesByWordsAndAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "words_result \n" +
                "AS ( \n" +
                "   SELECT DISTINCT word_uuid, entry_uuid, POSITION, INDEX \n" +
                "   FROM words_in_entries we\n" +
                "   WHERE we.word_uuid IN (", "?", ", ", ") \n" +
                "   ), \n" +
                "rejected_entries \n" +
                "AS ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_result wr1 \n" +
                "      JOIN words_result wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.INDEX = wr2.INDEX AND \n" +
                "            wr1.POSITION = 'LAST' AND \n" +
                "            wr2.POSITION = 'MIDDLE' \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.user_uuid     AS l_user_uuid, \n" +
                "   l.time          AS l_time, \n" +
                "   l.name          AS l_name \n" +
                "FROM words_result wr \n" +
                "   JOIN entries e \n" +
                "      ON e.uuid = wr.entry_uuid \n" +
                "   JOIN labels_to_entries le \n" +
                "      ON le.entry_uuid = e.uuid \n" +
                "   JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.time >= ? AND \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT entry_uuid \n" +
                "       FROM rejected_entries) ");

        this.sqlSelectFromWordsIntersect = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */  \n" +
                "       SELECT cw.word_uuid \n" +
                "       FROM chars_in_words cw \n" +
                "       WHERE \n" +
                "           cw.ch = ? AND \n" +
                "           cw.qty >= ? AND \n" +
                "           cw.word_size >= ? AND \n" +
                "           cw.user_uuid = ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ) \n" +
                "SELECT DISTINCT w.uuid, w.string \n" +
                "FROM chars_scan c \n" +
                "   JOIN words w \n" +
                "       ON c.word_uuid = w.uuid ");

        this.sqlSelectFromPhrasesIntersect = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ),  \n" +
                "entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT pe.entry_uuid AS uuid \n" +
                "    FROM chars_scan c \n" +
                "        JOIN phrases_in_entries pe \n" +
                "            ON c.phrase_uuid = pe.phrase_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT \n" +
                "       er.uuid         AS uuid, \n" +
                "       we.word_uuid    AS word_uuid, \n" +
                "       we.index        AS word_index, \n" +
                "       we.position     AS word_position \n" +
                "    FROM entries_results er \n" +
                "        JOIN words_in_entries we \n" +
                "            ON er.uuid = we.entry_uuid \n" +
                "    ) \n" +
                "SELECT DISTINCT \n" +
                "    e.uuid            AS e_uuid, \n" +
                "    e.string_origin   AS e_string_origin, \n" +
                "    e.string_lower    AS e_string_lower, \n" +
                "    e.time            AS e_time, \n" +
                "    e.user_uuid       AS e_user_uuid, \n" +
                "    l.uuid             AS l_uuid, \n" +
                "    l.name             AS l_name, \n" +
                "    l.time             AS l_time, \n" +
                "    l.user_uuid        AS l_user_uuid \n" +
                "FROM entries_results er \n" +
                "    JOIN entries e \n" +
                "        ON e.uuid = er.uuid \n" +
                "    LEFT JOIN labels_to_entries le \n" +
                "        ON er.uuid = le.entry_uuid \n" +
                "    LEFT JOIN labels l \n" +
                "        ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN \n" +
                "   ( \n" +
                "    SELECT wr2.uuid \n" +
                "    FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "           ON \n" +
                "               wr1.word_uuid = wr2.word_uuid   AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST'      AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "   ) ");

        this.sqlSelectFromPhrasesIntersectAndBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ),  \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT e.* \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e \n" +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time < ? \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT \n" +
                "       er.uuid         AS uuid, \n" +
                "       we.word_uuid    AS word_uuid, \n" +
                "       we.index        AS word_index, \n" +
                "       we.position     AS word_position \n" +
                "    FROM entries_results er \n" +
                "        JOIN words_in_entries we \n" +
                "            ON er.uuid = we.entry_uuid \n" +
                "    ) \n" + // finished here
                "SELECT DISTINCT \n" +
                "    er.uuid            AS e_uuid, \n" +
                "    er.string_origin   AS e_string_origin, \n" +
                "    er.string_lower    AS e_string_lower, \n" +
                "    er.time            AS e_time, \n" +
                "    er.user_uuid       AS e_user_uuid, \n" +
                "    l.uuid             AS l_uuid, \n" +
                "    l.name             AS l_name, \n" +
                "    l.time             AS l_time, \n" +
                "    l.user_uuid        AS l_user_uuid \n" +
                "FROM entries_results er \n" +
                "    LEFT JOIN labels_to_entries le \n" +
                "        ON er.uuid = le.entry_uuid \n" +
                "    LEFT JOIN labels l \n" +
                "        ON l.uuid = le.label_uuid \n" +
                "WHERE er.uuid NOT IN \n" +
                "   ( \n" +
                "    SELECT wr2.uuid \n" +
                "    FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "           ON \n" +
                "               wr1.word_uuid = wr2.word_uuid   AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST'      AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "   ) ");

        this.sqlSelectFromPhrasesIntersectAndAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ),  \n" +
                "entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT e.* \n" +
                "    FROM chars_scan c \n" +
                "        JOIN phrases_in_entries pe \n" +
                "            ON c.phrase_uuid = pe.phrase_uuid \n" +
                "        JOIN entries e \n" +
                "            ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time >= ? \n" +
                "   ),   \n" +
                "words_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT \n" +
                "       er.uuid         AS uuid, \n" +
                "       we.word_uuid    AS word_uuid, \n" +
                "       we.index        AS word_index, \n" +
                "       we.position     AS word_position \n" +
                "    FROM entries_results er \n" +
                "        JOIN words_in_entries we \n" +
                "            ON er.uuid = we.entry_uuid \n" +
                "    ) \n" +
                "SELECT DISTINCT \n" +
                "    er.uuid            AS e_uuid, \n" +
                "    er.string_origin   AS e_string_origin, \n" +
                "    er.string_lower    AS e_string_lower, \n" +
                "    er.time            AS e_time, \n" +
                "    er.user_uuid       AS e_user_uuid, \n" +
                "    l.uuid             AS l_uuid, \n" +
                "    l.name             AS l_name, \n" +
                "    l.time             AS l_time, \n" +
                "    l.user_uuid        AS l_user_uuid \n" +
                "FROM entries_results er \n" +
                "    LEFT JOIN labels_to_entries le \n" +
                "        ON er.uuid = le.entry_uuid \n" +
                "    LEFT JOIN labels l \n" +
                "        ON l.uuid = le.label_uuid \n" +
                "WHERE er.uuid NOT IN \n" +
                "   ( \n" +
                "    SELECT wr2.uuid \n" +
                "    FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "           ON \n" +
                "               wr1.word_uuid = wr2.word_uuid   AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST'      AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "   ) ");

        this.sqlSelectFromPhrasesIntersectWithAllOfLabels = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "      JOIN phrases_in_entries pe \n" +
                "         ON c.phrase_uuid = pe.phrase_uuid \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(le.label_uuid) = ? \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.index     AS word_index, \n" +
                "      we.position  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_results wr1 \n" +
                "      JOIN words_results wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.word_index = wr2.word_index AND \n" +
                "            wr1.word_position = 'LAST' AND \n" +
                "            wr2.word_position = 'MIDDLE' \n" +
                "   ) "
        );

        // EXAMPLE ABOVE

        this.sqlSelectFromPhrasesIntersectWithAllOfLabelsAndBeforeTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e " +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time < ? \n " +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(le.label_uuid) = ? \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "           JOIN words_results wr2 \n" +
                "               ON  wr1.word_uuid = wr2.word_uuid AND \n" +
                "                   wr1.word_index = wr2.word_index AND \n" +
                "                   wr1.word_position = 'LAST' AND \n" +
                "                   wr2.word_position = 'MIDDLE' \n" +
                "       ) " // here
        );

        this.sqlSelectFromPhrasesIntersectWithAllOfLabelsAndAfterOrEqualTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e " +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time >= ? \n " +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(le.label_uuid) = ? \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "           JOIN words_results wr2 \n" +
                "               ON  wr1.word_uuid = wr2.word_uuid AND \n" +
                "                   wr1.word_index = wr2.word_index AND \n" +
                "                   wr1.word_position = 'LAST' AND \n" +
                "                   wr2.word_position = 'MIDDLE' \n" +
                "       ) "
        ); // here

        this.sqlSelectFromPhrasesIntersectWithAnyOfLabels = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ),   \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT DISTINCT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_results wr1 \n" +
                "      JOIN words_results wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.word_index = wr2.word_index AND \n" +
                "            wr1.word_position = 'LAST' AND \n" +
                "            wr2.word_position = 'MIDDLE' \n" +
                "   ) "); // here

        this.sqlSelectFromPhrasesIntersectWithAnyOfLabelsAndBeforeTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ),   \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e " +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time < ? \n " +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT DISTINCT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "           JOIN words_results wr2 \n" +
                "               ON  wr1.word_uuid = wr2.word_uuid AND \n" +
                "                   wr1.word_index = wr2.word_index AND \n" +
                "                   wr1.word_position = 'LAST' AND \n" +
                "                   wr2.word_position = 'MIDDLE' \n" +
                "       ) "); // here

        this.sqlSelectFromPhrasesIntersectWithAnyOfLabelsAndAfterOrEqualTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   INTERSECT \n",

                "   ),   \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e " +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time >= ? \n " +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT DISTINCT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "           JOIN words_results wr2 \n" +
                "               ON  wr1.word_uuid = wr2.word_uuid AND \n" +
                "                   wr1.word_index = wr2.word_index AND \n" +
                "                   wr1.word_position = 'LAST' AND \n" +
                "                   wr2.word_position = 'MIDDLE' \n" +
                "       ) "); // done

        this.sqlSelectFromWordsUnionAllGroupByAndSum = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cw.word_uuid, 1 AS count \n" +
                "       FROM chars_in_words cw \n" +
                "       WHERE \n" +
                "           cw.ch = ? AND \n" +
                "           cw.qty >= ? AND \n" +
                "           cw.word_size >= ? AND \n" +
                "           cw.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "    ),\n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT \n" +
                "       c.word_uuid, \n" +
                "       SUM(c.count) AS rate \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.word_uuid \n" +
                "   HAVING rate >= ? \n" +
                "   ) \n" +
                "SELECT DISTINCT w.uuid, w.string \n" +
                "FROM chars_scan c \n" +
                "   JOIN words w \n" +
                "       ON c.word_uuid = w.uuid ");

        this.sqlSelectFromPhrasesUnionAllGroupByAndSum = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "    ),\n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid, SUM(c.count) AS rate \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING rate >= ? \n" +
                "   ),\n" +
                "entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT pe.entry_uuid AS uuid \n" +
                "    FROM chars_scan c \n" +
                "        JOIN phrases_in_entries pe \n" +
                "            ON c.phrase_uuid = pe.phrase_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT \n" +
                "       er.uuid         AS uuid, \n" +
                "       we.word_uuid    AS word_uuid, \n" +
                "       we.index        AS word_index, \n" +
                "       we.position     AS word_position \n" +
                "    FROM entries_results er \n" +
                "        JOIN words_in_entries we \n" +
                "            ON er.uuid = we.entry_uuid \n" +
                "    ) \n" +
                "SELECT DISTINCT \n" +
                "    e.uuid            AS e_uuid, \n" +
                "    e.string_origin   AS e_string_origin, \n" +
                "    e.string_lower    AS e_string_lower, \n" +
                "    e.time            AS e_time, \n" +
                "    e.user_uuid       AS e_user_uuid, \n" +
                "    l.uuid             AS l_uuid, \n" +
                "    l.name             AS l_name, \n" +
                "    l.time             AS l_time, \n" +
                "    l.user_uuid        AS l_user_uuid \n" +
                "FROM entries_results er \n" +
                "    JOIN entries e \n" +
                "        ON er.uuid = e.uuid \n" +
                "    LEFT JOIN labels_to_entries le \n" +
                "        ON er.uuid = le.entry_uuid \n" +
                "    LEFT JOIN labels l \n" +
                "        ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN \n" +
                "   ( \n" +
                "    SELECT wr2.uuid \n" +
                "    FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "           ON \n" +
                "               wr1.word_uuid = wr2.word_uuid AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST' AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "   ) ");

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumAndBeforeTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "    ),\n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid, SUM(c.count) AS rate \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING rate >= ? \n" +
                "   ),\n" +
                "entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT e.* \n" +
                "    FROM chars_scan c \n" +
                "        JOIN phrases_in_entries pe \n" +
                "            ON c.phrase_uuid = pe.phrase_uuid \n" +
                "        JOIN entries e \n" +
                "            ON e.uuid = pe.entry_uuid \n" +
                "    WHERE e.time < ? \n" +
                "   ),   \n" +
                "words_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT \n" +
                "       er.uuid         AS uuid, \n" +
                "       we.word_uuid    AS word_uuid, \n" +
                "       we.index        AS word_index, \n" +
                "       we.position     AS word_position \n" +
                "    FROM entries_results er \n" +
                "        JOIN words_in_entries we \n" +
                "            ON er.uuid = we.entry_uuid \n" +
                "    ) \n" +
                "SELECT DISTINCT \n" +
                "    er.uuid            AS e_uuid, \n" +
                "    er.string_origin   AS e_string_origin, \n" +
                "    er.string_lower    AS e_string_lower, \n" +
                "    er.time            AS e_time, \n" +
                "    er.user_uuid       AS e_user_uuid, \n" +
                "    l.uuid             AS l_uuid, \n" +
                "    l.name             AS l_name, \n" +
                "    l.time             AS l_time, \n" +
                "    l.user_uuid        AS l_user_uuid \n" +
                "FROM entries_results er \n" +
                "    LEFT JOIN labels_to_entries le \n" +
                "        ON er.uuid = le.entry_uuid \n" +
                "    LEFT JOIN labels l \n" +
                "        ON l.uuid = le.label_uuid \n" +
                "WHERE er.uuid NOT IN \n" +
                "   ( \n" +
                "    SELECT wr2.uuid \n" +
                "    FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "           ON \n" +
                "               wr1.word_uuid = wr2.word_uuid AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST' AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "   ) ");

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumAndAfterOrEqualTime = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "    ),\n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid, SUM(c.count) AS rate \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING rate >= ? \n" +
                "   ),\n" +
                "entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT e.* \n" +
                "    FROM chars_scan c \n" +
                "        JOIN phrases_in_entries pe \n" +
                "            ON c.phrase_uuid = pe.phrase_uuid \n" +
                "        JOIN entries e \n" +
                "            ON e.uuid = pe.entry_uuid \n" +
                "    WHERE e.time >= ? \n" +
                "   ),   \n" +
                "words_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT \n" +
                "       er.uuid         AS uuid, \n" +
                "       we.word_uuid    AS word_uuid, \n" +
                "       we.index        AS word_index, \n" +
                "       we.position     AS word_position \n" +
                "    FROM entries_results er \n" +
                "        JOIN words_in_entries we \n" +
                "            ON er.uuid = we.entry_uuid \n" +
                "    ) \n" +
                "SELECT DISTINCT \n" +
                "    er.uuid            AS e_uuid, \n" +
                "    er.string_origin   AS e_string_origin, \n" +
                "    er.string_lower    AS e_string_lower, \n" +
                "    er.time            AS e_time, \n" +
                "    er.user_uuid       AS e_user_uuid, \n" +
                "    l.uuid             AS l_uuid, \n" +
                "    l.name             AS l_name, \n" +
                "    l.time             AS l_time, \n" +
                "    l.user_uuid        AS l_user_uuid \n" +
                "FROM entries_results er \n" +
                "    LEFT JOIN labels_to_entries le \n" +
                "        ON er.uuid = le.entry_uuid \n" +
                "    LEFT JOIN labels l \n" +
                "        ON l.uuid = le.label_uuid \n" +
                "WHERE er.uuid NOT IN \n" +
                "   ( \n" +
                "    SELECT wr2.uuid \n" +
                "    FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "           ON \n" +
                "               wr1.word_uuid = wr2.word_uuid AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST' AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "   ) "); // done

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabels = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "   ),   \n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING SUM(c.count) >= ? \n" +
                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "      JOIN phrases_in_entries pe \n" +
                "         ON c.phrase_uuid = pe.phrase_uuid \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(le.label_uuid) = ? \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.index     AS word_index, \n" +
                "      we.position  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_results wr1 \n" +
                "      JOIN words_results wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.word_index = wr2.word_index AND \n" +
                "            wr1.word_position = 'LAST' AND \n" +
                "            wr2.word_position = 'MIDDLE' \n" +
                "   ) "); // done

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabelsAndBeforeTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "   ),   \n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING SUM(c.count) >= ? \n" +
                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "      JOIN phrases_in_entries pe \n" +
                "         ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e \n" +
                "           ON pe.entry_uuid = e.uuid \n" +
                "   WHERE e.time < ? \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(le.label_uuid) = ? \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.index     AS word_index, \n" +
                "      we.position  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "             ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "                wr1.word_index = wr2.word_index AND \n" +
                "                wr1.word_position = 'LAST' AND \n" +
                "                wr2.word_position = 'MIDDLE' \n" +
                "       ) "); // done

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabelsAndAfterOrEqualTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "   ),   \n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING SUM(c.count) >= ? \n" +
                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "      JOIN phrases_in_entries pe \n" +
                "         ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e \n" +
                "           ON pe.entry_uuid = e.uuid \n" +
                "   WHERE e.time >= ? \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   GROUP BY le.entry_uuid \n" +
                "   HAVING COUNT(le.label_uuid) = ? \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.index     AS word_index, \n" +
                "      we.position  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "             ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "                wr1.word_index = wr2.word_index AND \n" +
                "                wr1.word_position = 'LAST' AND \n" +
                "                wr2.word_position = 'MIDDLE' \n" +
                "       ) "); // done

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabels = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "   ),   \n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING SUM(c.count) >= ? \n" +
                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT DISTINCT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE e.uuid NOT IN ( \n" +
                "   SELECT wr2.entry_uuid \n" +
                "   FROM words_results wr1 \n" +
                "      JOIN words_results wr2 \n" +
                "         ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "            wr1.word_index = wr2.word_index AND \n" +
                "            wr1.word_position = 'LAST' AND \n" +
                "            wr2.word_position = 'MIDDLE' \n" +
                "   ) "); // done

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabelsAndBeforeTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "   ),   \n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING SUM(c.count) >= ? \n" +
                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e " +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time < ? \n " +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT DISTINCT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "            ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST' AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "       ) "); // done

        this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabelsAndAfterOrEqualTime = new StringCacheFor2RepeatedSeparatedPrefixSuffix(
                "WITH \n" +
                "chars_scan_raw \n" +
                "AS ( \n",

                "   /* repeated condition */ \n" +
                "       SELECT cp.phrase_uuid, 1 AS count \n" +
                "       FROM chars_in_phrases cp \n" +
                "       WHERE \n" +
                "           cp.ch = ? AND \n" +
                "           cp.qty >= ? AND \n" +
                "           cp.phrase_size >= ? AND \n" +
                "           cp.user_uuid >= ? \n",

                "   /* repeated separator */ \n" +
                "   UNION ALL \n",

                "   ), \n" +
                "chars_scan \n" +
                "AS ( \n" +
                "   SELECT c.phrase_uuid \n" +
                "   FROM chars_scan_raw c \n" +
                "   GROUP BY c.phrase_uuid \n" +
                "   HAVING SUM(c.count) >= ? \n" +
                "   ), \n" +
                "entries_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT pe.entry_uuid \n" +
                "   FROM chars_scan c \n" +
                "       JOIN phrases_in_entries pe \n" +
                "           ON c.phrase_uuid = pe.phrase_uuid \n" +
                "       JOIN entries e " +
                "           ON e.uuid = pe.entry_uuid \n" +
                "   WHERE e.time >= ? \n " +
                "   ), \n" +
                "labeled_entries \n" +
                "AS (" +
                "   SELECT DISTINCT le.entry_uuid " +
                "   FROM labels_to_entries le \n" +
                "   WHERE le.label_uuid IN ( \n",
                "       /* repeated */ ?",  " /* repeated */ , \n",
                "       ) \n" +
                "   ), \n" +
                "labeled_entries_results \n" +
                "AS ( \n" +
                "    SELECT DISTINCT er.entry_uuid \n" +
                "    FROM entries_results er \n" +
                "        JOIN labeled_entries ler \n" +
                "            ON er.entry_uuid = ler.entry_uuid \n" +
                "   ), \n" +
                "words_results \n" +
                "AS ( \n" +
                "   SELECT DISTINCT \n" +
                "      ler.entry_uuid, \n" +
                "      we.word_uuid, \n" +
                "      we.INDEX     AS word_index, \n" +
                "      we.POSITION  AS word_position \n" +
                "   FROM labeled_entries_results ler \n" +
                "      JOIN words_in_entries we \n" +
                "         ON ler.entry_uuid = we.entry_uuid \n" +
                "   ) \n" +
                "SELECT DISTINCT \n" +
                "   e.uuid          AS e_uuid, \n" +
                "   e.string_origin AS e_string_origin, \n" +
                "   e.string_lower  AS e_string_lower, \n" +
                "   e.time          AS e_time, \n" +
                "   e.user_uuid     AS e_user_uuid, \n" +
                "   l.uuid          AS l_uuid, \n" +
                "   l.name          AS l_name, \n" +
                "   l.time          AS l_time, \n" +
                "   l.user_uuid     AS l_user_uuid \n" +
                "FROM labeled_entries_results ler \n" +
                "   JOIN entries e \n" +
                "      ON ler.entry_uuid = e.uuid \n" +
                "   LEFT JOIN labels_to_entries le \n" +
                "      ON e.uuid = le.entry_uuid \n" +
                "   LEFT JOIN labels l \n" +
                "      ON l.uuid = le.label_uuid \n" +
                "WHERE \n" +
                "   e.uuid NOT IN ( \n" +
                "       SELECT wr2.entry_uuid \n" +
                "       FROM words_results wr1 \n" +
                "       JOIN words_results wr2 \n" +
                "            ON wr1.word_uuid = wr2.word_uuid AND \n" +
                "               wr1.word_index = wr2.word_index AND \n" +
                "               wr1.word_position = 'LAST' AND \n" +
                "               wr2.word_position = 'MIDDLE' \n" +
                "       ) ");

        this.searchProcessPool = new GuardedPool<>(SearchProcess::new);
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern) {
        try ( SearchProcess searching = this.searchProcessPool.give() ) {
            searching.fill(user, pattern);

            this.execute(searching);

            List<Entry> entries;
            if ( searching.hasResult() ) {
                entries = searching.result();
            }
            else {
                entries = emptyList();
            }

            return entries;
        }
    }

    @Override
    public List<Entry> findBy(
            User user,
            String pattern,
            Entry.Label.Matching matching,
            List<Entry.Label> labels) {
        try ( SearchProcess searching = this.searchProcessPool.give() ) {
            searching.fill(user, pattern, matching, labels);

            this.execute(searching);

            List<Entry> entries;
            if ( searching.hasResult() ) {
                entries = searching.result();
            }
            else {
                entries = emptyList();
            }

            return entries;
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
        try ( SearchProcess searching = this.searchProcessPool.give() ) {
            searching.fill(user, pattern, matching, labels, timeDirection, time);

            this.execute(searching);

            List<Entry> entries;
            if ( searching.hasResult() ) {
                entries = searching.result();
            }
            else {
                entries = emptyList();
            }

            return entries;
        }
    }

    private void execute(SearchProcess search) {
        if ( search.labels().isEmpty() ) {
            if ( search.hasTime() ) {
                if ( search.hasPatternLength(1) ) {
                    search.finishedEmpty();
                }
                else if ( search.hasPatternLength(2) ) {
                    searchInWordsBySubstringAndTime(search);

                    if ( search.hasNoResult() ) {

                    }
                }
                else {
                    searchInWordsAndPhrasesByFullCriteriaAndTime(search);

                    while ( search.hasNoResult() && search.ifCanDecreaseCriteria() ) {
                        search.decreaseCriteria();
                        searchInWordsAndPhrasesByDecreasedCriteriaAndTime(search);
                    }
                }
            }
            else {
                if ( search.hasPatternLength(1) ) {
                    search.finishedEmpty();
                }
                else if ( search.hasPatternLength(2) ) {
                    searchInWordsBySubstring(search);

                    if ( search.hasNoResult() ) {

                    }
                }
                else {
                    searchInWordsAndPhrasesByFullCriteria(search);

                    while ( search.hasNoResult() && search.ifCanDecreaseCriteria() ) {
                        search.decreaseCriteria();
                        searchInWordsAndPhrasesByDecreasedCriteria(search);
                    }
                }
            }
        }
        else {
            if ( search.hasTime() ) {
                if ( search.hasPatternLength(1) ) {
                    search.finishedEmpty();
                }
                else if ( search.hasPatternLength(2) ) {
                    searchInWordsBySubstringAndLabelsAndTime(search);

                    if ( search.hasNoResult() ) {

                    }
                }
                else {
                    searchInWordsAndPhrasesByFullCriteriaAndLabelsAndTime(search);

                    while ( search.hasNoResult() && search.ifCanDecreaseCriteria() ) {
                        search.decreaseCriteria();
                        searchInWordsAndPhrasesByDecreasedCriteriaAndLabelsAndTime(search);
                    }
                }
            }
            else {
                if ( search.hasPatternLength(1) ) {
                    search.finishedEmpty();
                }
                else if ( search.hasPatternLength(2) ) {
                    searchInWordsBySubstringAndLabels(search);

                    if ( search.hasNoResult() ) {

                    }
                }
                else {
                    searchInWordsAndPhrasesByFullCriteriaAndLabels(search);

                    while ( search.hasNoResult() && search.ifCanDecreaseCriteria() ) {
                        search.decreaseCriteria();
                        searchInWordsAndPhrasesByDecreasedCriteriaAndLabels(search);
                    }
                }
            }
        }
    }

    private void searchInWordsBySubstring(SearchProcess searching) {

    }

    private void searchInWordsAndPhrasesByFullCriteria(SearchProcess search) {
        if ( search.charsQueryArguments().isEmpty() ) {
            search.fillCharsArgumentsForPreciseQuery();
        }
        
        List<Object> queryArguments = search.charsQueryArguments();
        int repeatedClausesQty = search.charsQueryArgumentsClausesQty();

        String queryWords = this.sqlSelectFromWordsIntersect.getFor(repeatedClausesQty);
        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, queryArguments);

        List<Entry> foundEntries = this.findEntriesBy(words);

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases = this.sqlSelectFromPhrasesIntersect.getFor(repeatedClausesQty);
            foundEntries = this.findEntriesBy(queryByPhrases, queryArguments);
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsAndPhrasesByDecreasedCriteria(SearchProcess search) {
        int decreasedRate = search.pattern().length() - search.decreasedCriteriaSteps();

        List<Object> queryArguments = search.charsQueryArguments();

        if ( search.decreasedCriteriaSteps() == 1 ) {
            search.clearQueryArguments(); // clear argument filled for precise query
            search.fillCharsArgumentsForNonPreciseQuery();
            search.charsQueryArguments().add(decreasedRate);
        }
        else { // > 1
            int rateArgumentIndex = search.charsQueryArgumentsClausesQty() * 4;
            search.charsQueryArguments().set(rateArgumentIndex, decreasedRate); // change dercreased rate argument
        }
        
        int clausesQty = search.charsQueryArgumentsClausesQty();

        String queryWords = this.sqlSelectFromWordsUnionAllGroupByAndSum.getFor(clausesQty);

        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, queryArguments);
        List<Entry> foundEntries = this.findEntriesBy(words);

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSum.getFor(clausesQty);
            foundEntries = this.findEntriesBy(queryByPhrases, queryArguments);
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsBySubstringAndTime(SearchProcess search) {

    }

    private void searchInWordsAndPhrasesByFullCriteriaAndTime(SearchProcess search) {
        if ( search.charsQueryArguments().isEmpty() ) {
            search.fillCharsArgumentsForPreciseQuery();

            search.extendedArguments().add(search.time().value());
        }

        int clausesQty = search.charsQueryArgumentsClausesQty();
        String queryWords = this.sqlSelectFromWordsIntersect.getFor(clausesQty);
        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, search.charsQueryArguments());

        List<Entry> foundEntries = this.findEntriesBy(words, search.time());

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases;

            if ( search.time().direction().equalTo(BEFORE) ) {
                queryByPhrases = this.sqlSelectFromPhrasesIntersectAndBeforeTime.getFor(clausesQty);
            }
            else { // AFTER_OR_EQUAL
                queryByPhrases = this.sqlSelectFromPhrasesIntersectAndAfterOrEqualTime.getFor(clausesQty);
            }

            foundEntries = this.findEntriesBy(queryByPhrases, search.extendedArguments());
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsAndPhrasesByDecreasedCriteriaAndTime(SearchProcess search) {
        int decreasedRate = search.pattern().length() - search.decreasedCriteriaSteps();

        if ( search.decreasedCriteriaSteps() == 1 ) {
            search.clearQueryArguments(); // clear argument filled for precise query
            search.fillCharsArgumentsForNonPreciseQuery();
            search.charsQueryArguments().add(decreasedRate);
            search.extendedArguments().add(decreasedRate);

            search.extendedArguments().add(search.time().value());
        }
        else { // > 1
            int rateArgumentIndex = search.charsQueryArgumentsClausesQty() * 4;
            search.charsQueryArguments().set(rateArgumentIndex, decreasedRate); // change dercreased rate argument
            search.extendedArguments().set(rateArgumentIndex, decreasedRate); // change dercreased rate argument
        }

        int clausesQty = search.charsQueryArgumentsClausesQty();
        String queryWords = this.sqlSelectFromWordsUnionAllGroupByAndSum.getFor(clausesQty);
        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, search.charsQueryArguments());

        List<Entry> foundEntries = this.findEntriesBy(words, search.time());

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases;

            if ( search.time().direction().equalTo(BEFORE) ) {
                queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumAndBeforeTime.getFor(clausesQty);
            }
            else { // AFTER_OR_EQUAL
                queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumAndAfterOrEqualTime.getFor(clausesQty);
            }

            foundEntries = this.findEntriesBy(queryByPhrases, search.extendedArguments());
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsBySubstringAndLabels(SearchProcess search) {

    }

    private void searchInWordsAndPhrasesByFullCriteriaAndLabels(SearchProcess search) {
        if ( search.charsQueryArguments().isEmpty() ) {
            search.fillCharsArgumentsForPreciseQuery();

            for ( Entry.Label label : search.labels() ) {
                search.extendedArguments().add(label.uuid());
            }

            int labelsQty = search.labels().size();
            if ( search.labelsMatching().equalTo(ALL_OF) && (labelsQty > 1) ) {
                search.extendedArguments().add(labelsQty);
            }
        }

        int clausesQty = search.charsQueryArgumentsClausesQty();
        int labelsQty = search.labels().size();

        String queryWords = this.sqlSelectFromWordsIntersect.getFor(clausesQty);

        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, search.charsQueryArguments());
        List<Entry> foundEntries = this.findEntriesBy(words, search.labelsMatching(), search.labels());

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases;
            if ( search.labelsMatching().equalTo(ALL_OF) && labelsQty > 1 ) {
                queryByPhrases = this.sqlSelectFromPhrasesIntersectWithAllOfLabels.getFor(clausesQty, labelsQty);
            }
            else { // == labels matching is ANY_OF || labels.size == 1
                queryByPhrases = this.sqlSelectFromPhrasesIntersectWithAnyOfLabels.getFor(clausesQty, labelsQty);
            }

            foundEntries = this.findEntriesBy(queryByPhrases, search.extendedArguments());
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsAndPhrasesByDecreasedCriteriaAndLabels(SearchProcess search) {
        int decreasedRate = search.pattern().length() - search.decreasedCriteriaSteps();

        if ( search.decreasedCriteriaSteps() == 1 ) {
            search.clearQueryArguments(); // clear argument filled for precise query
            search.fillCharsArgumentsForNonPreciseQuery();
            search.charsQueryArguments().add(decreasedRate);
            search.extendedArguments().add(decreasedRate);

            for ( Entry.Label label : search.labels() ) {
                search.extendedArguments().add(label.uuid());
            }

            int labelsQty = search.labels().size();
            if ( search.labelsMatching().equalTo(ALL_OF) && (labelsQty > 1) ) {
                search.extendedArguments().add(labelsQty);
            }
        }
        else { // > 1
            int rateArgumentIndex = search.charsQueryArgumentsClausesQty() * 4;
            search.charsQueryArguments().set(rateArgumentIndex, decreasedRate); // change decreased rate argument
            search.extendedArguments().set(rateArgumentIndex, decreasedRate); // change decreased rate argument
        }

        int clausesQty = search.charsQueryArgumentsClausesQty();
        int labelsQty = search.labels().size();

        String queryWords = this.sqlSelectFromWordsUnionAllGroupByAndSum.getFor(clausesQty);

        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, search.charsQueryArguments());
        List<Entry> foundEntries = this.findEntriesBy(words, search.labelsMatching(), search.labels());

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases;
            if ( search.labelsMatching().equalTo(ALL_OF) && labelsQty > 1 ) {
                queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabels.getFor(clausesQty, labelsQty);
            }
            else { // == labels matching is ANY_OF || labels.size == 1
                queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabels.getFor(clausesQty, labelsQty);
            }

            foundEntries = this.findEntriesBy(queryByPhrases, search.extendedArguments());
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsBySubstringAndLabelsAndTime(SearchProcess search) {

    }

    private void searchInWordsAndPhrasesByFullCriteriaAndLabelsAndTime(SearchProcess search) {
        if ( search.charsQueryArguments().isEmpty() ) {
            search.fillCharsArgumentsForPreciseQuery();
            search.extendedArguments().add(search.time().value());

            for ( Entry.Label label : search.labels() ) {
                search.extendedArguments().add(label.uuid());
            }

            int labelsQty = search.labels().size();
            if ( search.labelsMatching().equalTo(ALL_OF) && (labelsQty > 1) ) {
                search.extendedArguments().add(labelsQty);
            }
        }

        int clausesQty = search.charsQueryArgumentsClausesQty();
        int labelsQty = search.labels().size();

        String queryWords = this.sqlSelectFromWordsIntersect.getFor(clausesQty);

        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, search.charsQueryArguments());
        List<Entry> foundEntries = this.findEntriesBy(words, search.labelsMatching(), search.labels(), search.time());

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases;
            if ( search.time().direction().equalTo(BEFORE) ) {
                if ( search.labelsMatching().equalTo(ALL_OF) && labelsQty > 1 ) {
                    queryByPhrases = this.sqlSelectFromPhrasesIntersectWithAllOfLabelsAndBeforeTime.getFor(clausesQty, labelsQty);
                }
                else { // == labels matching is ANY_OF || labels.size == 1
                    queryByPhrases = this.sqlSelectFromPhrasesIntersectWithAnyOfLabelsAndBeforeTime.getFor(clausesQty, labelsQty);
                }
            }
            else { // AFTER_OR_EQUAL
                if ( search.labelsMatching().equalTo(ALL_OF) && labelsQty > 1 ) {
                    queryByPhrases = this.sqlSelectFromPhrasesIntersectWithAllOfLabelsAndAfterOrEqualTime.getFor(clausesQty, labelsQty);
                }
                else { // == labels matching is ANY_OF || labels.size == 1
                    queryByPhrases = this.sqlSelectFromPhrasesIntersectWithAnyOfLabelsAndAfterOrEqualTime.getFor(clausesQty, labelsQty);
                }
            }

            foundEntries = this.findEntriesBy(queryByPhrases, search.extendedArguments());
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private void searchInWordsAndPhrasesByDecreasedCriteriaAndLabelsAndTime(SearchProcess search) {
        int decreasedRate = search.pattern().length() - search.decreasedCriteriaSteps();

        if ( search.decreasedCriteriaSteps() == 1 ) {
            search.clearQueryArguments(); // clear argument filled for precise query
            search.fillCharsArgumentsForNonPreciseQuery();
            search.charsQueryArguments().add(decreasedRate);
            search.extendedArguments().add(decreasedRate);

            search.extendedArguments().add(search.time().value());

            for ( Entry.Label label : search.labels() ) {
                search.extendedArguments().add(label.uuid());
            }

            int labelsQty = search.labels().size();
            if ( search.labelsMatching().equalTo(ALL_OF) && (labelsQty > 1) ) {
                search.extendedArguments().add(labelsQty);
            }
        }
        else { // > 1
            int rateArgumentIndex = search.charsQueryArgumentsClausesQty() * 4;
            search.charsQueryArguments().set(rateArgumentIndex, decreasedRate); // change dercreased rate argument
            search.extendedArguments().set(rateArgumentIndex, decreasedRate); // change dercreased rate argument
        }

        int clausesQty = search.charsQueryArgumentsClausesQty();
        int labelsQty = search.labels().size();

        String queryWords = this.sqlSelectFromWordsUnionAllGroupByAndSum.getFor(clausesQty);

        List<WordUuidString> words = this.findWordsSimilarToPatternBy(search.pattern(), queryWords, search.charsQueryArguments());
        List<Entry> foundEntries = this.findEntriesBy(words, search.labelsMatching(), search.labels(), search.time());

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases;
            if ( search.time().direction().equalTo(BEFORE) ) {
                if ( search.labelsMatching().equalTo(ALL_OF) && labelsQty > 1 ) {
                    queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabelsAndBeforeTime.getFor(clausesQty, labelsQty);
                }
                else { // == labels matching is ANY_OF || labels.size == 1
                    queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabelsAndBeforeTime.getFor(clausesQty, labelsQty);
                }
            }
            else { // AFTER_OR_EQUAL
                if ( search.labelsMatching().equalTo(ALL_OF) && labelsQty > 1 ) {
                    queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAllOfLabelsAndAfterOrEqualTime.getFor(clausesQty, labelsQty);
                }
                else { // == labels matching is ANY_OF || labels.size == 1
                    queryByPhrases = this.sqlSelectFromPhrasesUnionAllGroupByAndSumWithAnyOfLabelsAndAfterOrEqualTime.getFor(clausesQty, labelsQty);
                }
            }

            foundEntries = this.findEntriesBy(queryByPhrases, search.extendedArguments());
        }

        if ( ! foundEntries.isEmpty() ) {
            search.finishedWith(foundEntries);
        }
    }

    private List<Entry> findEntriesBy(String query, List<Object> args) {
        try (PooledRowCollectorForEntriesAndLabels rowCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction().doQuery(rowCollector.get(), query, args);
            return rowCollector.get().ones();
        }
    }

    private List<WordUuidString> findWordsSimilarToPatternBy(String pattern, String query, List<Object> args) {
        return super.currentTransaction()
                .doQueryAndStream(WordUuidString::new, query, args)
                .filter(word -> similarity.isSimilar(pattern, word.string()))
                .collect(toList());
    }

    private List<Entry> findEntriesBy(List<WordUuidString> words) {
        if ( words.isEmpty() ) {
            return emptyList();
        }

        String query = this.sqlSelectEntriesByWords.getFor(words);

        try (PooledRowCollectorForEntriesAndLabels rowCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction().doQuery(
                    rowCollector.get(),
                    query,
                    WordUuidString.uuidsOf(words));

            return rowCollector.get().ones();
        }
    }

    private List<Entry> findEntriesBy(List<WordUuidString> words, SearchProcess.Time time) {
        if ( words.isEmpty() ) {
            return emptyList();
        }

        String query;
        if ( time.direction().equalTo(BEFORE) ) {
            query = this.sqlSelectEntriesByWordsAndBeforeTime.getFor(words);
        }
        else { // AFTER_OR_EQUAL
            query = this.sqlSelectEntriesByWordsAndAfterOrEqualTime.getFor(words);
        }

        List args = WordUuidString.uuidsOf(words);

        args.add(time.value());

        try (PooledRowCollectorForEntriesAndLabels rowCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction().doQuery(
                    rowCollector.get(),
                    query,
                    args);

            return rowCollector.get().ones();
        }
    }

    private List<Entry> findEntriesBy(List<WordUuidString> words, Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( words.isEmpty() ) {
            return emptyList();
        }

        List args = WordUuidString.uuidsOf(words);

        String query;

        if ( labels.size() == 1 ) {
            query = this.sqlSelectEntriesByWordsAndLabel.getFor(words);
            args.add(labels.get(0).uuid());
        }
        else {
            if ( matching.equalTo(ALL_OF) ) {
                query = this.sqlSelectEntriesByWordsAndAllOfLabels.getFor(words, labels);
                for ( Entry.Label label : labels ) {
                    args.add(label.uuid());
                }
                args.add(labels.size());
            }
            else if ( matching.equalTo(ANY_OF) ) {
                query = this.sqlSelectEntriesByWordsAndAnyOfLabels.getFor(words, labels);
                for ( Entry.Label label : labels ) {
                    args.add(label.uuid());
                }
            }
            else {
                throw matching.unsupported();
            }
        }

        try (PooledRowCollectorForEntriesAndLabels rowCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction().doQuery(
                    rowCollector.get(),
                    query,
                    args);

            return rowCollector.get().ones();
        }
    }

    private List<Entry> findEntriesBy(
            List<WordUuidString> words, Entry.Label.Matching matching, List<Entry.Label> labels, SearchProcess.Time time) {
        if ( words.isEmpty() ) {
            return emptyList();
        }

        List args = WordUuidString.uuidsOf(words);

        String query;

        if ( time.direction().equalTo(BEFORE) ) {
            if ( labels.size() == 1 ) {
                query = this.sqlSelectEntriesByWordsAndLabelAndBeforeTime.getFor(words);
                args.add(labels.get(0).uuid());
            }
            else if ( matching.equalTo(ALL_OF) ) {
                query = this.sqlSelectEntriesByWordsAndAllOfLabelsAndBeforeTime.getFor(words, labels);
                for ( Entry.Label label : labels ) {
                    args.add(label.uuid());
                }
                args.add(labels.size());
            }
            else if ( matching.equalTo(ANY_OF) ) {
                query = this.sqlSelectEntriesByWordsAndAnyOfLabelsAndBeforeTime.getFor(words, labels);
                for ( Entry.Label label : labels ) {
                    args.add(label.uuid());
                }
            }
            else {
                throw matching.unsupported();
            }
        }
        else { // AFTER_OR_EQUAL
            if ( labels.size() == 1 ) {
                query = this.sqlSelectEntriesByWordsAndLabelAndAfterOrEqualTime.getFor(words);
                args.add(labels.get(0).uuid());
            }
            else if ( matching.equalTo(ALL_OF) ) {
                query = this.sqlSelectEntriesByWordsAndAllOfLabelsAndAfterOrEqualTime.getFor(words, labels);
                for ( Entry.Label label : labels ) {
                    args.add(label.uuid());
                }
                args.add(labels.size());
            }
            else if ( matching.equalTo(ANY_OF) ) {
                query = this.sqlSelectEntriesByWordsAndAnyOfLabelsAndAfterOrEqualTime.getFor(words, labels);
                for ( Entry.Label label : labels ) {
                    args.add(label.uuid());
                }
            }
            else {
                throw matching.unsupported();
            }
        }

        args.add(time.value());

        try (PooledRowCollectorForEntriesAndLabels rowCollector = this.rowCollectorsPool.give()) {
            super.currentTransaction().doQuery(
                    rowCollector.get(),
                    query,
                    args);

            return rowCollector.get().ones();
        }
    }

}
