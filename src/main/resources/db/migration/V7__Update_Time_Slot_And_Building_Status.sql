-- 1. Dọn dẹp các ràng buộc cũ (Giữ nguyên logic của Thắng)
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS check_times,
                     DROP CONSTRAINT IF EXISTS check_times_valid,
                     DROP CONSTRAINT IF EXISTS chk_booking_time,
                     DROP CONSTRAINT IF EXISTS chk_booking_time_presence,
                     DROP CONSTRAINT IF EXISTS exclude_booking_overlap;

-- 2. Thêm cột mới (Tách riêng việc thêm cột và đổi kiểu dữ liệu)
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS booking_date DATE,
    ADD COLUMN IF NOT EXISTS time_slot_id INT;

ALTER TABLE buildings
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 3. Data Migration: PHẢI CHẠY KHI start_time VẪN LÀ timestamptz
-- Lấy phần Ngày từ cột cũ sang cột mới
UPDATE bookings
SET booking_date = (start_time AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
WHERE booking_date IS NULL AND start_time IS NOT NULL;

-- Gán ngày hiện tại cho những đơn chưa có ngày (để tránh lỗi NOT NULL bước sau)
UPDATE bookings SET booking_date = CURRENT_DATE WHERE booking_date IS NULL;

-- 4. Bây giờ mới được đổi kiểu dữ liệu cho start_time/end_time
-- Dùng USING để ép kiểu tường minh từ timestamp về time
ALTER TABLE bookings
    ALTER COLUMN booking_date SET NOT NULL,
    ALTER COLUMN start_time TYPE TIME WITHOUT TIME ZONE USING (start_time AT TIME ZONE 'UTC')::time,
    ALTER COLUMN end_time TYPE TIME WITHOUT TIME ZONE USING (end_time AT TIME ZONE 'UTC')::time;

-- 5. Thiết lập Foreign Key và Index
ALTER TABLE bookings
    ADD CONSTRAINT fk_booking_timeslot
        FOREIGN KEY (time_slot_id) REFERENCES time_slots(id);

CREATE INDEX IF NOT EXISTS idx_booking_user_date_effective
    ON bookings (user_id, booking_date)
    WHERE status IN ('PENDING', 'APPROVED');

-- 6. UNIQUE INDEX: Bảo vệ dữ liệu
CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_active_timeslot
    ON bookings (classroom_id, booking_date, time_slot_id)
    WHERE (time_slot_id IS NOT NULL AND status NOT IN ('CANCELLED', 'REJECTED'));

CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_active_timerange
    ON bookings (classroom_id, booking_date, start_time, end_time)
    WHERE (time_slot_id IS NULL AND status NOT IN ('CANCELLED', 'REJECTED'));

-- 7. GIST OVERLAP: Sửa lỗi cộng DATE + TIME
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Lưu ý: Phải dùng (booking_date + start_time) và ép kiểu về timestamp
ALTER TABLE bookings
    ADD CONSTRAINT exclude_booking_overlap
        EXCLUDE USING gist (
        classroom_id WITH =,
        booking_date WITH =,
        tsrange((booking_date + start_time), (booking_date + end_time)) WITH &&
        )
        WHERE (time_slot_id IS NULL AND status NOT IN ('CANCELLED', 'REJECTED'));

-- 8. CHECK CONSTRAINTS: Toàn vẹn dữ liệu
ALTER TABLE bookings
    ADD CONSTRAINT chk_time_order
        CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time);

-- Ràng buộc XOR chuẩn như Thắng mong muốn
ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_time_presence
        CHECK (
            (time_slot_id IS NOT NULL AND start_time IS NULL AND end_time IS NULL)
                OR
            (time_slot_id IS NULL AND start_time IS NOT NULL AND end_time IS NOT NULL)
            );

-- Dọn dẹp nốt
ALTER TABLE bookings DROP COLUMN IF EXISTS idempotency_key;