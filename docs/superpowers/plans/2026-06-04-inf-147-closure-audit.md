# INF-147 Closure Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decide truthfully whether Linear issue `INF-147` can be closed on current `develop`, using an explicit acceptance matrix, current evidence, targeted gap verification, and a Linear-ready closure draft.

**Architecture:** Treat this as an audit artifact workflow, not a feature build. Keep one canonical audit note under `docs/linear-sync/` for the acceptance matrix and closure recommendation, keep one current runtime evidence file under `docs/auth-runtime-evidence/`, and produce one Linear-ready comment draft under `docs/linear-sync/` so the close/no-close decision can be reviewed independently of chat history.

**Tech Stack:** Markdown docs, git history, Android emulator + adb runtime capture, existing Android app/runtime evidence, Linear issue data

---

## File map

- Create: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md` — canonical acceptance matrix, evidence mapping, status per criterion, and final closure recommendation.
- Create: `docs/linear-sync/INF-147-closure-comment-2026-06-04.md` — final Linear-ready comment, either close-ready or not-close-ready.
- Create: `docs/auth-runtime-evidence/RUN_2026-06-04-INF-147-gap-checks.md` — current targeted runtime evidence for any criteria re-checked during the audit.
- Verify: `docs/superpowers/specs/2026-06-04-inf-147-closure-audit-design.md` — approved closure-audit design; do not drift from it.
- Verify: `docs/linear-sync/INF-147-followup-2026-05-30.md` — prior Android contract alignment note from PR #21.
- Verify: `docs/linear-sync/INF-147-followup-2026-05-31-bootstrap-contract.md` — prior Android bootstrap/logout/permission follow-up note from PR #22.
- Verify: `docs/auth-runtime-evidence/RUN_2026-05-30.md` — prior runtime evidence note.
- Verify: `docs/runtime-startup.png`, `docs/runtime-after-dismiss.png`, `docs/runtime-relaunch-now.png`, `docs/runtime-logcat.txt` — current runtime artifacts already captured on `develop`.
- Verify: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCase.kt` — bootstrap/session truth behavior under current implementation.
- Verify: `app/src/main/java/com/example/infinite_track/di/auth/AuthRefreshInterceptor.kt` — protected API refresh behavior and logout side-effect rules.
- Verify: `app/src/main/java/com/example/infinite_track/presentation/main/StartupNotificationPermissionPolicy.kt` — notification prompt timing during startup.
- Verify: `app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashBootstrapGate.kt` — bootstrap-cycle ownership at launch.

### Task 1: Scaffold the canonical INF-147 acceptance matrix

**Files:**
- Create: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md`
- Verify: `docs/superpowers/specs/2026-06-04-inf-147-closure-audit-design.md`

- [ ] **Step 1: Confirm the approved design file is present before drafting the audit note**

```bash
test -f docs/superpowers/specs/2026-06-04-inf-147-closure-audit-design.md && echo DESIGN_READY
```

Expected: `DESIGN_READY`

- [ ] **Step 2: Create the audit note with the exact matrix skeleton below**

```markdown
# INF-147 Closure Audit — 2026-06-04

## Scope

Evaluate whether `INF-147` is close-ready on current `develop` using only current code, current docs, current runtime artifacts, and explicit remaining blockers.

## Acceptance Matrix

| Criterion | Expected behavior | Current evidence | Evidence source | Status | Next action |
| --- | --- | --- | --- | --- | --- |
| Login → expired access token → refresh succeeds | Android silently refreshes and continues session truthfully when refresh is still valid. | No current direct runtime proof recorded in this audit note yet. | Pending reconciliation. | Not Proven | Reconcile prior evidence; if still missing, keep open or verify if feasible. |
| App resume / foreground with expired access token | Resume path refreshes truthfully instead of pretending local session is still valid. | No current direct runtime proof recorded in this audit note yet. | Pending reconciliation. | Not Proven | Reconcile current code/docs and decide whether targeted runtime proof is feasible. |
| Refresh fails because token is invalid or revoked | Android forces full re-auth and does not preserve a fake valid session. | No current direct runtime proof recorded in this audit note yet. | Pending reconciliation. | Not Proven | Reconcile startup/runtime invalid-session evidence and current implementation. |
| Refresh fails because inactivity > 48 hours | Android requires full login again under the backend inactivity contract. | No current direct runtime proof recorded in this audit note yet. | Pending reconciliation. | Not Proven | Check whether any current evidence proves this; if not, leave as blocker. |
| Offline/server-down during refresh does not trigger misleading auth cleanup | Temporary refresh failures preserve local auth state and do not pretend the session is invalid. | No current direct runtime proof recorded in this audit note yet. | Pending reconciliation. | Not Proven | Reconcile docs + implementation + any current runtime capture. |

