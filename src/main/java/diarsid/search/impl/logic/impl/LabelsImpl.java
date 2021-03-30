package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.ThreadBoundJdbcTransaction;
import diarsid.search.api.Labels;
import diarsid.search.api.model.Entry;
import diarsid.search.api.model.User;
import diarsid.search.impl.logic.impl.support.ThreadBoundTransactional;
import diarsid.search.impl.model.RealLabel;
import diarsid.support.strings.StringCacheForRepeatedSeparatedPrefixSuffix;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import static diarsid.support.model.Storable.State.STORED;

public class LabelsImpl extends ThreadBoundTransactional implements Labels {

    private final StringCacheForRepeatedSeparatedPrefixSuffix sqlSelectLabelsWhereNameIn;

    public LabelsImpl(Jdbc jdbc) {
        super(jdbc);
        this.sqlSelectLabelsWhereNameIn = new StringCacheForRepeatedSeparatedPrefixSuffix(
                "SELECT * \n" +
                "FROM labels \n" +
                "WHERE \n" +
                "   name IN ( \n",
                "      ? ", ", \n",
                "       ) AND \n" +
                "   user_uuid = ? "
        );
    }

    @Override
    public Entry.Label getBy(User user, UUID uuid) {
        return null;
    }

    @Override
    public Entry.Label getOrSave(User user, String name) {
        name = name.trim().strip().toLowerCase();

        ThreadBoundJdbcTransaction transaction = super.currentTransaction();

        Optional<Entry.Label> storedLabel = this.findBy(user, name);

        if ( storedLabel.isPresent() ) {
            return storedLabel.get();
        }

        RealLabel label = new RealLabel(randomUUID(), now(), user.uuid(), name);

        int inserted = transaction
                .doUpdate(
                        "INSERT INTO labels (uuid, user_uuid, time, name) \n" +
                        "VALUES (?, ?, ?, ?)",
                        label.uuid(), label.userUuid(), label.createdAt(), label.name());

        if ( inserted == 1 ) {
            label.setState(STORED);
            return label;
        }
        else {
            throw new IllegalStateException();
        }
    }

    @Override
    public List<Entry.Label> getOrSave(User user, List<String> names) {
        if ( names.isEmpty() ) {
            return emptyList();
        }

        if ( names.size() == 1 ) {
            return List.of(this.getOrSave(user, names.get(0)));
        }

        normalizeAll(names);

        List<Entry.Label> foundLabels = super.currentTransaction()
                .doQueryAndStream(
                        RealLabel::new,
                        this.sqlSelectLabelsWhereNameIn.getFor(names),
                        names, user.uuid())
                .collect(toList());

        if ( foundLabels.size() == names.size() ) {
            return foundLabels;
        }
        else if ( foundLabels.isEmpty() ) {
            List<Entry.Label> labels = this.saveLabels(user, names);

            return labels;
        }
        else {
            List<String> notFoundNames = new ArrayList<>();

            boolean nameNotFound;
            for ( String name : names ) {
                nameNotFound = true;

                for (Entry.Label label : foundLabels ) {
                    if ( label.name().equals(name) ) {
                        nameNotFound = false;
                        break;
                    }
                }

                if ( nameNotFound ) {
                    notFoundNames.add(name);
                }
            }

            foundLabels.addAll(this.saveLabels(user, notFoundNames));

            return foundLabels;
        }
    }

    private List<Entry.Label> saveLabels(User user, List<String> names) {
        LocalDateTime time = now();
        UUID userUuid = user.uuid();

        List<Entry.Label> labels = names
                .stream()
                .map(labelName -> new RealLabel(randomUUID(), time, userUuid, labelName))
                .collect(toList());

        List<List> labelsArgs = labels
                .stream()
                .map(label -> List.of(label.uuid(), userUuid, time, label.name()))
                .collect(toList());

        int[] inserted = super.currentTransaction()
                .doBatchUpdate(
                        "INSERT INTO labels (uuid, user_uuid, time, name) \n" +
                        "VALUES (?, ?, ?, ?)",
                        labelsArgs);

        if ( inserted.length != labels.size() ) {
            throw new IllegalStateException();
        }

        labels.forEach(label -> label.setState(STORED));
        return labels;
    }

    @Override
    public Optional<Entry.Label> findBy(User user, String name) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        RealLabel::new,
                        "SELECT * \n" +
                        "FROM labels \n" +
                        "WHERE \n" +
                        "   name = ? AND \n" +
                        "   user_uuid = ? ",
                        name, user.uuid());
    }

    private static void normalizeAll(List<String> names) {
        String name;
        for (int i = 0; i < names.size(); i++) {
            name = names.get(i).trim().strip().toLowerCase();
            names.set(i, name);
        }
    }
}
