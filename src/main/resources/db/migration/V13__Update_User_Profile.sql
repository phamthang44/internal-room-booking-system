-- 1. Add column (cho phép NULL trước)
ALTER TABLE users
    ADD phone VARCHAR(25) DEFAULT '';

ALTER TABLE users
    ADD student_code VARCHAR(10);

-- 2. Generate student_code cho dữ liệu cũ
-- Format: SE000001
UPDATE users
SET student_code = 'SE' || LPAD(id::text, 6, '0')
WHERE student_code IS NULL;

-- 3. Set NOT NULL sau khi đã có data
ALTER TABLE users
    ALTER COLUMN student_code SET NOT NULL;

-- 4. Add UNIQUE constraint
ALTER TABLE users
    ADD CONSTRAINT uc_users_student_code UNIQUE (student_code);