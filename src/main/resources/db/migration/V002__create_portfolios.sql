CREATE TABLE portfolios (
                            id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                            user_id       UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                            name          VARCHAR(255)   NOT NULL DEFAULT 'My Portfolio',
                            cash_balance  NUMERIC(20,8)  NOT NULL DEFAULT 0.00000000,
                            created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                            updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                            CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_portfolios_user_id ON portfolios (user_id);

COMMENT ON TABLE  portfolios              IS 'One portfolio per user — created automatically on registration';
COMMENT ON COLUMN portfolios.cash_balance IS 'USD cash balance, NUMERIC(20,8) — never FLOAT (ADR-012)';
COMMENT ON COLUMN portfolios.user_id      IS 'FK → users.id; 1:1 enforced by UNIQUE index';
