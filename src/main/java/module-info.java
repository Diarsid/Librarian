module BeamSearch {

    requires java.sql;
    requires slf4j.api;
    requires diarsid.support;
    requires diarsid.jdbc;

    exports diarsid.search.api;
    exports diarsid.search.api.model;
    exports diarsid.search.api.interaction;
}
