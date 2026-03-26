-- 1. Dọn dẹp các ràng buộc cũ để tránh xung đột kiểu dữ liệu
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS check_times,
                     DROP CONSTRAINT IF EXISTS check_times_valid,
                     DROP CONSTRAINT IF EXISTS chk_booking_time,
                     DROP CONSTRAINT IF EXISTS chk_booking_time_presence;

-- 2. Cập nhật cấu trúc bảng (Thêm cột và đổi kiểu dữ liệu)
-- Sử dụng ALTER TABLE gộp để tối ưu hiệu năng
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS booking_date DATE,
    ADD COLUMN IF NOT EXISTS time_slot_id INT,

    ALTER COLUMN start_time TYPE TIME USING (start_time AT TIME ZONE 'UTC')::time,
    ALTER COLUMN end_time TYPE TIME USING (end_time AT TIME ZONE 'UTC')::time;

ALTER TABLE buildings
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 3. Data Migration: Cập nhật dữ liệu cho cột mới từ dữ liệu cũ (nếu có)
UPDATE bookings
SET booking_date = COALESCE(booking_date, (start_time AT TIME ZONE 'UTC')::date); -- Hoặc logic mapping từ start_time cũ

-- 4. Áp dụng ràng buộc NOT NULL sau khi đã update data
ALTER TABLE bookings
    ALTER COLUMN booking_date SET NOT NULL;

-- 5. Thiết lập Foreign Key
ALTER TABLE bookings
    ADD CONSTRAINT fk_booking_timeslot
        FOREIGN KEY (time_slot_id) REFERENCES time_slots(id);

-- 6. UNIQUE INDEX: Lớp phòng thủ quan trọng nhất
-- Chỉ chặn trùng nếu đơn đặt phòng đang ở trạng thái hiệu lực (Tránh chặn đơn đã CANCELLED/REJECTED)
CREATE UNIQUE INDEX uq_booking_active_timeslot
    ON bookings (classroom_id, booking_date, time_slot_id)
    WHERE (time_slot_id IS NOT NULL AND status NOT IN ('CANCELLED', 'REJECTED'));

CREATE UNIQUE INDEX uq_booking_active_timerange
    ON bookings (classroom_id, booking_date, start_time, end_time)
    WHERE (time_slot_id IS NULL AND status NOT IN ('CANCELLED', 'REJECTED'));

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Chặn Overlap cho giờ tự do (Dùng tstzrange bằng cách kết hợp date + time lại để so sánh chính xác)
ALTER TABLE bookings
    ADD CONSTRAINT exclude_booking_overlap
        EXCLUDE USING gist (
        classroom_id WITH =,
        booking_date WITH =,
        tstzrange(booking_date + start_time, booking_date + end_time) WITH &&
        )
        WHERE (time_slot_id IS NULL AND status NOT IN ('CANCELLED', 'REJECTED'));

-- 7. CHECK CONSTRAINTS: Đảm bảo tính toàn vẹn dữ liệu
-- Ràng buộc 1: Start luôn phải trước End
ALTER TABLE bookings
    ADD CONSTRAINT chk_time_order
        CHECK (start_time IS NULL OR end_time IS NULL OR start_time < end_time);

-- Ràng buộc 2: Logic giữa Slot và Giờ tự do
-- Cho phép có cả Slot và Giờ (để lưu snapshot) nhưng bắt buộc phải có ít nhất 1 trong 2 cách định nghĩa thời gian
ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_time_presence
        CHECK (
            (time_slot_id IS NOT NULL AND start_time IS NULL AND end_time IS NULL)
                OR
            (time_slot_id IS NULL AND start_time IS NOT NULL AND end_time IS NOT NULL)
            );
