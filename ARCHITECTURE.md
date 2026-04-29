# Architecture

> Generated from the graphify knowledge graph (1,647 nodes · 2,529 edges · 177 communities, 2026-04-29).
> Refresh with `/graphify --update` after significant structural changes, then update this file.

---

## System Overview

REST API for an internal university room-booking system. Students submit booking requests for classrooms; admins/staff approve or reject. The system enforces concurrency-safe slot allocation, automated lifecycle jobs, a configurable penalty engine, and real-time WebSocket notifications.

**Runtime stack:** Spring Boot 4 · Java 25 · PostgreSQL · Redis · RabbitMQ · Flyway  
**Supporting infrastructure:** Cloudinary (images) · Supabase (files) · Mailjet + Spring Mail (email) · Thymeleaf (email templates) · ShedLock (distributed scheduler) · Bucket4j (rate limiting) · SpringDoc (OpenAPI)

---

## Layer Map

```
┌─────────────────────────────────────────────────────────┐
│  HTTP / WebSocket                                        │
│  controller/          AdminBookingController             │
│                       BookingController                  │
│                       NotificationController  ...        │
├─────────────────────────────────────────────────────────┤
│  Service layer                                           │
│  service/             *CommandService  (mutations)       │
│  service/impl/        *QueryService    (reads)           │
│  service/policy/      BookingPolicyManager               │
│                       BookingFlowPolicy  RoomPolicy      │
├─────────────────────────────────────────────────────────┤
│  Domain events  (common/event/)                          │
│  BookingStatusChangedEvent                               │
│  BookingNotificationRequestedEvent                       │
│  ViolationCreatedEvent  OtpRequestedEvent  ...           │
├────────────────────────┬────────────────────────────────┤
│  Spring listeners      │  RabbitMQ consumers            │
│  service/listener/     │  infrastructure/listener/       │
│  (sync/async, in-JVM)  │  (InAppNotificationConsumer     │
│                        │   BookingEmailEventListener)    │
├────────────────────────┴────────────────────────────────┤
│  Infrastructure                                          │
│  security/    messaging/   scheduler/   redis/           │
│  storage/     idempotency/ mail/        i18n/            │
├─────────────────────────────────────────────────────────┤
│  Data layer                                              │
│  repository/  (Spring Data JPA)                          │
│  entity/      (JPA entities)                             │
│  Flyway migrations  (src/main/resources/db/migration/)   │
└─────────────────────────────────────────────────────────┘
```

---

## God Nodes

These are the highest-connectivity symbols in the graph. They are the load-bearing abstractions — changes here ripple widest. Avoid increasing their coupling further.

| Rank | Symbol | Edges | Why it matters |
|------|--------|-------|----------------|
| 1 | `BookingRepository` | 37 | Queried by nearly every booking service; used directly by recommendation engine — bypass of query service layer |
| 2 | `BookingQueryServiceImpl` | 30 | Central read path for all booking views (student, admin, dashboard) |
| 3 | `BookingCommandServiceImpl` | 18 | Central write path: create, approve, reject, check-in, checkout, cancel |
| 4 | `NotificationServiceImpl` | 17 | All WebSocket pushes and notification DB writes flow through here |
| 5 | `BookingPolicyManagerImpl` | 16 | All booking policy checks (penalty, quota, overlap, timing) pass through this orchestrator |
| 6 | `BookingPolicyManager` | 15 | Interface contract for the above; injected into `BookingCommandServiceImpl` |
| 7 | `ClassroomQueryServiceImpl` | 17 | Availability queries and room search; called from booking flow and admin panel |
| 8 | `GlobalHandlerError` | 14 | Central exception handler; all `AppException` routing passes here |

---

## Community Map

The graph detected 177 communities. The 50 named ones (ranked by size) represent the logical modules:

### Core Domain

