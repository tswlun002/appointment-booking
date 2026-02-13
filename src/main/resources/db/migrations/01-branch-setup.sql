-- liquibase formatted sql

-- changeset Lunga:1-func-branch
CREATE OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date = CURRENT_TIMESTAMP;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- changeset Lunga:2 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'branch') THEN
        CREATE TABLE branch
        (
            id                 SERIAL PRIMARY KEY,
            created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
            last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
            branch_id          VARCHAR(36) NOT NULL,
            branch_name        VARCHAR(72) NOT NULL,
            CONSTRAINT unique_branch_id UNIQUE (branch_id),
            CONSTRAINT branch_id_is_not_blank CHECK (TRIM(branch_id) <> ''),
            CONSTRAINT branch_name_is_not_blank CHECK (TRIM(branch_name) <> '')
        );
    END IF;
END $$;
--ROLLBACK DROP TABLE branch


-- changeset Lunga:3 splitStatements:false
DROP TRIGGER IF EXISTS set_last_modified_date_branch ON branch;
CREATE TRIGGER set_last_modified_date_branch
    BEFORE UPDATE ON branch FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- changeset Lunga:4 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'branch_appointment_info') THEN
        CREATE TABLE branch_appointment_info
        (
            id                 SERIAL PRIMARY KEY,
            created_at         TIMESTAMP DEFAULT LOCALTIMESTAMP,
            last_modified_date TIMESTAMP DEFAULT LOCALTIMESTAMP,
            branch_id          SERIAL           NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
            branch_business_id VARCHAR(36)      NOT NULL,
            branch_key         VARCHAR(16)      NOT NULL,
            slot_duration      INTEGER          NOT NULL,
            utilization_factor DOUBLE PRECISION NOT NULL,
            staff_count        INTEGER          NOT NULL,
            day                VARCHAR(16)      NOT NULL,
            max_booking_capacity INTEGER        NOT NULL,
            CONSTRAINT unique_branch_day UNIQUE (branch_id, day),
            CONSTRAINT check_slot_duration_positive CHECK (slot_duration > 0),
            CONSTRAINT check_utilization_factor_range CHECK (utilization_factor > 0 AND utilization_factor <= 1),
            CONSTRAINT check_staff_count_positive CHECK (staff_count > 0),
            CONSTRAINT check_day_branch_key_equal CHECK (day = branch_key),
            CONSTRAINT check_max_booking_capacity_positive CHECK (max_booking_capacity>0)
        );
    END IF;
END $$;
--ROLLBACK DROP TABLE branch_appointment_info

-- changeset Lunga:5 splitStatements:false
DROP TRIGGER IF EXISTS set_last_modified_date_branch_appointment_info ON branch_appointment_info;
CREATE TRIGGER set_last_modified_date_branch_appointment_info
    BEFORE UPDATE ON branch_appointment_info FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- changeset Lunga:6 splitStatements:false

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'operation_hours_override') THEN
        CREATE TABLE operation_hours_override
        (
            id                 SERIAL PRIMARY KEY,
            branch_id          SERIAL       NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
            branch_key         DATE         NOT NULL,
            branch_business_id VARCHAR(36)  NOT NULL,
            effective_date     DATE         NOT NULL,
            open_at            TIME         NOT NULL,
            close_at           TIME         NOT NULL,
            closed             BOOLEAN      NOT NULL,
            reason             VARCHAR(255) NOT NULL,
            created_date       TIMESTAMP DEFAULT LOCALTIMESTAMP,
            last_modified_date TIMESTAMP,
            CONSTRAINT check_effective_day_future CHECK (effective_date >= CURRENT_DATE),
            CONSTRAINT check_open_at_close_at_valid CHECK (open_at < close_at),
            CONSTRAINT check_reason_not_blank CHECK (TRIM(reason) <> ''),
            CONSTRAINT check_effective_date_day_equal CHECK (effective_date = branch_key),
            CONSTRAINT unique_branch_id_effective_date UNIQUE (branch_id, effective_date)
        );
    END IF;
END $$;
--ROLLBACK DROP TABLE operation_hours_override
