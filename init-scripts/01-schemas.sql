-- 1. Quản lý phân quyền (Giữ nguyên hoặc dùng Enum nếu DB hỗ trợ)
CREATE TABLE "roles" (
                         "id" SERIAL PRIMARY KEY,
                         "name" varchar(50) UNIQUE NOT NULL
);

-- 2. Người dùng
CREATE TABLE "users" (
                         "id" SERIAL PRIMARY KEY,
                         "username" varchar(50) UNIQUE NOT NULL,
                         "email" varchar(100) UNIQUE NOT NULL,
                         "password_hash" varchar(255) NOT NULL,
                         "role_id" int NOT NULL REFERENCES "roles"("id"),
                         "created_at" timestamp DEFAULT CURRENT_TIMESTAMP
);

-- 3. Cơ sở vật chất (Gộp đơn giản)
CREATE TABLE "classrooms" (
                              "id" SERIAL PRIMARY KEY,
                              "room_number" varchar(20) NOT NULL,
                              "building_name" varchar(100) NOT NULL, -- Gộp building vào đây nếu quy mô nhỏ để bớt Join
                              "capacity" int NOT NULL,
                              "description" text, -- Lưu thông tin thiết bị (máy chiếu, điều hòa) dạng text cho nhanh
                              "is_active" boolean DEFAULT true,
                              "version" int DEFAULT 0 -- Phục vụ Optimistic Locking [cite: 32, 119]
);

-- 4. Trái tim của hệ thống: Bookings
CREATE TABLE "bookings" (
                            "id" SERIAL PRIMARY KEY,
                            "user_id" int NOT NULL REFERENCES "users"("id"),
                            "classroom_id" int NOT NULL REFERENCES "classrooms"("id"),
                            "start_time" timestamp NOT NULL,
                            "end_time" timestamp NOT NULL,
                            "status" varchar(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, CANCELLED [cite: 79]
                            "purpose" text,
                            "idempotency_key" varchar(100) UNIQUE NOT NULL, -- Chống trùng lặp request [cite: 33, 84]
                            "created_at" timestamp DEFAULT CURRENT_TIMESTAMP,
                            "updated_at" timestamp DEFAULT CURRENT_TIMESTAMP,

    -- Constraint đảm bảo logic thời gian
                            CONSTRAINT check_times CHECK (end_time > start_time)
);

-- Index thần thánh để check Overlap nhanh:
CREATE INDEX idx_booking_overlap ON bookings (classroom_id, start_time, end_time)
    WHERE status IN ('PENDING', 'APPROVED');

-- 5. Lịch sử phê duyệt (Chỉ lưu khi cần thiết)
CREATE TABLE "booking_approvals" (
                                     "id" SERIAL PRIMARY KEY,
                                     "booking_id" int NOT NULL REFERENCES "bookings"("id") ON DELETE CASCADE,
                                     "approver_id" int NOT NULL REFERENCES "users"("id"),
                                     "note" text,
                                     "created_at" timestamp DEFAULT CURRENT_TIMESTAMP
);