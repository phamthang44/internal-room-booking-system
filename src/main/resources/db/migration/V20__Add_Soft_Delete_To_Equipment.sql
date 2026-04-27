ALTER TABLE equipments ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_equipment_deleted_at
    ON equipments (deleted_at)
    WHERE deleted_at IS NULL;