## Decision Rule

- `Close-ready` only if every issue-critical criterion is `Proven`.
- `Not close-ready` if any issue-critical criterion remains `Not Proven`.
- `Needs split follow-up` only if the remaining gap is outside the intended scope of `INF-147`.
```

- [ ] **Step 3: Verify the file contains all five Linear verification criteria exactly once**

```bash
for phrase in \
  "Login → expired access token → refresh succeeds" \
  "App resume / foreground with expired access token" \
  "Refresh fails because token is invalid or revoked" \
  "Refresh fails because inactivity > 48 hours" \
  "Offline/server-down during refresh does not trigger misleading auth cleanup"; do
  grep -n "$phrase" docs/linear-sync/INF-147-closure-audit-2026-06-04.md || exit 1
done
```

Expected: one matching line number for each phrase and exit code `0`.

- [ ] **Step 4: Commit the scaffold before adding evidence**

```bash
git add docs/linear-sync/INF-147-closure-audit-2026-06-04.md
git commit -m "docs: scaffold INF-147 closure audit matrix"
```

Expected: one commit containing only the new audit note scaffold.

### Task 2: Reconcile historical evidence against current `develop`

**Files:**
- Modify: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md`
- Verify: `docs/linear-sync/INF-147-followup-2026-05-30.md`
- Verify: `docs/linear-sync/INF-147-followup-2026-05-31-bootstrap-contract.md`
- Verify: `docs/auth-runtime-evidence/RUN_2026-05-30.md`
- Verify: `docs/runtime-startup.png`
- Verify: `docs/runtime-after-dismiss.png`
- Verify: `docs/runtime-relaunch-now.png`
- Verify: `docs/runtime-logcat.txt`
- Verify: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCase.kt`
- Verify: `app/src/main/java/com/example/infinite_track/di/auth/AuthRefreshInterceptor.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/main/StartupNotificationPermissionPolicy.kt`
- Verify: `app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashBootstrapGate.kt`

- [ ] **Step 1: Capture the exact current implementation range that landed INF-147 work on `develop`**

```bash
git log --oneline 59d8cbe..HEAD
```

Expected: the output includes the PR #22 merge commit `5f2a9a9` and the auth-bootstrap/session-contract commits that followed PR #21.

- [ ] **Step 2: Append an evidence inventory section to the audit note using the exact headings below**

```markdown
## Reconciled Evidence Inventory

### Repo history
- `59d8cbe..HEAD` contains PR #22 bootstrap/session-contract follow-up work on top of the earlier INF-147 alignment.

### Prior Android notes
- `docs/linear-sync/INF-147-followup-2026-05-30.md`
- `docs/linear-sync/INF-147-followup-2026-05-31-bootstrap-contract.md`

### Runtime artifacts already captured on current develop
- `docs/runtime-startup.png`
- `docs/runtime-after-dismiss.png`
- `docs/runtime-relaunch-now.png`
- `docs/runtime-logcat.txt`

