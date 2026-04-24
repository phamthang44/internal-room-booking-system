-- 1. Clean up any existing constraint to avoid duplicates or stale logic
-- We drop it first to ensure we replace the logic from V16 correctly.
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS exclude_booking_overlap;

-- 2. Create the robust exclusion constraint
-- This constraint prevents overlapping bookings for any status EXCEPT 'CANCELLED' and 'REJECTED'.
-- This ensures that:
--   - PENDING bookings (new requests) block others
--   - APPROVED bookings block others
--   - CHECKED_IN (currently in progress) block others
--   - COMPLETED (historical same-day) block others (as they occupied the slot)
--   - Any new future statuses added will be protected by default unless explicitly excluded here.
ALTER TABLE bookings
    ADD CONSTRAINT exclude_booking_overlap
        EXCLUDE USING gist (
            classroom_id WITH =,
            booking_date WITH =,
            tsrange((booking_date + start_time), (booking_date + end_time)) WITH &&
        )
        WHERE (status NOT IN ('CANCELLED', 'REJECTED') AND deleted_at IS NULL);
