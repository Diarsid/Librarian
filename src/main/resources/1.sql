CREATE TABLE users(
    uuid UUID       PRIMARY KEY,
    name VARCHAR    NOT NULL,
    time TIMESTAMP WITHOUT TIME ZONE NOT NULL);


CREATE TABLE entries(
    uuid            UUID    PRIMARY KEY,
    time            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_uuid       UUID    NOT NULL,
    string_origin   VARCHAR NOT NULL,
    string_lower    VARCHAR NOT NULL);

ALTER TABLE entries
ADD CONSTRAINT FK_entries_to_users
FOREIGN KEY (user_uuid) REFERENCES users(uuid);


CREATE TABLE patterns(
    uuid        UUID        PRIMARY KEY,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_uuid   UUID        NOT NULL,
    string      VARCHAR     NOT NULL);

ALTER TABLE patterns
ADD CONSTRAINT FK_patterns_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);


CREATE TABLE patterns_to_entries(
    uuid            UUID PRIMARY KEY,
    entry_uuid      UUID NOT NULL,
    pattern_uuid    UUID NOT NULL,
    time            TIMESTAMP WITHOUT TIME ZONE NOT NULL);

ALTER TABLE patterns_to_entries
ADD CONSTRAINT FK_relations_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

ALTER TABLE patterns_to_entries
ADD CONSTRAINT FK_relations_to_patterns
FOREIGN KEY(pattern_uuid) REFERENCES patterns(uuid);


CREATE TABLE choices(
    uuid            UUID    PRIMARY KEY,
    relation_uuid   UUID    NOT NULL,
    time            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    time_actual     TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE choices
ADD CONSTRAINT FK_choices_to_relations
FOREIGN KEY(relation_uuid) REFERENCES patterns_to_entries(uuid);


CREATE TABLE characters_in_entries(
    uuid            UUID PRIMARY KEY,
    character       CHAR(1) NOT NULL,
    character_count INT NOT NULL,
    entry_uuid      UUID NOT NULL);

ALTER TABLE characters_in_entries
ADD CONSTRAINT FK_characters_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);


CREATE TABLE labels(
    uuid        UUID    PRIMARY KEY,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name        VARCHAR NOT NULL,
    user_uuid   UUID    NOT NULL);

ALTER TABLE labels
ADD CONSTRAINT FK_labels_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);


CREATE TABLE labels_to_entries(
    uuid        UUID    PRIMARY KEY,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    label_uuid   UUID    NOT NULL,
    entry_uuid   UUID    NOT NULL);

ALTER TABLE labels_to_entries
ADD CONSTRAINT FK_assignedLabels_to_labels
FOREIGN KEY(label_uuid) REFERENCES labels(uuid);

ALTER TABLE labels_to_entries
ADD CONSTRAINT FK_assignedLabels_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

CREATE INDEX IX_CHARS ON characters_in_entries(character, character_count);
