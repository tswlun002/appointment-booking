-- changeset Lunga:1-func-apt
CREATE OR REPLACE FUNCTION update_last_modified_date()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_date = CURRENT_TIMESTAMP;
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- changeset Lunga:11 splitStatements:false

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = 'slot') THEN
        CREATE TABLE slot
        (
            id                   UUID PRIMARY KEY NOT NULL,
            day                  DATE        NOT NULL,
            start_time           TIME        NOT NULL,
            end_time             TIME        NOT NULL,
            booking_count        INTEGER     NOT NULL,
            max_booking_capacity INTEGER     NOT NULL,
            branch_id            VARCHAR(36) NOT NULL,
            status               VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
            created_at           TIMESTAMP   DEFAULT LOCALTIMESTAMP,
            last_modified_date   TIMESTAMP   DEFAULT LOCALTIMESTAMP,
            version              INTEGER     DEFAULT 1,
            CONSTRAINT positive_booking_count CHECK (booking_count >= 0),
            CONSTRAINT positive_max_booking_capacity CHECK (max_booking_capacity >= 0),
            CONSTRAINT positive_version CHECK (version >= 1),
            CONSTRAINT booking_capacity_check CHECK (booking_count <= max_booking_capacity),
            CONSTRAINT start_before_end CHECK (start_time < end_time),
            CONSTRAINT unique_slot_per_branch_day UNIQUE (branch_id, day, start_time, end_time),
            CONSTRAINT check_day_within_year CHECK (day <= CURRENT_DATE + INTERVAL '365 days')
            );
    END IF;
END $$;
-- ROLLBACK DROP TABLE slot

-- changeset Lunga:11a
CREATE INDEX IF NOT EXISTS idx_branch_day_status ON slot (branch_id, day, status);

-- changeset Lunga:12 splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_branch_id' AND table_name = 'slot') THEN
ALTER TABLE slot ADD CONSTRAINT fk_branch_id FOREIGN KEY (branch_id) REFERENCES branch (branch_id) ON DELETE RESTRICT;
END IF;
END $$;

-- changeset Lunga:13 splitStatements:false
DROP TRIGGER IF EXISTS set_last_modified_date_slot ON slot;
CREATE TRIGGER set_last_modified_date_slot
    BEFORE UPDATE ON slot FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_date();
