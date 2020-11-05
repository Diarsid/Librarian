CREATE TABLE users(
    uuid UUID       PRIMARY KEY,
    name VARCHAR    NOT NULL,
    time TIMESTAMP WITHOUT TIME ZONE NOT NULL);


CREATE TABLE entries(
    uuid            UUID    PRIMARY KEY,
    time            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_uuid       UUID    NOT NULL,
    string_origin   VARCHAR NOT NULL,
    string_lower    VARCHAR NOT NULL,
    type            VARCHAR NOT NULL);

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


CREATE TABLE chars_in_entries(
    uuid        UUID    PRIMARY KEY,
    ch          CHAR(1) NOT NULL,
    qty         INT     NOT NULL,
    entry_size  INT     NOT NULL,
    entry_uuid  UUID    NOT NULL,
    user_uuid   UUID    NOT NULL);

ALTER TABLE chars_in_entries
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
    label_uuid  UUID    NOT NULL,
    entry_uuid  UUID    NOT NULL);

ALTER TABLE labels_to_entries
ADD CONSTRAINT FK_assignedLabels_to_labels
FOREIGN KEY(label_uuid) REFERENCES labels(uuid);

ALTER TABLE labels_to_entries
ADD CONSTRAINT FK_assignedLabels_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

--    === phrases ===

CREATE TABLE phrases(
    uuid        UUID    PRIMARY KEY,
    user_uuid   UUID    NOT NULL,
    string      VARCHAR NOT NULL,
    phrase_size INT     NOT NULL,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL);

ALTER TABLE phrases
ADD CONSTRAINT FK_phrases_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

CREATE TABLE phrases_in_entries(
    uuid        UUID    PRIMARY KEY,
    phrase_uuid UUID    NOT NULL,
    entry_uuid  UUID    NOT NULL);

ALTER TABLE phrases_in_entries
ADD CONSTRAINT FK_phrase_relations_to_words
FOREIGN KEY(phrase_uuid) REFERENCES phrases(uuid);

ALTER TABLE phrases_in_entries
ADD CONSTRAINT FK_phrase_relations_to_entries
FOREIGN KEY(entry_uuid) REFERENCES entries(uuid);

CREATE TABLE chars_in_phrases(
    uuid        UUID    PRIMARY KEY,
    ch          CHAR(1) NOT NULL,
    qty         INT     NOT NULL,
    phrase_size INT     NOT NULL,
    phrase_uuid UUID    NOT NULL,
    user_uuid   UUID    NOT NULL);

ALTER TABLE chars_in_phrases
ADD CONSTRAINT FK_chars_to_phrases
FOREIGN KEY(phrase_uuid) REFERENCES phrases(uuid);

ALTER TABLE chars_in_phrases
ADD CONSTRAINT FK_phrases_chars_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

--    === words ===

CREATE TABLE words(
    uuid        UUID    PRIMARY KEY,
    user_uuid   UUID    NOT NULL,
    string      VARCHAR NOT NULL,
    word_size   INT     NOT NULL,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL);

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

CREATE TABLE words_in_phrases(
    uuid        UUID    PRIMARY KEY,
    word_uuid   UUID    NOT NULL,
    phrase_uuid  UUID    NOT NULL
);

-- ALTER TABLE *
-- ADD CONSTRAINT *
-- FOREIGN KEY(*) REFERENCES *(*);

ALTER TABLE words_in_phrases
ADD CONSTRAINT FK_word_phrase_relations_to_words
FOREIGN KEY(word_uuid) REFERENCES words(uuid);

ALTER TABLE words_in_phrases
ADD CONSTRAINT FK_word_phrase_relations_to_phrases
FOREIGN KEY(phrase_uuid) REFERENCES phrases(uuid);

CREATE TABLE chars_in_words(
    uuid        UUID    PRIMARY KEY,
    ch          CHAR(1) NOT NULL,
    qty         INT     NOT NULL,
    word_size   INT     NOT NULL,
    word_uuid   UUID    NOT NULL,
    user_uuid   UUID    NOT NULL);

