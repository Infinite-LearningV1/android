# INF-238 — Attendance Preparation Design

Date: 2026-07-23  
Branch: `djangosuryaa/inf-238-attendance-preparation-spec-plan`

## 1. Decision

Permission Readiness, Work Mode, Target Location, and Preparation Eligibility form one cohesive Attendance Preparation stage.

```text
Permission Readiness
+ Work Mode Selection
+ Target Location Resolution
+ Preparation Eligibility
= Attendance Preparation
```

This stage answers one product question:

```text
Is the user ready to perform Attendance from this mode and target?
```

Geofencing, Face Recognition, and WFA Request remain separate bounded features because each owns a distinct runtime lifecycle, state machine, and failure model.

## 2. Goals

1. Create one authoritative preparation state for permission, mode, target, and eligibility.
2. Keep the current Attendance flow and backend semantics intact.
3. Remove duplicate preparation rules from `AttendanceViewModel` and `AttendanceActionResolver`.
4. Preserve Clean Architecture boundaries.
5. Make the state contract stable before visual redesign.
6. Keep migration incremental and reviewable.

## 3. Non-goals

- Rewriting the full Attendance feature.
- Moving final attendance validation to the client.
- Reworking Face Recognition internals.
- Reworking geofence runtime lifecycle in this issue.
- Rebuilding the WFA Request form in this issue.
- Changing check-in payload category mapping.
- Requiring work-mode or target reselection for checkout.

## 4. Current problems

`AttendanceScreenState` currently stores overlapping preparation values such as:

```text
targetLocation
wfoLocation
wfhLocation
selectedWfaLocation
selectedWorkMode
selectedTargetLocation
workModeEligibility
targetLocationMarker
isWfaModeActive
```

The same resolved target can be represented by `selectedTargetLocation.location`, `targetLocation`, and `targetLocationMarker`.

`AttendanceViewModel` currently owns permission readiness, target source mapping, WFA recommendations, mode selection, eligibility, map animation, geofence reminder orchestration, navigation route construction, action state, face handoff, and submission.

`AttendanceActionResolver` rechecks per-mode blockers after `EvaluateWorkModeEligibilityUseCase` has already evaluated them.

Asynchronous WFA eligibility can complete after a newer mode selection because mode resolution is launched without latest-selection-wins protection.

## 5. Architecture

### 5.1 Stage ownership

```text
AttendanceRoute / AttendanceScreen
├── AttendancePreparationViewModel
│   ├── required and optional permission readiness
│   ├── selected work mode
│   ├── WFO / WFH / approved-WFA target resolution
│   ├── preparation eligibility
│   ├── contextual recovery actions
│   └── preparation UI state and one-time effects
│
└── AttendanceViewModel
    ├── today attendance session
    ├── check-in / checkout action intent
    ├── face-verification handoff
    ├── backend submission
    └── final attendance result
```

Boundary rule:

```text
AttendancePreparationViewModel decides whether the user may proceed.
AttendanceViewModel executes the attendance action and reports the final backend result.
```

### 5.2 Standard dependency flow

```text
AttendancePreparationSection
→ AttendancePreparationViewModel
→ AttendancePreparationUseCase
→ repository interface
→ repository implementation
→ API / profile source / platform permission source
```

Presentation must not depend on DTO, Entity, DAO, Retrofit service, `NavController`, `Activity`, or `Context`.

Domain must not depend on Compose, Android framework classes, Retrofit, Room, or navigation types.

## 6. Domain model

### 6.1 Preparation eligibility

```kotlin
sealed interface AttendancePreparationEligibility {
    data object Resolving : AttendancePreparationEligibility

    data class Ready(
        val selectedTarget: SelectedTargetLocation
    ) : AttendancePreparationEligibility

    data class Blocked(
        val reason: AttendancePreparationBlockReason,
        val recoveryAction: AttendancePreparationRecoveryAction?
    ) : AttendancePreparationEligibility
}
```

### 6.2 Block reasons

```kotlin
sealed interface AttendancePreparationBlockReason {
    data object CameraPermissionMissing : AttendancePreparationBlockReason
    data object ForegroundLocationPermissionMissing : AttendancePreparationBlockReason
    data object DeviceLocationDisabled : AttendancePreparationBlockReason
    data object WfoTargetUnavailable : AttendancePreparationBlockReason
    data object WfhLocationMissing : AttendancePreparationBlockReason
    data object WfaApprovedTargetMissing : AttendancePreparationBlockReason
    data object AttendanceDateUnavailable : AttendancePreparationBlockReason
    data object TargetRefreshFailed : AttendancePreparationBlockReason
}
```