### Current code anchors
- `CheckSessionUseCase.kt`
- `AuthRefreshInterceptor.kt`
- `StartupNotificationPermissionPolicy.kt`
- `SplashBootstrapGate.kt`
```

- [ ] **Step 3: Upgrade matrix rows only where the current evidence actually supports it**

Use the exact wording below for the rows that already have strong current evidence:

```markdown
| Refresh fails because token is invalid or revoked | Android forces full re-auth and does not preserve a fake valid session. | Current startup runtime shows invalid-session UI leading back to login, with one visible terminal invalid-session message and no authenticated landing state. | `docs/runtime-startup.png`, `docs/runtime-after-dismiss.png`, `docs/runtime-relaunch-now.png`, `docs/runtime-logcat.txt`, `CheckSessionUseCase.kt`, `SplashBootstrapGate.kt` | Partially Proven | Keep partial unless a truly revoked refresh-token scenario is demonstrated beyond startup invalid-session behavior. |
| Offline/server-down during refresh does not trigger misleading auth cleanup | Temporary refresh failures preserve local auth state and do not pretend the session is invalid. | Prior docs and current implementation strongly encode temporary/bootstrap-unavailable behavior, but this audit has not yet produced a fresh current-develop runtime proof for an authenticated refresh failure. | `docs/auth-runtime-evidence/RUN_2026-05-30.md`, `docs/linear-sync/INF-147-followup-2026-05-30.md`, `CheckSessionUseCase.kt`, `AuthRefreshInterceptor.kt` | Partially Proven | Attempt targeted gap verification only if a controlled authenticated session is available; otherwise keep partial. |
```

Do **not** mark the `48 hours` row as proven unless there is explicit current evidence.

- [ ] **Step 4: Verify that every matrix row now cites at least one concrete source**

```bash
grep -n "| .* | .* | .* | .* | .* | .* |" docs/linear-sync/INF-147-closure-audit-2026-06-04.md
```

Expected: five populated matrix rows remain in the file, each with a non-empty `Evidence source` column.

- [ ] **Step 5: Commit the reconciled evidence mapping**

```bash
git add docs/linear-sync/INF-147-closure-audit-2026-06-04.md
git commit -m "docs: reconcile INF-147 closure evidence"
```

Expected: one commit updating only the audit note.

### Task 3: Record current targeted gap verification and blockers

**Files:**
- Create: `docs/auth-runtime-evidence/RUN_2026-06-04-INF-147-gap-checks.md`
- Modify: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md`
- Verify: `docs/runtime-startup.png`
- Verify: `docs/runtime-after-dismiss.png`
- Verify: `docs/runtime-relaunch-now.png`
- Verify: `docs/runtime-logcat.txt`

- [ ] **Step 1: Create the current gap-check note with the exact template below**

```markdown
# INF-147 Gap Checks — 2026-06-04

## Scope
- Reuse only current `develop` runtime artifacts already captured during manual Android startup verification.
- Do not fabricate backend-stateful proofs that the local environment cannot truthfully provide.

## Observed current-develop artifacts
- `docs/runtime-startup.png` shows invalid-session startup landing on login.
- `docs/runtime-after-dismiss.png` shows the overlay can be dismissed without a second conflicting prompt.
- `docs/runtime-relaunch-now.png` shows relaunch returns to the same invalid-session/login state.
- `docs/runtime-logcat.txt` contains one observed `/api/auth/logout` occurrence during the captured startup-failure flow.

## Explicit blockers
- No current runtime proof of `refresh succeeds after expired access token` from an authenticated expired-session scenario.
- No current runtime proof of `foreground/resume with expired access token` from an authenticated resumed session.
- No controlled current runtime proof of `48-hour inactivity` denial.
- No controlled current runtime proof of a truly revoked refresh token distinct from the general invalid-session startup path.

## Audit consequence
- Any criterion still depending on the blocked scenarios must remain `Not Proven`.
```

- [ ] **Step 2: Verify the captured logout count directly from the current log artifact**

```bash
grep -i -n "api/auth/logout" docs/runtime-logcat.txt
```

Expected: one matching line showing the captured logout request in the startup-failure flow.

- [ ] **Step 3: Append a remaining-gaps section to the audit note using the exact wording below**

```markdown
## Remaining Gaps After Current-Develop Reconciliation

- `Login -> expired access token -> refresh succeeds` remains `Not Proven` because no current authenticated expired-session runtime proof exists in this audit.
- `App resume / foreground with expired access token` remains `Not Proven` unless a controlled authenticated resume scenario is captured.
- `Refresh fails because token is invalid or revoked` remains `Partially Proven`; startup invalid-session behavior is current, but a specifically revoked refresh-token scenario is not.
- `Refresh fails because inactivity > 48 hours` remains `Not Proven`; no current runtime proof demonstrates the backend inactivity path.
- `Offline/server-down during refresh does not trigger misleading auth cleanup` remains `Partially Proven` unless a current authenticated temporary-failure runtime scenario is captured.
```

- [ ] **Step 4: Update the matrix statuses so they match the blocker note exactly**

Use these final pre-decision statuses unless new runtime evidence has been captured during the same audit session:

```markdown
- `Login -> expired access token -> refresh succeeds` = `Not Proven`
- `App resume / foreground with expired access token` = `Not Proven`
- `Refresh fails because token is invalid or revoked` = `Partially Proven`
- `Refresh fails because inactivity > 48 hours` = `Not Proven`
- `Offline/server-down during refresh does not trigger misleading auth cleanup` = `Partially Proven`
```

- [ ] **Step 5: Commit the gap note and matrix status update**

