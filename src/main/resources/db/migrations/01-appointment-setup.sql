-- liquibase formatted sql

-- changeset Lunga:14 splitStatements:false

DO $$
    BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'appointment') THEN
        CREATE TABLE appointment
        (
            id                     UUID PRIMARY KEY         NOT NULL,
            slot_id                UUID                     NOT NULL REFERENCES slot (id) ON DELETE RESTRICT,
            branch_id              VARCHAR(50)              NOT NULL,
            customer_username      VARCHAR(10)              NOT NULL,
            service_type           VARCHAR(100)             NOT NULL,
            status                 VARCHAR(50)              NOT NULL,
            reference              VARCHAR(20)              NOT NULL,
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
            previous_slot_id       UUID REFERENCES slot (id) ON DELETE SET NULL,
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
    END IF;
END $$;
-- ROLLBACK DROP TABLE appointment

-- changeset Lunga:14a
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_booking_action_day_status ON appointment (customer_username, day)
    WHERE status IN ('BOOKED', 'CHECKED_IN', 'IN_PROGRESS');
-- ROLLBACK DROP INDEX idx_unique_booking_action_day_status;

-- changeset Lunga:14b
CREATE INDEX IF NOT EXISTS idx_appointment_branch_day ON appointment (branch_id, day, status);
-- ROLLBACK DROP INDEX idx_appointment_branch_day;

-- changeset Lunga:14c
CREATE INDEX IF NOT EXISTS idx_appointment_branch_reference ON appointment (branch_id, reference);
-- ROLLBACK DROP INDEX idx_appointment_branch_reference;

-- changeset Lunga:15 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_branch_id' AND table_name = 'appointment') THEN
ALTER TABLE appointment ADD CONSTRAINT fk_branch_id FOREIGN KEY (branch_id) REFERENCES branch (branch_id) ON DELETE RESTRICT;
END IF;
END $$;

-- changeset Lunga:16 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_assigned_consultant_id' AND table_name = 'appointment') THEN
ALTER TABLE appointment ADD CONSTRAINT fk_assigned_consultant_id FOREIGN KEY (assigned_consultant_id) REFERENCES staff (username) ON DELETE RESTRICT;
END IF;
END $$;