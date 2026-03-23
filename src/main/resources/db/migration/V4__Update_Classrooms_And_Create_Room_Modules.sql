-- 1. Tạo bảng room_types trước để làm gốc tham chiếu
CREATE TABLE IF NOT EXISTS room_types
(
    id              BIGSERIAL,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    name_key        VARCHAR(255) NOT NULL,
    description_key VARCHAR(255),
    CONSTRAINT pk_room_types PRIMARY KEY (id),
    CONSTRAINT uc_room_types_name_key UNIQUE (name_key) -- Đưa lên đầu để phục vụ INSERT ON CONFLICT
);

-- 2. Tạo bảng room_assets
CREATE TABLE IF NOT EXISTS room_assets
(
    id           BIGSERIAL,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW(),
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    classroom_id BIGINT       NOT NULL,
    url          VARCHAR(255) NOT NULL,
    asset_type   VARCHAR(50),
    is_primary   BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_room_assets PRIMARY KEY (id)
);

-- 3. Cập nhật cấu trúc bảng classrooms
-- Đổi tên cột (Dữ liệu cũ sẽ được giữ nguyên trong cột mới)
ALTER TABLE classrooms RENAME COLUMN room_number TO room_name;
ALTER TABLE classrooms ALTER COLUMN room_name TYPE VARCHAR(50);

-- Thêm cột mới (cho phép NULL tạm thời)
ALTER TABLE classrooms ADD COLUMN IF NOT EXISTS room_type_id BIGINT;

-- 4. Seed dữ liệu Room Types mẫu
INSERT INTO room_types (name_key, created_by) VALUES
                                                  ('room_type.lecture_hall', 'system'),
                                                  ('room_type.computer_lab', 'system'),
                                                  ('room_type.meeting_room', 'system')
ON CONFLICT (name_key) DO NOTHING;

-- 5. Data Migration: Gán loại phòng mặc định cho dữ liệu cũ
UPDATE classrooms
SET room_type_id = (SELECT id FROM room_types WHERE name_key = 'room_type.lecture_hall')
WHERE room_type_id IS NULL;

-- 6. Siết chặt ràng buộc sau khi đã có dữ liệu
ALTER TABLE classrooms ALTER COLUMN room_type_id SET NOT NULL;

-- Cập nhật Constraints cho classrooms
ALTER TABLE classrooms DROP CONSTRAINT IF EXISTS classrooms_building_id_room_number_key;
ALTER TABLE classrooms DROP CONSTRAINT IF EXISTS unique_classroom_name_per_building;
ALTER TABLE classrooms ADD CONSTRAINT unique_classroom_name_per_building UNIQUE (building_id, room_name);

ALTER TABLE classrooms
    ADD CONSTRAINT fk_classrooms_on_room_type FOREIGN KEY (room_type_id) REFERENCES room_types (id);

-- Cập nhật Constraints cho room_assets
ALTER TABLE room_assets
    ADD CONSTRAINT fk_assets_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE;

-- 7. Seed Translations (Dùng sub-query để lấy đúng ID vừa sinh ra)
INSERT INTO translations (entity_type, entity_id, locale, field_name, content)
VALUES
    ('ROOM_TYPE', (SELECT id FROM room_types WHERE name_key = 'room_type.lecture_hall'), 'vi', 'name', 'Phòng lý thuyết'),
    ('ROOM_TYPE', (SELECT id FROM room_types WHERE name_key = 'room_type.computer_lab'), 'vi', 'name', 'Phòng thực hành máy tính'),
    ('ROOM_TYPE', (SELECT id FROM room_types WHERE name_key = 'room_type.meeting_room'), 'vi', 'name', 'Phòng họp / Hội thảo'),
    ('ROOM_TYPE', (SELECT id FROM room_types WHERE name_key = 'room_type.lecture_hall'), 'en', 'name', 'Lecture Hall'),
    ('ROOM_TYPE', (SELECT id FROM room_types WHERE name_key = 'room_type.computer_lab'), 'en', 'name', 'Computer Lab'),
    ('ROOM_TYPE', (SELECT id FROM room_types WHERE name_key = 'room_type.meeting_room'), 'en', 'name', 'Meeting Room')
ON CONFLICT DO NOTHING;