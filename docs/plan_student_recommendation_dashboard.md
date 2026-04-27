# Plan: Student Recommendation System & Enhanced Dashboard

## Context

The current student dashboard (`GET /api/v1/students/dashboard`) returns only 3 counters and two raw booking lists. This plan adds:
1. A rule-based room recommendation engine using the student's own booking history
2. An enriched dashboard with attendance stats, penalty awareness, and booking quality metrics

No DB migration is required — all required columns (`attendance_status`, `actual_attendees`, `status`, `capacity`) already exist.

---

## Baseline (what exists today)

| Symbol | Location | Notes |
|---|---|---|
| `StudentDashboardController` | `controller/StudentDashboardController.java` | Single `GET /dashboard` endpoint |
| `StudentDashboardResponse` | `common/dto/response/StudentDashboardResponse.java` | 3 counters + 2 lists only |
| `getStudentDashboard` | `service/impl/BookingQueryServiceImpl.java:135` | Fetches upcoming/recent top-5, 3 counts |
| `BookingRepository` | `repository/BookingRepository.java` | No classroom-grouped or attendance-status queries |
| `ClassroomRepository` | `repository/ClassroomRepository.java` | No availability/capacity queries |
| `PenaltyRecordRepository` | `repository/PenaltyRecordRepository.java` | `findByUserIdAndIsActiveTrue(userId)` available |
| `attendanceStatus` / `actualAttendees` | `entity/Booking.java` | Now populated (wired in previous task) |

---

## Feature 1 — Recommendation System

### New endpoint
```
GET /api/v1/students/recommendations?attendees={n}&date={yyyy-MM-dd}
```
Returns top-5 ranked room suggestions. Both params optional — defaults to student's historical avg group size and today's date.

### New files

| File | Purpose |
|---|---|
| `service/StudentRecommendationService.java` | Interface |
| `service/impl/StudentRecommendationServiceImpl.java` | Implementation |
| `common/dto/response/RoomRecommendationResponse.java` | Response DTO |

### `RoomRecommendationResponse` fields
```java
Long classroomId
String roomName
String buildingName           // translated via TranslationService
String roomTypeName           // translated via TranslationService
Integer capacity
RoomStatus status
int bookingCount              // how many times this student used this room
BigDecimal avgActualAttendees // from their COMPLETED bookings in this room
String reasonKey              // i18n key (see i18n section below)
```

### New queries — `BookingRepository`

**1. Student's most-used rooms with real attendance data:**
```jpql
@Query("""
    SELECT b.classroom.id,
           COUNT(b.id),
           AVG(CAST(COALESCE(b.actualAttendees, b.attendees) AS double))
    FROM Booking b
    WHERE b.user.id = :userId
      AND b.status IN :statuses
      AND b.deletedAt IS NULL
    GROUP BY b.classroom.id
    ORDER BY COUNT(b.id) DESC
""")
List<Object[]> findTopClassroomsByUser(
    @Param("userId") Long userId,
    @Param("statuses") List<BookingStatus> statuses,
    Pageable pageable);
```

**2. Student's preferred time slots:**
```jpql
@Query("""
    SELECT bts.timeSlot.id, COUNT(bts.id)
    FROM Booking b JOIN b.bookingTimeSlots bts
    WHERE b.user.id = :userId
      AND b.status IN :statuses
      AND b.deletedAt IS NULL
    GROUP BY bts.timeSlot.id
    ORDER BY COUNT(bts.id) DESC
""")
List<Object[]> findTopTimeSlotsByUser(
    @Param("userId") Long userId,
    @Param("statuses") List<BookingStatus> statuses,
    Pageable pageable);
```

### New query — `ClassroomRepository`

```jpql
@Query("""
    SELECT c FROM Classroom c
    WHERE c.status = 'AVAILABLE'
      AND c.capacity >= :minCapacity
      AND c.deletedAt IS NULL
""")
List<Classroom> findAvailableWithMinCapacity(@Param("minCapacity") int minCapacity);
```
> Note: `c.deletedAt IS NULL` is technically redundant because `Classroom` carries `@SQLRestriction("deleted_at IS NULL")`, but it is kept for consistency with the existing `countActiveByStatus` query in the same repository which also includes it explicitly.

### Scoring logic

```
score = (bookingCount × 3)
      + (capacityMatchBonus: +5 if capacity is within +50% of requested attendees, else 0)
      + (attendanceRateBonus: +2 if avgActualAttendees/capacity > 0.5)
```

- History-based rooms → `reasonKey = "recommendation.reason.frequent"`
- New rooms (no prior history, capacity match only) → `reasonKey = "recommendation.reason.capacity_match"`
- Results capped at top 5, sorted by score descending

---

## Feature 2 — Enhanced Student Dashboard

### Extend `StudentDashboardResponse`

Add to existing DTO (no breaking change — additive fields):
```java
// Attendance stats
Double attendanceRate           // ATTENDED / (ATTENDED + NO_SHOW)
Long noShowCount                // lifetime NO_SHOW count
Long cancelledThisMonthCount    // cancellations in rolling 30 days

// Penalty awareness — PenaltyRecord.penaltyAction is type PenaltyAction enum
Boolean hasPenalty              // any active PenaltyRecord
PenaltyAction penaltyLevel      // WARNING | REQUIRE_APPROVAL | BAN_TEMP | PERMANENT_BAN | null
                                // severity order: PERMANENT_BAN > BAN_TEMP > REQUIRE_APPROVAL > WARNING
Instant penaltyExpiresAt        // null if no active penalty or PERMANENT_BAN

// Booking quality
Double avgActualAttendees       // avg of actualAttendees across COMPLETED bookings
```

