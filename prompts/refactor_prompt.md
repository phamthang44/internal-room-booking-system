### ROLE: Senior Java Backend Engineer

### CONTEXT:
I am refactoring a "Classroom Booking System" project that has become over-engineered, especially in the User, Authentication, and Security modules.

The goal is to simplify the system into a clean, maintainable, and production-like backend suitable for a 2–4 week project, while still demonstrating solid backend engineering practices.

---

### OBJECTIVES:

#### 1. Database & Entity Refactor
- Simplify User model:
  - Each user has exactly ONE role (STUDENT, STAFF, ADMIN)
  - Use a simple `role` field (ENUM or VARCHAR), NOT a separate roles table
- Use a shared `BaseEntity`:
  - `id`, `created_at`, `updated_at`
- Use standard numeric IDs (AUTO_INCREMENT or UUID)
  - Avoid complex ID strategies (e.g., TSID) unless truly necessary
- Keep schema minimal and aligned with actual business needs

---

#### 2. Authentication Flow Refactor
Implement a simple and clear authentication system:

- Login flow:
  - Validate email & password
  - Generate:
    - Access Token (JWT, short-lived)
    - Refresh Token (stored in DB)

- Refresh Token:
  - Stored in database
  - Used to issue new access tokens
  - Can be revoked (deleted or invalidated)

- Logout:
  - Invalidate refresh token
  - Optionally blacklist access token (DB-based, simple)

- Avoid over-complication:
  - No Redis required
  - No multi-device session management
  - No complex token rotation

---

#### 3. Security Configuration Cleanup
Refactor Spring Security configuration to be minimal and clear:

- Keep only:
  - JWT authentication filter
  - Stateless session management
  - Role-based authorization (RBAC)

- Remove:
  - Unused filters
  - Overly complex configurations from previous IAM-style implementation

- Use simple role checks:
  - `hasRole("STUDENT")`
  - `hasRole("STAFF")`
  - `hasRole("ADMIN")`

---

#### 4. Code Structure Simplification
Follow clean structure, but avoid over-abstraction:

- Layers:
  - Controller → Service → Repository

- Service should represent **use cases**, not fragmented technical services

- Avoid:
  - Too many small services
  - Overuse of interfaces where unnecessary
  - Premature generalization

---

### REQUIREMENTS & CONSTRAINTS:

- Use:
  - Spring Boot 3.x
  - Spring Security 6.x
- Use:
  - Lombok (reduce boilerplate)
- Optional:
  - MapStruct (only if mapping becomes complex)

- Database-first approach:
  - Use Flyway for schema management
  - Ensure constraints (e.g., unique, foreign key, time validation)

- Ensure critical operations are safe:
  - Token storage and validation must be atomic
  - Avoid duplicate refresh tokens

---

### DESIGN PRINCIPLES:

- Prefer simplicity over flexibility
- Prefer clarity over abstraction
- Focus on business flow (authentication, booking) rather than infrastructure complexity
- Avoid turning this project into a full IAM system

---

### INPUT:
Provide existing code for:
- SecurityConfig
- User Entity
- AuthController

---

### EXPECTED OUTPUT:

- Refactored:
  - User Entity
  - SecurityConfig
  - AuthController / AuthService

- Clean, minimal, production-like implementation
- Clear authentication flow (login → token → refresh → logout)