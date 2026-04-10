CREATE TABLE price_history (
                               id           BIGSERIAL      PRIMARY KEY,
                               coin_id      VARCHAR(50)    NOT NULL,
                               price        NUMERIC(20,8)  NOT NULL,
                               recorded_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                               CONSTRAINT fk_price_history_coin FOREIGN KEY (coin_id) REFERENCES coins (id) ON DELETE CASCADE
);

CREATE INDEX idx_price_history_coin_id    ON price_history (coin_id);
CREATE INDEX idx_price_history_recorded_at ON price_history (recorded_at DESC);
CREATE INDEX idx_price_history_coin_recorded ON price_history (coin_id, recorded_at DESC);

COMMENT ON TABLE  price_history             IS 'Append-only price snapshots sampled every 30 s per coin';
COMMENT ON COLUMN price_history.price       IS 'USD price at sample time, NUMERIC(20,8)';
COMMENT ON COLUMN price_history.recorded_at IS 'Wall-clock time of the price snapshot (TIMESTAMPTZ)';
