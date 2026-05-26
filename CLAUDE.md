# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **room-booking** (3594 symbols, 10434 relationships, 283 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## When Debugging

1. `gitnexus_query({query: "<error or symptom>"})` — find execution flows related to the issue
2. `gitnexus_context({name: "<suspect function>"})` — see all callers, callees, and process participation
3. `READ gitnexus://repo/room-booking/process/{processName}` — trace the full execution flow step by step
4. For regressions: `gitnexus_detect_changes({scope: "compare", base_ref: "main"})` — see what your branch changed

## When Refactoring

- **Renaming**: MUST use `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` first. Review the preview — graph edits are safe, text_search edits need manual review. Then run with `dry_run: false`.
- **Extracting/Splitting**: MUST run `gitnexus_context({name: "target"})` to see all incoming/outgoing refs, then `gitnexus_impact({target: "target", direction: "upstream"})` to find all external callers before moving code.
- After any refactor: run `gitnexus_detect_changes({scope: "all"})` to verify only expected files changed.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Tools Quick Reference

| Tool | When to use | Command |
|------|-------------|---------|
| `query` | Find code by concept | `gitnexus_query({query: "auth validation"})` |
| `context` | 360-degree view of one symbol | `gitnexus_context({name: "validateUser"})` |
| `impact` | Blast radius before editing | `gitnexus_impact({target: "X", direction: "upstream"})` |
| `detect_changes` | Pre-commit scope check | `gitnexus_detect_changes({scope: "staged"})` |
| `rename` | Safe multi-file rename | `gitnexus_rename({symbol_name: "old", new_name: "new", dry_run: true})` |
| `cypher` | Custom graph queries | `gitnexus_cypher({query: "MATCH ..."})` |

## Impact Risk Levels

| Depth | Meaning | Action |
|-------|---------|--------|
| d=1 | WILL BREAK — direct callers/importers | MUST update these |
| d=2 | LIKELY AFFECTED — indirect deps | Should test |
| d=3 | MAY NEED TESTING — transitive | Test if critical path |

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/room-booking/context` | Codebase overview, check index freshness |
| `gitnexus://repo/room-booking/clusters` | All functional areas |
| `gitnexus://repo/room-booking/processes` | All execution flows |
| `gitnexus://repo/room-booking/process/{name}` | Step-by-step execution trace |

## Self-Check Before Finishing

Before completing any code modification task, verify:
1. `gitnexus_impact` was run for all modified symbols
2. No HIGH/CRITICAL risk warnings were ignored
3. `gitnexus_detect_changes()` confirms changes match expected scope
4. All d=1 (WILL BREAK) dependents were updated

## Keeping the Index Fresh

After committing code changes, the GitNexus index becomes stale. Re-run analyze to update it:

```bash
npx gitnexus analyze
```

If the index previously included embeddings, preserve them by adding `--embeddings`:

```bash
npx gitnexus analyze --embeddings
```

To check whether embeddings exist, inspect `.gitnexus/meta.json` — the `stats.embeddings` field shows the count (0 means no embeddings). **Running analyze without `--embeddings` will delete any previously generated embeddings.**

> Claude Code users: A PostToolUse hook handles this automatically after `git commit` and `git merge`.

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->

---

## Project Overview

Spring Boot 4.x / Java 25 REST API for an internal university room-booking system. Students book classrooms; admins approve/reject. The system enforces concurrency-safe availability grids, automated lifecycle jobs, a penalty/violation engine, and real-time WebSocket notifications.

**Stack:** Spring Boot 4 · Java 25 · PostgreSQL · Flyway · Redis · RabbitMQ · Spring Security (JWT/OAuth2) · MapStruct · Lombok · ShedLock · Bucket4j · Cloudinary · Mailjet · Thymeleaf · SpringDoc (OpenAPI)

## Common Commands

