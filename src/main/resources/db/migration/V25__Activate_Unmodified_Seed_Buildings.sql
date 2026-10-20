-- V6 introduced is_active DEFAULT FALSE, so its IS NULL backfill never ran.
-- Repair only the untouched bootstrap buildings, preserving administrator changes.
UPDATE buildings
SET is_active = TRUE
WHERE (id, name_key, address) IN (
    (1, 'building.a', 'Block A'),
    (2, 'building.b', 'Block B')
)
  AND is_active = FALSE
  AND deleted_at IS NULL
  AND created_by IS NULL
  AND updated_by IS NULL
  AND created_at = updated_at;
