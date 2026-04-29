# /commit — Pre-Commit Checklist + Commit

Use when: "commit", "save changes", "git commit", or invoked as `/commit`.

## What This Skill Does

Runs the required pre-commit checks, summarises what changed, and writes a conventional commit message matching this project's style.

---

## Steps

### Step 1 — Check what changed

Run both in parallel:
```bash
git status
git diff --stat
```

If there are no changes, stop and tell the user.

### Step 2 — Run gitnexus detect_changes (required by CLAUDE.md)

```
gitnexus_detect_changes({scope: "staged", repo: "room-booking"})
```

If nothing is staged yet, run on unstaged:
```
gitnexus_detect_changes({scope: "all", repo: "room-booking"})
```

Report:
- Which **symbols** changed
- Which **execution flows** are affected
- Whether any changes are outside the expected scope

If unexpected files appear, stop and ask the user to confirm before committing.

### Step 3 — Self-check (from CLAUDE.md)

Confirm all of these are true before proceeding:
- [ ] `gitnexus_impact` was run for all modified symbols earlier in this session
- [ ] No HIGH/CRITICAL warnings were ignored
- [ ] All d=1 (WILL BREAK) dependents were updated
- [ ] i18n keys added in both EN and VI (if user-facing strings changed)
- [ ] No hardcoded strings in service/controller code (use `I18nUtils.get` or error code keys)
- [ ] No `LocalDateTime` used for stored timestamps (must be `Instant`)

### Step 4 — Determine the conventional commit scope

| Changed area | Scope |
|-------------|-------|
| Booking create/approve/reject/cancel | `booking` |
| Auth, JWT, OAuth | `auth` |
| Notifications, WebSocket | `notification` |
| Penalty, violations | `penalty` |
| Admin controllers/services | `admin` |
| Classroom, building, room | `classroom` |
| Equipment | `equipment` |
| Scheduler jobs | `scheduler` |
| RabbitMQ, messaging infra | `infra` |
| Security config | `security` |
| i18n, translations | `i18n` |
| Student recommendations | `recommendation` |
| Analytics, dashboard | `analytics` |

### Step 5 — Write the commit message

Format:
```
<type>(<scope>): <short imperative description>

[optional body: why, not what]
```

Types: `feat`, `fix`, `refactor`, `docs`, `chore`, `test`, `perf`

Examples from this project:
```
feat(booking): add room feedback submission endpoint
fix(notification): use @TransactionalEventListener to eliminate pre-commit race
refactor(policy): extract validateCancellationSpam into BookingPolicyManager
docs(architecture): update community map after adding recommendation engine
chore(infra): add .claude skill files and workflow docs
```

### Step 6 — Stage and commit

Stage only the files that belong to this change:
```bash
git add <specific files>
git commit -m "$(cat <<'EOF'
<commit message>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

Never use `git add .` or `git add -A` — stage files explicitly to avoid accidentally committing `.env`, secrets, or temp files.
