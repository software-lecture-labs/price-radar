-- Accounts sign in with email and password. Emails are stored trimmed and
-- lower-cased, so the plain UNIQUE constraint is case-insensitive.
CREATE TABLE users (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email text NOT NULL UNIQUE CHECK (email = lower(btrim(email)) AND email LIKE '_%@_%'),
    full_name text NOT NULL CHECK (btrim(full_name) <> ''),
    -- E.164, for example +905321234567.
    phone text NOT NULL CHECK (phone ~ '^\+[1-9][0-9]{7,14}$'),
    password_hash text NOT NULL CHECK (password_hash <> ''),
    role text NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- Only the SHA-256 of a refresh token is stored; the token itself stays in the
-- client's cookie. Signing out revokes the row.
CREATE TABLE refresh_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash text NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CHECK (expires_at > created_at)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
