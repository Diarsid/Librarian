CREATE TABLE users(
    uuid UUID       PRIMARY KEY,
    name VARCHAR    NOT NULL,
    time TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL);

CREATE TABLE entries(
    uuid            UUID    PRIMARY KEY,
    time            TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL,
    user_uuid       UUID    NOT NULL,
    string_origin   VARCHAR NOT NULL,
    string_lower    VARCHAR NOT NULL,
    type            VARCHAR NOT NULL);

ALTER TABLE entries
ADD CONSTRAINT FK_entries_to_users
FOREIGN KEY (user_uuid) REFERENCES users(uuid);

CREATE TABLE patterns(
    uuid        UUID        PRIMARY KEY,
    time        TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL,
    user_uuid   UUID        NOT NULL,
    string      VARCHAR     NOT NULL);

ALTER TABLE patterns
ADD CONSTRAINT FK_patterns_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

CREATE TABLE patterns_to_entries(
    uuid            UUID    PRIMARY KEY,
    entry_uuid      UUID    NOT NULL,
    pattern_uuid    UUID    NOT NULL,
    algorithm       VARCHAR NOT NULL,
    weight          FLOAT4  NOT NULL,
    time            TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL);

ALTER TABLE patterns_to_entries
ADD CONSTRAINT FK_relations_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

ALTER TABLE patterns_to_entries
ADD CONSTRAINT FK_relations_to_patterns
FOREIGN KEY(pattern_uuid) REFERENCES patterns(uuid);

CREATE TABLE choices(
    uuid            UUID    PRIMARY KEY,
    relation_uuid   UUID    NOT NULL,
    time            TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL,
    time_actual     TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE choices
ADD CONSTRAINT FK_choices_to_relations
FOREIGN KEY(relation_uuid) REFERENCES patterns_to_entries(uuid);

CREATE TABLE labels(
    uuid        UUID    PRIMARY KEY,
    time        TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL,
    name        VARCHAR NOT NULL,
    user_uuid   UUID    NOT NULL);

ALTER TABLE labels
ADD CONSTRAINT FK_labels_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

CREATE TABLE labels_to_entries(
    uuid        UUID    PRIMARY KEY,
    time        TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL,
    label_uuid  UUID    NOT NULL,
    entry_uuid  UUID    NOT NULL);

ALTER TABLE labels_to_entries
ADD CONSTRAINT FK_assignedLabels_to_labels
FOREIGN KEY(label_uuid) REFERENCES labels(uuid);

ALTER TABLE labels_to_entries
ADD CONSTRAINT FK_assignedLabels_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

--    === words ===

CREATE TABLE words(
    uuid        UUID    PRIMARY KEY,
    user_uuid   UUID    NOT NULL,
    string      VARCHAR NOT NULL,
    string_sort VARCHAR NOT NULL,
    word_size   INT     NOT NULL,
    time        TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL);

ALTER TABLE words
ADD CONSTRAINT FK_words_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

CREATE TABLE words_in_entries(
    uuid        UUID    PRIMARY KEY,
    word_uuid   UUID    NOT NULL,
    entry_uuid  UUID    NOT NULL,
    position    VARCHAR(6) NOT NULL,
    index       INT NOT NULL);

ALTER TABLE words_in_entries
ADD CONSTRAINT FK_word_entry_relations_to_words
FOREIGN KEY(word_uuid) REFERENCES words(uuid);

ALTER TABLE words_in_entries
ADD CONSTRAINT FK_word_entry_relations_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

CREATE TABLE behavior_features_by_users(
    user_uuid   UUID    NOT NULL,
    name        VARCHAR NOT NULL,
    enabled     BOOLEAN NOT NULL,
    time        TIMESTAMP(9) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY(user_uuid, name)
);

ALTER TABLE behavior_features_by_users
ADD CONSTRAINT FK_behaviors_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

CREATE INDEX IX_ENTRIES_TIME
ON entries(time);

CREATE INDEX IX_STRING_AND_USER_IN_WORDS
ON words(string, user_uuid);