| Community | Key Members | Role |
|-----------|-------------|------|
| **Core Booking & User Domain** | `BookingCommandServiceImpl`, `BookingQueryServiceImpl`, `UserAccount`, `TimeSlot` | Central domain — booking state machine and user model |
| **Booking Flow & Orchestration** | `BookingPolicyManager`, `BookingFlowPolicy`, `BookingValidatorService`, `ClassroomValidatorService` | Pre-submission validation pipeline |
| **Booking Policy Validation** | `BookingPolicy`, `BookingPolicyImpl`, `BookingRepository`, scheduler jobs | Policy rules + automated lifecycle jobs |
| **Booking Policy Manager** | `BookingPolicyManagerImpl`, cancellation/check-in/approve/reject validators | Fine-grained policy method implementations |
| **Booking Policy Interface** | `BookingPolicy` interface methods | Contract surface for policy implementations |
| **Booking Lifecycle Commands** | `BookingCommandService` (approve, cancel, auto-reject, auto-checkout) | Explicit command surface |
| **Booking Approval & History** | `BookingApprovalCommandService`, `BookingHistoryCommandService`, `BookingStatusChangedRabbitPublisher` | Approval audit trail + event-to-RabbitMQ bridge |
| **Booking Approval Query** | `BookingApprovalQueryService`, `BookingApprovalController` | Read path for approval history |
| **Booking Query Service** | `BookingQueryService`, admin/student search | Read path for booking lists and details |
| **Booking Validator Service** | `BookingValidatorService` (date, classroom, purpose, slots) | Layer-1 validation before policy checks |

### Notification & Messaging

| Community | Key Members | Role |
|-----------|-------------|------|
| **Booking Events & WebSocket** | `BookingStatusChangedEvent`, `BookingNotificationListener`, `NotificationService`, `/topic/admin/bookings` | Spring event → async dispatch → RabbitMQ + direct WS |
| **Booking Email Notifications** | `BookingEmailEventListener`, `BookingEmailNotifier`, `ThymeleafTemplateRenderer`, `MailSender` | RabbitMQ consumer → email render → send |
| **In-App Notifications** | `InAppNotificationConsumer`, `NotificationServiceImpl`, `NotificationService` | RabbitMQ consumer → DB persist → WebSocket push |
| **Notification CRUD** | `NotificationService` (getAll, markRead, delete) | REST read/management path for bell-icon notifications |
| **Notification Controller** | `NotificationController` | REST surface for notification endpoints |
| **Email OTP & Registration** | `EmailService`, OTP send, registration HTML | Auth-triggered email path (separate from booking emails) |

### Auth & Security

| Community | Key Members | Role |
|-----------|-------------|------|
| **Auth & Cloud Storage** | `AuthService`, `CloudinaryStorageServiceImpl`, `GlobalHandlerError`, `BaseErrorCode` | Login, OAuth2, file upload, error handling hub |
| **Auth Service** | `AuthService` (login, Google, logout, refresh) | Auth method surface |
| **JWT Authentication Filter** | `JwtAuthenticationFilter`, `SecurityUserDetails`, `UserDetailsServiceImpl` | Per-request JWT validation chain |
| **Security Error Handlers** | `JwtAccessDeniedHandler`, `JwtAuthenticationEntryPoint` | 401/403 response formatting |
| **App Properties Config** | `AppProperties`, Mail, Social, Support sub-records | Typed configuration binding |

### Admin

| Community | Key Members | Role |
|-----------|-------------|------|
| **Admin Controllers & Services** | All `Admin*Controller`, `AdminUserService`, `BehaviorTrackingService`, `ClassroomQueryService` | Largest community — admin surface area |
| **Admin User Management** | `AdminUserService` (ban, create, update password/name) | User lifecycle from admin side |
| **Analytics & Dashboard** | `AdminDashboardServiceImpl`, `AnalyticsSnapshotServiceImpl`, `RedisService`, snapshot repos | Caching + snapshot-based analytics |
| **Analytics Snapshot Service** | `AnalyticsSnapshotService` (daily trend, room stats, violation trend) | Snapshot methods only |

### Infrastructure

