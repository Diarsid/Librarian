package diarsid.search.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.api.internal.EntriesByChars;
import diarsid.search.impl.model.RealEntry;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.support.strings.CharactersCount;

import static java.util.stream.Collectors.toList;

public class EntriesByCharsImpl extends ThreadTransactional implements EntriesByChars {
    
    private static final String SELECT = 
            "SELECT DISTINCT * " +
            "FROM entries e " +
            "   JOIN characters_in_entries ce " +
            "       ON ce.entry_uuid = e.uuid " +
            "WHERE " +
            "   :cases = ? ";

    private static final String SELECT_AFTER_TIME =
            "SELECT DISTINCT * " +
            "FROM entries e " +
            "   JOIN characters_in_entries ce " +
            "       ON ce.entry_uuid = e.uuid " +
            "WHERE " +
            "   time >= ? AND " +
            "   :cases = ? ";

    private static final String SELECT_BEFORE_TIME =
            "SELECT DISTINCT * " +
            "FROM entries e " +
            "   JOIN characters_in_entries ce " +
            "       ON ce.entry_uuid = e.uuid " +
            "WHERE " +
            "   time >= ? AND " +
            "   :cases = ? ";

    private static final String CASE_PLUS =
            "CASE " +
            "   WHEN ( " +
            "       ce.character IS ? AND " +
            "       ce.character_count >= ? ) " +
            "   THEN 1 " +
            "   ELSE 0 " +
            "END + ";

    private static final String CASE =
            "CASE " +
            "   WHEN ( " +
            "       ce.character IS ? AND " +
            "       ce.character_count >= ? ) " +
            "   THEN 1 " +
            "   ELSE 0 " +
            "END ";

    public EntriesByCharsImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    private List<Entry> queryEntries(String select, List<Object> args) {
        return super.currentTransaction()
                .doQueryAndStream(
                        row -> new RealEntry("entries.", row),
                        select,
                        args)
                .collect(toList());
    }

    private List<Entry> queryEntriesInLoop(String select, List<Object> args) {
        List<Entry> entries;
        entries = this.queryEntries(select, args);

        boolean countReduced = reduceCountIn(args);
        while ( entries.isEmpty() && countReduced ) {
            entries = this.queryEntries(select, args);

            if ( entries.isEmpty() ) {
                countReduced = reduceCountIn(args);
            }
        }

        return entries;
    }

    private static List<Object> generateQueryArgsFor(String pattern) {
        CharactersCount count = new CharactersCount();
        count.calculateIn(pattern);

        List<Object> args = new ArrayList<>();

        count.forEach((c, qty) -> {
            for (int i = qty; i > 0; i--) {
                args.add(c);
                args.add(i);
            }
        });

        args.add(count.uniqueCharsQty());

        return args;
    }

    private static List<Object> generateQueryArgsWithTimeFor(String pattern, LocalDateTime time) {
        List<Object> args = generateQueryArgsFor(pattern);
        args.add(args.size() - 2, time);

        return args;
    }

    private static boolean reduceCountIn(List<Object> args) {
        int countArgIndex = args.size() - 1;
        int countArg = (int) args.get(countArgIndex);

        if ( countArg < 3 ) {
            return false;
        }
        else {
            countArg--;
            args.set(countArgIndex, countArg);
            return true;
        }
    }

    @Override
    public List<Entry> findByChars(User user, String pattern) {
        String cases = CASE_PLUS.repeat(pattern.length() - 1) + CASE;
        String select = SELECT.replace(":cases", cases);

        List<Object> args = generateQueryArgsFor(pattern);

        return this.queryEntriesInLoop(select, args);
    }

    @Override
    public List<Entry> findByCharsAfterOrEqualTime(User user, String pattern, LocalDateTime time) {
        String cases = CASE_PLUS.repeat(pattern.length() - 1) + CASE;
        String select = SELECT_AFTER_TIME.replace(":cases", cases);

        List<Object> args = generateQueryArgsWithTimeFor(pattern, time);

        return this.queryEntriesInLoop(select, args);
    }

    @Override
    public List<Entry> findByCharsBeforeTime(User user, String pattern, LocalDateTime time) {
        String cases = CASE_PLUS.repeat(pattern.length() - 1) + CASE;
        String select = SELECT_BEFORE_TIME.replace(":cases", cases);

        List<Object> args = generateQueryArgsWithTimeFor(pattern, time);

        return this.queryEntriesInLoop(select, args);
    }
}
