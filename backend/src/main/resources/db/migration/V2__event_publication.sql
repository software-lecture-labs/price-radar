-- Spring Modulith event publication registry (JDBC, schema v2).
--
-- Every event a module publishes to a transactional listener in another module
-- is recorded here before the listener runs and marked complete afterwards, so
-- a crash between the two cannot silently lose it. Copied from
-- spring-modulith-events-jdbc 2.1.1: schemas/v2/schema-postgresql.sql.
-- Changing the Spring Modulith version means checking that file for changes.

CREATE TABLE event_publication
(
  id                     UUID                     NOT NULL,
  listener_id            TEXT                     NOT NULL,
  event_type             TEXT                     NOT NULL,
  serialized_event       TEXT                     NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
