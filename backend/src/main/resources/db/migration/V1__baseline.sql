-- Baseline migration: proves Flyway is wired and enables the extensions the
-- application will rely on. Application tables belong in later versions.

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