Optional notification and background-location permissions are capability states, not blocking reasons for manual attendance.

### 6.3 Recovery actions

```kotlin
sealed interface AttendancePreparationRecoveryAction {
    data object RequestRequiredPermission : AttendancePreparationRecoveryAction
    data object OpenApplicationSettings : AttendancePreparationRecoveryAction
    data object OpenDeviceLocationSettings : AttendancePreparationRecoveryAction
    data object RefreshTargets : AttendancePreparationRecoveryAction
    data object OpenWfhLocationSettings : AttendancePreparationRecoveryAction
    data object OpenWfaRequest : AttendancePreparationRecoveryAction
    data object OpenWfaRequests : AttendancePreparationRecoveryAction
    data object ChooseAnotherMode : AttendancePreparationRecoveryAction
}
```

Domain returns typed reasons and actions only. Presentation owns title, message, icon, status color, button label, and component variant.

## 7. Target source contract

```text
WFO target
→ `status-today.activeLocation`

WFH target
→ registered home location from logged-in profile

WFA recommendation
→ discovery and request input only

Approved WFA booking
→ authoritative WFA Attendance target
```

A selected recommendation must not silently replace an approved WFA Attendance target.

The implementation must not synthesize final WFA truth with hard-coded `locationId = 0` or `radius = 100` when approved-booking data exists.

## 8. Presentation state

```kotlin
data class AttendancePreparationUiState(
    val permissionReadiness: AttendancePermissionReadinessUiModel,
    val selectedMode: WorkMode,
    val availableModes: List<WorkModeOptionUiModel>,
    val selectedTarget: SelectedTargetLocationUiModel?,
    val eligibility: AttendancePreparationEligibilityUiModel,
    val isResolving: Boolean,
    val error: AttendancePreparationErrorUiModel?
)
```

`selectedTarget` is the only authoritative target exposed to preparation UI and downstream action-state integration.

Map marker presentation is derived from `selectedTarget` unless the map is explicitly showing a WFA preview. Preview state must be named separately and must not be treated as Attendance truth.

## 9. Events and effects

### 9.1 Events

```kotlin
sealed interface AttendancePreparationEvent {
    data object Entered : AttendancePreparationEvent
    data class WorkModeSelected(val mode: WorkMode) : AttendancePreparationEvent
    data object RefreshRequested : AttendancePreparationEvent
    data object RecoveryActionClicked : AttendancePreparationEvent
    data object PermissionStateChanged : AttendancePreparationEvent
}
```

### 9.2 Effects

```kotlin
sealed interface AttendancePreparationEffect {
    data class RequestPermissions(val permissions: Set<AttendancePermission>) : AttendancePreparationEffect
    data object OpenApplicationSettings : AttendancePreparationEffect
    data object OpenDeviceLocationSettings : AttendancePreparationEffect
    data class FocusSelectedTarget(val location: Location) : AttendancePreparationEffect
    data object OpenWfaRequest : AttendancePreparationEffect
    data object OpenWfaRequests : AttendancePreparationEffect
    data object OpenWfhLocationSettings : AttendancePreparationEffect
    data object ProceedToFaceVerification : AttendancePreparationEffect
}
```

Activity Result launchers, Android settings intents, map-camera execution, raw route construction, and `NavController` remain in the Route/UI layer.

## 10. Concurrency contract

Mode and target resolution must use latest-selection-wins semantics.

```text
Select WFA
→ asynchronous approved-target lookup begins
Select WFO before lookup completes
→ WFA work is cancelled or ignored
→ WFO remains authoritative
```

Use `flatMapLatest`, a dedicated cancellable `Job`, or an equivalent request-identity guard.

No stale result may overwrite a newer mode selection.

## 11. Integration with Attendance execution

```text
AttendancePreparationEligibility.Ready
→ AttendanceActionState may become Ready(CHECK_IN)

AttendancePreparationEligibility.Blocked(reason)
→ AttendanceActionState.Blocked(reason)
```

`AttendanceViewModel` and `AttendanceActionResolver` consume the typed preparation result. They must not independently re-evaluate permission, work-mode, or target-location rules after migration.

Checkout continues from the active backend attendance session and does not require mode or target reselection.

Face verification success remains evidence only. Final success occurs only after backend submission succeeds.

## 12. Separate feature boundaries

### 12.1 Geofencing

