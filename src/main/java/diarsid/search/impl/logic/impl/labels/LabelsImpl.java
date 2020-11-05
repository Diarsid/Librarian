package diarsid.search.impl.logic.impl.labels;

import java.util.Optional;

import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.api.Labels;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;
import diarsid.search.impl.model.RealLabel;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.JdbcTransaction;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;

import static diarsid.search.api.model.meta.Storable.State.STORED;

public class LabelsImpl extends ThreadTransactional implements Labels {

    public LabelsImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public Entry.Label getOrSave(User user, String name) {
        name = name.trim().strip().toLowerCase();

        JdbcTransaction transaction = super.currentTransaction();

        Optional<Entry.Label> storedLabel = this.findLabelBy(user, name);

        if ( storedLabel.isPresent() ) {
            return storedLabel.get();
        }

        RealLabel label = new RealLabel(randomUUID(), now(), user.uuid(), name);

        int inserted = transaction
                .doUpdate(
                        "INSERT INTO labels (uuid, user_uuid, time, name) " +
                        "VALUES (?, ?, ?, ?)",
                        label.uuid(), label.userUuid(), label.time(), label.name());

        if ( inserted == 1 ) {
            label.setState(STORED);
            return label;
        }
        else {
            throw new IllegalStateException();
        }
    }

    @Override
    public Optional<Entry.Label> findLabelBy(User user, String name) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        row -> new RealLabel(row),
                        "SELECT * " +
                        "FROM labels " +
                        "WHERE " +
                        "   name = ? AND " +
                        "   user_uuid = ? ",
                        name, user.uuid());
    }
}