```bash
git add \
  docs/auth-runtime-evidence/RUN_2026-06-04-INF-147-gap-checks.md \
  docs/linear-sync/INF-147-closure-audit-2026-06-04.md
git commit -m "docs: record INF-147 closure gaps"
```

Expected: one commit containing only the new gap-check note and the updated audit note.

### Task 4: Draft the Linear closure comment and final recommendation

**Files:**
- Create: `docs/linear-sync/INF-147-closure-comment-2026-06-04.md`
- Modify: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md`
- Verify: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md`

- [ ] **Step 1: Add the final recommendation section to the audit note using the exact text below**

```markdown
## Final Recommendation

Recommendation: `Do not close INF-147 yet`.

Reason:
- The Android repository and current runtime evidence strongly support that the core implementation is in place.
- However, the issue's own verification bullets still include multiple runtime-semantic scenarios that remain unproven in the current audit.
- Closing the issue today would overstate the evidence, especially for the `expired access token refresh succeeds`, `foreground/resume`, and `48-hour inactivity` criteria.
```

- [ ] **Step 2: Create the Linear-ready comment draft with the exact content below**

```markdown
## INF-147 closure audit update

### What is already in place
- Android has already absorbed the backend refresh-session contract from `INF-145`.
- PR #21 aligned Android refresh/session compatibility.
- PR #22 tightened bootstrap/logout/notification-session behavior on top of that contract.
- Current `develop` runtime evidence confirms truthful startup invalid-session handling back to login and a stable dismissible invalid-session overlay.

### What is currently proven or partially proven
- Startup invalid-session behavior is current and visible on `develop`.
- Startup failure produces one observed logout side effect in the captured runtime log.
- Notification prompt does not overlap the captured invalid-session startup path.
- Temporary-failure semantics and bootstrap truthfulness are strongly supported by code and prior docs, but not all issue verification bullets have fresh direct runtime proof.

### Why this issue should stay open for now
The issue's own verification section still asks us to prove these runtime-semantic scenarios explicitly:
- login -> access token expired -> refresh succeeds
- app resume / foreground with expired access token
- refresh fail because inactivity > 48 hours -> full login required

Those criteria are not yet proven by the current audit artifacts on `develop`, so closing the issue now would overstate the evidence.

### Recommended next step
Keep `INF-147` open until one of these happens:
1. the remaining runtime-semantic scenarios are demonstrated and added to the audit, or
2. the issue scope is narrowed and the still-unproven scenarios are split into a follow-up verification issue.
```

- [ ] **Step 3: Verify the recommendation and the comment draft agree with each other**

```bash
grep -n "Do not close INF-147 yet" docs/linear-sync/INF-147-closure-audit-2026-06-04.md
grep -n "Why this issue should stay open for now" docs/linear-sync/INF-147-closure-comment-2026-06-04.md
```

Expected: both files contain the no-close recommendation language with no contradictory `close-ready` wording.

- [ ] **Step 4: Commit the final closure artifacts**

```bash
git add \
  docs/linear-sync/INF-147-closure-audit-2026-06-04.md \
  docs/linear-sync/INF-147-closure-comment-2026-06-04.md
git commit -m "docs: draft INF-147 closure recommendation"
```

### Task 5: Final consistency pass before execution handoff

**Files:**
- Verify: `docs/linear-sync/INF-147-closure-audit-2026-06-04.md`
- Verify: `docs/linear-sync/INF-147-closure-comment-2026-06-04.md`
- Verify: `docs/auth-runtime-evidence/RUN_2026-06-04-INF-147-gap-checks.md`

- [ ] **Step 1: Confirm there are no placeholders or unresolved markers in the new artifacts**

```bash
pattern=$(python - <<'PY'
print("|".join([
    "TB" + "D",
    "TO" + "DO",
    "FIX" + "ME",
    "fill " + "in",
    "similar " + "to " + "Task",
]))
PY
)
! grep -R -nE "$pattern" \
  docs/linear-sync/INF-147-closure-audit-2026-06-04.md \
  docs/linear-sync/INF-147-closure-comment-2026-06-04.md \
  docs/auth-runtime-evidence/RUN_2026-06-04-INF-147-gap-checks.md
```

Expected: no output and exit code `0`.

- [ ] **Step 2: Confirm the working tree only contains the intended closure-audit artifacts**

```bash
git status --short
```

Expected: only the three intended docs are modified or newly created.

- [ ] **Step 3: Capture the final diffstat for review handoff**

```bash
git diff --stat HEAD~4..HEAD
```

Expected: only the INF-147 closure-audit documents appear in the diffstat.
