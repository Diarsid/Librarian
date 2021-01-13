package diarsid.search.impl.logic.impl;

import java.util.Optional;

import diarsid.jdbc.api.Jdbc;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.Pattern;
import diarsid.search.api.model.PatternToEntry;
import diarsid.search.api.model.PatternToEntryChoice;
import diarsid.search.impl.logic.api.Choices;
import diarsid.search.impl.logic.impl.jdbc.RowCollectorForPatternToEntryChoice;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.RealPatternToEntryChoice;
import diarsid.support.objects.GuardedPool;

import static diarsid.search.api.model.meta.Storable.State.STORED;
import static diarsid.search.api.model.meta.Storable.checkMustBeStored;

public class ChoicesImpl extends ThreadBoundTransactional implements Choices {

    private final GuardedPool<RowCollectorForPatternToEntryChoice> rowCollectors;

    public ChoicesImpl(Jdbc jdbc, GuardedPool<RowCollectorForPatternToEntryChoice> rowCollectors) {
        super(jdbc);
        this.rowCollectors = rowCollectors;
    }

    @Override
    public PatternToEntryChoice save(PatternToEntry newRelation) {
        checkMustBeStored(newRelation);

        RealPatternToEntryChoice choice = new RealPatternToEntryChoice(newRelation);

        int inserted = super.currentTransaction()
                .doUpdate(
                        "INSERT INTO choices (uuid, time, time_actual, relation_uuid) \n" +
                        "VALUES (?, ?, ?) \n",
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

        try (RowCollectorForPatternToEntryChoice rowCollector = this.rowCollectors.give()) {
            super.currentTransaction()
                    .doQuery(
                            rowCollector,
                            "SELECT * \n" +
                            "FROM choices c \n" +
                            "   JOIN patterns_to_entries pe \n" +
                            "       ON c.relation_uuid = pe.uuid \n" +
                            "   JOIN entries e \n" +
                            "       ON pe.entry_uuid = e.uuid \n" +
                            "   JOIN patterns p \n" +
                            "       ON pe.pattern_uuid = p.uuid \n" +
                            "   LEFT JOIN labels_to_entries le \n" +
                            "       ON le.entry_uuid = e.uuid \n" +
                            "   LEFT JOIN labels l \n" +
                            "       ON le.label_uuid = l.uuid \n" +
                            "WHERE p.uuid = ? ",
                            pattern.uuid());

            return rowCollector.patternToEntryChoice();
        }
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
