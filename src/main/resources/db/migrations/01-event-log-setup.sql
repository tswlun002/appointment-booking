-- liquibase formatted sql

-- changeset Lunga:1-func-event
CREATE OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date = CURRENT_TIMESTAMP;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- changeset Lunga:19 splitStatements:false

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'user_dead_letter_event') THEN
CREATE TABLE user_dead_letter_event
(
    event_id           VARCHAR(255) PRIMARY KEY,
    key                TEXT NOT NULL,
    value              TEXT         NOT NULL,
    topic              VARCHAR(255) NOT NULL,
    partition          INTEGER,
    event_offset       INTEGER,
    headers            TEXT,
    is_retryable       BOOLEAN      NOT NULL DEFAULT FALSE,
    retry_count        INTEGER      NOT NULL DEFAULT 0,
    exception          TEXT         NOT NULL,
    exception_class    VARCHAR(255),
    exception_cause    VARCHAR(255),
    stack_trace        TEXT         NOT NULL,
    trace_id           VARCHAR(255) NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    published_time     TIMESTAMP    NOT NULL,
    next_retry_at      TIMESTAMP,
    created_date       TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    CONSTRAINT unique_key_trace_id UNIQUE (key, trace_id),
    CONSTRAINT check_retry_count_non_negative CHECK (retry_count >= 0),
    CONSTRAINT check_partition_non_negative CHECK (partition IS NULL OR partition >= 0),
    CONSTRAINT check_event_offset_non_negative CHECK (event_offset IS NULL OR event_offset >= 0),
    CONSTRAINT check_published_before_created CHECK (published_time <= created_date),
    CONSTRAINT check_created_before_modified CHECK (created_date <= last_modified_date),
    CONSTRAINT check_event_id_not_empty CHECK (TRIM(event_id) <> ''),
    CONSTRAINT check_topic_not_empty CHECK (TRIM(topic) <> ''),
    CONSTRAINT check_trace_id_not_empty CHECK (TRIM(trace_id) <> '')
    );
    END IF;
END $$;
--rollback DROP TABLE user_dead_letter_event;

-- changeset Lunga:19a
CREATE INDEX IF NOT EXISTS idx_user_dead_letter_event_topic_trace_id ON user_dead_letter_event (topic, trace_id);

-- changeset Lunga:19b
CREATE INDEX IF NOT EXISTS idx_user_dead_letter_event_time_stamp ON user_dead_letter_event (published_time);

-- changeset Lunga:19c
CREATE INDEX IF NOT EXISTS idx_dead_letter_recovery ON user_dead_letter_event (is_retryable, status, retry_count, next_retry_at);

-- changeset Lunga:20 splitStatements:false
DROP TRIGGER IF EXISTS set_last_modified_date_event ON user_dead_letter_event;
CREATE TRIGGER set_last_modified_date_event
    BEFORE UPDATE ON user_dead_letter_event FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();