```bash
# Start infra only (recommended) — then run Spring from IntelliJ with profile=dev
make infra
# Infra URLs: MailDev http://localhost:1080 · RabbitMQ http://localhost:15672 (guest/guest) · Postgres :5432 · Redis :6379

# Full dev stack in Docker
make dev && make dev-down

# Build with Maven (from project root)
./mvnw clean package -DskipTests
./mvnw clean package                    # with tests

# Run single test class
./mvnw test -Dtest=MyServiceTest

# Follow app logs (dev)
make dev-logs
```

> **Windows:** Use Git Bash for `make` commands, or use `run.ps1` in PowerShell instead.

Active Spring profiles: `local` | `dev` (default) | `test` | `prod`. Config files: `application-{profile}.yml`.

### Required env vars (dev profile)

Set these in your IntelliJ run config or a `.env` file:

```
CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET
POSTGRES_USER, POSTGRES_PASSWORD        # default: postgres / root
MAIL_FROM_EMAIL, MAIL_ADMIN_EMAIL       # can be any string in local dev (MailDev catches all)
```

## Architecture

### Package Layout (`com.thang.roombooking`)

```
controller/          REST endpoints — one controller per domain
service/             Business logic interfaces
  impl/              Service implementations (Command/Query split — see below)
  policy/            Strategy pattern: BookingFlowPolicy, RoomPolicy, etc.
  listener/          Spring @EventListener handlers (booking, OTP, violations)
  notification/      Notification dispatch logic
common/
  dto/request|response/  API contracts (no entity exposure)
  dto/model/         Shared DTO models reused across request/response
  enums/             Domain enums (BookingStatus, RoomStatus, ViolationType, etc.)
  event/             Spring ApplicationEvent definitions (published by services)
  mapper/            MapStruct mappers (entity ↔ DTO)
  exception/         GlobalExceptionHandler + BaseErrorCode interface
    errorcode/       Domain error code enums (AuthErrorCode, BookingErrorCode, etc.)
  constant/          BookingMessageKeys, RabbitMQConstants, TimeConstant
  search/            Search/filter Specification builders
  utils/             Utility helpers
  validator/         Custom JSR-303 validators
entity/              JPA entities: UserAccount, Booking, Classroom, etc.
repository/          Spring Data JPA repositories
infrastructure/
  security/          SecurityConfig, JwtAuthenticationFilter, etc.
  configuration/     App-wide config beans (scheduler config, etc.)
  scheduler/         ShedLock-protected background jobs
  messaging/         RabbitMQ publishers
  listener/          RabbitMQ consumers (email, in-app notifications)
  mail/              Mailjet + Spring Mail email senders (booking/, core/, spring/)
  redis/             Token blacklist, rate limiting (Bucket4j)
  oauth/google/      Google OAuth2 token verification
  storage/           Cloudinary image upload, Supabase storage, CSV importer
  idempotency/       Idempotency key tracking (prevent duplicate submissions)
  i18n/              MessageSource wrappers
seeder/              Database seeders (dev/local only)
```

### Key Domain Concepts

**Booking lifecycle:** `PENDING → APPROVED/REJECTED → IN_USE → COMPLETED/CANCELLED`. Slot statuses on the `BookingTimeSlot` join table drive the availability grid.

**Concurrency:** PostgreSQL exclusion constraints on `booking_time_slots` prevent double-booking at the DB level. Application-level locks guard the critical section in `BookingCommandService`.

**Scheduler jobs** (ShedLock-protected, run on schedule):
- `AutoRejectPendingBookingJob` — rejects bookings admins didn't approve in time
- `AutoCancelBookingJob` — cancels when user didn't check in
- `AutoCheckoutBookingJob` — auto-checks out after session end
- `PenaltyExpirationJob` — expires temporary bans / requirement-for-approval states

**Penalty engine:** Configurable in `application.yml` under `penalty.rules`. Points accumulate per violation type (`NO_SHOW`, `LATE_CHECK_IN`, etc.) within a rolling `window-days`. Thresholds trigger `WARNING`, `REQUIRE_APPROVAL`, or `BAN_TEMP` actions on the user.

