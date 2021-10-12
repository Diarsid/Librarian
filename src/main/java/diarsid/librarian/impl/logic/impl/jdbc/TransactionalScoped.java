package diarsid.librarian.impl.logic.impl.jdbc;

import java.util.UUID;

public interface TransactionalScoped {

    UUID transactionUuid();
}
