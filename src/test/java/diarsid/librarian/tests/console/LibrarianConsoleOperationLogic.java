package diarsid.librarian.tests.console;

import diarsid.console.api.io.operations.OperationLogic;
import diarsid.jdbc.api.Jdbc;
import diarsid.librarian.api.Core;
import diarsid.librarian.api.model.User;

public abstract class LibrarianConsoleOperationLogic implements OperationLogic {

    protected final Core core;
    protected final User user;
    protected final Jdbc jdbc;

    public LibrarianConsoleOperationLogic(Core core, User user, Jdbc jdbc) {
        this.core = core;
        this.user = user;
        this.jdbc = jdbc;
    }
}
