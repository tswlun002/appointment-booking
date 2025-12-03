-- liquibase formatted sql

-- changeset lungatsewu:-1
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='slot';
-- comment: /* Create table SLOT only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE  slot(
    id SERIAL NOT NULL ,
    day DATE NOT NULL ,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL ,
    number INTEGER NOT NULL ,
    is_booked BOOLEAN NOT NULL,
    created_at TIMESTAMP  DEFAULT  LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    version INTEGER DEFAULT  1,
    CONSTRAINT pk_slot PRIMARY KEY (id),
    CONSTRAINT  unique_day_start_time_end_time UNIQUE (day, start_time,end_time)
);
CREATE INDEX idx_day_is_booked ON slot(day,is_booked);

-- ROLLBACK DROP TABLE slot

-- changeset lungatsewu:-2
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='otp';
-- comment: /* Create table OTP only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE otp (
    id  SERIAL NOT NULL,
    code VARCHAR(32) NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT  LOCALTIMESTAMP,
    expire_date timestamp NULL,
    purpose VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    username CHAR(10) NOT NULL,
    verification_attempts INT DEFAULT 0 NULL,
    CONSTRAINT pk_otp PRIMARY KEY (id),
    CONSTRAINT  unique_username_code_status UNIQUE (username,code,status)
);
CREATE INDEX idx_username_code_status ON otp(username,code,status);
CREATE INDEX idx_username_status ON otp(username,status);
-- ROLLBACK DROP TABLE otp

-- changeset lungatsewu:1736901183105-3
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='otp';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'otp' AND column_name = 'username';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_entity' AND column_name = 'username';
ALTER TABLE otp ADD CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES user_entity(value) ON DELETE CASCADE; -- Foreign key reference
-- ROLLBACK ALTER TABLE otp DROP CONSTRAINT unique_username;


-- changeset lungatsewu:-4
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'user_dead_letter_event' AND table_schema = current_schema()
CREATE TABLE user_dead_letter_event (
    event_id VARCHAR(255) PRIMARY KEY,
    key VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition INTEGER ,
    event_offset INTEGER ,
    headers TEXT,
    is_retryable BOOLEAN NOT NULL DEFAULT FALSE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    exception TEXT NOT NULL ,
    exception_class VARCHAR(255),
    cause_class VARCHAR(255),
    stack_trace TEXT NOT NULL ,
    trace_id VARCHAR(255) NOT NULL ,
    status VARCHAR(50) NOT NULL,
    published_time TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
    fullname VARCHAR(128) NOT NULL ,
    username CHAR(10) NOT NULL,
    email VARCHAR(128) NOT NULL ,
    CONSTRAINT unique_key_trace_id UNIQUE (key, trace_id)
);

CREATE INDEX idx_user_dead_letter_event_trace_id ON user_dead_letter_event(trace_id);
CREATE INDEX idx_user_dead_letter_event_topic ON user_dead_letter_event(topic);
CREATE INDEX idx_user_dead_letter_event_is_retryable ON user_dead_letter_event(is_retryable);
CREATE INDEX idx_user_dead_letter_event_status ON user_dead_letter_event(status);
CREATE INDEX idx_user_dead_letter_event_time_stamp ON user_dead_letter_event(published_time);
--rollback DROP TABLE user_dead_letter_event;


-- changeset lungatsewu:-5
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='user_dead_letter_event';
CREATE OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date = CURRENT_TIMESTAMP;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- changeset lungatsewu:-6
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='user_dead_letter_event';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_event' AND event_object_table='user_dead_letter_event';
CREATE TRIGGER set_last_modified_date_event
    BEFORE UPDATE ON user_dead_letter_event
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- changeset lungatsewu:-7
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='slot';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_slot' AND event_object_table='slot';
CREATE TRIGGER set_last_modified_date_slot
    BEFORE UPDATE ON slot
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();