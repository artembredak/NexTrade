CREATE TABLE coins (
                       id                VARCHAR(50)    PRIMARY KEY,
                       symbol            VARCHAR(20)    NOT NULL,
                       name              VARCHAR(100)   NOT NULL,
                       image_url         VARCHAR(500),
                       current_price     NUMERIC(20,8),
                       price_change_24h  NUMERIC(10,4),
                       market_cap        NUMERIC(30,2),
                       volume_24h        NUMERIC(30,2),
                       last_updated      TIMESTAMPTZ
);

CREATE INDEX idx_coins_symbol ON coins (symbol);

COMMENT ON TABLE  coins                  IS 'Cryptocurrency reference data, refreshed from CoinGecko every 30 s';
COMMENT ON COLUMN coins.id               IS 'CoinGecko coin slug, e.g. "bitcoin", "ethereum"';
COMMENT ON COLUMN coins.symbol           IS 'Ticker symbol, e.g. "BTC", "ETH"';
COMMENT ON COLUMN coins.price_change_24h IS 'Price change over last 24 h as a percentage, NUMERIC(10,4)';
COMMENT ON COLUMN coins.market_cap       IS 'Market capitalisation in USD, NUMERIC(30,2)';
COMMENT ON COLUMN coins.volume_24h       IS '24 h trading volume in USD, NUMERIC(30,2)';
COMMENT ON COLUMN coins.last_updated     IS 'Timestamp of most recent price refresh from CoinGecko';
