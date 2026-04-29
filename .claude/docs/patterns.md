# Implementation Patterns

Step-by-step checklists for the most common development tasks. Follow in order.

---

## Add a New Domain Feature (e.g. "room feedback")

### 1. Entity
- [ ] Create `entity/RoomFeedback.java` extending `BaseAuditEntity`
- [ ] Add `@Version` field for optimistic locking if status mutations are expected
- [ ] Use `Instant` for all timestamps, not `LocalDateTime`

### 2. Flyway Migration
- [ ] Create `src/main/resources/db/migration/V{N}__Add_room_feedback.sql`
- [ ] Version N = last migration number + 1 (check existing files)
- [ ] Never modify existing migration files

### 3. Repository
- [ ] Create `repository/RoomFeedbackRepository.java` extending `JpaRepository<RoomFeedback, Long>`
- [ ] Add any custom `@Query` methods needed

### 4. DTOs
- [ ] `common/dto/request/CreateRoomFeedbackRequest.java` — use Java records, add JSR-303 annotations
- [ ] `common/dto/response/RoomFeedbackResponse.java` — use Java records
- [ ] Never expose entity fields directly in responses

### 5. Error Codes
- [ ] Create `common/exception/errorcode/RoomFeedbackErrorCode.java` implementing `BaseErrorCode`
- [ ] Add i18n keys to `messages_en.properties` and `messages_vi.properties`

### 6. Mapper
- [ ] Create `common/mapper/RoomFeedbackMapper.java` — MapStruct interface
- [ ] Annotate with `@Mapper(componentModel = "spring")`

### 7. Service interfaces
- [ ] `service/RoomFeedbackCommandService.java` — mutation methods
- [ ] `service/RoomFeedbackQueryService.java` — read methods

### 8. Service implementations
- [ ] `service/impl/RoomFeedbackCommandServiceImpl.java` — `@Transactional(rollbackFor = Exception.class)` on mutating methods
- [ ] `service/impl/RoomFeedbackQueryServiceImpl.java` — `@Transactional(readOnly = true)` on all methods
- [ ] Publish events for side effects, do not call notification/email directly

### 9. Controller(s)
- [ ] User-facing: `controller/RoomFeedbackController.java` mapping `/api/v1/feedbacks`
- [ ] Admin-facing: `controller/AdminRoomFeedbackController.java` mapping `/api/v1/admin/feedbacks` with `@PreAuthorize`
- [ ] Return `ResponseEntity<ApiResult<T>>` from every method

### 10. Events (if side effects needed)
- [ ] Define `common/event/RoomFeedbackCreatedEvent.java` as a record
- [ ] Create listener in `service/listener/` (in-JVM) or `infrastructure/listener/` (RabbitMQ consumer)
- [ ] Annotate listeners with `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`

---

## Add a New REST Endpoint to an Existing Domain

### 1. Check conventions first
- Read `.claude/docs/conventions.md`
- Run `/graphify query "<domain> <action>"` to understand current structure

### 2. Request/Response DTOs
- [ ] Add `*Request` record in `common/dto/request/` if new input shape
- [ ] Add `*Response` record in `common/dto/response/` if new output shape

### 3. Error Codes
- [ ] Add new variants to the existing `*ErrorCode` enum for this domain
- [ ] Add i18n keys

### 4. Service method
- [ ] Add method to the **interface** first
- [ ] Implement in the existing `*CommandServiceImpl` or `*QueryServiceImpl`
- [ ] Command methods: `@Transactional(rollbackFor = Exception.class)`
- [ ] Query methods: `@Transactional(readOnly = true)`

### 5. Controller method
- [ ] Add `@GetMapping`/`@PostMapping`/`@PatchMapping`/`@DeleteMapping` to the existing controller
- [ ] Use `@Valid` on `@RequestBody` / `@ModelAttribute`
- [ ] Use `@PathVariable @Positive` for ID parameters
- [ ] Use `@AuthenticationPrincipal SecurityUserDetails` to get the current user

---

## Add a New Scheduler Job

- [ ] Create class in `infrastructure/scheduler/`
- [ ] Annotate with `@Component` + `@Slf4j`
- [ ] Use ShedLock: `@SchedulerLock(name = "UniqueJobName", lockAtMostFor = "PT5M")`
- [ ] Annotate method with `@Scheduled(cron = "...")` + `@EnableScheduling` must be on a config class
- [ ] Job methods must be `@Transactional` if they write to DB
- [ ] Use `SYSTEM_ACTOR` constant when publishing events from jobs (no real user)
- [ ] Publish events for side effects, do not send emails directly

---

## Add a New RabbitMQ Consumer

- [ ] Create class in `infrastructure/listener/`
- [ ] Annotate with `@Component` + `@RabbitListener(queues = "${roombooking.rabbitmq.queues.xxx}")`
- [ ] Queue key must exist in `application.yml` under `roombooking.rabbitmq.queues`
- [ ] Queue binding must exist in `RabbitMQConfig`
- [ ] Add `@Transactional(readOnly = true)` if reading DB, `@Transactional` if writing
- [ ] Never throw unchecked exceptions — log and swallow or send to DLQ

---

## Add a New Policy Check

- [ ] Add method to `BookingPolicyManager` interface
- [ ] Implement in `BookingPolicyManagerImpl`
- [ ] Call from the appropriate `CommandServiceImpl` **before** the DB write
- [ ] Throw `AppException` with the relevant error code on failure
- [ ] Write the policy check to be side-effect free (no DB writes)

---

## Add Bilingual Content

Every user-facing string needs both languages:

```properties
# messages_en.properties
room.feedback.created.title=Room Feedback Submitted
room.feedback.created.message=Your feedback for room {0} has been received.

# messages_vi.properties
room.feedback.created.title=Phản hồi phòng đã gửi
room.feedback.created.message=Phản hồi của bạn về phòng {0} đã được ghi nhận.
```

For translatable entity names (not UI strings): use `Translation` entity + `TranslatableEntityType`.