ALTER TABLE chars_in_words
ADD CONSTRAINT FK_chars_to_words
FOREIGN KEY(word_uuid) REFERENCES words(uuid);

ALTER TABLE chars_in_words
ADD CONSTRAINT FK_words_chars_to_users
FOREIGN KEY(user_uuid) REFERENCES users(uuid);

-- labels
CREATE TABLE labels_to_chars_in_entries(
    uuid        UUID    PRIMARY KEY,
    label_uuid  UUID    NOT NULL,
    chars_uuid  UUID    NOT NULL,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE labels_to_chars_in_entries
ADD CONSTRAINT FK_entrychars_to_labels
FOREIGN KEY(label_uuid) REFERENCES labels(uuid);

ALTER TABLE labels_to_chars_in_entries
ADD CONSTRAINT FK_entrychars_to_chars
FOREIGN KEY(chars_uuid) REFERENCES chars_in_entries(uuid);

CREATE TABLE labels_to_chars_in_phrases(
    uuid        UUID    PRIMARY KEY,
    label_uuid  UUID    NOT NULL,
    chars_uuid  UUID    NOT NULL,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE labels_to_chars_in_phrases
ADD CONSTRAINT FK_phrasechars_to_labels
FOREIGN KEY(label_uuid) REFERENCES labels(uuid);

ALTER TABLE labels_to_chars_in_phrases
ADD CONSTRAINT FK_phrasechars_to_chars
FOREIGN KEY(chars_uuid) REFERENCES chars_in_phrases(uuid);

CREATE TABLE labels_to_chars_in_words(
    uuid        UUID    PRIMARY KEY,
    label_uuid  UUID    NOT NULL,
    chars_uuid  UUID    NOT NULL,
    time        TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

ALTER TABLE labels_to_chars_in_words
ADD CONSTRAINT FK_wordchars_to_labels
FOREIGN KEY(label_uuid) REFERENCES labels(uuid);

ALTER TABLE labels_to_chars_in_words
ADD CONSTRAINT FK_wordchars_to_chars
FOREIGN KEY(chars_uuid) REFERENCES chars_in_words(uuid);

CREATE INDEX IX_CHARS_IN_ENTRIES
ON chars_in_entries(ch, qty, entry_size, user_uuid);

CREATE INDEX IX_CHARS_IN_WORDS
ON chars_in_words(ch, qty, word_size, user_uuid);

CREATE INDEX IX_CHARS_IN_PHRASES
ON chars_in_phrases(ch, qty, phrase_size, user_uuid);

CREATE INDEX IX_STRING_AND_USER_IN_PHRASES
ON phrases(string, user_uuid);

CREATE INDEX IX_STRING_AND_USER_IN_WORDS
ON words(string, user_uuid);

DROP TABLE labels_to_chars_in_entries;
DROP TABLE labels_to_chars_in_words;
DROP TABLE labels_to_chars_in_phrases;
DROP TABLE chars_in_words;
DROP TABLE chars_in_phrases;
DROP TABLE chars_in_entries;
DROP TABLE words_in_entries;
DROP TABLE phrases_in_entries;
DROP TABLE words_in_phrases;
DROP TABLE words;
DROP TABLE phrases;
DROP TABLE labels_to_entries;
DROP TABLE labels;
DROP TABLE choices;
DROP TABLE patterns_to_entries;
DROP TABLE patterns;
DROP TABLE entries;
DROP TABLE users;

DELETE FROM labels_to_chars_in_entries;
DELETE FROM labels_to_chars_in_words;
DELETE FROM labels_to_chars_in_phrases;
DELETE FROM chars_in_words;
DELETE FROM chars_in_entries;
DELETE FROM chars_in_phrases;
DELETE FROM words_in_entries;
DELETE FROM phrases_in_entries;
DELETE FROM words_in_phrases;
DELETE FROM words;
DELETE FROM phrases;
DELETE FROM entries;
