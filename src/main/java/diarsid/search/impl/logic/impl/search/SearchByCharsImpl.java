package diarsid.search.impl.logic.impl.search;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.api.search.SearchByChars;
import diarsid.search.impl.logic.impl.jdbc.EntriesAndLabelsRowCollector;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.support.objects.GuardedPool;
import diarsid.support.strings.CharactersCount;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

public class SearchByCharsImpl extends ThreadTransactional implements SearchByChars {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromWordsIntersect;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesIntersect;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromWordsUnionAllGroupByAndSum;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectFromPhrasesUnionAllGroupByAndSum;
    private final GuardedPool<SearchProcess> searchProcessPool;

    public SearchByCharsImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);

        this.sqlSelectFromWordsIntersect = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH " +
                "chars_scan " +
                "AS ( ",
                "/* repeated condition */   SELECT cw.word_uuid " +
                "/* repeated condition */   FROM chars_in_words cw " +
                "/* repeated condition */   WHERE " +
                "/* repeated condition */       cw.ch = ? AND " +
                "/* repeated condition */       cw.qty >= ? AND " +
                "/* repeated condition */       cw.word_size >= ? AND " +
                "/* repeated condition */       cw.user_uuid = ? ",
                "/* repeated separator */           INTERSECT ",
                "   ) " +
                "SELECT DISTINCT " +
                "   e.uuid AS e_uuid, " +
                "   e.string_origin AS e_string_origin, " +
                "   e.string_lower AS e_string_lower, " +
                "   e.time AS e_time, " +
                "   e.user_uuid AS e_user_uuid, " +
                "   l.uuid AS l_uuid, " +
                "   l.user_uuid AS l_user_uuid, " +
                "   l.time AS l_time, " +
                "   l.name AS l_name " +
                "FROM chars_scan c " +
                "   JOIN words_in_entries we " +
                "       ON c.word_uuid = we.word_uuid " +
                "   JOIN entries e " +
                "       ON we.entry_uuid = e.uuid " +
                "   LEFT JOIN labels_to_entries le " +
                "       ON e.uuid = le.entry_uuid " +
                "   LEFT JOIN labels l " +
                "       ON le.label_uuid = l.uuid ");

        this.sqlSelectFromPhrasesIntersect = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH " +
                "chars_scan " +
                "AS ( ",
                "/* repeated condition */    SELECT cp.phrase_uuid " +
                "/* repeated condition */    FROM chars_in_phrases cp " +
                "/* repeated condition */    WHERE " +
                "/* repeated condition */       cp.ch = ? AND " +
                "/* repeated condition */       cp.qty >= ? AND " +
                "/* repeated condition */       cp.phrase_size >= ? AND " +
                "/* repeated condition */       cp.user_uuid >= ? ",
                "/* repeated separator */           INTERSECT ",
                "   ),   " +
                "entities_results " +
                "AS ( " +
                "    SELECT DISTINCT e.*, c.phrase_uuid AS phrase_uuid " +
                "    FROM chars_scan c " +
                "        JOIN phrases_in_entries pe " +
                "            ON c.phrase_uuid = pe.phrase_uuid " +
                "        JOIN entries e " +
                "            ON e.uuid = pe.entry_uuid " +
                "),   " +
                "words_results " +
                "AS ( " +
                "    SELECT DISTINCT " +
                "       er.uuid, " +
                "       we.word_uuid    AS word_uuid, " +
                "       we.index        AS word_index, " +
                "       we.position     AS word_position " +
                "    FROM entities_results er " +
                "        JOIN words_in_phrases wp " +
                "            ON wp.phrase_uuid = er.phrase_uuid " +
                "        JOIN words_in_entries we " +
                "            ON we.word_uuid = wp.word_uuid AND " +
                "               er.uuid = we.entry_uuid " +
                "    ) " +
                "SELECT DISTINCT " +
                "    er.uuid            AS e_uuid, " +
                "    er.string_origin   AS e_string_origin, " +
                "    er.string_lower    AS e_string_lower, " +
                "    er.time            AS e_time, " +
                "    er.user_uuid       AS e_user_uuid, " +
                "    l.uuid             AS l_uuid, " +
                "    l.name             AS l_name, " +
                "    l.time             AS l_time, " +
                "    l.user_uuid        AS l_user_uuid " +
                "FROM entities_results er " +
                "    LEFT JOIN labels_to_entries le " +
                "        ON er.uuid = le.entry_uuid " +
                "    LEFT JOIN labels l " +
                "        ON l.uuid = le.label_uuid " +
                "WHERE er.uuid NOT IN ( " +
                "    SELECT wr2.uuid " +
                "    FROM words_results wr1 " +
                "    JOIN words_results wr2 " +
                "    ON wr1.word_uuid = wr2.word_uuid AND " +
                "       wr1.word_index = wr2.word_index AND " +
                "       wr1.word_position = 'LAST' AND " +
                "       wr2.word_position = 'MIDDLE' " +
                ")");

        this.sqlSelectFromWordsUnionAllGroupByAndSum = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH chars AS " +
                "   (",
                "   SELECT cw.word_uuid, 1 AS count  " +
                "   FROM chars_in_words cw  " +
                "   WHERE " +
                "       cw.ch = ? AND " +
                "       cw.qty >= ? AND " +
                "       cw.word_size >= ? AND " +
                "       cw.user_uuid = ? ",
                "           UNION ALL ",
                "   ) " +
                "SELECT c.word_uuid AS word_uuid, SUM(c.count) AS rate " +
                "FROM chars c  " +
                "GROUP BY c.word_uuid " +
                "HAVING rate >= ? ");

        this.sqlSelectFromPhrasesUnionAllGroupByAndSum = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "WITH chars AS " +
                "   (",
                "   SELECT cp.word_uuid, 1 AS count  " +
                "   FROM chars_in_phrases cp  " +
                "   WHERE " +
                "       cp.ch = ? AND " +
                "       cp.qty >= ? AND " +
                "       cp.phrase_size >= ? AND " +
                "       cp.user_uuid = ? ",
                "           UNION ALL ",
                "   ) " +
                "SELECT c.phrase_uuid AS phrase_uuid, SUM(c.count) AS rate " +
                "FROM chars c  " +
                "GROUP BY c.phrase_uuid " +
                "HAVING rate >= ? ");

        this.searchProcessPool = new GuardedPool<>(SearchProcess::new);
    }

    @Override
    public List<Entry> findByChars(
            User user, String pattern, List<Entry.Label> labels) {
        try ( SearchProcess searching = this.searchProcessPool.give() ) {
            searching.fill(user, pattern, labels);
            this.execute(searching);
            return searching.result();
        }
    }

    @Override
    public List<Entry> findByChars(
            User user, String pattern, List<Entry.Label> labels, TimeDirection timeDirection, LocalDateTime time) {
        try ( SearchProcess searching = this.searchProcessPool.give() ) {
            searching.fill(user, pattern, labels, timeDirection, time);
            this.execute(searching);
            return searching.result();
        }
    }

    private void execute(SearchProcess search) {
        if ( search.hasPatternLength(1) ) {
            search.finishedEmpty();
        }
        else if ( search.hasPatternLength(2) ) {
            searchInWordsBySubstring(search);

            if ( search.hasNoResult() ) {

            }
        }
        else {
            searchInWordsAndPhrases(search);

            if ( search.hasNoResult() ) {
                while ( search.hasNoResult() && search.ifCanDecreaseWordsCriteria() && ! search.isReasonableSearchInPhrases() ) {
                    search.decreaseWordsCriteria();
                    searchInWordsAndPhrases(search);
                }
            }

            if ( search.hasNoResult() ) {
                searchInPhrases(search);

                while ( search.hasNoResult() && search.ifCanDecreasePhrasesCriteria() && ! search.isReasonableSearchInEntries() ) {
                    search.decreasePhrasesCriteria();
                    searchInPhrases(search);
                }
            }
        }

    }

    private void searchInWordsBySubstring(SearchProcess searching) {

    }

    private void searchInWordsAndPhrases(SearchProcess search) {
        List<Object> args = new ArrayList<>();

        CharactersCount.Result countResult = search.charactersCount().result();

        countResult.occurrences().forEach(occurrence -> {
            args.add(occurrence.character());
            args.add(occurrence.count());
            args.add(search.pattern().length());
            args.add(search.user().uuid());
        });

        String queryByWords = this.sqlSelectFromWordsIntersect.getFor(countResult.occurrences());

        List<Entry> foundEntries = this.findEntriesBy(queryByWords, args);

        if ( foundEntries.isEmpty() ) {
            String queryByPhrases = this.sqlSelectFromPhrasesIntersect.getFor(countResult.occurrences());
            foundEntries = this.findEntriesBy(queryByPhrases, args);
        }

        search.finishedWith(foundEntries);
    }

    private void searchInPhrases(SearchProcess searching) {

    }

    private boolean isToPreferDecreaseCriteriaOverPhrases() {
        return false;
    }

    private List<Entry> findEntriesBy(String query, List<Object> args) {
        EntriesAndLabelsRowCollector rowCollector = new EntriesAndLabelsRowCollector("e_", "l_");

        super.currentTransaction().doQuery(rowCollector, query, args);

        return rowCollector.ones();
    }
}
