# INF-223 final-review fixes report

Base commit: `eccc433a90297d0d21da0ca4ff9f78e6921ed480`
Worktree: `E:\skrisi\android\.worktrees\inf-223-geofence-runtime-spec-plan`
Scope: final-review findings only; no auth-contract or device/runtime verification status was promoted.

## 1. Cancellation is not converted into retryable UI failure

`AttendanceViewModel.refreshAttendanceAndRuntime` now delegates its boundary exception
handling to `runAttendanceRuntimeRefresh`. That boundary immediately rethrows
`CancellationException`; only unexpected `Exception`s call the existing retryable status
error path. The existing reconciled, backend-failure, and `AuthUnavailable` branches were
otherwise retained.

- RED: `app:testDebugUnitTest --tests '*AttendanceRuntimeRefreshCancellationPolicyTest'`
  failed because `runAttendanceRuntimeRefresh` did not exist.
- GREEN: the same command passed after the cancellation boundary and test were added.
- The test starts with a successful runtime UI state, cancels the operation, and verifies
  the cancellation propagates without applying retryable error UI state.

## 2. Permission/settings completion reaches the coordinator safely

`AttendancePermissionReadinessViewModel` owns a durable, coalesced pending-reconciliation
state. It reads the refreshed readiness synchronously after each successful refresh, then
tracks the first readiness refresh, the most recently reconciled readiness, and relevant
pending permission/settings/resume completions. It queues at most one request after a real
runtime-relevant readiness change, so the initial render and repeated resume callbacks do
not create a loop. Camera-only completion remains excluded.

`AttendanceScreen` collects that pending state only while its lifecycle is `STARTED`, calls
`AttendanceViewModel.onGeofenceRuntimeReadinessChanged`, and then acknowledges the request.
The pending state survives a stopped collector; the permission host also refreshes on every
screen resume whether or not its sheet is visible. The coordinator performs the normal
foreground refresh/reconcile. The presentation also uses the concrete
`GeofenceRuntimeUiState.requiresPermissionReadinessRecovery` decision to reopen the
permission surface only for actionable registration blockers (precise location, background
location, device location, or Play Services). Notification and generic degraded state are
not misrepresented as a registration recovery success.

- RED: `app:testDebugUnitTest --tests '*AttendancePermissionReadinessViewModelTest'` failed
  with seven unresolved durable-pending/acknowledgement references before that contract was
  added.
- GREEN: `app:testDebugUnitTest --tests '*AttendancePermissionReadinessViewModelTest' --tests '*GeofenceRuntimePresentationDecisionTest'`
  passed after the ownership/lifecycle path was corrected.
- Tests cover background-permission completion, settings + resume coalescing, and actionable
  versus non-actionable runtime-state presentation decisions. A regression test specifically
  makes `refreshReadiness()` produce the changed snapshot, proving reconciliation evaluates
  the refreshed value rather than a lagging collector value.

## 3. Event side effects and reconcile/logout have one order

`GeofenceRuntimeOperationLock` is an injected `@Singleton` coroutine mutex. Both
`AndroidGeofenceRuntimeRepository` and `GeofenceEventProcessor` receive the same Hilt
instance. The lock covers the full event validation-to-inside/evidence/notification side
effects and full reconcile/logout registration replacement, avoiding a stale pre-side-effect
snapshot. It does not use main-thread blocking and preserves coroutine cancellation.

- RED: `app:testDebugUnitTest --tests '*GeofenceRuntimeOperationRaceTest'` failed with
  `expected [inside, evidence, clear] but was [clear, inside, evidence]` before the shared
  lock.
- GREEN: the event/repository/race suite passed after lock injection and coverage were added.
- The deterministic race test pauses the event inside-state write, starts logout clear, then
  proves the clear waits until inside and evidence have completed.
- A companion deterministic test proves `reconcile()` likewise waits before removing or
  replacing registrations.

The ADR now records the cross-component serialization decision and its cancellation/non-main
thread properties.

## Verification evidence

All commands used JBR 17 (`C:\Users\Febriyadi\.jdks\jbr-17.0.14`) and the local Android SDK:

| Gate | Result |
| --- | --- |
| `app:testDebugUnitTest` | PASS: 558 tests, 0 failures, 0 errors, 2 skipped |
| `app:compileDebugAndroidTestKotlin` | PASS |
| `app:lintDebug` | PASS: 0 errors; 320 warnings and 20 informational findings already reported by lint |
| `app:assembleDebug` | PASS |

Focused combined regression coverage for the cancellation, permission lifecycle, runtime UI,
repository, event, and race suites also passed. `git diff --check` passed.

## Runtime limits

This is source/unit/build verification only. The 13 auth/device/runtime rows in
`docs/linear-sync/INF-223-runtime-verification.md` remain **Needs Verification**: no device,
permission-dialog, settings-return, foreground-service, or backend-auth flow was executed or
claimed as verified by this review.

## Independent review

The first independent read-only review found the lagging-readiness decision, dismissed-panel
resume, transient-delivery, and event-versus-reconcile coverage gaps described above. The
corrective delta received a second read-only review with **no remaining issues** and a
**Ready to merge** verdict, subject to the verification gates recorded here.
