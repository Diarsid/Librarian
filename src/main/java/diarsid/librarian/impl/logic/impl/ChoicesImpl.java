package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.exceptions.NotFoundException;
import diarsid.librarian.api.model.Entry;
import diarsid.librarian.api.model.Pattern;
import diarsid.librarian.api.model.PatternToEntry;
import diarsid.librarian.api.model.PatternToEntryChoice;
import diarsid.librarian.impl.logic.api.Choices;
import diarsid.librarian.impl.logic.api.UuidSupplier;
import diarsid.librarian.impl.logic.impl.jdbc.ThreadBoundTransactional;
import diarsid.librarian.impl.model.RealPatternToEntryChoice;

import static java.time.LocalDateTime.now;

import static diarsid.support.model.Storable.State.STORED;
import static diarsid.support.model.Storable.checkMustBeStored;

public class ChoicesImpl extends ThreadBoundTransactional implements Choices {

    public ChoicesImpl(Jdbc jdbc, UuidSupplier uuidSupplier) {
        super(jdbc, uuidSupplier);
    }

    @Override
    public PatternToEntryChoice save(PatternToEntry newRelation) {
        checkMustBeStored(newRelation);

        RealPatternToEntryChoice choice = new RealPatternToEntryChoice(super.nextRandomUuid(), newRelation);

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO choices (uuid, time, time_actual, relation_uuid) \n" +
                        "VALUES (?, ?, ?) \n",
                        choice.uuid(),
                        choice.createdAt(),
                        choice.patternToEntry().uuid());

        if ( inserted != 1 ) {
            throw new IllegalStateException();
        }

        choice.setState(STORED);

        return choice;
    }

    @Override
    public Optional<PatternToEntryChoice> findBy(Pattern pattern) {
        checkMustBeStored(pattern);
        LocalDateTime entryActualAt = now();

        Optional<PatternToEntryChoice> choice = super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        row -> new RealPatternToEntryChoice(row, "c_", "p_", "e_", "pe_", entryActualAt),
                        "SELECT \n" +
                        "   p.uuid          AS p_uuid, \n" +
                        "   p.string        AS p_string, \n" +
                        "   p.time          AS p_time, \n" +
                        "   p.user_uuid     AS p_user_uuid, \n" +
                        "   e.uuid          AS e_uuid, \n" +
                        "   e.time          AS e_time, \n" +
                        "   e.user_uuid     AS e_user_uuid, \n" +
                        "   e.string_origin AS e_string_origin \n" +
                        "   e.string_lower  AS e_string_lower \n" +
                        "   pe.uuid         AS pe_uuid, \n" +
                        "   pe.time         AS pe_time, \n" +
                        "   pe.algorithm    AS pe_algorithm, \n" +
                        "   pe.weight       AS pe_weight \n" +
                        "FROM choices c \n" +
                        "   JOIN patterns_to_entries pe \n" +
                        "       ON c.relation_uuid = pe.uuid \n" +
                        "   JOIN entries e \n" +
                        "       ON pe.entry_uuid = e.uuid \n" +
                        "   JOIN patterns p \n" +
                        "       ON pe.pattern_uuid = p.uuid \n" +
                        "WHERE p.uuid = ? ",
                        pattern.uuid());

        return choice;
    }

    @Override
    public PatternToEntryChoice replace(PatternToEntryChoice oldChoice, PatternToEntry newRelation) {
        checkMustBeStored(oldChoice);
        checkMustBeStored(newRelation);

        if ( oldChoice.patternToEntry().entry().doesNotHaveUserUuid(newRelation.userUuid()) ) {
            throw new IllegalArgumentException();
        }

        int found = super.currentTransaction()
                .countQueryResults(
                        "SELECT * \n" +
                        "FROM choices \n" +
                        "WHERE uuid = ? ",
                        oldChoice.uuid());

        if ( found != 1 ) {
            throw new NotFoundException();
        }

        PatternToEntryChoice newChoice = oldChoice.changeTo(newRelation);

        int updated = super.currentTransaction()
                .doUpdate(
                        "UPDATE choices \n" +
                        "SET \n" +
                        "   uuid = ? \n" +
                        "   time_actual = ? \n" +
                        "   relation_uuid = ?, \n" +
                        "WHERE uuid = ?",
                        newChoice.uuid(), newChoice.actualTime(), newRelation.uuid(), oldChoice.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        newChoice.setState(STORED);

        return newChoice;
    }

    @Override
    public void remove(PatternToEntryChoice oldChoice) {
        checkMustBeStored(oldChoice);

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM choices \n" +
                        "WHERE uuid = ?",
                        oldChoice.uuid());

        if ( removed == 0 ) {
            throw new NotFoundException();
        }

        if ( removed > 1 ) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void removeAllBy(Entry entry) {
        checkMustBeStored(entry);

        int removed = super.currentTransaction()
                .doUpdate(
                        "DELETE FROM choices \n" +
                        "   JOIN patterns_to_entries relations \n" +
                        "       ON choices.relation_uuid = relations.uuid" +
                        "WHERE relations.entry_uuid = ? ",
                        entry.uuid());
    }

    @Override
    public void assertActual(PatternToEntryChoice choice) {
        checkMustBeStored(choice);

        choice.actualize();

        int updated = super.currentTransaction()
                .doUpdate(
                        "UPDATE choices \n" +
                        "SET time_actual = ? \n" +
                        "WHERE uuid = ?",
                        choice.actualTime(), choice.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        choice.setState(STORED);
    }
}
