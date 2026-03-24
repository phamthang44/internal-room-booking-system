-- 1. Roles (Chỉ cần Audit cơ bản)
CREATE TABLE IF NOT EXISTS roles (
                                     id SERIAL PRIMARY KEY,
                                     name VARCHAR(20) NOT NULL UNIQUE,
                                     description VARCHAR(255),
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Users (Cần Full Audit + Soft Delete)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY, -- Khuyên dùng TSID ở tầng Application
                                     username VARCHAR(30) NOT NULL UNIQUE,
                                     email VARCHAR(255) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     role_id INT NOT NULL REFERENCES roles(id),
                                     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, BANNED, PENDING
                                     version INT DEFAULT 0, -- Optimistic Locking cho User profile
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ, -- Soft Delete sinh viên/giảng viên nghỉ học/nghỉ việc
                                     created_by VARCHAR(100),
                                     updated_by VARCHAR(100)
);



-- 4. Bookings (Trái tim - Cần Full Audit + Idempotency)
CREATE TABLE IF NOT EXISTS bookings (
                                        id BIGSERIAL PRIMARY KEY,
                                        user_id BIGINT NOT NULL REFERENCES users(id),
                                        classroom_id INT NOT NULL REFERENCES classrooms(id),
                                        start_time TIMESTAMPTZ NOT NULL,
                                        end_time TIMESTAMPTZ NOT NULL,
                                        status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, CANCELLED
                                        purpose TEXT NOT NULL,
                                        idempotency_key VARCHAR(100) UNIQUE NOT NULL, -- Chống trùng lặp request khi mạng lag
                                        rejection_reason TEXT, -- Lưu lý do nếu bị Staff từ chối
                                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        deleted_at TIMESTAMPTZ,

                                        CONSTRAINT check_times CHECK (end_time > start_time) -- Ràng buộc logic thời gian
);

-- 5. Refresh Tokens (Quản lý phiên đăng nhập)
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGSERIAL PRIMARY KEY,
                                              token VARCHAR(255) NOT NULL UNIQUE,
                                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                              expiry_date TIMESTAMPTZ NOT NULL,
                                              revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. Booking Approvals (Audit log phê duyệt)
