# Daily Workflow

Reference for starting sessions and common tasks.

---

## Starting a New Session

1. **Orient** — what's the current state?
   ```bash
   git status && git log --oneline -10
   ```

2. **Explore** — what does the feature touch?
   ```
   /graphify query "<feature area>"
   /graphify explain "<key symbol>"
   ```

3. **Plan** — before writing code, read:
   - `ARCHITECTURE.md` → community map + god nodes
   - `.claude/docs/conventions.md` → naming + patterns
   - `.claude/docs/patterns.md` → step-by-step checklist

4. **Check impact before editing** (required by CLAUDE.md):
   - Run `gitnexus_impact` on any symbol you plan to modify

---

## Starting a New Feature

Use the `/feature` skill:
```
/feature <short description>
```
This will: query the graph, identify affected communities, check god node risk, and produce a scoped implementation plan.

---

## Adding a New Endpoint

Use the `/endpoint` skill:
```
/endpoint POST /api/v1/bookings/{id}/feedback
```
This will: follow `.claude/docs/patterns.md`, check conventions, create all layers in the right packages.

---

## Before Every Commit

Use the `/commit` skill:
```
/commit
```
This will: run `gitnexus_detect_changes`, summarize what changed, and write a conventional commit message.

Conventional commit format used in this project:
```
feat(booking): add room feedback submission endpoint
fix(notification): correct race condition in BookingNotificationListener
refactor(policy): extract cancellation spam check to separate method
docs(architecture): update community map after penalty engine changes
```

Scopes: `booking`, `auth`, `notification`, `penalty`, `admin`, `classroom`, `equipment`, `scheduler`, `infra`, `security`, `i18n`, `recommendation`

---

## Debugging an Issue

Use the `/debug` skill:
```
/debug <symptom or error>
```
Or manually:
1. `/graphify query "<error symptom>"`
2. Read the flows that come back
3. `/graphify explain "<suspect symbol>"`
4. Read the source file at the identified location

---

## Keeping Things Fresh

After committing (hook runs automatically, but if needed manually):
```bash
npx gitnexus analyze
/graphify --update
```

After large structural changes, update `ARCHITECTURE.md`:
- Re-run `/graphify --update`
- Update the community map table if communities shifted
- Update god nodes table if connectivity changed
