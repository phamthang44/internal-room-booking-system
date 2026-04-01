ALTER TABLE bookings
    ADD attendees INTEGER;

UPDATE bookings SET attendees = 20 WHERE attendees IS NULL;

ALTER TABLE bookings
    ALTER COLUMN attendees SET NOT NULL;

