# /feature — Start a New Feature

Use when the user says: "add feature X", "implement Y", "I want to build Z", or invokes `/feature <description>`.

## What This Skill Does

Explores the codebase architecture before any code is written, identifies the communities affected, flags god node risk, and produces a scoped implementation plan aligned with project conventions.

---

## Steps (follow in order, do not skip)

### Step 1 — Read orientation docs

Read both of these before doing anything else:
- `ARCHITECTURE.md` — community map, god nodes, key flows
- `.claude/docs/conventions.md` — naming, patterns, rules

### Step 2 — Query the graph

Run a graphify query for the feature area:
```
/graphify query "<feature keywords>"
```

If the feature connects two distant areas, also run:
```
/graphify path "<entry point>" "<data store or service>"
```

Report back:
- Which **communities** are touched
- Which **god nodes** sit on the path (flag if any)
- Whether the feature crosses community boundaries (higher risk)

### Step 3 — Identify what needs to be created or modified

Based on the graph and ARCHITECTURE.md, list:

**New files needed:**
- Entity + migration (if new domain object)
- Repository (if new entity)
- Request/Response DTOs
- Error codes (in the right `*ErrorCode` enum)
- Service interface + impl (Command and/or Query)
- Controller method or new controller
- Event record (if side effects)
- Listener (if consuming the event)
- i18n keys (both EN and VI)

**Existing files modified:**
- Name the specific files and methods to add/change

### Step 4 — Check god node risk

For each **existing** symbol you plan to modify:
```
gitnexus_impact({target: "<symbolName>", direction: "upstream", repo: "room-booking"})
```

Report the risk level. If HIGH or CRITICAL: stop and warn the user before proceeding.

### Step 5 — Produce the implementation plan

Output a numbered plan in this format:

```
## Feature: <name>

### Communities touched
- <community name> — <why>

### God nodes on path (handle carefully)
- <symbol> (<N> edges) — <what to avoid>

### Implementation order
1. Migration V{N}__<name>.sql
2. Entity: entity/<Name>.java
3. Repository: repository/<Name>Repository.java
4. DTOs: common/dto/request/<Name>Request.java, common/dto/response/<Name>Response.java
5. Error codes: add to common/exception/errorcode/<Domain>ErrorCode.java
6. Mapper: common/mapper/<Name>Mapper.java
7. Service interfaces: service/<Name>CommandService.java, service/<Name>QueryService.java
8. Service impls: service/impl/<Name>CommandServiceImpl.java, service/impl/<Name>QueryServiceImpl.java
9. Event + Listener (if async side effects)
10. Controller: controller/<Name>Controller.java
11. i18n keys: messages_en.properties + messages_vi.properties

### Risk flags
- <anything the user should know before starting>
```

### Step 6 — Ask before coding

Present the plan and ask: "Should I proceed with implementation, or do you want to adjust the scope first?"

Do not write any code until the user confirms.
