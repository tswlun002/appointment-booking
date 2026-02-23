-- liquibase formatted sql

-- changeset Lunga:1-func-staff
CREATE OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date = CURRENT_TIMESTAMP;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- changeset Lunga:7 splitStatements:false

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'staff') THEN
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
    END IF;
END $$;
-- ROLLBACK DROP TABLE staff

-- changeset Lunga:7a
CREATE INDEX IF NOT EXISTS idx_branch_id_username_status ON staff (branch_id, username, status);
-- ROLLBACK DROP INDEX idx_branch_id_username_status;


-- changeset Lunga:8 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_branch_id' AND table_name = 'staff') THEN
ALTER TABLE staff ADD CONSTRAINT fk_branch_id FOREIGN KEY (branch_id) REFERENCES branch (branch_id) ON DELETE RESTRICT;
END IF;
END $$;

-- changeset Lunga:9 splitStatements:false
DROP TRIGGER IF EXISTS set_last_modified_date_staff ON staff;
CREATE TRIGGER set_last_modified_date_staff
    BEFORE UPDATE ON staff FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();

-- changeset Lunga:10 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'branch_staff_assignment') THEN
        CREATE TABLE branch_staff_assignment
        (
            id         SERIAL PRIMARY KEY,
            branch_id  VARCHAR(36) NOT NULL,
            username   VARCHAR(32) NOT NULL,
            day        DATE        NOT NULL,
            created_at TIMESTAMP DEFAULT LOCALTIMESTAMP,
            CONSTRAINT unique_branch_id_username_day UNIQUE (branch_id, username, day),
            CONSTRAINT check_day_not_future CHECK (day <= CURRENT_DATE + INTERVAL '365 days')
            );
    END IF;
END $$;
-- ROLLBACK DROP TABLE branch_staff_assignment

-- changeset Lunga:10a
CREATE INDEX IF NOT EXISTS idx_branch_date_status ON branch_staff_assignment (branch_id, day, username);


