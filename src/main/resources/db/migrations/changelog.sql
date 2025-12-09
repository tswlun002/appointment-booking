-- liquibase formatted sql

-- changeset lungatsewu:-1
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='branch';
-- comment: /* Create table ADDRESS only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE branch
(
    id                 SERIAL PRIMARY KEY,
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    branch_id          VARCHAR(36) NOT NULL,
    open_time          TIME        NOT NULL,
    close_time         TIME,
    CONSTRAINT unique_branch_id UNIQUE (branch_id)
);
--ROLLBACK DROP TABLE branch


-- changeset lungatsewu:-2
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='branch_appointment_info';
-- comment: /* Create table branchAppointmentInfo only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE branch_appointment_info
(
    id                 SERIAL PRIMARY KEY,
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    branch_id          VARCHAR(36)      NOT NULL REFERENCES branch (branch_id),
    branch_key         VARCHAR(16)      NOT NULL,
    slot_duration      INTEGER          NOT NULL,
    utilization_factor DOUBLE PRECISION NOT NULL,
    day_type           VARCHAR(16)      NOT NULL,
    CONSTRAINT unique_branch_id_branch_key_slot_duration_utilization_factor_day_type UNIQUE (branch_id, branch_key, slot_duration, utilization_factor, day_type)

);
--ROLLBACK DROP TABLE branch_appointment_info


-- changeset lungatsewu:-3
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='staff';
-- comment: /* Create table staff only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE staff
(
    id                 SERIAL PRIMARY KEY,
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    username           VARCHAR(32) NOT NULL UNIQUE ,
    branch_id          VARCHAR(36) NOT NULL, --REFERENCES branch (branch_id),
    status             VARCHAR(16),
    CONSTRAINT unique_username UNIQUE (username),
    CONSTRAINT unique_branch_id_username_status UNIQUE (branch_id,username,status)
);
CREATE INDEX idx_branch_id_username_status ON staff(branch_id,username,status);
-- ROLLBAK DROP TABLE staff


-- changeset lungatsewu:-4
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='address';
-- comment: /* Create table ADDRESS only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE address
(
    branch_id          VARCHAR(36) PRIMARY KEY REFERENCES branch (branch_id),
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    street_number      VARCHAR(16),
    street_name        VARCHAR(32),
    suburbs            VARCHAR(32),
    city               VARCHAR(32),
    province           VARCHAR(16),
    postal_code        VARCHAR(16),
    country            VARCHAR(32),
    CONSTRAINT unique_address UNIQUE (street_number, street_name, suburbs, city, province, postal_code, country)
);
--ROLLBACK DROP TABLE address

-- changeset lungatsewu:-5
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='slot';
-- comment: /* Create table SLOT only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE  slot(
                      id SERIAL PRIMARY KEY,
    day DATE NOT NULL ,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL ,
    number INTEGER NOT NULL ,
    is_booked BOOLEAN NOT NULL,
    created_at TIMESTAMP  DEFAULT  LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    version INTEGER DEFAULT  1,
    CONSTRAINT  unique_day_start_time_end_time UNIQUE (day, start_time,end_time)
);
CREATE INDEX idx_day_is_booked ON slot(day,is_booked);
-- ROLLBACK DROP TABLE slot

-- changeset lungatsewu:-6
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

-- changeset lungatsewu:1736901183105-7
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='otp';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'otp' AND column_name = 'username';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_entity' AND column_name = 'username';
ALTER TABLE otp ADD CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES user_entity(value) ON DELETE CASCADE; -- Foreign key reference
-- ROLLBACK ALTER TABLE otp DROP CONSTRAINT unique_username;


-- changeset lungatsewu:-8
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


-- changeset lungatsewu:-9
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='user_dead_letter_event';
CREATE OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date = CURRENT_TIMESTAMP;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- changeset lungatsewu:-10
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='user_dead_letter_event';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_event' AND event_object_table='user_dead_letter_event';
CREATE TRIGGER set_last_modified_date_event
    BEFORE UPDATE ON user_dead_letter_event
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- changeset lungatsewu:-11
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='slot';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_slot' AND event_object_table='slot';
CREATE TRIGGER set_last_modified_date_slot
    BEFORE UPDATE ON slot
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- changeset lungatsewu:-12
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='branch';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_slot' AND event_object_table='branch';
CREATE TRIGGER set_last_modified_date_slot
    BEFORE UPDATE
    ON branch
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- -- changeset lungatsewu:-13
-- -- preconditions onFail:MARK_RAN onError:HALT
-- -- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='staff';
-- -- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_slot' AND event_object_table='staff';
-- CREATE TRIGGER set_last_modified_date_slot
--     BEFORE UPDATE
--     ON staff
--     FOR EACH ROW
--     EXECUTE FUNCTION update_last_modified_date();

-- changeset lungatsewu:-14
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='branch_appointment_info';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_slot' AND event_object_table='branch_appointment_info';
CREATE TRIGGER set_last_modified_date_slot
    BEFORE UPDATE
    ON branch_appointment_info
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();