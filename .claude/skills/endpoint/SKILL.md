# /endpoint — Add a REST Endpoint

Use when: "add endpoint", "create API for X", "add route", or invoked as `/endpoint <METHOD> <path>`.

## What This Skill Does

Adds a single REST endpoint following all project conventions — correct package, DTOs, service method, error codes, i18n, and security annotation.

---

## Steps

### Step 1 — Parse the request

Extract from the user's input:
- HTTP method (GET/POST/PATCH/PUT/DELETE)
- Path (e.g. `/api/v1/admin/bookings/{id}/feedback`)
- Domain (derive from path — `bookings` → Booking domain)
- Admin or student endpoint? (`/api/v1/admin/**` = admin)

### Step 2 — Find the right controller

```
/graphify query "<domain> controller"
```

Identify the existing controller to add the method to (or confirm a new one is needed).

### Step 3 — Check conventions

Read `.claude/docs/conventions.md` sections:
- Naming conventions
- Response wrapping (`ApiResult<T>`)
- Security annotations
- Logging pattern

### Step 4 — Check impact on service layer

If adding a method to an **existing** service, check its impact using graphify:
```
/graphify query "<ExistingServiceImpl>" --dfs
```

Proceed only if the blast radius is small and doesn't heavily impact distant communities.

### Step 5 — Implement in order

Follow this exact order (do not skip layers):

1. **Request DTO** (if input needed)
   - `common/dto/request/<Action><Domain>Request.java`
   - Use Java record
   - Add `@NotNull`/`@Size`/`@Valid` annotations

2. **Response DTO** (if new output shape)
   - `common/dto/response/<Domain><Action>Response.java`
   - Use Java record
   - Never expose entity fields directly

3. **Error codes** (add to existing `<Domain>ErrorCode` enum)
   - Add i18n key to `messages_en.properties` and `messages_vi.properties`

4. **Service interface method**
   - Add to existing `<Domain>CommandService` or `<Domain>QueryService`

5. **Service implementation method**
   - Mutation: `@Transactional(rollbackFor = Exception.class)`
   - Query: `@Transactional(readOnly = true)`
   - Log with `LogConstant.ACTION_START`, `ACTION_SUCCESS`, `BIZ_ERROR`, `SYS_ERROR`
   - Publish event for side effects — never call notification/email directly

6. **Controller method**
   ```java
   @PostMapping("/{id}/feedback")
   @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")   // admin only
   public ResponseEntity<ApiResult<FeedbackResponse>> submitFeedback(
           @PathVariable @Positive Long id,
           @Valid @RequestBody CreateFeedbackRequest req,
           @AuthenticationPrincipal SecurityUserDetails userDetails) {
       log.info("...", id, userDetails.getUser().getId());
       return ResponseEntity.ok(ApiResult.success(service.doThing(id, req, userDetails.getUser())));
   }
   ```

7. **i18n keys** — success message key + any new error message keys

### Step 6 — Verify

After writing all files, state:
- Which files were created/modified
- The full endpoint signature: `POST /api/v1/admin/bookings/{id}/feedback`
- Required roles (or public)
- Request body shape
- Response shape
