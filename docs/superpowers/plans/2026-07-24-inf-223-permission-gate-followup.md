# INF-223 Permission Gate Follow-up Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure Attendance opens the permission panel automatically only when one of the three required readiness checks is incomplete, while showing Work Mode directly when all three are ready.

**Architecture:** Keep `AttendancePermissionReadinessUiState.canContinue` as the sole authority for automatic permission-panel visibility. Remove the independent geofence-runtime visibility side effect and its obsolete presentation predicate while preserving runtime reconciliation, monitoring status, and manual panel access.

**Tech Stack:** Kotlin, Jetpack Compose, StateFlow, JUnit 4, Gradle, Hilt.

## Global Constraints

- The required readiness checks remain precise foreground location, camera permission, and device location services enabled.
- Notification permission and background location permission remain optional.
- Google Play Services and geofence registration health must not block the manual Work Mode flow.
- While required readiness is loading, do not open the permission panel.
- When required readiness finishes with `canContinue == false`, open the panel once for that Attendance screen entry.
- When `canContinue == true`, keep the permission panel closed and show Work Mode as the primary experience.
- Manual permission-panel access from the Attendance top bar must remain available.
- Do not change backend attendance truth, attendance submission, geofence reconciliation, or monitoring-state ownership.
- Do not modify the user's local `NetworkModule.kt` or `network_security_config.xml` changes in the main checkout.

---

## File Structure

- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
  - Remove the geofence-runtime `LaunchedEffect` that writes permission-panel visibility.
  - Preserve the existing readiness-driven initial gate and manual open callbacks.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiState.kt`
  - Remove the obsolete `requiresPermissionReadinessRecovery()` predicate after its UI consumer is removed.
- Modify `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionRouteVisibilityTest.kt`
  - Retain pure required-readiness visibility tests.
  - Add an architectural regression test that forbids geofence runtime from auto-opening the panel.
- Delete `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimePresentationDecisionTest.kt`
  - Remove tests for the obsolete geofence-owned visibility predicate.

### Task 1: Make Required Readiness the Sole Auto-open Authority

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt:137-173`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiState.kt:1-24`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionRouteVisibilityTest.kt`
- Delete: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimePresentationDecisionTest.kt`

**Interfaces:**
- Consumes: `AttendancePermissionReadinessUiState.isLoading: Boolean`
- Consumes: `AttendancePermissionReadinessUiState.canContinue: Boolean`
- Consumes: `shouldAutoOpenPermissionPanel(isLoading: Boolean, canContinue: Boolean, initialCheckHandled: Boolean): Boolean`
- Preserves: `AttendancePermissionReadinessEvent.ScreenResumed`
- Produces: a single automatic visibility path owned by `shouldAutoOpenPermissionPanel`

- [ ] **Step 1: Add the failing architectural regression test**

Append this test and source helper to
`AttendancePermissionRouteVisibilityTest.kt`:

```kotlin
// Add these imports beside the existing JUnit imports.
import java.io.File
import org.junit.Assert.assertFalse

// Add these members inside AttendancePermissionPanelVisibilityTest.
private val attendanceScreenSource = File(
    requireNotNull(System.getProperty("user.dir")),
    "src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt"
).readText()

@Test
fun geofenceRuntimeCannotAutoOpenPermissionPanel() {
    assertFalse(
        attendanceScreenSource.contains("LaunchedEffect(uiState.geofenceRuntime)")
    )
    assertFalse(
        attendanceScreenSource.contains("requiresPermissionReadinessRecovery()")
    )
}
```

Keep the existing tests proving that loading stays closed, ready required access
stays closed, and missing required access opens only once.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process `
  app:testDebugUnitTest `
  --tests "*AttendancePermissionPanelVisibilityTest"
```

Expected: FAIL in `geofenceRuntimeCannotAutoOpenPermissionPanel` because the
merged `AttendanceScreen.kt` contains both forbidden geofence-driven visibility
expressions.

- [ ] **Step 3: Remove the independent geofence visibility side effect**

Delete only this block from `AttendanceScreen.kt`:

```kotlin
LaunchedEffect(uiState.geofenceRuntime) {
    if (uiState.geofenceRuntime.requiresPermissionReadinessRecovery()) {
        showPermissionPanel = true
    }
}
```

Do not change:

```kotlin
LaunchedEffect(
    permissionUiState.isLoading,
    permissionUiState.canContinue,
    initialPermissionCheckHandled
) {
    if (
        shouldAutoOpenPermissionPanel(
            isLoading = permissionUiState.isLoading,
            canContinue = permissionUiState.canContinue,
            initialCheckHandled = initialPermissionCheckHandled
        )
    ) {
        initialPermissionCheckHandled = true
        showPermissionPanel = true
    } else if (!permissionUiState.isLoading && !initialPermissionCheckHandled) {
        initialPermissionCheckHandled = true
    }
}
```

Also preserve the two manual callbacks that send
`AttendancePermissionReadinessEvent.ScreenResumed` and then assign
`showPermissionPanel = true`.

- [ ] **Step 4: Remove the obsolete geofence visibility predicate and test**

Delete this function from `GeofenceRuntimeUiState.kt`:

```kotlin
internal fun GeofenceRuntimeUiState.requiresPermissionReadinessRecovery(): Boolean =
    !monitoringAvailable && reason in setOf(
        GeofenceRuntimeUiReason.PRECISE_LOCATION_REQUIRED,
        GeofenceRuntimeUiReason.BACKGROUND_LOCATION_REQUIRED,
        GeofenceRuntimeUiReason.DEVICE_LOCATION_DISABLED,
        GeofenceRuntimeUiReason.PLAY_SERVICES_UNAVAILABLE
    )
```

Delete `GeofenceRuntimePresentationDecisionTest.kt`; its assertions encode the
rejected behavior where geofence runtime owns permission-panel visibility.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process `
  app:testDebugUnitTest `
  --tests "*AttendancePermissionPanelVisibilityTest"
```

Expected: PASS. Confirm specifically:

- loading readiness does not open the panel;
- `canContinue == true` does not open the panel;
- `canContinue == false` opens the panel once; and
- `AttendanceScreen.kt` has no geofence-runtime auto-open path.

- [ ] **Step 6: Search for stale visibility consumers**

Run:

```powershell
rg -n "requiresPermissionReadinessRecovery|LaunchedEffect\\(uiState\\.geofenceRuntime\\)" `
  app/src/main app/src/test
```

Expected: no matches.

Then confirm manual access remains:

```powershell
rg -n -C 3 "ScreenResumed|showPermissionPanel = true" `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt
```

Expected: the readiness-driven initial gate and user-initiated permission
callbacks remain.

- [ ] **Step 7: Run the complete Android verification gate**

Use JBR 17 and the process-local Kotlin compiler:

```powershell
$env:JAVA_HOME="$env:USERPROFILE\.jdks\jbr-17.0.14"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process `
  app:testDebugUnitTest `
  app:compileDebugAndroidTestKotlin `
  app:lintDebug `
  app:assembleDebug
```

Expected:

- Gradle exits with code `0`;
- unit test XML reports `0` failures and `0` errors;
- Android test Kotlin compilation passes;
- lint reports `0` fatal and `0` error findings; and
- `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 8: Perform device verification when prerequisites exist**

On an emulator or physical device with a disposable authenticated account:

1. Grant precise location and camera, then enable device location.
2. Deny notification and background location.
3. Enter Attendance.
4. Confirm the permission panel does not appear and Work Mode is the primary
   view.
5. Revoke one required access, re-enter Attendance, and confirm the permission
   panel appears first.
6. Restore the required access and confirm the panel can still be opened
   manually from the top bar.

If an authenticated disposable account is unavailable, record these scenarios
as `Needs Verification`; do not infer runtime success from compilation.

- [ ] **Step 9: Commit the implementation**

```powershell
git add -- `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt `
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiState.kt `
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionRouteVisibilityTest.kt `
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimePresentationDecisionTest.kt
git commit -m "fix(attendance): gate permission panel on required access"
```

Expected: one focused implementation commit with no unrelated files.