| Community | Key Members | Role |
|-----------|-------------|------|
| **Availability & RabbitMQ** | `AvailabilityServiceImpl`, `RabbitMQConfig`, `GenericSpecificationBuilder` | Slot grid queries + broker topology config |
| **Idempotency & Room Status Policy** | `IdempotencyAspect`, `IdempotencyService`, `ChangeRoomStatusPolicyImpl`, `RoomPolicyFactory` | Duplicate-request guard + room state machine |
| **Penalty Enforcement** | `PenaltyCommandServiceImpl`, `PenaltyEnforcementListener`, `PenaltyReasonUtils` | Point accumulation → ban/warn/require-approval actions |
| **Redis Service** | `RedisService` (get, set, delete, exists) | Token blacklist + rate-limit bucket backing store |
| **Cloudinary Cleanup** | `CloudinaryCleanupJob`, `CloudinaryClientImpl` | Orphaned-image GC job |
| **File Storage Service** | `FileStorageService` (save, confirm, delete) | Abstraction over Cloudinary/Supabase |
| **Cloudinary Client** | `CloudinaryClient` (delete, search by tag, remove tag) | Raw Cloudinary API wrapper |
| **Base Entity & Audit** | `BaseEntity`, `BaseAuditEntity`, `BaseSoftDeleteEntity` | JPA entity base classes |
| **WebSocket Config** | `WebSocketConfig`, STOMP endpoint registration | WS broker and endpoint setup |
| **i18n & Locale Config** | `AcceptHeaderLocaleResolver`, `MessageSource` bean | Locale resolution from `Accept-Language` header |
| **App Exception Handling** | `AppException`, HTTP status resolution, message formatting | Typed exception with i18n message key |

### Supporting

| Community | Key Members | Role |
|-----------|-------------|------|
| **Classroom Command & API Config** | `ClassroomCommandServiceImpl`, `OpenApiConfig`, `WebClientConfig`, `Building`, `Classroom` | Room management + OpenAPI + HTTP client config |
| **Equipment Management** | `EquipmentCommandService`, `EquipmentQueryService`, impls, `EquipmentRepository` | Classroom equipment CRUD |
| **Input Validation Utilities** | `TextValidationUtils`, `AccountAuthenticationValidator` | Shared validation helpers |
| **Sort Type Converters** | `BookingSortConverter`, `StringToRoomSortConverter` | String → enum converters for query params |
| **Translation Service** | `TranslationService` (get, save, getAll) | i18n for translatable entities (room types etc.) |
| **Student Recommendations** | `StudentRecommendationServiceImpl`, `UserRoomPreferenceScoreRepository`, `UserBehaviorRepository` | ML-lite scoring based on past behavior |
| **Violation Management** | `AdminViolationController`, `ViolationCommandServiceImpl` | Manual violation recording by admin |
| **Async Config & Validators** | `AsyncConfiguration`, `PhoneValidator` | Thread pool config + phone format validator |
| **Application Bootstrap** | `RoomBookingApplication`, `UserSeeder` | Entry point + dev-mode seed data |

---

## Key Flows

### Booking Submission
```
BookingController.createBooking()
  → [idempotency check via @IdempotencyAspect]
  → BookingCommandServiceImpl.createBooking()
      → BookingValidatorService (classroom, date, purpose, slots)
      → BookingPolicyManager (penalty, pendingQuota, overlap, quota, workingHours)
      → TimeSlotService.getTimeSlotsByIds()
      → INSERT booking + booking_time_slots  [TX]
      → publish BookingStatusChangedEvent(PENDING)
  → BookingStatusChangedRabbitPublisher [@TransactionalEventListener AFTER_COMMIT]
      → RabbitMQ → email-booking queue → BookingEmailEventListener (email + WS)
  → BookingNotificationListener [@TransactionalEventListener AFTER_COMMIT]
      → RabbitMQ → in-app queue → InAppNotificationConsumer (DB persist + WS)
```