Owns reminder geofences, active monitoring geofences, registration/removal, background capability, session synchronization, and restart recovery where required.

It consumes selected target and session inputs but does not choose the target or decide preparation eligibility.

Main implementation remains coordinated through INF-223.

### 12.2 Face Recognition

Owns camera lifecycle, face detection, liveness, mismatch, timeout, retry, cancellation, and typed verification result.

It does not declare Attendance success.

### 12.3 WFA Request

Owns recommendation discovery, preview selection, request draft, validation, submission, pending/approved/rejected states, and request history/detail.

Attendance Preparation consumes only an approved WFA booking as the authoritative WFA Attendance target.

## 13. Permission behavior

Normal Attendance entry always rechecks current OS/device state.

```text
Required permission and device state ready
→ bypass blocking permission UI
→ resolve work mode and target

Required permission missing or GPS disabled
→ preparation blocked with contextual recovery
```

No persisted setup-completed flag is introduced.

Optional notification and background-location access never block manual attendance. They remain manageable from Attendance through a non-blocking access-management entry.

## 14. UI direction after state lock

Permission, Work Mode, and Target Location should be presented as one coherent preparation experience.

Suggested composition:

```text
Attendance Preparation header
→ permission readiness summary
→ WFO / WFH / WFA option cards
→ selected target card
→ eligibility or recovery alert
→ contextual supporting action
→ honest Continue to Face Verification CTA
```

Reusable Infinite components and design tokens must be used. Visual redesign must not alter business semantics.

## 15. Error handling

- Platform permission and device-state failures map to typed preparation blockers.
- Target-source failures map to typed domain failure classifications.
- Raw exception messages are never displayed directly.
- A target refresh failure must not silently reuse an incompatible target from another mode.
- Optional capability failures must not block manual attendance.
- Loading and blocked states must disable progression to Face Verification.

## 16. Testing strategy

### Domain tests

- WFO target resolves from `status-today.activeLocation`.
- WFH target resolves from profile home location.
- Missing WFH location blocks preparation.
- WFA uses approved booking as authoritative target.
- WFA recommendation alone does not make preparation ready.
- Required permissions and GPS states block correctly.
- Optional permissions do not block manual attendance.

### ViewModel tests

- Initial entry rechecks permission and target sources.
- Mode selection updates target and eligibility atomically.
- Latest selection wins during asynchronous resolution.
- Effects are emitted once and are not stored as persistent state.
- Refresh does not leave stale target or eligibility.

### Integration tests

- Ready preparation enables check-in action.
- Blocked preparation produces blocked action state.
- Checkout bypasses work-mode reselection.
- Proceed effect opens Face Recognition only when preparation is ready.

### UI/runtime evidence

- Required permission missing.
- GPS disabled.
- Optional permission missing without manual-attendance blocking.
- WFO ready with office target.
- WFH ready with home target.
- WFH blocked without home target.
- WFA blocked without approved booking.
- WFA ready with approved booking.
- Rapid WFA → WFO mode switch does not show stale WFA state.

## 17. Migration sequence

1. Lock typed preparation contracts and tests.
2. Introduce `AttendancePreparationViewModel` with bridge integration.
3. Move permission, mode, target, and eligibility ownership out of `AttendanceViewModel`.
4. Integrate typed preparation result into action state.
5. Remove duplicate preparation fields and fallback rules.
6. Move platform/map/navigation actions to effects.
7. Perform visual redesign after behavior and tests are stable.

## 18. Acceptance criteria

- Permission, Work Mode, Target Location, and preparation eligibility share one feature owner and `StateFlow`.
- `AttendancePreparationViewModel` has no `NavController`, Activity, DTO, Entity, Retrofit, Room, or Compose dependency.
- One authoritative selected target is consumed by preparation UI and downstream action state.
- Required permission or GPS unavailability blocks preparation.
- Optional permission absence degrades capability without blocking manual attendance.
- WFO uses backend status-today target.
- WFH uses registered profile home target.
- Approved WFA booking is authoritative for WFA Attendance target.
- WFA recommendation alone cannot enable Face Verification.
- Latest mode/target selection wins.
- `AttendanceViewModel` does not duplicate preparation rules after migration.
- Geofencing remains a separate runtime feature.
- Face Recognition remains a separate verification feature.
- WFA Request remains a separate form/submission feature.
- Checkout does not require mode or target reselection.
- One-time platform, map, and navigation actions use effects.
- Relevant unit and integration tests pass.
- `./gradlew app:assembleDebug` passes.
- Relevant runtime evidence is attached to the PR.
