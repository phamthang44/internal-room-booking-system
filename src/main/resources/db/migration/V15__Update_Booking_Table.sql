ALTER TABLE bookings
    ADD actual_attendees INTEGER;

ALTER TABLE bookings
    ADD attendance_status VARCHAR(30);

ALTER TABLE bookings
    ADD checkin_time TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE bookings
    ADD checkout_time TIMESTAMP WITHOUT TIME ZONE;
