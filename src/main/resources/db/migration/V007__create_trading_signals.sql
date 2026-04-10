CREATE TABLE trading_signals (
                                 id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                                 coin_id             VARCHAR(50)    NOT NULL,
                                 created_by          UUID           NOT NULL,
                                 signal_type         VARCHAR(10)    NOT NULL
                                     CONSTRAINT chk_trading_signals_type    CHECK (signal_type IN ('BUY', 'SELL', 'HOLD')),
                                 confidence_percent  INTEGER        NOT NULL
                                     CONSTRAINT chk_trading_signals_confidence CHECK (confidence_percent BETWEEN 0 AND 100),
                                 entry_price         NUMERIC(20,8)  NOT NULL,
                                 target_price        NUMERIC(20,8)  NOT NULL,
                                 stop_loss_price     NUMERIC(20,8)  NOT NULL,
                                 rationale           TEXT,
                                 status              VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE'
                                     CONSTRAINT chk_trading_signals_status  CHECK (status IN ('ACTIVE', 'CLOSED')),
                                 closed_at           TIMESTAMPTZ    NULL,
                                 created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

                                 CONSTRAINT fk_trading_signals_coin FOREIGN KEY (coin_id)     REFERENCES coins (id),
                                 CONSTRAINT fk_trading_signals_user FOREIGN KEY (created_by)  REFERENCES users (id)
);

CREATE INDEX idx_trading_signals_coin_id    ON trading_signals (coin_id);
CREATE INDEX idx_trading_signals_created_by ON trading_signals (created_by);
CREATE INDEX idx_trading_signals_status     ON trading_signals (status);

COMMENT ON TABLE  trading_signals                    IS 'Manual trading signals created by ADMIN users only';
COMMENT ON COLUMN trading_signals.signal_type        IS 'BUY, SELL, or HOLD';
COMMENT ON COLUMN trading_signals.confidence_percent IS 'Analyst confidence 0-100, enforced by CHECK constraint';
COMMENT ON COLUMN trading_signals.entry_price        IS 'Recommended entry price at signal creation time';
COMMENT ON COLUMN trading_signals.target_price       IS 'Price target for the signal';
COMMENT ON COLUMN trading_signals.stop_loss_price    IS 'Stop-loss threshold — never go lower than this';
COMMENT ON COLUMN trading_signals.closed_at          IS 'NULL while ACTIVE; set when ADMIN closes the signal';
