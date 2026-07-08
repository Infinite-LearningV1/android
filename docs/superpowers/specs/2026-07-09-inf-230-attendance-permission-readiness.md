# INF-230 — Android Attendance Permission Readiness

Date: 2026-07-09
Branch/worktree: `fix/android-attendance-permission-readiness` / `C:\Users\Febriyadi\.claude\worktrees\android-attendance-permission-readiness`

## Summary

INF-230 consolidates Attendance-related permission ownership into one readiness gate before the user reaches the existing Attendance work-mode UI. This is a permission ownership change, not a backend contract change and not a redesign of Work Mode Selection, Attendance submission, or FaceScanner.

Final target flow:

```text
Home / Attendance entry
→ Attendance Permission Readiness Screen
→ required access check/request
→ Work Mode Selection / AttendanceScreen
→ Attendance flow
```

Android remains a trusted data-capture client. Backend remains authoritative for attendance outcomes, auth/session validity, booking approval semantics, scheduled-job effects, and final reporting state.

## Current Repo Evidence

- Attendance route is `Screen.Attendance : Screen("attendance")` in `presentation/navigation/Screen.kt`.
- Home attendance entry currently navigates directly to `Screen.Attendance.route` in `presentation/navigation/MainContentNavGraph.kt`.
- Internship FAB currently navigates directly to `Screen.Attendance.route` in `presentation/main/MainScreen.kt`.
- `MainActivity` currently requests `POST_NOTIFICATIONS` after startup navigation resolves to Home through `StartupNotificationPermissionPolicy`.
- `AttendanceMap` currently owns foreground location request and background location request/settings fallback.
- `AttendanceScreen` still shows `LocationPermissionDialog` through `LocationPermissionHelper`.
- `FaceScannerScreen` currently launches camera permission request from `LaunchedEffect(Unit)` when the scanner opens.
- `LocationPermissionHelper` currently treats foreground + background location as all required permissions.
- `GeofenceManager` currently treats foreground + background location as full geofence readiness.

## Product Contract

### Required access — hard blocker before Work Mode Selection

1. Foreground precise location.
2. Camera access.
3. Device location setting ON.

The readiness screen must not continue to Work Mode Selection until all required access is ready.

### Optional / degraded access — not a blocker

4. Notification reminder.
5. Background location for geofence reminder / active monitoring.

Notification denied/skipped and background location denied/skipped must not block basic/manual attendance. They should mark reminder/background monitoring as degraded.

### Progress copy

Primary progress must use required count only:

```text
2/3 akses wajib siap
3/3 akses wajib siap
```

Do not use `2/5 akses siap` as the primary progress because notification/background location are optional rows.

## UX Requirements

The readiness screen should follow the visual direction from the issue:

- Attendance top bar.
- Soft blurred map-like background.
- Semi-liquid white/lavender card.
- Hero illustration/metaphor for secure location and camera access.
- Checklist permission rows.
- Progress indicator using required gate count.
- Info box explaining required vs optional/degraded access.
- Primary CTA:
  - `Minta Izin` / `Lanjutkan Setup` while required access is pending.
  - `Lanjut ke Mode Kerja` when required access is complete.
- Secondary action for device location settings or optional setup when applicable.

Use existing INF-222 design components where practical:

- `InfiniteCard` / `InfiniteSurface`
- `InfiniteStatusPill`
- `InfiniteInfoRow`
- `InfiniteButton`
- `InfiniteIconButton`
- `InfiniteInlineAlert`

Small local wrappers are allowed:

- `AttendancePermissionReadinessScreen`
- `PermissionMissionRow`
- `PermissionProgressHeader`
- `PermissionReadinessInfoBox`

## State Model

Recommended presentation state:

```kotlin
data class AttendancePermissionReadinessUiState(
    val foregroundLocationGranted: Boolean,
    val cameraGranted: Boolean,
    val deviceLocationEnabled: Boolean,
    val notificationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val requiredReadyCount: Int,
    val requiredTotalCount: Int = 3,
    val canContinueToWorkMode: Boolean,
    val nextRequiredAction: AttendancePermissionAction?,
    val warningMessage: String? = null
)

enum class AttendancePermissionAction {
    REQUEST_FOREGROUND_LOCATION,
    REQUEST_CAMERA,
    OPEN_DEVICE_LOCATION_SETTINGS,
    REQUEST_NOTIFICATION,
    REQUEST_BACKGROUND_LOCATION,
    CONTINUE_TO_WORK_MODE
}
```

