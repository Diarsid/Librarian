module diarsid.search {

    requires java.sql;
    requires slf4j.api;
    requires com.h2database;
    requires diarsid.jdbc;
    requires diarsid.strings.similarity;
    requires diarsid.support;

    exports diarsid.search.api;
    exports diarsid.search.api.annotations;
    exports diarsid.search.api.exceptions;
    exports diarsid.search.api.model;
    exports diarsid.search.api.model.meta;
    exports diarsid.search.api.required;
    exports diarsid.search.api.interaction;
}
