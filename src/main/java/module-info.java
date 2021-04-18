module diarsid.search {

    requires java.sql;
    requires slf4j.api;
    requires com.h2database;
    requires diarsid.jdbc;
    requires diarsid.strings.similarity;
    requires diarsid.support;

    exports diarsid.librarian.api;
    exports diarsid.librarian.api.annotations;
    exports diarsid.librarian.api.exceptions;
    exports diarsid.librarian.api.model;
    exports diarsid.librarian.api.model.meta;
    exports diarsid.librarian.api.required;
    exports diarsid.librarian.api.interaction;
}
