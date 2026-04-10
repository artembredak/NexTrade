CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                       email            VARCHAR(255)  NOT NULL UNIQUE,
                       username         VARCHAR(100)  NOT NULL UNIQUE,
                       hashed_password  VARCHAR(255)  NOT NULL,
                       role             VARCHAR(20)   NOT NULL DEFAULT 'USER'
                           CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
                       is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
                       is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
                       deleted_at       TIMESTAMPTZ   NULL,
                       created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
                       updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Partial indexes exclude soft-deleted users from lookups — keeps index small and fast.
CREATE INDEX idx_users_email    ON users (email)    WHERE is_deleted = FALSE;
CREATE INDEX idx_users_username ON users (username) WHERE is_deleted = FALSE;

COMMENT ON TABLE  users                  IS 'Application users. Soft-deleted rows are retained for audit purposes.';
COMMENT ON COLUMN users.id               IS 'UUID primary key, generated server-side via gen_random_uuid()';
COMMENT ON COLUMN users.hashed_password  IS 'BCrypt-hashed password — never stored in plain text';
COMMENT ON COLUMN users.role             IS 'Access role: USER (default) or ADMIN';
COMMENT ON COLUMN users.is_deleted       IS 'Soft-delete flag — never hard-delete users';
COMMENT ON COLUMN users.deleted_at       IS 'Timestamp of soft deletion, NULL while active';
