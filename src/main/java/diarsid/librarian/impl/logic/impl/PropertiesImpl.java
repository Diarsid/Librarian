package diarsid.librarian.impl.logic.impl;

import java.time.LocalDateTime;

import diarsid.jdbc.api.Jdbc;
import diarsid.jdbc.api.sqltable.columns.ColumnGetter;
import diarsid.jdbc.api.sqltable.rows.RowGetter;
import diarsid.librarian.api.Properties;
import diarsid.librarian.api.exceptions.NotFoundException;
import diarsid.librarian.api.model.User;
import diarsid.librarian.impl.logic.impl.support.ThreadBoundTransactional;

public class PropertiesImpl extends ThreadBoundTransactional implements Properties {

    private static final RowGetter<LocalDateTime> GET_TIME = ColumnGetter.timeOf("time");

    public PropertiesImpl(Jdbc jdbc) {
        super(jdbc);
    }

    @Override
    public LocalDateTime lastModificationTime(User user) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        GET_TIME,
                        "SELECT time \n" +
                        "FROM entries \n" +
                        "ORDER BY time DESC")
                .orElseThrow(NotFoundException::new);
    }
}
