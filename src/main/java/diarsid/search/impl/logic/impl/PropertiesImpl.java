package diarsid.search.impl.logic.impl;

import java.time.LocalDateTime;

import diarsid.search.api.Properties;
import diarsid.search.api.exceptions.NotFoundException;
import diarsid.search.api.model.User;
import diarsid.jdbc.JdbcTransactionThreadBindings;
import diarsid.jdbc.api.rows.ColumnGetter;
import diarsid.search.impl.logic.impl.support.ThreadTransactional;

public class PropertiesImpl extends ThreadTransactional implements Properties {

    public PropertiesImpl(JdbcTransactionThreadBindings transactionThreadBindings) {
        super(transactionThreadBindings);
    }

    @Override
    public LocalDateTime lastModificationTime(User user) {
        return super.currentTransaction()
                .doQueryAndConvertFirstRow(
                        ColumnGetter.timeOf("time"),
                        "SELECT time " +
                        "FROM entries " +
                        "ORDER BY time DESC")
                .orElseThrow(NotFoundException::new);
    }
}
