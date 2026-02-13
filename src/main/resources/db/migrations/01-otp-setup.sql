-- liquibase formatted sql

-- changeset Lunga:17  splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'otp') THEN
        CREATE TABLE otp
        (
            id                    SERIAL      NOT NULL,
            code                  VARCHAR(32) NOT NULL,
            created_date          TIMESTAMP   NOT NULL DEFAULT LOCALTIMESTAMP,
            expire_date           TIMESTAMP,
            updated_at            TIMESTAMP,
            purpose               VARCHAR(32) NOT NULL,
            status                VARCHAR(32) NOT NULL,
            username              VARCHAR(10) NOT NULL,
            verification_attempts INT                  DEFAULT 0 NULL,
            version               INT,
            CONSTRAINT pk_otp PRIMARY KEY (id),
            CONSTRAINT unique_username_code_status UNIQUE (username, code, status),
            CONSTRAINT check_verification_attempts_non_negative CHECK (verification_attempts >= 0),
            CONSTRAINT check_expire_after_created CHECK (expire_date IS NULL OR expire_date > created_date)
        );
    END IF;
END $$;

-- changeset Lunga:17a
CREATE INDEX IF NOT EXISTS idx_username_code_status ON otp (username, code, status);

-- changeset Lunga:17b
CREATE INDEX IF NOT EXISTS idx_username_status ON otp (username, status);

-- changeset Lunga:17c
CREATE INDEX IF NOT EXISTS idx_username_creation_date ON otp (username, created_date);