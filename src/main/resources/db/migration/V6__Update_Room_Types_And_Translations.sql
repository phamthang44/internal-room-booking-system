-- V6__Safe_Update_Room_Types_And_Translations.sql
ALTER TABLE buildings
    ADD is_active BOOLEAN DEFAULT FALSE;

UPDATE buildings SET is_active = TRUE WHERE buildings.is_active IS NULL;

-- 1. Thêm cột mới nếu chưa tồn tại (Dành cho logic Business mới)
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS min_capacity INT DEFAULT 0;
ALTER TABLE room_types ADD COLUMN IF NOT EXISTS max_capacity INT DEFAULT 1000;

-- 2. Cập nhật ràng buộc cho các bản ghi hiện có (Dựa trên name_key)
UPDATE room_types SET min_capacity = 30, max_capacity = 200 WHERE name_key = 'room_type.lecture_hall';
UPDATE room_types SET min_capacity = 20, max_capacity = 45 WHERE name_key = 'room_type.computer_lab';
UPDATE room_types SET min_capacity = 5, max_capacity = 20 WHERE name_key = 'room_type.meeting_room';

-- 1. Chèn loại phòng mới vào room_types (Dùng Long/BigInt cho ID vì Entity của Thắng dùng Long)
-- Đặt min = 20 và max = 30 để ép người dùng chỉ được nhập trong khoảng này
INSERT INTO room_types (name_key, description_key, min_capacity, max_capacity, created_at)
SELECT 'room_type.standard_classroom', 'room_type.standard_classroom.desc', 20, 30, NOW()
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE name_key = 'room_type.standard_classroom');

-- 2. Chèn bản dịch tiếng Việt
INSERT INTO translations (entity_type, entity_id, locale, field_name, content)
SELECT 'ROOM_TYPE', id, 'vi', 'name', 'Phòng học lý thuyết (Cố định)'
FROM room_types WHERE name_key = 'room_type.standard_classroom'
                  AND NOT EXISTS (
        SELECT 1 FROM translations
        WHERE entity_type = 'ROOM_TYPE' AND entity_id = room_types.id AND locale = 'vi'
    );

-- 3. Chèn bản dịch tiếng Anh
INSERT INTO translations (entity_type, entity_id, locale, field_name, content)
SELECT 'ROOM_TYPE', id, 'en', 'name', 'Standard Classroom'
FROM room_types WHERE name_key = 'room_type.standard_classroom'
                  AND NOT EXISTS (
        SELECT 1 FROM translations
        WHERE entity_type = 'ROOM_TYPE' AND entity_id = room_types.id AND locale = 'en'
    );

-- 3. Chỉ INSERT những Room Type chưa có để tránh lỗi Unique Constraint
INSERT INTO room_types (name_key, description_key, min_capacity, max_capacity, created_at)
SELECT 'room_type.auditorium', 'room_type.auditorium.desc', 100, 500, NOW()
WHERE NOT EXISTS (SELECT 1 FROM room_types WHERE name_key = 'room_type.auditorium');

-- 4. Chèn thêm bản dịch mới (Nếu chưa có)
-- Lưu ý: Bạn cần lấy ID của room_type tương ứng.
-- Ở đây mình dùng Subquery để an toàn hơn thay vì fix cứng ID.
INSERT INTO translations (entity_type, entity_id, locale, field_name, content)
SELECT 'ROOM_TYPE', id, 'vi', 'name', 'Hội trường lớn'
FROM room_types WHERE name_key = 'room_type.auditorium'
                  AND NOT EXISTS (
        SELECT 1 FROM translations
        WHERE entity_type = 'ROOM_TYPE'
          AND entity_id = room_types.id
          AND locale = 'vi'
    );