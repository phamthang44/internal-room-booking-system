-- ==========================================================
-- 1. NHÓM BẢNG ĐỘC LẬP (MASTER DATA)
-- ==========================================================

-- Roles
CREATE TABLE IF NOT EXISTS roles (
                                     id SERIAL PRIMARY KEY,
                                     name VARCHAR(20) NOT NULL UNIQUE,
                                     description VARCHAR(255),
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Buildings
CREATE TABLE IF NOT EXISTS buildings (
                                         id BIGSERIAL PRIMARY KEY,
                                         name_key VARCHAR(100),
                                         address TEXT,
                                         created_at TIMESTAMPTZ DEFAULT NOW(),
                                         updated_at TIMESTAMPTZ DEFAULT NOW(),
                                         deleted_at TIMESTAMPTZ,
                                         created_by VARCHAR(255),
                                         updated_by VARCHAR(255)
);

-- Equipments
CREATE TABLE IF NOT EXISTS equipments (
                                          id SERIAL PRIMARY KEY,
                                          name_key VARCHAR(100) UNIQUE NOT NULL,
                                          description_key VARCHAR(255),
                                          created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Idempotency Keys (Hạ tầng)
CREATE TABLE IF NOT EXISTS idempotency_keys (
                                                id SERIAL PRIMARY KEY,
                                                key_hash VARCHAR(128) NOT NULL UNIQUE,
                                                resoure_path VARCHAR(255),
                                                response_code INTEGER,
                                                response_body TEXT,
                                                request_fingerprint VARCHAR(255),
                                                expires_at TIMESTAMPTZ,
                                                created_at TIMESTAMPTZ DEFAULT NOW(),
                                                updated_at TIMESTAMPTZ DEFAULT NOW(),
                                                created_by VARCHAR(255),
                                                updated_by VARCHAR(255)
);

-- ==========================================================
-- 2. NHÓM BẢNG PHỤ THUỘC CẤP 1
-- ==========================================================

-- Users (Phụ thuộc Roles)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     username VARCHAR(30) NOT NULL UNIQUE,
                                     full_name VARCHAR(100) NOT NULL,
                                     email VARCHAR(255) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     role_id INT NOT NULL REFERENCES roles(id),
                                     status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                     version INT DEFAULT 0,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ,
                                     created_by VARCHAR(100),
                                     updated_by VARCHAR(100)
);

-- Classrooms (Phụ thuộc Buildings)
CREATE TABLE IF NOT EXISTS classrooms (
                                          id BIGSERIAL PRIMARY KEY,
                                          building_id BIGINT NOT NULL REFERENCES buildings(id),
                                          room_number VARCHAR(20) NOT NULL,
                                          capacity INTEGER NOT NULL,
                                          status VARCHAR(20) DEFAULT 'AVAILABLE',
                                          version INTEGER DEFAULT 0,
                                          created_at TIMESTAMPTZ DEFAULT NOW(),
                                          updated_at TIMESTAMPTZ DEFAULT NOW(),
                                          deleted_at TIMESTAMPTZ,
                                          created_by VARCHAR(255),
                                          updated_by VARCHAR(255),
                                          UNIQUE (building_id, room_number)
);

-- ==========================================================
-- 3. NHÓM BẢNG PHỤ THUỘC CẤP 2 (CORE LOGIC)
-- ==========================================================

-- Bookings (Phụ thuộc Users & Classrooms)
CREATE TABLE IF NOT EXISTS bookings (
                                        id BIGSERIAL PRIMARY KEY,
                                        user_id BIGINT NOT NULL REFERENCES users(id),
                                        classroom_id BIGINT NOT NULL REFERENCES classrooms(id),
                                        start_time TIMESTAMPTZ NOT NULL,
                                        end_time TIMESTAMPTZ NOT NULL,
                                        status VARCHAR(20) DEFAULT 'PENDING',
                                        purpose TEXT NOT NULL,
                                        idempotency_key VARCHAR(100) UNIQUE NOT NULL,
                                        rejection_reason TEXT,
                                        version INTEGER DEFAULT 0,
                                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        deleted_at TIMESTAMPTZ,
                                        created_by VARCHAR(255),
                                        updated_by VARCHAR(255),
                                        CONSTRAINT check_times CHECK (end_time > start_time)
);

-- Classroom Equipment (Bảng trung gian)
CREATE TABLE IF NOT EXISTS classroom_equipment (
                                                   classroom_id BIGINT NOT NULL REFERENCES classrooms(id) ON DELETE CASCADE,
                                                   equipment_id INTEGER NOT NULL REFERENCES equipments(id) ON DELETE CASCADE,
                                                   quantity INTEGER DEFAULT 1,
                                                   PRIMARY KEY (classroom_id, equipment_id)
);

-- Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGSERIAL PRIMARY KEY,
                                              token VARCHAR(255) NOT NULL UNIQUE,
                                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                              expiry_date TIMESTAMPTZ NOT NULL,
                                              revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Booking Approvals
CREATE TABLE IF NOT EXISTS booking_approvals (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                                                 approver_id BIGINT NOT NULL REFERENCES users(id),
                                                 approval_status VARCHAR(20) NOT NULL,
                                                 note TEXT,
                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Translations (Dùng chung)
CREATE TABLE IF NOT EXISTS translations (
                                            id BIGSERIAL PRIMARY KEY,
                                            entity_type VARCHAR(50) NOT NULL,
                                            entity_id BIGINT NOT NULL,
                                            locale VARCHAR(5) NOT NULL,
                                            field_name VARCHAR(50) NOT NULL,
                                            content TEXT NOT NULL,
                                            UNIQUE(entity_type, entity_id, locale, field_name)
);

-- ==========================================================
-- 4. INDEXES & SEED DATA
-- ==========================================================

CREATE INDEX idx_booking_overlap ON bookings (classroom_id, start_time, end_time)
    WHERE status IN ('PENDING', 'APPROVED') AND deleted_at IS NULL;

CREATE INDEX idx_users_search ON users(username, email) WHERE deleted_at IS NULL;
CREATE INDEX idx_refresh_token_lookup ON refresh_tokens(token) WHERE revoked = FALSE;

-- SEED DATA
INSERT INTO roles (name, description) VALUES
                                          ('ADMIN', 'Full access'), ('STAFF', 'Approve bookings'), ('STUDENT', 'Request bookings');

INSERT INTO buildings (name_key, address) VALUES
                                              ('building.a', 'Block A'), ('building.b', 'Block B');

INSERT INTO equipments (name_key) VALUES
                                      ('eq.projector'), ('eq.aircon'), ('eq.whiteboard');

INSERT INTO users (username, full_name, email, password, role_id, status) VALUES
                                                                              ('admin_thang', 'Phạm Đức Thắng', 'thang.admin@univ.edu.vn', 'admin123', 1, 'ACTIVE'),
                                                                              ('staff_lan', 'Nguyễn Thị Lan','lan.staff@univ.edu.vn', 'staff123', 2, 'ACTIVE'),
                                                                              ('student_nam', 'Lâm Nguyễn Trung Nam','nam.student@univ.edu.vn', 'student123', 3, 'ACTIVE');

INSERT INTO classrooms (building_id, room_number, capacity, status) VALUES
                                                                        (1, 'A.101', 50, 'AVAILABLE'), (1, 'A.102', 30, 'AVAILABLE'), (2, 'B.201', 100, 'AVAILABLE');

INSERT INTO classroom_equipment (classroom_id, equipment_id, quantity) VALUES
                                                                           (1, 1, 1), (1, 2, 2), (3, 1, 2);

INSERT INTO translations (entity_type, entity_id, locale, field_name, content) VALUES
                                                                                   ('BUILDING', 1, 'vi', 'name', 'Tòa nhà A'), ('BUILDING', 1, 'en', 'name', 'Building A'),
                                                                                   ('CLASSROOM', 1, 'vi', 'description', 'Phòng học lý thuyết'), ('CLASSROOM', 1, 'en', 'description', 'Theory classroom');

INSERT INTO bookings (user_id, classroom_id, start_time, end_time, status, purpose, idempotency_key) VALUES
    (3, 1, '2026-04-01 08:00:00+07', '2026-04-01 10:00:00+07', 'APPROVED', 'Học nhóm Java', 'key-001');