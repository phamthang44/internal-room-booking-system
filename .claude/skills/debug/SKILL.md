# /debug — Debug an Issue

Use when: "why is X failing", "there's a bug in", "getting error", "not working", or invoked as `/debug <symptom>`.

## What This Skill Does

Traces the execution flow for the reported symptom using the graphify graph and source reads, identifies the root cause, and proposes a targeted fix.

---

## Steps

### Step 1 — Parse the symptom

From the user's description, extract:
- The **error message** or **unexpected behaviour**
- The **entry point** (endpoint, scheduler job, RabbitMQ consumer, etc.)
- The **area** (booking, auth, notification, etc.)

### Step 2 — Query the graph

```
/graphify query "<symptom keywords>"
```

Look for:
- Which communities are returned
- Which execution flows (edges) are involved
- Whether any god nodes are on the path

If the symptom mentions a specific symbol:
```
/graphify explain "<symbolName>"
```

### Step 3 — Trace the execution path

For the most likely entry point, trace the call chain:

1. Find the controller method (the HTTP entry)
2. Follow the service call
3. Identify where the error is thrown or the wrong behaviour occurs
4. Check event listeners and async paths if the symptom appears after the HTTP response

Use `Read` to read the relevant source files at the identified locations. Do not guess — read the actual code.

### Step 4 — Check cross-cutting concerns

Many bugs in this codebase fall into these categories. Check each that is relevant:

**Transaction boundary issues**
- Is the failing code in an `@Async` listener that runs before the TX commits?
- Is there a `@Transactional(readOnly = true)` on a method that writes?
- Is lazy loading failing because there's no active session in an async thread?

**Concurrency**
- Is `updatedRows == 0` from an atomic update being swallowed instead of thrown?
- Is the version column stale (entity fetched before an update)?

**Event ordering**
- Is the listener using `@EventListener` instead of `@TransactionalEventListener(AFTER_COMMIT)`?
- Is the RabbitMQ message published before the TX commits, so the consumer can't find the booking?

**i18n / locale**
- Is the locale being set correctly in async threads? (Locale must be passed via the event record — `LocaleContextHolder` is thread-local)

**Timezone**
- Is a comparison using `LocalDateTime` instead of `Instant`?
- Is a stored UTC value being compared with a local-time value?

**Optimistic lock**
- Is `ObjectOptimisticLockingFailureException` being swallowed?
- Is the entity being re-used after an `atomicTransition()` call instead of re-fetched?

### Step 5 — Identify the fix

State clearly:
- **Root cause**: one sentence
- **File:line** where the bug lives
- **Proposed fix**: the minimal change that fixes it
- **Side effects of the fix**: anything else that may need updating

### Step 6 — Check impact before fixing

For the symbol you plan to change:
```
gitnexus_impact({target: "<symbolName>", direction: "upstream", repo: "room-booking"})
```

If risk is HIGH or CRITICAL, warn the user before applying the fix.

### Step 7 — Apply fix and verify

After applying:
- State what changed and why
- Run `/commit` workflow to stage and commit the fix
