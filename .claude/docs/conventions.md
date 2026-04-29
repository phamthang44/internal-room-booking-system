# Project Conventions

Authoritative rules for this codebase. Read this before writing any new code.

---

## Naming

| Thing | Convention | Example |
|-------|-----------|---------|
| Mutation service | `*CommandService` interface + `*CommandServiceImpl` | `BookingCommandService` |
| Read service | `*QueryService` interface + `*QueryServiceImpl` | `BookingQueryServiceImpl` |
| Policy interface | `*Policy` or `*PolicyManager` | `BookingPolicy`, `BookingPolicyManager` |
| Policy impl | `*PolicyImpl` or `*PolicyManagerImpl` | `BookingPolicyManagerImpl` |
| Spring events | `*Event` record | `BookingStatusChangedEvent` |
| Request DTOs | `*Request` record | `CreateBookingRequest` |
| Response DTOs | `*Response` record | `BookingDetailResponse` |
| Error codes | `*ErrorCode` enum | `BookingErrorCode` |
| Scheduler jobs | `Auto*Job` or `*Job` | `AutoRejectPendingBookingJob` |
| RabbitMQ consumers | `*Consumer` or `*EventListener` | `InAppNotificationConsumer` |
| RabbitMQ publishers | `*RabbitPublisher` | `BookingStatusChangedRabbitPublisher` |
| MapStruct mappers | `*Mapper` interface | `BookingMapper` |

---

## Package Placement

```
New entity           → entity/
New repo             → repository/
New request/response → common/dto/request|response/
New enum             → common/enums/
New event            → common/event/
New error codes      → common/exception/errorcode/  (implement BaseErrorCode)
New mapper           → common/mapper/
New validator        → common/validator/
New const            → common/constant/
New mutation service → service/ (interface) + service/impl/ (impl)
New query service    → service/ (interface) + service/impl/ (impl)
New policy           → service/policy/ (interface) + service/policy/impl/ (impl)
New scheduler job    → infrastructure/scheduler/
New RabbitMQ pub     → infrastructure/messaging/
New RabbitMQ sub     → infrastructure/listener/
New admin controller → controller/  (prefix Admin*, map /api/v1/admin/*)
New user controller  → controller/  (map /api/v1/*)
```

---

## Error Codes

Every typed error must implement `BaseErrorCode`. Add to the matching domain enum, never create inline strings.

```java
// common/exception/errorcode/BookingErrorCode.java
@Getter
@RequiredArgsConstructor
public enum BookingErrorCode implements BaseErrorCode {
    BOOKING_NOT_FOUND("BOOKING_001", "booking.not_found", HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_PROCESSED("BOOKING_002", "booking.already_processed", HttpStatus.CONFLICT);

    private final String code;
    private final String message;   // i18n key in messages.properties
    private final HttpStatus httpStatus;

    @Override public String format(Object... args) { return String.format(message, args); }
}
```

Throw with: `throw new AppException(BookingErrorCode.BOOKING_NOT_FOUND);`

---

## Response Wrapping

All controller methods return `ResponseEntity<ApiResult<T>>`.

```java
// Success
return ResponseEntity.ok(ApiResult.success(data));
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(data));

// Success with i18n message string
return ResponseEntity.ok(ApiResult.success(I18nUtils.get("booking.approved.success", bookingId)));
```

Never return raw objects from controllers.

---

## Spring Events — Side Effects Rule

**Never call notification, penalty, email, or history services directly from a command service.**
Always publish a Spring `ApplicationEvent` and let a listener handle it.

```java
// ✅ Correct — in BookingCommandServiceImpl
eventPublisher.publishEvent(new BookingStatusChangedEvent(booking, APPROVED, ...));

// ❌ Wrong — direct call couples command service to notification
notificationService.saveAndPush(userId, payload);
```

**Listener annotation rules:**
- Side effects after a DB write → `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`
- Idempotent read-only listeners → `@EventListener` is acceptable
- RabbitMQ publish must use `@TransactionalEventListener(AFTER_COMMIT)` — never publish to broker before commit

---

## Transactions

```java
// Mutation service methods
@Transactional(rollbackFor = Exception.class)

// Read-only service methods
@Transactional(readOnly = true)

// Listener methods that open their own TX
@Transactional   // joins if exists, creates new in async thread
```

Secondary `@Transactional` methods called from within a transaction join the outer TX by default (PROPAGATION.REQUIRED). Only use `REQUIRES_NEW` if explicitly needed for audit isolation.

---

## i18n

All user-facing strings must use `MessageSource` keys — never hardcode English strings.

```java
// Get message in current request locale
I18nUtils.get("booking.approved.success", bookingId)

// Get message in a specific locale (for async listeners)
I18nUtils.get("notification.booking.approved.title", locale)
```

Add keys to both `messages_en.properties` and `messages_vi.properties`.

Translatable entities (room types, etc.) → use the `Translation` entity + `TranslatableEntityType` enum. Never add a plain `nameEn`/`nameVi` column pair.

---

## Optimistic Locking

For any status-change mutation that could race:

```java
int rows = repository.atomicTransition(id, NEW_STATUS, entity.getVersion());
if (rows == 0) throw new AppException(BookingErrorCode.BOOKING_ALREADY_PROCESSED);
Booking updated = repository.findById(id).orElseThrow(...);  // re-fetch after update
```

Repository method uses `WHERE id=? AND version=? AND status=?` and Spring's `@Modifying @Query`.

---

## Idempotency

Booking submission is guarded by `@Idempotent` (via `IdempotencyAspect`). Before adding idempotency to any other mutation endpoint, check `infrastructure/idempotency/` first.

---

## UTC / Timezone

- Store all timestamps as `Instant` (UTC) in JPA entities.
- Never use `LocalDateTime` for stored timestamps.
- Convert to GMT+7 only in response mappers or `@JsonFormat` annotations, not in service logic.

---

## Security

- Admin endpoints: `@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")` on the controller method.
- Student endpoints: `authenticated()` (default) — no extra annotation needed.
- Public endpoints: add to `SecurityConfig.PUBLIC_LIST`.

---

## Logging

```java
log.info("{} | <action> | <context>", LogConstant.ACTION_START, ...);
log.info("{} | <action> | <context>", LogConstant.ACTION_SUCCESS, ...);
log.warn("{}: <reason>", LogConstant.BIZ_ERROR, e.getErrorCode());
log.error("{} | <context>", LogConstant.SYS_ERROR, e);
```

Use `LogConstant` prefixes consistently so logs can be grepped by phase.
