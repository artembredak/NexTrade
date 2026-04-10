CREATE TABLE portfolio_snapshots (
                                     id             BIGSERIAL      PRIMARY KEY,
                                     portfolio_id   UUID           NOT NULL,
                                     total_value    NUMERIC(20,8)  NOT NULL,
                                     snapshot_time  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                                     CONSTRAINT fk_portfolio_snapshots_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE
);

CREATE INDEX idx_portfolio_snapshots_portfolio_id   ON portfolio_snapshots (portfolio_id);
CREATE INDEX idx_portfolio_snapshots_time           ON portfolio_snapshots (snapshot_time DESC);
CREATE INDEX idx_portfolio_snapshots_portfolio_time ON portfolio_snapshots (portfolio_id, snapshot_time DESC);

COMMENT ON TABLE  portfolio_snapshots              IS 'Portfolio total value recorded every 30 min for chart history';
COMMENT ON COLUMN portfolio_snapshots.total_value  IS 'cash_balance + sum(quantity × current_price) at snapshot time';
COMMENT ON COLUMN portfolio_snapshots.snapshot_time IS 'Wall-clock time of the snapshot (TIMESTAMPTZ)';
