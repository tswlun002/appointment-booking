-- liquibase formatted sql

-- changeset Lunga:1
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM  information_schema.tables  where table_name='user_dead_letter_event';
CREATE
OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date
= CURRENT_TIMESTAMP;
RETURN NEW;
END
$$
LANGUAGE plpgsql;

-- changeset Lunga:2
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
    CONSTRAINT unique_branch_id UNIQUE (branch_id),
    CONSTRAINT check_open_before_close CHECK (close_time IS NULL OR open_time < close_time)
);
--ROLLBACK DROP TABLE branch

-- changeset Lunga:3
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='branch';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_branch' AND event_object_table='branch';
CREATE TRIGGER set_last_modified_date_branch
    BEFORE UPDATE
    ON branch
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();


-- changeset Lunga:4
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='branch_appointment_info';
-- comment: /* Create table branchAppointmentInfo only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE branch_appointment_info
(
    id                 SERIAL PRIMARY KEY,
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    branch_id  SERIAL NOT NULL REFERENCES branch(id) ON DELETE CASCADE,
    branch_business_id  VARCHAR(36) NOT NULL,
    branch_key         VARCHAR(16)      NOT NULL,
    slot_duration      INTEGER          NOT NULL,
    utilization_factor DOUBLE PRECISION NOT NULL,
    staff_count        INTEGER          NOT NULL,
    day_type           VARCHAR(16)      NOT NULL,
    CONSTRAINT unique_branch_day UNIQUE (branch_id, day_type),
    CONSTRAINT check_slot_duration_positive CHECK (slot_duration > 0),
    CONSTRAINT check_utilization_factor_range CHECK (utilization_factor > 0 AND utilization_factor <= 1),
    CONSTRAINT check_staff_count_positive CHECK (staff_count > 0)
);
--ROLLBACK DROP TABLE branch_appointment_info

-- changeset Lunga:5
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='branch_appointment_info';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_branch_appointment_info' AND event_object_table='branch_appointment_info';
CREATE TRIGGER set_last_modified_date_branch_appointment_info
    BEFORE UPDATE
    ON branch_appointment_info
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();


-- changeset Lunga:6
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='staff';
-- comment: /* Create table staff only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE staff
(
    id                 SERIAL PRIMARY KEY,
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    username           VARCHAR(32) NOT NULL,
    branch_id          VARCHAR(36) NOT NULL,
    status             VARCHAR(16),
    CONSTRAINT unique_username UNIQUE (username)
);
CREATE INDEX idx_branch_id_username_status ON staff (branch_id, username, status);
-- ROLLBAK DROP TABLE staff

-- changeset Lunga:7
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='staff';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'staff' AND column_name = 'username';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_entity' AND column_name = 'username';
ALTER TABLE staff ADD CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES user_entity (username) ON DELETE RESTRICT;
-- ROLLBACK ALTER TABLE staff DROP CONSTRAINT fk_username;

-- changeset Lunga:8
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='staff';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'staff' AND column_name = 'branch_id';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'branch' AND column_name = 'branch_id';
ALTER TABLE staff ADD CONSTRAINT fk_branch_id FOREIGN KEY (branch_id) REFERENCES branch (branch_id) ON DELETE RESTRICT;
-- ROLLBACK ALTER TABLE staff DROP CONSTRAINT fk_branch_id;

-- changeset Lunga:9
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='staff';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_staff' AND event_object_table='staff';
CREATE TRIGGER set_last_modified_date_staff
    BEFORE UPDATE
    ON staff
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();


-- changeset Lunga:10
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='branch_staff_assignment';
-- comment: /* Create table branch_staff_assignment only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE branch_staff_assignment
(
    id        SERIAL PRIMARY KEY,
    branch_id VARCHAR(36) NOT NULL,
    username   VARCHAR(32) NOT NULL,
    day        DATE        NOT NULL,
    created_at TIMESTAMP DEFAULT LOCALTIMESTAMP,
    CONSTRAINT  unique_branch_id_username_day UNIQUE(branch_id, username, day),
    CONSTRAINT check_day_not_future CHECK (day <= CURRENT_DATE + INTERVAL '365 days')
    );
-- Index for querying "who was working on dateOfSlots X?"
CREATE INDEX idx_branch_date_status ON branch_staff_assignment (branch_id, day, username);
-- ROLLBAK DROP TABLE branch_staff_assignment


-- changeset Lunga:11
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='address';
-- comment: /* Create table ADDRESS only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE address
(
    branch_id          SERIAL PRIMARY KEY REFERENCES branch (id) ON DELETE CASCADE,
    created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
    street_number      VARCHAR(16),
    street_name        VARCHAR(32),
    suburb             VARCHAR(32),
    city               VARCHAR(32),
    province           VARCHAR(16),
    postal_code        VARCHAR(16),
    country            VARCHAR(32),
    CONSTRAINT unique_address UNIQUE (street_number, street_name, suburb, city, province, postal_code, country),
    CONSTRAINT check_postal_code_format CHECK (postal_code ~ '^[0-9]{4}$')
    );
--ROLLBACK DROP TABLE address


-- changeset Lunga:12
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='slot';
-- comment: /* Create table SLOT only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE slot
(
    id                 UUID PRIMARY KEY NOT NULL,
    day                DATE        NOT NULL,
    start_time         TIME        NOT NULL,
    end_time           TIME        NOT NULL,
    booking_count      INTEGER     NOT NULL,
    max_booking_capacity      INTEGER     NOT NULL,
    branch_id          VARCHAR(36) NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    created_at         TIMESTAMP   DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP   DEFAULT LOCALTIMESTAMP,
    version            INTEGER     DEFAULT 1,
    CONSTRAINT positive_booking_count CHECK (booking_count >= 0),
    CONSTRAINT positive_max_booking_capacity CHECK (max_booking_capacity >= 0),
    CONSTRAINT positive_version CHECK (version >= 1),
    CONSTRAINT booking_capacity_check CHECK (booking_count <= max_booking_capacity),
    CONSTRAINT start_before_end CHECK (start_time < end_time),
    CONSTRAINT unique_slot_per_branch_day UNIQUE (branch_id, day, start_time, end_time),
    CONSTRAINT check_day_within_year CHECK (day <= CURRENT_DATE + INTERVAL '365 days')
    );
CREATE INDEX idx_branch_day_status ON slot (branch_id, day,status);
-- ROLLBACK DROP TABLE slot

-- changeset Lunga:13
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='slot';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'slot' AND column_name = 'branch_id';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'branch' AND column_name = 'branch_id';
ALTER TABLE slot ADD CONSTRAINT fk_branch_id FOREIGN KEY (branch_id) REFERENCES branch (branch_id) ON DELETE RESTRICT;
-- ROLLBACK ALTER TABLE slot DROP CONSTRAINT fk_branch_id;

-- changeset Lunga:14
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='slot';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_slot' AND event_object_table='slot';
CREATE TRIGGER set_last_modified_date_slot
    BEFORE UPDATE
    ON slot
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();


-- changeset Lunga:15
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='appointment';
-- comment: /* Create table APPOINTMENT only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE appointment
(
    id                     UUID PRIMARY KEY         NOT NULL,
    slot_id                UUID                     NOT NULL REFERENCES slot (id) ON DELETE RESTRICT,
    branch_id              VARCHAR(50)              NOT NULL,
    customer_username      VARCHAR(10)              NOT NULL,
    service_type           VARCHAR(100)             NOT NULL,
    status                 VARCHAR(50)              NOT NULL,
    reference      VARCHAR(20)                      NOT NULL,
    date_time              TIMESTAMP WITH TIME ZONE NOT NULL,
    version                INT                      NOT NULL DEFAULT 0,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    checked_in_at          TIMESTAMP WITH TIME ZONE,
    in_progress_at         TIMESTAMP WITH TIME ZONE,
    completed_at           TIMESTAMP WITH TIME ZONE,
    terminated_at          TIMESTAMP WITH TIME ZONE,
    terminated_by          VARCHAR(50),
    termination_reason     VARCHAR(50),
    termination_notes      VARCHAR(500),
    assigned_consultant_id VARCHAR(10),
    service_notes          VARCHAR(1000),
    previous_slot_id       UUID                     REFERENCES slot (id) ON DELETE SET NULL,
    reschedule_count       INT                      NOT NULL DEFAULT 0,
    day                    DATE GENERATED ALWAYS AS ((date_time AT TIME ZONE 'UTC')::date) STORED,
     CONSTRAINT check_reschedule_count_non_negative CHECK (reschedule_count >= 0),
     CONSTRAINT check_version_non_negative CHECK (version >= 1),
     CONSTRAINT check_updated_after_created CHECK (updated_at >= created_at),
     CONSTRAINT check_checked_in_after_created CHECK (checked_in_at IS NULL OR checked_in_at >= created_at),
     CONSTRAINT check_in_progress_after_checked_in CHECK (in_progress_at IS NULL OR checked_in_at IS NULL OR in_progress_at >= checked_in_at),
     CONSTRAINT check_completed_after_in_progress CHECK (completed_at IS NULL OR in_progress_at IS NULL OR completed_at >= in_progress_at),
     CONSTRAINT check_terminated_after_created CHECK (terminated_at IS NULL OR terminated_at >= created_at),
     CONSTRAINT check_termination_reason_when_terminated CHECK ((terminated_at IS NULL AND termination_reason IS NULL) OR (terminated_at IS NOT NULL AND termination_reason IS NOT NULL)),
     CONSTRAINT check_terminated_by_when_terminated CHECK ((terminated_at IS NULL AND terminated_by IS NULL) OR (terminated_at IS NOT NULL AND terminated_by IS NOT NULL)),
     CONSTRAINT check_only_one_completion_type CHECK ((completed_at IS NULL AND terminated_at IS NULL) OR (completed_at IS NOT NULL AND terminated_at IS NULL) OR (completed_at IS NULL AND terminated_at IS NOT NULL))
 );
--// Ensure user has one booked appointment per day
CREATE UNIQUE INDEX idx_unique_booking_action_day_status
    ON appointment (customer_username, day) WHERE status IN('BOOKED', 'CHECKED_IN','IN_PROGRESS');
CREATE INDEX idx_appointment_branch_day ON appointment (branch_id, day,status);
CREATE INDEX idx_appointment_branch_reference ON appointment (branch_id,reference);
-- ROLLBACK DROP TABLE appointment

-- changeset Lunga:16
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='appointment';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'appointment' AND column_name = 'branch_id';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'branch' AND column_name = 'branch_id';
ALTER TABLE appointment ADD CONSTRAINT fk_branch_id FOREIGN KEY (branch_id) REFERENCES branch (branch_id) ON DELETE RESTRICT;
-- ROLLBACK ALTER TABLE appointment DROP CONSTRAINT fk_branch_id;

-- changeset Lunga:17
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='appointment';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'appointment' AND column_name = 'assigned_consultant_id';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'staff' AND column_name = 'username';
ALTER TABLE appointment ADD CONSTRAINT fk_assigned_consultant_id FOREIGN KEY (assigned_consultant_id) REFERENCES staff (username) ON DELETE RESTRICT;
-- ROLLBACK ALTER TABLE appointment DROP CONSTRAINT fk_assigned_consultant_id;

-- changeset Lunga:18
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='appointment';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'appointment' AND column_name = 'customer_username';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_entity' AND column_name = 'username';
ALTER TABLE appointment ADD CONSTRAINT fk_customer_username FOREIGN KEY (customer_username) REFERENCES user_entity (username) ON DELETE RESTRICT;
-- ROLLBACK ALTER TABLE appointment DROP CONSTRAINT fk_customer_username;


-- changeset Lunga:19
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables  where table_name='otp';
-- comment: /* Create table OTP only if it does not exist. ZERO means the schema does not exist*/
CREATE TABLE otp(
    id                    SERIAL      NOT NULL,
    code                  VARCHAR(32) NOT NULL,
    created_date          TIMESTAMP   NOT NULL DEFAULT LOCALTIMESTAMP,
    expire_date           timestamp NULL,
    purpose               VARCHAR(32) NOT NULL,
    status                VARCHAR(32) NOT NULL,
    username              CHAR(10)    NOT NULL,
    verification_attempts INT                  DEFAULT 0 NULL,
    CONSTRAINT pk_otp PRIMARY KEY (id),
    CONSTRAINT unique_username_code_status UNIQUE (username, code, status),
    CONSTRAINT check_verification_attempts_non_negative CHECK (verification_attempts >= 0),
    CONSTRAINT check_expire_after_created CHECK (expire_date IS NULL OR expire_date > created_date)
);
CREATE INDEX idx_username_code_status ON otp (username, code, status);
CREATE INDEX idx_username_status ON otp (username, status);
-- CREATE INDEX idx_otp_expire_date ON otp (expire_date) WHERE status = 'PENDING';
-- ROLLBACK DROP TABLE otp