CREATE TABLE IF NOT EXISTS booking_approvals (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                                                 approver_id BIGINT NOT NULL REFERENCES users(id),
                                                 approval_status VARCHAR(20) NOT NULL,
                                                 note TEXT,
                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1. Tòa nhà
CREATE TABLE buildings (
                           id BIGSERIAL PRIMARY KEY,
                           name_key VARCHAR(100), -- Ví dụ: 'building.a1'
                           address TEXT,
                           created_at TIMESTAMPTZ DEFAULT NOW(),
                           updated_at TIMESTAMPTZ DEFAULT NOW(),
                           deleted_at TIMESTAMPTZ
);

-- 2. Thiết bị (Danh mục dùng chung)
CREATE TABLE equipments (
                           id SERIAL PRIMARY KEY,
                           name_key VARCHAR(100) UNIQUE NOT NULL, -- Ví dụ: 'eq.projector'
                           description_key VARCHAR(255),
                           created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Phòng học
CREATE TABLE classrooms (
                            id BIGSERIAL PRIMARY KEY,
                            building_id BIGINT REFERENCES buildings(id),
                            room_number VARCHAR(20) NOT NULL,
                            capacity INT NOT NULL,
                            status VARCHAR(20) DEFAULT 'AVAILABLE',
                            version INT DEFAULT 0, -- Optimistic Locking
                            created_at TIMESTAMPTZ DEFAULT NOW(),
                            updated_at TIMESTAMPTZ DEFAULT NOW(),
                            deleted_at TIMESTAMPTZ,
                            UNIQUE (building_id, room_number)
);

-- 4. Bảng trung gian: Thiết bị trong mỗi phòng
CREATE TABLE classroom_equipment (
                                     classroom_id BIGINT REFERENCES classrooms(id) ON DELETE CASCADE,
                                     equipment_id INT REFERENCES equipments(id) ON DELETE CASCADE,
                                     quantity INT DEFAULT 1,
                                     PRIMARY KEY (classroom_id, equipment_id)
);

CREATE TABLE translations (
                              id BIGSERIAL PRIMARY KEY,
                              entity_type VARCHAR(50) NOT NULL, -- 'CLASSROOM', 'BUILDING', 'EQUIPMENT'
                              entity_id BIGINT NOT NULL,
                              locale VARCHAR(5) NOT NULL, -- 'vi', 'en'
                              field_name VARCHAR(50) NOT NULL, -- 'description', 'name'
                              content TEXT NOT NULL,
                              UNIQUE(entity_type, entity_id, locale, field_name)
);

-- 7. Indexes (Tối ưu truy vấn & Chống trùng lịch)
CREATE INDEX idx_booking_overlap ON bookings (classroom_id, start_time, end_time)
    WHERE status IN ('PENDING', 'APPROVED') AND deleted_at IS NULL; -- Chỉ check trùng trên các đơn có hiệu lực

CREATE INDEX idx_users_search ON users(username, email) WHERE deleted_at IS NULL;
CREATE INDEX idx_refresh_token_lookup ON refresh_tokens(token) WHERE revoked = FALSE;

-- Roles
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'Administrator with full access'),
                                          ('STAFF', 'Facility staff who approve bookings'),
                                          ('STUDENT', 'Student who can request bookings');

-- Buildings (Dùng key để map i18n nếu cần, hoặc dùng bảng translations)
INSERT INTO buildings (name_key, address) VALUES
                                              ('building.a', '123 University Ave, Block A'),
                                              ('building.b', '123 University Ave, Block B');

-- Equipment (Danh mục thiết bị dùng chung)
INSERT INTO equipments (name_key, description_key) VALUES
                                                       ('eq.projector', 'desc.high_quality_projector'),
                                                       ('eq.aircon', 'desc.central_air_conditioning'),
                                                       ('eq.whiteboard', 'desc.large_whiteboard');

-- Users (Password là 'password' đã được mã hóa giả lập)
INSERT INTO users (username, email, password, role_id, status) VALUES
                                                                   ('admin_thang', 'thang.admin@univ.edu.vn', '$2a$10$xyz...', 1, 'ACTIVE'),
                                                                   ('staff_lan', 'lan.staff@univ.edu.vn', '$2a$10$xyz...', 2, 'ACTIVE'),
                                                                   ('student_nam', 'nam.student@univ.edu.vn', '$2a$10$xyz...', 3, 'ACTIVE');

-- Classrooms
INSERT INTO classrooms (building_id, room_number, capacity, status) VALUES
                                                                        (1, 'A.101', 50, 'AVAILABLE'),
                                                                        (1, 'A.102', 30, 'AVAILABLE'),
                                                                        (2, 'B.201', 100, 'AVAILABLE');

-- Gán thiết bị cho phòng
INSERT INTO classroom_equipment (classroom_id, equipment_id, quantity) VALUES
                                                                           (1, 1, 1), -- Phòng A.101 có 1 máy chiếu
                                                                           (1, 2, 2), -- Phòng A.101 có 2 điều hòa
                                                                           (3, 1, 2); -- Phòng B.201 có 2 máy chiếu

INSERT INTO translations (entity_type, entity_id, locale, field_name, content) VALUES
-- Dịch cho Building A
('BUILDING', 1, 'vi', 'name', 'Tòa nhà A'),
('BUILDING', 1, 'en', 'name', 'Building A'),
-- Dịch cho Classroom A.101
('CLASSROOM', 1, 'vi', 'description', 'Phòng học lý thuyết tiêu chuẩn'),
('CLASSROOM', 1, 'en', 'description', 'Standard theory classroom'),
-- Dịch cho thiết bị máy chiếu
('EQUIPMENT', 1, 'vi', 'name', 'Máy chiếu HD'),
('EQUIPMENT', 1, 'en', 'name', 'HD Projector');

INSERT INTO bookings (user_id, classroom_id, start_time, end_time, status, purpose, idempotency_key) VALUES
                                                                                                         (3, 1, '2026-04-01 08:00:00+07', '2026-04-01 10:00:00+07', 'APPROVED', 'Học nhóm Java', 'key-001'),
                                                                                                         (3, 1, '2026-04-01 10:30:00+07', '2026-04-01 12:00:00+07', 'PENDING', 'Họp CLB Tech', 'key-002');