**Messaging (RabbitMQ):** A single topic exchange (`roombooking.core.exchange`) fans out to four queues — priority email, normal email, booking email, and in-app notifications. Publishers live in `infrastructure/messaging`; consumers in `infrastructure/listener`.

**i18n:** All user-facing messages use `MessageSource` keys. Translatable entities (room types, etc.) are stored via a `Translation` entity keyed by `TranslatableEntityType`. Do not flatten translations to plain strings.

**Idempotency:** Booking submission is idempotency-key protected. The key is stored in the DB and cleaned up by a scheduler. Check `infrastructure/idempotency` before adding any other mutation-safe endpoints.

**Security:** JWT issued by the app (RS256, keys under `src/main/resources/certs/`). Google OAuth2 login supported via `OAuthService`. Refresh tokens stored in DB. Token blacklist in Redis. Rate limiting via Bucket4j backed by Redis.

**WebSocket:** `/ws/**` is public (no JWT required at handshake). The `WebSocketTestController` under `/api/v1/ws-test/**` is for dev testing only.

**Command/Query service split:** Services handling mutations are named `*CommandService` (e.g. `BookingCommandService`, `BookingApprovalCommandService`); read-only services are `*QueryService`. Both have an interface in `service/` and an `impl/` class. Prefer this split when adding new service pairs.

**Spring Events:** Side effects are decoupled from the main request via Spring `ApplicationEvent`. Services publish events from `common/event/`; `service/listener/` classes consume them (e.g. `BookingStatusChangedEvent` triggers notification dispatch; `ViolationCreatedEvent` triggers penalty calculation). Do not call notification or penalty logic directly from command services — publish an event instead.

**Error codes:** All typed errors implement `BaseErrorCode` (code, message, HttpStatus, format). Add domain-specific codes to the matching enum in `common/exception/errorcode/` (e.g. `BookingErrorCode`, `AuthErrorCode`). `GlobalExceptionHandler` handles all `AppException` instances centrally.

### API Conventions

- All responses wrap in `ApiResult<T>` (data, meta, error fields).
- Pagination uses Spring Data `Pageable`.
- URL prefix: `/api/v1/**` for students, `/api/v1/admin/**` for admins.
- Public endpoints (no JWT): `/api/v1/auth/login`, `/api/v1/auth/register`, `/api/v1/auth/refresh`, `/api/v1/auth/google-login`.
- OpenAPI docs available at `/swagger-ui.html` when running locally.

## Domain Rules

1. **Bilingual content is required.** All i18n structures (translation table, `messages.properties`, `MessageSource` keys) must be preserved. Do not simplify to English-only.
2. **Roles live in the database.** The `roles` table and `role_id` FK must not be replaced with a string enum field.
3. **UTC internally, GMT+7 at presentation.** Never store or compare times in local timezone — use UTC throughout the service layer and convert only in response mappers.
4. **Simple RBAC only.** No wildcard permissions, no dynamic permission system — keep it `ROLE_STUDENT` / `ROLE_ADMIN`.

# Code Intelligence

## Exploration (use graphify)

For understanding architecture, tracing flows, and answering "how does X work":

```bash
/graphify query "concept"           # find communities and paths related to a concept
/graphify path "SymbolA" "SymbolB"  # shortest path between two nodes
/graphify explain "SymbolName"      # plain-language explanation of one node
```

Graph outputs live in `graphify-out/`:
- `graph.html` — open in browser for interactive exploration
- `GRAPH_REPORT.md` — community map, god nodes, surprising connections
- `graph.json` — raw data for programmatic queries

Keep the graph fresh after significant changes: `/graphify --update`

> See `ARCHITECTURE.md` for the community map, god nodes, and key cross-cutting flows derived from the graph.

## Before Editing Code (gitnexus required)

The GitNexus rules above (`<!-- gitnexus:start -->` block) still apply for all code modifications:
- MUST run `gitnexus_impact` before editing any symbol
- MUST run `gitnexus_detect_changes` before committing
- NEVER ignore HIGH or CRITICAL risk warnings