Required readiness must be derived from:

```text
foregroundLocationGranted && cameraGranted && deviceLocationEnabled
```

Optional readiness must be derived separately:

```text
hasOptionalReminderAccess = notificationGranted
hasOptionalBackgroundGeofenceAccess = backgroundLocationGranted
```

## Architecture / Ownership

### New owner

`AttendancePermissionReadinessScreen` owns the normal permission request sequence for Attendance:

- foreground location request
- camera request
- device location settings action
- optional notification request after user action
- optional background location request/settings action after user action

The screen must refresh readiness when returning from permission or settings flows.

### Downstream consumers

`AttendanceScreen`, `AttendanceMap`, `FaceScannerScreen`, and `GeofenceManager` are consumers/defensive fallbacks, not normal permission owners.

- `AttendanceMap` may show a defensive fallback if foreground permission is unexpectedly missing, but should not own normal request flow and should not require background location to render basic/manual attendance UI.
- `FaceScannerScreen` may show a defensive camera permission fallback, but should not automatically request camera permission on open.
- `AttendanceScreen` should avoid prompting location permission as the normal path. Existing dialog can remain only if it is clearly defensive and does not reintroduce foreground/background as required before readiness.
- `GeofenceManager` may still require background location to register geofences, but helper naming/copy must not imply background location blocks basic/manual attendance.

## Navigation Contract

Add a new route before Attendance:

```text
Screen.AttendancePermissionReadiness.route
```

Normal attendance entries should navigate to readiness:

- Home attendance action.
- Internship FAB attendance action.
- AppNavigator / notification-triggered attendance navigation.

Readiness should navigate to `Screen.Attendance.route` only when required access is ready.

## Non-goals

- Do not change backend contracts.
- Do not change check-in/check-out submission logic.
- Do not redesign Work Mode Selection.
- Do not redesign FaceScanner.
- Do not request all permissions automatically on screen open.
- Do not make notification/background location hard blockers.
- Do not create a global `InfiniteIcons` object.
- Do not refactor unrelated Home dashboard features.

## Acceptance Criteria

- Attendance-specific notification permission is no longer requested automatically on Home/startup.
- User sees notification permission request only after entering readiness screen and taking an action.
- Readiness checks foreground location, camera, device location setting, notification, and background location in one grouped screen.
- Progress uses required gate count, e.g. `2/3 akses wajib siap`.
- Foreground location, camera, and device location setting are hard blockers before Work Mode Selection.
- Notification denied/skipped does not block Work Mode Selection.
- Background location denied/skipped does not block Work Mode Selection.
- Background location denied/skipped marks reminder/background geofence as degraded.
- AttendanceMap no longer owns the normal permission request flow.
- FaceScanner no longer owns the normal camera permission request flow.
- Existing fallback permission UIs may remain defensive only.
- Permission Readiness screen appears before Work Mode Selection when required readiness is incomplete.
- Screen follows the provided visual direction.
- `./gradlew app:assembleDebug` passes.

## Verification Requirements

Build:

```bash
./gradlew app:assembleDebug
```

Recommended additional checks:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime/emulator evidence required before Done:

- App opens to Home without notification permission prompt.
- User taps Attendance and sees Permission Readiness screen.
- Screenshot: default readiness screen.
- Screenshot: granted/check state.
- Screenshot: required pending state.
- Screenshot: optional skipped/degraded state.
- Screenshot: `3/3 akses wajib siap` and CTA `Lanjut ke Mode Kerja`.
- Recording: camera permission requested from readiness, not FaceScanner.
- Recording: foreground location requested from readiness, not AttendanceMap.
- Recording: notification permission requested from readiness, not Home.
- Recording: background location denied but manual attendance can continue.

If runtime/emulator verification cannot be executed, status must remain `Needs Verification`.

## Docs / ADR Note

This work changes permission ownership architecture and navigation gating. At minimum, PR notes must document:

- Attendance permissions are now grouped under one readiness gate.
- Notification prompt removed from startup/Home flow.
- Required vs optional access clarified.
- Downstream screens keep defensive fallback only.

A focused ADR/implementation note is recommended if this change becomes a durable architecture precedent for feature-scoped permission ownership.
