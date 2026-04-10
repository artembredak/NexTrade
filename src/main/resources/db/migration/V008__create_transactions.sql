CREATE TABLE transactions (
                              id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                              portfolio_id    UUID           NOT NULL,
                              coin_id         VARCHAR(50)    NULL,
                              type            VARCHAR(20)    NOT NULL
                                  CONSTRAINT chk_transactions_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'BUY', 'SELL')),
                              amount          NUMERIC(20,8)  NOT NULL,
                              quantity        NUMERIC(20,8)  NULL,
                              price_per_coin  NUMERIC(20,8)  NULL,
                              description     VARCHAR(500)   NULL,
                              created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                              CONSTRAINT fk_transactions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
                              CONSTRAINT fk_transactions_coin      FOREIGN KEY (coin_id)      REFERENCES coins (id)
);

CREATE INDEX idx_transactions_portfolio_id ON transactions (portfolio_id);
CREATE INDEX idx_transactions_type         ON transactions (type);
CREATE INDEX idx_transactions_created_at   ON transactions (created_at DESC);

COMMENT ON TABLE  transactions               IS 'Immutable financial ledger — append-only, never update or delete';
COMMENT ON COLUMN transactions.type          IS 'DEPOSIT/WITHDRAWAL affect cash only; BUY/SELL also record coin fields';
COMMENT ON COLUMN transactions.amount        IS 'USD value of the transaction, NUMERIC(20,8)';
COMMENT ON COLUMN transactions.quantity      IS 'Coin quantity for BUY/SELL; NULL for DEPOSIT/WITHDRAWAL';
COMMENT ON COLUMN transactions.price_per_coin IS 'Price per coin at execution time; NULL for DEPOSIT/WITHDRAWAL';
