package diarsid.librarian.impl.logic.impl;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.librarian.api.Behavior;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.impl.support.ThreadBoundTransactional;

import static java.time.LocalDateTime.now;

public class BehaviorImpl extends ThreadBoundTransactional implements Behavior {

    public BehaviorImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public void set(User user, Feature feature, boolean enabled) {
        int changed = super.currentTransaction()
                .doUpdate(
                        "MERGE INTO behavior_features_by_users (user_uuid, name, enabled, time) \n" +
                        "KEY (user_uuid, name) \n" +
                        "VALUES(?, ?, ?, ?)",
                        user.uuid(), feature.name(), enabled, now());

        if ( changed != 1 ) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean get(User user, Feature feature) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        ColumnGetter.booleanOf("enabled"),
                        "SELECT enabled \n" +
                        "FROM behavior_features_by_users \n" +
                        "WHERE \n" +
                        "   user_uuid = ? AND \n" +
                        "   name = ?",
                        user.uuid(), feature.name())
                .orElseGet(() -> {
                    boolean defaultValue = feature.defaultValue();
                    this.set(user, feature, defaultValue);
                    return defaultValue;
                });
    }
}
