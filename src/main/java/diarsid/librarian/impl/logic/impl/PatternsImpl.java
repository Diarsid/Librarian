package diarsid.librarian.impl.logic.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.rows.RowGetter;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.api.Patterns;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealPattern;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static diarsid.librarian.api.model.Entry.Label.Matching.NONE_OF;
import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.model.Unique.uuidsOf;

public class PatternsImpl extends ThreadBoundTransactional implements Patterns {

    private final RowGetter<Pattern> rowToPattern;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectPatternByAnyOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectPatternByAllOfLabels;
    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectPatternByNoneOfLabels;

    public PatternsImpl(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);
        this.rowToPattern = RealPattern::new;

        this.sqlSelectPatternByAnyOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT p.* \n" +
                "FROM patterns p \n" +
                "    JOIN patterns_to_entries pe \n" +
                "        ON pe.pattern_uuid = p.uuid \n" +
                "WHERE \n" +
                "    p.string = ? AND \n" +
                "    p.user_uuid = ? AND \n" +
                "    pe.entry_uuid IN ( \n" +
                "        SELECT entry_uuid \n" +
                "        FROM labels_to_entries \n" +
                "        WHERE label_uuid IN ( ",
                "            ?", ", \n",
                "        ) \n" +
                "    ) "
        );

        this.sqlSelectPatternByAllOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT p.* \n" +
                "FROM patterns p \n" +
                "    JOIN patterns_to_entries pe \n" +
                "        ON pe.pattern_uuid = p.uuid \n" +
                "WHERE \n" +
                "    p.string = ? AND \n" +
                "    p.user_uuid = ? AND \n" +
                "    pe.entry_uuid IN ( \n" +
                "        SELECT entry_uuid \n" +
                "        FROM labels_to_entries \n" +
                "        WHERE label_uuid IN ( ",
                "            ?", ", \n",
                "        ) \n" +
                "        GROUP BY entry_uuid \n" +
                "        HAVING COUNT(label_uuid) = ? \n" +
                "    ) "
        );

        this.sqlSelectPatternByNoneOfLabels = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT p.* \n" +
                "FROM patterns p \n" +
                "    JOIN patterns_to_entries pe \n" +
                "        ON pe.pattern_uuid = p.uuid \n" +
                "WHERE \n" +
                "    p.string = ? AND \n" +
                "    p.user_uuid = ? AND \n" +
                "    pe.entry_uuid NOT IN ( \n" +
                "        SELECT entry_uuid \n" +
                "        FROM labels_to_entries \n" +
                "        WHERE label_uuid IN ( ",
                "            ?", ", \n",
                "        ) \n" +
                "    ) "
        );
    }

    @Override
    public Optional<Pattern> findBy(User user, String pattern) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        "SELECT * \n" +
                        "FROM patterns \n" +
                        "WHERE \n" +
                        "   string = ? AND \n" +
                        "   user_uuid = ? ",
                        pattern.trim().toLowerCase(), user.uuid());
    }

    @Override
    public Optional<Pattern> findBy(User user, String pattern, Entry.Label.Matching matching, List<Entry.Label> labels) {
        if ( labels.isEmpty() ) {
            return this.findBy(user, pattern);
        }
        else if ( labels.size() == 1 ) {
            Entry.Label label = labels.get(0);
            if ( matching.equalTo(NONE_OF) ) {
                return this.findByNotLabel(user, pattern, label);
            }
            else {
                return this.findByLabel(user, pattern, label);
            }
        }
        else {
            switch ( matching ) {
                case ANY_OF: return this.findByAnyOfLabels(user, pattern, labels);
                case ALL_OF: return this.findByAllOfLabels(user, pattern, labels);
                case NONE_OF: return this.findByNoneOfLabels(user, pattern, labels);
                default: throw matching.unsupported();
            }
        }
    }

    private Optional<Pattern> findByLabel(User user, String pattern, Entry.Label label) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        "SELECT p.* \n" +
                        "FROM patterns p \n" +
                        "    JOIN patterns_to_entries pe \n" +
                        "        ON pe.pattern_uuid = p.uuid \n" +
                        "    JOIN labels_to_entries le \n" +
                        "        ON le.entry_uuid = pe.entry_uuid \n" +
                        "WHERE \n" +
                        "    p.string = ? AND \n" +
                        "    p.user_uuid = ? AND \n" +
                        "    le.label_uuid = ? ",
                        pattern.trim().toLowerCase(), user.uuid(), label.uuid());
    }

    private Optional<Pattern> findByNotLabel(User user, String pattern, Entry.Label label) {
        UUID userUuid = user.uuid();
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        "WITH not_labeled_entries AS (" +
                        "    SELECT uuid AS entry_uuid \n" +
                        "    FROM entries \n" +
                        "    WHERE uuid NOT IN ( \n" +
                        "    	 SELECT entry_uuid \n" +
                        "        FROM labels_to_entries \n" +
                        "        WHERE label_uuid = ? \n" +
                        "    ) \n" +
                        "    AND \n" +
                        "    user_uuid = ? \n" +
                        ") \n" +
                        "SELECT p.* \n" +
                        "FROM patterns p \n" +
                        "    JOIN patterns_to_entries pe \n" +
                        "        ON pe.pattern_uuid = p.uuid \n" +
                        "WHERE \n" +
                        "    p.string = ? AND \n" +
                        "    p.user_uuid = ? AND \n" +
                        "    pe.entry_uuid NOT IN ( \n" +
                        "        SELECT entry_uuid \n" +
                        "        FROM not_labeled_entries \n" +
                        "        ) ",
                        label.uuid(), userUuid, pattern.trim().toLowerCase(), userUuid);
    }

    private Optional<Pattern> findByAnyOfLabels(User user, String pattern, List<Entry.Label> labels) {
        UUID userUuid = user.uuid();
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        this.sqlSelectPatternByAnyOfLabels.getFor(labels),
                        pattern.trim().toLowerCase(), userUuid, uuidsOf(labels));
    }

    private Optional<Pattern> findByAllOfLabels(User user, String pattern, List<Entry.Label> labels) {
        UUID userUuid = user.uuid();
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        this.sqlSelectPatternByAllOfLabels.getFor(labels),
                        pattern.trim().toLowerCase(), userUuid, uuidsOf(labels), labels.size());
    }

    private Optional<Pattern> findByNoneOfLabels(User user, String pattern, List<Entry.Label> labels) {
        UUID userUuid = user.uuid();
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        this.rowToPattern,
                        this.sqlSelectPatternByNoneOfLabels.getFor(labels),
                        pattern.trim().toLowerCase(), userUuid, uuidsOf(labels));
    }

    @Override
    public Pattern save(User user, String patternString) {
        UUID userUuid = user.uuid();

        boolean notExist = super.currentTransaction()
                .countQueryResults(
                        "SELECT * \n" +
                        "FROM patterns \n" +
                        "WHERE \n" +
                        "   user_uuid = ? AND \n" +
                        "   string = ?",
                        userUuid, patternString)
                == 0;

        if ( ! notExist ) {
            throw new IllegalArgumentException();
        }

        RealPattern pattern = new RealPattern(super.nextRandomUuid(), patternString, userUuid);

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO patterns (uuid, time, user_uuid, string) \n" +
                        "VALUES (?, ?, ?, ?) ",
                        pattern.uuid(), pattern.createdAt(), userUuid, pattern.string());

        if ( inserted == 1 ) {
            pattern.setState(STORED);
            return pattern;
        }
        else {
            throw new IllegalStateException();
        }
    }
}
