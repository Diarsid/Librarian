package diarsid.search.impl;

import java.util.Optional;

import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;
import diarsid.search.impl.api.internal.Choices;
import diarsid.search.impl.model.RealPatternToEntryChoice;
import diarsid.jdbc.JdbcTransactionThreadBindings;

import static diarsid.search.api.model.Storable.State.STORED;
import static diarsid.search.api.model.Storable.checkMustBeStored;

public class ChoicesImpl extends ThreadTransactional implements Choices {

    public ChoicesImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public PatternToEntryChoice save(PatternToEntry newRelation) {
        checkMustBeStored(newRelation);

        RealPatternToEntryChoice choice = new RealPatternToEntryChoice(newRelation);

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO choices (uuid, time, time_actual, relation_uuid) " +
                        "VALUES (?, ?, ?) ",
                        choice.uuid(),
                        choice.time(),
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

        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        RealPatternToEntryChoice::new,
                        "SELECT * " +
                        "FROM choices " +
                        "   JOIN patterns_to_entries relations " +
                        "       ON choices.relation_uuid = relations.uuid" +
                        "   JOIN entries " +
                        "       ON relations.entry_uuid = entries.uuid " +
                        "   JOIN patterns " +
                        "       ON relations.pattern_uuid = pattern.uuid " +
                        "WHERE patterns.uuid = ?",
                        pattern.uuid());
    }

    @Override
    public PatternToEntryChoice replace(PatternToEntryChoice oldChoice, PatternToEntry newRelation) {
        checkMustBeStored(oldChoice);
        checkMustBeStored(newRelation);

        if ( oldChoice.patternToEntry().entry().doesNotHaveUserUuid(newRelation.entry().userUuid()) ) {
            throw new IllegalArgumentException();
        }

        int found = super.currentTransaction()
                .countQueryResults(
                        "SELECT * " +
                        "FROM choices " +
                        "WHERE uuid = ?",
                        oldChoice.uuid());

        if ( found != 1 ) {
            throw new NotFoundException();
        }

        PatternToEntryChoice newChoice = oldChoice.changeTo(newRelation);

        int updated = super.currentTransaction()
                .doUpdate(
                        "UPDATE choices " +
                        "SET " +
                        "   uuid = ? " +
                        "   time_actual = ? " +
                        "   relation_uuid = ?, " +
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
                        "DELETE FROM choices " +
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
                        "DELETE FROM choices " +
                        "   JOIN patterns_to_entries relations " +
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
                        "UPDATE choices " +
                        "SET time_actual = ? " +
                        "WHERE uuid = ?",
                        choice.actualTime(), choice.uuid());

        if ( updated != 1 ) {
            throw new IllegalStateException();
        }

        choice.setState(STORED);
    }
}