-- changeset Lunga:20
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='otp';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'otp' AND column_name = 'username';
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'user_entity' AND column_name = 'username';
ALTER TABLE otp
    ADD CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES user_entity (value) ON DELETE CASCADE;
-- Foreign key reference
-- ROLLBACK ALTER TABLE otp DROP CONSTRAINT unique_username;


-- changeset Lunga:21
--preconditions onFail:MARK_RAN onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'user_dead_letter_event' AND table_schema = current_schema()
CREATE TABLE user_dead_letter_event
(
    event_id           VARCHAR(255) PRIMARY KEY,
    key                VARCHAR(255) NOT NULL,
    value              TEXT         NOT NULL,
    topic              VARCHAR(255) NOT NULL,
    partition          INTEGER,
    event_offset       INTEGER,
    headers            TEXT,
    is_retryable       BOOLEAN      NOT NULL DEFAULT FALSE,
    retry_count        INTEGER      NOT NULL DEFAULT 0,
    exception          TEXT         NOT NULL,
    exception_class    VARCHAR(255),
    exception_cause        VARCHAR(255),
    stack_trace        TEXT         NOT NULL,
    trace_id           VARCHAR(255) NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    published_time     TIMESTAMP    NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    last_modified_date TIMESTAMP    NOT NULL DEFAULT LOCALTIMESTAMP,
    data           TEXT ,
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
CREATE INDEX idx_user_dead_letter_event_trace_id ON user_dead_letter_event (trace_id);
CREATE INDEX idx_user_dead_letter_event_topic ON user_dead_letter_event (topic);
CREATE INDEX idx_user_dead_letter_event_is_retryable ON user_dead_letter_event (is_retryable);
CREATE INDEX idx_user_dead_letter_event_status ON user_dead_letter_event (status);
CREATE INDEX idx_user_dead_letter_event_time_stamp ON user_dead_letter_event (published_time);
--rollback DROP TABLE user_dead_letter_event;

-- changeset Lunga:22
-- preconditions onFail:MARK_RAN onError:HALT
-- precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM  information_schema.tables  where table_name='user_dead_letter_event';
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name='set_last_modified_date_event' AND event_object_table='user_dead_letter_event';
CREATE TRIGGER set_last_modified_date_event
    BEFORE UPDATE
    ON user_dead_letter_event
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();