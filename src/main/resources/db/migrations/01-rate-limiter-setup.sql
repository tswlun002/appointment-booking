-- liquibase formatted sql

-- changeset Lunga:18 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'rate_limit') THEN
        CREATE TABLE rate_limit
        (
            id              BIGSERIAL PRIMARY KEY,
            identifier      VARCHAR(255) NOT NULL,
            purpose         VARCHAR(50)  NOT NULL,
            attempt_count   INT          NOT NULL DEFAULT 0,
            window_start_at TIMESTAMP    NOT NULL,
            last_attempt_at TIMESTAMP,
            CONSTRAINT uk_rate_limit_identifier_purpose UNIQUE (identifier, purpose)
        );
    END IF;
END $$;
-- rollback DROP TABLE rate_limit;

-- changeset Lunga:18a
CREATE INDEX IF NOT EXISTS idx_rate_limit_identifier ON rate_limit (identifier);

-- changeset Lunga:18b
CREATE INDEX IF NOT EXISTS idx_rate_limit_purpose ON rate_limit (purpose);