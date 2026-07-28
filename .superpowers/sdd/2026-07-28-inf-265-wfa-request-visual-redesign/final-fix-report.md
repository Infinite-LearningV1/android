# INF-265 Final Fix Wave Report

Date: 2026-07-28

Branch: `codex/inf-265-wfa-request-visual-redesign`

Worktree: `E:\skrisi\android\.worktrees\inf-265-wfa-request-visual-redesign`

## Finding Resolutions

### Important 1 — Review Close destination

Resolved. Review Close now dispatches `WfaRequestEvent.EditClicked`, matching
Review Back/Edit. The graph-scoped controller handles `ReturnToForm`, so the
editable draft remains owned by the same nested-graph entry. Form Close and
Result Close still return to Attendance.

Files:

- `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`

Coverage now asserts the Form route, `Editing` phase, retained notes value, and
rendered retained draft after Review Close.

### Important 2 — Attendance rotation regression

Resolved. `MapInteractionMode.Interactive` again matches the legacy
`MapUiSettings`: compass off, indoor picker off, toolbar off, my-location
button off, rotation off, scroll on, tilt off, zoom controls off, and zoom
gestures on. Read-only remains fully disabled. Marker click policy is
unchanged.

Files:

- `app/src/main/java/com/example/infinite_track/presentation/map/adapter/GoogleAttendanceMap.kt`
- `app/src/test/java/com/example/infinite_track/presentation/map/adapter/MapPresentationPolicyTest.kt`

### Important 3 — Unsupported Pending hero claim

Resolved with neutral success copy:
`Permintaan WFA Anda berhasil dikirim. Lihat status permintaan untuk pembaruan.`
The backend status row remains visible and status-derived. The non-Pending
screen fixture uses `APPROVED` and now asserts the neutral copy while rejecting
the old Pending-only claim.

Files:

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`

### Minor 1 — Invalid coordinate truthfulness

Resolved. The Form conditions both the location status and the first checklist
fact on `hasValidCoordinates`. Invalid coordinates render error-semantic shared
status/checklist components with truthful copy; valid coordinates retain the
existing success presentation.

Continue still delegates `ReviewClicked` to the existing transaction owner.
The existing `ValidateWfaRequestDraftUseCaseTest.invalid location blocks
review` covers the validation boundary and passed in focused and full unit
gates.

Files:

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`

### Minor 2 — Dead resource

Resolved. `wfa_request_review_action` was removed from both locale files after
a repository-wide `rg` showed only the two declarations. A post-removal
repository-wide `rg` returned zero hits.

Files:

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`

### Spec exception — Result location provenance

Documented narrowly. The approved Result confirmed-detail list now explicitly
contains booking ID, backend status, date, reason, and applied radius, and
omits location until the domain can distinguish backend-provided location from
the request fallback. No API, repository, domain, mapper, or backend code was
changed.

Files:

- `docs/superpowers/specs/2026-07-28-inf-265-wfa-request-visual-redesign.md`
- `docs/superpowers/plans/2026-07-28-inf-265-wfa-request-visual-redesign.md`

## TDD Evidence

### RED

Command:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*MapPresentationPolicyTest" --console=plain
```

Environment: JBR 17 at
`C:\Users\Febriyadi\.jdks\jbr-17.0.14`, process-local Android SDK variables,
and in-process Kotlin.

Result: expected failure; 3 tests completed, 1 failed.
`existing map defaults remain permission gated and interactive` failed at the
new legacy rotation assertion because Interactive rotation was incorrectly
enabled.

The Review Close, result-copy, and invalid-location assertions are Compose
instrumentation tests. Before the production fix,
`app:compileDebugAndroidTestKotlin` succeeded, proving only that the tests
compiled. No device was attached, so an executable behavioral RED for those
three Compose cases was not available and is not claimed.

### Focused GREEN

Command:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*MapPresentationPolicyTest" --tests "*GoogleAttendanceMap*" --tests "*AttendanceMap*" --tests "*WfaRequest*" app:compileDebugAndroidTestKotlin --console=plain
```

Result: `BUILD SUCCESSFUL`.

- 54 focused unit tests
- 0 failures
- 0 errors
- 0 skipped
- Android-test Kotlin compilation passed

The focused set includes the existing invalid-location validation test.

## Full Local Gate

Command:

```powershell
.\gradlew.bat --no-daemon --console=plain -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest app:compileDebugAndroidTestKotlin app:lintDebug app:assembleDebug
```

Result: `BUILD SUCCESSFUL` in 1m 37s.

- Unit tests: 680 total, 0 failures, 0 errors, 2 skipped
- Android-test Kotlin compilation: passed
- Lint: 0 fatal, 0 errors, 330 warnings, 20 informational
- Debug assembly: passed

The only lint warning located in a touched production screen is the existing
`ModifierParameter` warning for `WfaRequestFormScreen`; its public parameter
order predates and is unchanged by this fix wave.

Additional audits:

- `git diff --check`: passed
- dead-resource search: zero hits
- WFA screen raw-visual audit: zero hits

## ADB and Runtime Evidence

Command:

```powershell
adb devices -l
```

Result:

```text
List of devices attached
```

No online device or emulator was available.

Needs Verification:

- Review Close returns to the rendered Form with the retained draft on-device
- neutral hero and confirmed status row for non-Pending responses on-device
- invalid-coordinate shared status/checklist rendering on-device
- Google Maps rendering and read-only gesture behavior
- screenshots, compact/large-font behavior, and authenticated submission flow

## Commit

This report and all final-review corrections are included in the single commit
with subject `fix(wfa): address final review findings`. After creation, the
authoritative commit is the branch `HEAD` returned by `git rev-parse HEAD`.

## Concerns

- Device/runtime verification remains outstanding because ADB reported no
  attached device.
- The full lint gate passes with no fatal/errors, but the repository still
  reports 330 warnings and 20 informational findings outside this bounded fix
  wave.
