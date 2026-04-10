ALTER TABLE portfolios ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN portfolios.version IS 'Optimistic locking counter managed by Hibernate @Version — never set manually';
