module diarsid.librarian {

    requires java.sql;
    requires org.slf4j;
    requires com.h2database;
    requires com.fasterxml.uuid;
    requires diarsid.jdbc;
    requires diarsid.sceptre;
    requires diarsid.support;

    exports diarsid.librarian.api;
    exports diarsid.librarian.api.annotations;
    exports diarsid.librarian.api.exceptions;
    exports diarsid.librarian.api.interaction;
    exports diarsid.librarian.api.model;
    exports diarsid.librarian.api.model.meta;
    exports diarsid.librarian.api.required;
    exports diarsid.librarian.api.required.impl;

    opens diarsid.librarian.impl.logic.impl.jdbc.h2.extensions to com.h2database;
}
