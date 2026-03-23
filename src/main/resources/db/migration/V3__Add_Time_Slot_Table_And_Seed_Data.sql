CREATE TABLE time_slots
(
    id SERIAL,
    slot_number INT NOT NULL UNIQUE,
    start_time    time WITHOUT TIME ZONE,
    end_time      time WITHOUT TIME ZONE,
    slot_name_key VARCHAR(100),
    CONSTRAINT pk_time_slots PRIMARY KEY (id)
);

INSERT INTO time_slots (slot_number, start_time, end_time, slot_name_key) VALUES
     (1, '07:00:00', '09:00:00', 'shift.1'),
     (2, '09:30:00', '11:30:00', 'shift.2'),
     (3, '13:00:00', '15:00:00', 'shift.3'),
     (4, '15:30:00', '17:30:00', 'shift.4');

INSERT INTO translations (entity_type, entity_id, locale, field_name, content) VALUES
-- Ca 1
('TIME_SLOT', 1, 'vi', 'name', 'Ca sáng 1'),
('TIME_SLOT', 1, 'en', 'name', 'Morning Shift 1'),
-- Ca 2
('TIME_SLOT', 2, 'vi', 'name', 'Ca sáng 2'),
('TIME_SLOT', 2, 'en', 'name', 'Morning Shift 2'),
-- Ca 3
('TIME_SLOT', 3, 'vi', 'name', 'Ca chiều 1'),
('TIME_SLOT', 3, 'en', 'name', 'Afternoon Shift 1'),
-- Ca 4
('TIME_SLOT', 4, 'vi', 'name', 'Ca chiều 2'),
('TIME_SLOT', 4, 'en', 'name', 'Afternoon Shift 2');

INSERT INTO bookings (user_id, classroom_id, start_time, end_time, status, purpose, idempotency_key, created_by, updated_by) VALUES
-- CA 1: Đã duyệt (Trạng thái: OCCUPIED/BẬN)
(6, 1, '2026-04-01 07:00:00+07', '2026-04-01 09:00:00+07', 'APPROVED', 'Học nhóm Java', 'key-slot-1-approved', 'SYSTEM', 'SYSTEM'),
-- CA 2: Đang chờ duyệt (Trạng thái: PENDING/ĐANG CHỜ - Vẫn tính là OCCUPIED để chặn người khác đặt đè)
(6, 1, '2026-04-01 09:30:00+07', '2026-04-01 11:30:00+07', 'PENDING', 'Họp CLB Tech', 'key-slot-2-pending', 'SYSTEM', 'SYSTEM'),

-- CA 3: Trống (Không có record nào trong DB cho slot này -> Trạng thái: AVAILABLE)

-- CA 4: Đã bị từ chối hoặc hủy (Trạng thái: AVAILABLE - Vì đơn này không còn hiệu lực giữ chỗ)
(6, 1, '2026-04-01 15:30:00+07', '2026-04-01 17:30:00+07', 'REJECTED', 'Dạy thêm Toán', 'key-slot-4-rejected', 'SYSTEM', 'SYSTEM');