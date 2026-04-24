ALTER TABLE bookings DROP CONSTRAINT IF EXISTS exclude_booking_overlap;

ALTER TABLE bookings
    ADD CONSTRAINT exclude_booking_overlap
        EXCLUDE USING gist (
            classroom_id WITH =,
            booking_date WITH =,
            tsrange(booking_date + start_time, booking_date + end_time) WITH &&
        )
        WHERE (status IN ('PENDING', 'APPROVED', 'CHECKED_IN', 'COMPLETED') AND deleted_at IS NULL);
