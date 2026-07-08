# INF-230 Implementation Plan — Attendance Permission Readiness

Date: 2026-07-09
Branch/worktree: `fix/android-attendance-permission-readiness` / `C:\Users\Febriyadi\.claude\worktrees\android-attendance-permission-readiness`
Spec: `docs/superpowers/specs/2026-07-09-inf-230-attendance-permission-readiness.md`

## Scope

Implement an Attendance Permission Readiness screen before the existing Attendance work-mode UI and consolidate normal Attendance permission ownership there.

This plan intentionally avoids backend contract changes, check-in/check-out submission changes, Work Mode Selection redesign, and FaceScanner redesign.

## Pre-flight Evidence

- Main checkout `E:\skrisi\android` is dirty and must not be edited.
- Isolated branch/worktree created from `develop` HEAD `c5b8e6c`.
- Existing attendance-related worktrees are stale/different scope and not used for this issue.
- `.claude/rules/*.md` files requested by operator are not present; `CLAUDE.md` remains the active repo contract.

## Implementation Steps

### 1. Add navigation route and gate normal Attendance entries

Files:

- `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- `app/src/main/java/com/example/infinite_track/presentation/main/MainScreen.kt`

Actions:

1. Add `Screen.AttendancePermissionReadiness` route.
2. Add composable route for readiness screen in `MainContentNavGraph`.
3. Change Home attendance entry to navigate to readiness instead of `Screen.Attendance`.
4. Change Internship FAB attendance entry to navigate to readiness instead of `Screen.Attendance`.
5. Keep `Screen.Attendance` as the existing Work Mode Selection / Attendance screen target.

Expected outcome:

- Normal user path becomes Home/FAB → readiness → Attendance.
- Existing Attendance route remains available for post-readiness navigation and defensive direct route cases.

### 2. Remove startup/Home notification permission prompt

Files:

- `app/src/main/java/com/example/infinite_track/presentation/main/MainActivity.kt`
- `app/src/main/java/com/example/infinite_track/presentation/main/StartupNotificationPermissionPolicy.kt`
- Related unit tests if present.

Actions:

1. Stop observing startup navigation state for notification permission request.
2. Remove the `POST_NOTIFICATIONS` launcher from `MainActivity` if no longer needed.
3. Remove or neutralize startup policy usage so Home is not a prompt trigger.
4. Keep notification channel creation intact because channels are not the same as runtime permission prompts.
5. If policy tests exist, update them to reflect that startup no longer owns this prompt, or remove policy if unused.

Expected outcome:

- App can open to Home without Android 13+ notification prompt.
- Notification permission can only be requested from readiness after user action.

### 3. Add readiness state and permission gateway

Recommended files:

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessUiState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionAction.kt`

Optional if needed for testability:

- `app/src/main/java/com/example/infinite_track/domain/attendance/permission/*`
- `app/src/main/java/com/example/infinite_track/data/attendance/permission/*`

Actions:

1. Model required state:
   - foreground precise location granted
   - camera granted
   - device location setting enabled
2. Model optional state:
   - notification granted or not applicable pre-Android 13
   - background location granted or not applicable pre-Android 10
3. Derive:
   - `requiredReadyCount`
   - `requiredTotalCount = 3`
   - `canContinueToWorkMode`
   - `nextRequiredAction`
4. Use Android APIs carefully:
   - `ContextCompat.checkSelfPermission` for location/camera/notification/background.
   - `LocationManager` or Google location settings check for device location enabled.
   - Activity result launchers in the screen for permission/settings user actions.
5. Refresh readiness on resume/settings return.

Expected outcome:

- Screen can honestly show readiness and degraded optional access without invoking backend or attendance submission code.

### 4. Build readiness UI with existing design system components

Files:

- `AttendancePermissionReadinessScreen.kt`
- Small local wrapper composables in same package/file if simple.

Actions:

1. Compose top bar, background, hero/card, permission rows, progress header, info box, and CTAs.
2. Use progress copy like `2/3 akses wajib siap`.
3. Required rows:
   - Lokasi presisi
   - Kamera
   - Lokasi perangkat aktif
4. Optional/degraded rows:
   - Notifikasi pengingat
   - Lokasi latar belakang
5. Primary CTA behavior:
   - If required incomplete: execute next required action.
   - If required complete: navigate to `Screen.Attendance.route` with CTA `Lanjut ke Mode Kerja`.
6. Optional actions must be user-initiated and must not block continue.

Expected outcome:

- Readiness screen matches product direction and makes required vs optional semantics visible.

### 5. Refactor AttendanceMap to defensive fallback only

File:

- `app/src/main/java/com/example/infinite_track/presentation/components/maps/AttendanceMap.kt`

Actions:

1. Remove normal foreground permission request ownership from map.
2. Remove background location as a map rendering blocker.
3. Keep a defensive missing-foreground UI if route is reached unexpectedly without foreground permission.
4. Ensure `location.enabled = true` is only called when foreground location is granted.
5. Avoid opening settings/requesting background from map.

Expected outcome:

- Map consumes readiness assumptions but does not own normal permission flow.
- Background missing does not prevent basic/manual attendance UI from loading.

### 6. Refactor FaceScanner fallback

File:

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt`

Actions:

1. Remove automatic `cameraPermissionState.launchPermissionRequest()` from `LaunchedEffect(Unit)`.
2. Keep scanner initialization.
3. Keep rationale/denied UI as defensive fallback with explicit user action.
4. Ensure normal path after readiness opens camera content without prompt.

Expected outcome:

- Camera permission is requested from readiness, not FaceScanner, in the normal path.

### 7. Clarify LocationPermissionHelper and AttendanceScreen permission dialog behavior

Files:

- `app/src/main/java/com/example/infinite_track/utils/LocationPermissionHelper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`

Actions:

1. Split helper semantics so foreground required and background optional/degraded are not conflated.
2. Avoid using `checkAndRequestPermissions()` as a normal Attendance gate after readiness.
3. Keep `LocationPermissionDialog` only if it remains defensive and does not force background as a hard blocker.
4. Update user-facing copy to avoid saying all location/background access is required for basic attendance.

Expected outcome:

- Existing fallback does not fight the new readiness gate.

### 8. Clarify GeofenceManager readiness naming/copy

File:

- `app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceManager.kt`

Actions:

1. Preserve strict permission checks for actual geofence registration.
2. Clarify methods/copy so `hasAllRequiredPermissions()` is not used as basic attendance readiness.
3. Prefer names like `hasRequiredGeofencePermissions()` if changing call sites is safe.
4. Do not make background location required for continuing to Work Mode Selection.

Expected outcome:

- Background location remains required for geofence registration, but optional/degraded for basic attendance readiness.

### 9. Add/update tests where practical

Search first for existing tests around:

- startup notification policy
- readiness state derivation
- route policy/navigation helper if testable

Recommended tests:

1. Required count derives from foreground/camera/device-location only.
2. Notification denied does not block `canContinueToWorkMode`.
3. Background denied does not block `canContinueToWorkMode`.
4. Startup notification policy no longer allows Home prompt, or policy is removed and tests updated accordingly.

### 10. Verification

Run from isolated worktree:

```bash
./gradlew app:assembleDebug
```

Recommended if time/environment allows:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime/emulator smoke:

```text
Open app -> Home -> no notification permission prompt
Tap Attendance -> Permission Readiness screen
Grant foreground location from readiness
Grant camera from readiness
Turn device location ON via settings if needed
Skip/deny notification and background location
Continue to Work Mode Selection
Open scanner -> no normal camera prompt if readiness granted camera
```

If emulator/runtime capture cannot be produced, mark all required recordings/screenshots as `Needs Verification`.

## File Ownership Boundaries

Do not touch:

- Backend contract files.
- Attendance submission DTO/API logic unless compile forces an import-only change.
- Work Mode Selection layout beyond wiring route/gate.
- Face scanner visual design beyond removing auto permission request.
- Unrelated Home dashboard/service features.

High-risk files must be changed narrowly:

- `MainActivity.kt`
- `AttendanceMap.kt`
- `FaceScannerScreen.kt`
- `GeofenceManager.kt`
- `LocationPermissionHelper.kt`

## Rollback Strategy

If readiness route causes navigation issues:

1. Revert entry route changes to `Screen.Attendance.route`.
2. Keep new readiness files isolated if they compile independently.
3. Revert startup notification removal only if product explicitly wants Home prompt back.

If map/scanner fallback causes runtime issues:

1. Restore defensive UI first, but keep normal request ownership in readiness.
2. Do not restore startup/Home notification prompt unless directed.

## PR Notes Checklist

- Work done in isolated branch/worktree.
- INF-230 handled as permission ownership consolidation, not only UI.
- Attendance entry now goes through grouped readiness gate.
- Notification prompt removed from startup/Home.
- Foreground location, camera, and device location setting are hard blockers.
- Notification and background location are optional/degraded.
- AttendanceMap and FaceScanner no longer own normal permission request flow.
- Existing fallbacks remain defensive only.
- No backend/check-in/check-out contract changes.
- Build evidence included.
- Runtime evidence included or marked `Needs Verification`.
- Docs/ADR note included.
