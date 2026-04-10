CREATE TABLE portfolio_assets (
                                  id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                                  portfolio_id   UUID           NOT NULL,
                                  coin_id        VARCHAR(50)    NOT NULL,
                                  quantity       NUMERIC(20,8)  NOT NULL DEFAULT 0.00000000,
                                  avg_buy_price  NUMERIC(20,8)  NOT NULL DEFAULT 0.00000000,
                                  created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                                  updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                                  CONSTRAINT fk_portfolio_assets_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
                                  CONSTRAINT fk_portfolio_assets_coin      FOREIGN KEY (coin_id)      REFERENCES coins (id),
                                  CONSTRAINT uq_portfolio_coin             UNIQUE (portfolio_id, coin_id)
);

CREATE INDEX idx_portfolio_assets_portfolio_id ON portfolio_assets (portfolio_id);

COMMENT ON TABLE  portfolio_assets               IS 'Crypto holdings within a portfolio; one row per coin per portfolio';
COMMENT ON COLUMN portfolio_assets.quantity      IS 'Current held quantity, NUMERIC(20,8)';
COMMENT ON COLUMN portfolio_assets.avg_buy_price IS 'Weighted-average cost basis; recalculated on every BUY';