### New queries — `BookingRepository`

**Count by attendance status:**
```jpql
@Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.attendanceStatus = :status AND b.deletedAt IS NULL")
long countByUserIdAndAttendanceStatus(
    @Param("userId") Long userId,
    @Param("status") AttendanceStatus status);
```

**Cancelled in rolling 30 days:**
Reuse the existing `countCancelledBookingsByUserToday(userId, startOfDay)` — it already accepts an `Instant` lower bound and is functionally identical. Pass `Instant.now().minus(30, ChronoUnit.DAYS)` as `startOfDay`. **Do NOT add a duplicate method.**

**Average actual attendees:**
```jpql
@Query("SELECT AVG(CAST(b.actualAttendees AS double)) FROM Booking b WHERE b.user.id = :userId AND b.status = 'COMPLETED' AND b.actualAttendees IS NOT NULL AND b.deletedAt IS NULL")
Double avgActualAttendeesByUser(@Param("userId") Long userId);
```

### Penalty awareness

Inject `PenaltyRecordRepository` into `BookingQueryServiceImpl` (not currently present — must add `@RequiredArgsConstructor` field).
Call `findByUserIdAndIsActiveTrue(userId)`, map the most severe active record to the three new penalty fields.

Severity order (from `PenaltyAction` enum): `PERMANENT_BAN > BAN_TEMP > REQUIRE_APPROVAL > WARNING`
Note: `RESOLVED`, `ACTIVE`, `REVOKED` are lifecycle markers — skip them in severity ranking.

### Note on `StudentDashboardController` authorization
Current `getDashboard` has no `@PreAuthorize`. Add `@PreAuthorize("hasRole('STUDENT')")` to both `getDashboard` and the new `getRecommendations` endpoint.

---

## i18n Keys

Add to **all three** property files (`messages.properties`, `messages_en.properties`, `messages_vi.properties`):

```properties
# messages.properties / messages_en.properties
recommendation.reason.frequent=You have booked this room {0} times before
recommendation.reason.capacity_match=Fits your group size of {0}

# messages_vi.properties
recommendation.reason.frequent=Bạn đã đặt phòng này {0} lần trước đây
recommendation.reason.capacity_match=Phù hợp với nhóm {0} người của bạn
```

---

## Complete File Change List

| Action | File | Change |
|---|---|---|
| **Create** | `service/StudentRecommendationService.java` | New interface |
| **Create** | `service/impl/StudentRecommendationServiceImpl.java` | Scoring + ranking logic |
| **Create** | `common/dto/response/RoomRecommendationResponse.java` | Response DTO |
| **Modify** | `common/dto/response/StudentDashboardResponse.java` | +7 new fields |
| **Modify** | `service/impl/BookingQueryServiceImpl.java` | Extend `getStudentDashboard()`, inject `PenaltyRecordRepository` |
| **Modify** | `repository/BookingRepository.java` | +3 new JPQL queries |
| **Modify** | `repository/ClassroomRepository.java` | +1 new JPQL query |
| **Modify** | `controller/StudentDashboardController.java` | Add `GET /recommendations` endpoint |
| **Modify** | `resources/messages.properties` | +2 keys |
| **Modify** | `resources/messages_en.properties` | +2 keys (file exists — was missing from original plan) |
| **Modify** | `resources/messages_vi.properties` | +2 keys |

**No DB migration needed.**

---

## Validation Findings (loop pass 1 — corrected in this file)

| # | Finding | Status | Fix Applied |
|---|---|---|---|
| 1 | `messages_en.properties` exists but was missing from plan | **Fixed** | Added to i18n section and file change list |
| 2 | `PenaltyRecord.penaltyAction` is `PenaltyAction` enum, not `String penaltyLevel` | **Fixed** | Changed field type; added `PERMANENT_BAN` to severity order |
| 3 | `PenaltyAction` has extra values: `PERMANENT_BAN`, `RESOLVED`, `ACTIVE`, `REVOKED` | **Fixed** | Severity order updated; lifecycle markers noted as skip |
| 4 | `countCancelledSince` would duplicate `countCancelledBookingsByUserToday` | **Fixed** | Reuse existing method instead |
| 5 | `BookingQueryServiceImpl` does NOT inject `PenaltyRecordRepository` | **Noted** | Plan now explicitly states it must be added |
| 6 | `c.deletedAt IS NULL` is redundant due to `@SQLRestriction` on `Classroom` | **Noted** | Kept for consistency with existing `countActiveByStatus` pattern |
| 7 | `StudentDashboardController` has no `@PreAuthorize` on any endpoint | **Noted** | Plan now includes adding it to both endpoints |
| 8 | `StudentRecommendationServiceImpl` needs `TranslationService` injected for `buildingName`/`roomTypeName` | **Valid** | Already uses `TranslationService` — must be listed as dependency in new service |

---

## Verification

1. Seed a student with 10+ COMPLETED bookings across 3 classrooms → `GET /recommendations` → rooms ranked by frequency, reason keys populated correctly
2. Add a NO_SHOW violation → `GET /dashboard` → `hasPenalty=true`, correct `penaltyLevel`
3. `GET /dashboard` → verify `attendanceRate = ATTENDED / (ATTENDED + NO_SHOW)`
4. `GET /recommendations?attendees=15` with no booking history → capacity-matched rooms returned with `recommendation.reason.capacity_match`
5. `GET /dashboard` → verify `cancelledThisMonthCount` increments correctly after a cancel within 30 days
