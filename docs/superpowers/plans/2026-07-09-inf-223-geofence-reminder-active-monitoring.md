# INF-223 / INF-74 / INF-75 / INF-76 / INF-77 / INF-96 — Android Geofence Reminder vs Active Monitoring

## Scope

This implementation separates Android-owned local geofence registration into two runtime modes:

- Reminder geofences before check-in.
- Active monitoring geofence after backend-confirmed check-in.

Backend remains authoritative for attendance outcome, active attendance truth, booking approval, and final validation. Android only registers local geofences, shows local notifications when allowed, tracks local inside/outside state, and optionally syncs location events.

## Runtime contract

### Reminder mode

Condition:

```text
active_attendance_id == null
attendance_session_state.key == not_started
can_check_in == true
```

Android builds reminder candidates from:

1. `status-today.active_location` as the primary attendance target.
2. `/me` cached user home location for WFH when latitude/longitude exist.
3. Approved WFA booking from booking history when location coordinates exist and are not duplicate with the primary target.

Reminder notification is local-only and does not trigger backend check-in.

### Active monitoring mode

Condition:

```text
active_attendance_id != null
attendance_session_state.key == active
```

Only one active monitoring geofence is registered. Reminder geofences are detached from Play Services while preserving their persisted candidates for later restore.

### Completed / checkout mode

Checkout removes the active monitoring geofence, marks local session state completed, and restores persisted reminder geofences. The receiver suppresses reminder notifications when local state is completed.

## Lifecycle API split

New GeofenceManager intent APIs:

- `registerReminderGeofences(candidates)`
- `removeReminderGeofences()`
- `registerActiveMonitoringGeofence(location, activeAttendanceId)`
- `removeActiveMonitoringGeofence()`
- `restoreReminderGeofences()`
- `removeAllGeofencesForLogoutOnly()` / await variant

`removeAllGeofences()` remains only as a backward-compatible alias for full teardown semantics; normal mode switching should not use it.

## Duplicate and retry policy

- Geofence receiver gates active events by both active attendance ID and `attendance_session_state.key == active`.
- Active location event work uses unique work names per `activeAttendanceId + locationId + eventType` with `ExistingWorkPolicy.REPLACE`.
- Worker drops stale events older than six hours.
- Worker retries transient failures up to three attempts and treats common permanent HTTP client/auth failures as non-retryable.

## Permissions and notifications

- Geofence registration still requires foreground + background location permission.
- Attendance map rendering is foreground-only; background permission remains optional/degraded for geofencing.
- Notification channels are split into Attendance Reminder, Attendance Session Alert, and Attendance Evidence Sync.
- If `POST_NOTIFICATIONS` is denied, geofence registration is still allowed; local notification calls are skipped with logs.

## Verification status

- Static diff whitespace check: `git diff --check` passed.
- Debug build passed with the supported Windows/PowerShell Gradle environment:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command '$env:JAVA_HOME="D:\Java_Home\java 1.8.2"; $env:ANDROID_HOME="C:\Users\Febriyadi\AppData\Local\Android\Sdk"; $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME; $env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"; Set-Location "C:\Users\Febriyadi\.claude\worktrees\android-geofence-reminder-active-monitoring"; .\gradlew.bat --no-daemon app:assembleDebug'
```

Result: `BUILD SUCCESSFUL in 1m 52s`.

- Runtime/geofence/device evidence remains `Needs Verification` on emulator/device with Google Play Services.
