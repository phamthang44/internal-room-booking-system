ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_time_logic
        CHECK (
            (start_time IS NULL AND end_time IS NULL) -- Trường hợp dùng bảng trung gian BookingTimeSlot
                OR
            (start_time IS NOT NULL AND end_time IS NOT NULL) -- Trường hợp dùng giờ tự do
            );