### Booking Approval
```
PATCH /api/v1/admin/bookings/{id}/approve   [ROLE_ADMIN | ROLE_STAFF]
  → AdminBookingController.approveBooking()
  → BookingCommandServiceImpl.approveBooking()
      → validateApproveStatus (must be PENDING)
      → atomicApprove() UPDATE with optimistic lock  [TX]
      → BookingApprovalCommandService.saveApprovalBooking()  [joins TX]
      → publish BookingStatusChangedEvent(APPROVED)
  → BookingStatusChangedRabbitPublisher [@TransactionalEventListener AFTER_COMMIT]
      → email-booking queue → email to student
  → BookingNotificationListener [@TransactionalEventListener AFTER_COMMIT]
      → in-app queue → student WS push + DB row
      → saveForAdmins() → DB rows for all staff + /topic/admin/bookings WS broadcast
```

### Notification Delivery (RabbitMQ fan-out)
```
roombooking.core.exchange  (topic exchange)
  │
  ├─ notification.email.security.*  → email-priority queue
  │                                    → priority emails (OTP, password reset)
  │
  ├─ notification.email.*           → email-normal queue
  │                                    → general notification emails
  │
  ├─ notification.email.booking.*   → email-booking queue
  │                                    → BookingEmailEventListener
  │                                       → Thymeleaf render → Mailjet/SMTP
  │                                       → notificationService.notifyUser() (WS, best-effort)
  │
  └─ notification.in-app.*          → in-app queue
                                       → InAppNotificationConsumer
                                          → notificationService.saveAndPush()
                                             → INSERT notification row
                                             → /topic/notifications/{userId}  (WS)
```

### Penalty Accumulation
```
ViolationCreatedEvent published (by scheduler jobs or admin)
  → PenaltyEnforcementListener.onViolationCreated()
  → PenaltyCommandServiceImpl.enforce()
      → sum points within window-days (from penalty.rules in application.yml)
      → threshold crossed → set UserStatus: WARNING | REQUIRE_APPROVAL | BAN_TEMP
  → PenaltyExpirationJob (scheduled) → resets status when window expires
```

---

## Concurrency & Safety

| Concern | Mechanism | Location |
|---------|-----------|----------|
| Double-booking prevention | PostgreSQL exclusion constraint on `booking_time_slots` | `V8__Add_Booking_Violation_Add_Table_BookingTimeSlot.sql` |
| Concurrent approval race | `atomicApprove()` UPDATE with `WHERE version=N` (optimistic lock) | `BookingRepository`, `BookingCommandServiceImpl` |
| Duplicate submission | Idempotency key stored in DB, checked via `@IdempotencyAspect` | `infrastructure/idempotency/` |
| Distributed scheduler | ShedLock — each job acquires a named lock before running | `infrastructure/scheduler/` |
| Rate limiting | Bucket4j backed by Redis | `infrastructure/redis/` |
| Token revocation | JWT blacklist in Redis (logout, password-reset) | `infrastructure/redis/` |

---

## Event Listener Timing

Two annotation patterns are used for `BookingStatusChangedEvent` listeners. Prefer `@TransactionalEventListener` for all new listeners.

| Listener | Annotation | Fires | Risk |
|----------|------------|-------|------|
| `BookingStatusChangedRabbitPublisher` | `@TransactionalEventListener(AFTER_COMMIT)` | After TX commits | Safe |
| `BookingNotificationListener` | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | After TX commits, async thread | Safe |
| `BookingHistoryListener` | check source | — | Verify |

> `BookingNotificationListener` was fixed 2026-04-29 to use `@TransactionalEventListener` instead of `@EventListener` to eliminate the pre-commit race condition on `findByIdWithAdminDetail`.

---

## Graphify Queries

Useful starting points for exploring the graph:

```bash
# Trace the booking approval flow
/graphify query "booking approval status changed event notification"

# Understand the penalty system
/graphify query "penalty violation points threshold ban"

# Find all paths from a controller to the DB
/graphify path "AdminBookingController" "BookingRepository"

# Explain a god node
/graphify explain "BookingPolicyManagerImpl"
```
