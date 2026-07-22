# INF-230 Permission Readiness Architecture and Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved four-phase INF-230 design so Attendance permission readiness has typed Clean Architecture ownership, lifecycle-safe state/effects, a stateless accessible Compose UI, and a reusable Infinite glass feedback family with a designed auto-dismissing snackbar.

**Architecture:** Preserve the stable `attendance_permission_readiness` route while moving truth through `Android data source -> RepositoryImpl -> Repository -> UseCases -> ViewModel -> Route -> Screen`. The Route owns Activity-bound permission launchers, rationale checks, Settings intents, snackbar delivery, and navigation; the Screen owns only rendering and typed user events. Required access is precise location, camera, and enabled device location; notification and background location remain optional capabilities. Reusable status visuals share semantic glass tokens, while each component retains the correct interaction model.

**Tech Stack:** Kotlin 1.9, coroutines/Flow, Hilt, Jetpack Compose UI 1.7.4, Material 3 1.2.1, lifecycle-compose, JUnit 4, kotlinx-coroutines-test, Compose UI instrumentation tests, Android API 26-34.

## Global Constraints

- Work only in `E:\skrisi\android\.worktrees\inf-230-permission-readiness-refinement` on `codex/inf-230-permission-readiness-refinement`.
- Do not touch the user-owned changes in the main checkout, especially `NetworkModule.kt` and `network_security_config.xml`.
- Treat the approved spec at `docs/superpowers/specs/2026-07-22-inf-230-permission-readiness-architecture-and-feedback-design.md` as the product contract. This plan supersedes the narrower 2026-07-09 INF-230 plan.
- Keep `Screen.AttendancePermissionReadiness.route` and its bottom-bar visibility contract unchanged. INF-209 navigation modernization is out of scope.
- Keep backend attendance results authoritative. Readiness decides whether the client may enter the capture flow; it does not invent a check-in/check-out result.
- Required readiness is exactly precise location, camera, and enabled device location. Coarse-only location is explicitly insufficient. Notification and background location never block manual attendance.
- Permission callback booleans are signals only. Refresh the repository after every callback and Settings return before treating OS state as truth.
- Keep `Context`, `Activity`, `NavController`, `ActivityResultLauncher`, Android permission strings, Settings `Intent`, `SnackbarHostState`, `ImageVector`, and Material `SnackbarDuration` out of domain and ViewModel state.
- Keep legacy status component signatures source-compatible. Reusable visuals may change globally; semantic migration is limited to Permission Readiness and directly affected Attendance outcomes.
- Do not modify generic `InfiniteSurface`; a change there would restyle unrelated report/card surfaces.
- Glass correctness must not depend on blur, `RenderEffect`, backdrop filtering, API 31, Compose 1.9 shadow APIs, or Material Expressive motion APIs. Use gradients, alpha, a 1 dp highlight border, top highlight, semantic rail/halo, and ordinary elevation.
- Use only registered SF Compact Medium and Bold weights for feedback hierarchy. Do not request synthetic SemiBold. Do not claim dark-theme coverage because the current application theme is light-only.
- Preserve explicit defensive recovery after runtime revocation. "Normal ownership" means first-time setup and ordinary entry sequencing; those requests originate only from Permission Readiness. A Face Scanner request made only after camera was revoked/direct-entry drift, and only after an explicit user recovery action, is an exceptional defensive path rather than a second entry owner. Remove duplicate normal request ownership only after the Phase 4 runtime gate passes.
- No new dependency is required. Use manual fakes, `Flow.first()`, and Compose instrumentation APIs already present in `app/build.gradle.kts`.
- This is attendance/location/navigation-sensitive work. Compile and unit tests are necessary but not sufficient; missing emulator/device evidence means `Needs Verification`.
- The focused spec plus this plan satisfy the architecture documentation trigger. Add a separate ADR only if implementation changes route identity, backend attendance semantics, or a cross-feature permission contract beyond this approved scope.

## Working Environment

Run this once in each implementation session:

```powershell
$env:JAVA_HOME='C:\Users\Febriyadi\.jdks\jbr-17.0.14'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
Set-Location 'E:\skrisi\android\.worktrees\inf-230-permission-readiness-refinement'
```

Expected Java header: `openjdk version "17.0.14"`.

## Target File Structure

```text
app/src/main/java/com/example/infinite_track/
├── data/
│   ├── mapper/attendance/AttendancePermissionMapper.kt
│   ├── repository/attendance/AttendancePermissionRepositoryImpl.kt
│   └── soucre/local/permission/
│       ├── AttendancePermissionDataSource.kt
│       ├── AttendancePermissionPlatformSnapshot.kt
│       └── AndroidAttendancePermissionDataSource.kt
├── di/RepositoryModule.kt
├── domain/
│   ├── model/attendance/permission/
│   │   ├── AttendancePermissionAction.kt
│   │   └── AttendancePermissionReadiness.kt
│   ├── repository/AttendancePermissionRepository.kt
│   └── use_case/attendance/permission/
│       ├── ObserveAttendancePermissionReadinessUseCase.kt
│       ├── RefreshAttendancePermissionReadinessUseCase.kt
│       └── ResolveNextAttendancePermissionActionUseCase.kt
└── presentation/
    ├── design/
    │   ├── components/state/InfiniteStateComponents.kt
    │   ├── components/status/
    │   │   ├── InfiniteFeedback.kt
    │   │   ├── InfiniteFeedbackGlassSurface.kt
    │   │   ├── InfiniteSnackbar.kt
    │   │   └── InfiniteStatusPill.kt
    │   └── tokens/InfiniteFeedbackTokens.kt
    └── screen/attendance/permission/
        ├── AttendancePermissionReadinessContract.kt
        ├── AttendancePermissionReadinessRoute.kt
        ├── AttendancePermissionReadinessScreen.kt
        ├── AttendancePermissionReadinessUiMapper.kt
        ├── AttendancePermissionReadinessUiState.kt
        └── AttendancePermissionReadinessViewModel.kt
```

The existing misspelled `data/soucre` package is repository reality. Keep the new Android data source there; do not start a repository-wide rename in INF-230.

## Rollback Boundaries and Related Contracts

- Keep every phase in its own reviewable commits. If Phase 1 wiring fails, the existing screen remains usable while the new domain/data stack is corrected.
- If Phase 2 effect coordination fails, do not wire the ViewModel into navigation; correct state/effect tests first.
- If Phase 3 Route integration fails, restore the existing destination call while keeping the tested domain/ViewModel stack. If reusable visuals regress a legacy call site, keep the shared tokens and temporarily point that compatibility wrapper to its prior surface.
- Phase 4 helper deletion is conditional. A failed runtime gate means retaining the helper adapters, recording `Needs Verification`, and fixing the new owner before any deletion.
- INF-222 remains the broader component contract, INF-209 the future navigation contract, INF-223 the geofence semantics contract, INF-219 the wider status-component migration, and GitHub #71 the visual baseline. INF-230 consumes those boundaries without completing their unrelated backlog.

---

## Phase 1 — Architecture Extraction

### Task 1: Define the readiness aggregate and invariants

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/permission/AttendancePermissionReadiness.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/model/attendance/permission/AttendancePermissionReadinessTest.kt`

**Interfaces:**

- Consumes: no Android or presentation types.
- Produces: typed access, requirement, status, reason, recovery, request outcome, scope-aware inspection issues, aggregate counts, fail-closed required eligibility, and non-blocking optional degradation.

- [ ] **Step 1: Write the failing aggregate tests.**

Cover these cases in `AttendancePermissionReadinessTest`: all three required entries ready; coarse-only precise entry blocked with `APPROXIMATE_ONLY`; camera missing; device location disabled; optional entries degraded; optional entries unsupported; missing required entry; required inspection issue over a previously ready snapshot; optional-only inspection issue remaining non-blocking; denied and permanently-denied request evidence.

Use this helper shape so every test explicitly provides all five access entries:

```kotlin
private fun readiness(
    precise: AttendanceAccessStatus = AttendanceAccessStatus.READY,
    camera: AttendanceAccessStatus = AttendanceAccessStatus.READY,
    deviceLocation: AttendanceAccessStatus = AttendanceAccessStatus.READY,
    notification: AttendanceAccessStatus = AttendanceAccessStatus.READY,
    background: AttendanceAccessStatus = AttendanceAccessStatus.READY,
    inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
) = AttendancePermissionReadiness(
    entries = listOf(
        AttendanceAccessReadiness(AttendanceAccess.PRECISE_LOCATION, precise),
        AttendanceAccessReadiness(AttendanceAccess.CAMERA, camera),
        AttendanceAccessReadiness(AttendanceAccess.DEVICE_LOCATION, deviceLocation),
        AttendanceAccessReadiness(AttendanceAccess.NOTIFICATION, notification),
        AttendanceAccessReadiness(AttendanceAccess.BACKGROUND_LOCATION, background)
    ),
    inspectionIssues = inspectionIssues
)
```

- [ ] **Step 2: Run the test and confirm it fails because the domain types do not exist.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermissionReadinessTest'
```

Expected result: compilation failure naming the missing `domain.model.attendance.permission` types.

- [ ] **Step 3: Implement the minimum domain model.**

Use this vocabulary and keep all values Android-free:

```kotlin
enum class AttendanceAccessRequirement { REQUIRED, OPTIONAL }

enum class AttendanceAccess(val requirement: AttendanceAccessRequirement) {
    PRECISE_LOCATION(AttendanceAccessRequirement.REQUIRED),
    CAMERA(AttendanceAccessRequirement.REQUIRED),
    DEVICE_LOCATION(AttendanceAccessRequirement.REQUIRED),
    NOTIFICATION(AttendanceAccessRequirement.OPTIONAL),
    BACKGROUND_LOCATION(AttendanceAccessRequirement.OPTIONAL)
}

enum class AttendanceAccessStatus {
    READY,
    ACTION_REQUIRED,
    DENIED,
    PERMANENTLY_DENIED,
    DEGRADED,
    NOT_REQUIRED_ON_DEVICE,
    DEVICE_LOCATION_DISABLED
}

enum class AttendanceAccessReason { NONE, APPROXIMATE_LOCATION_ONLY }

enum class AttendanceAccessRecovery {
    NONE,
    REQUEST_PERMISSION,
    OPEN_APPLICATION_SETTINGS,
    OPEN_DEVICE_LOCATION_SETTINGS
}

enum class AttendancePermissionRequestOutcome {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED
}

enum class AttendancePermissionFailure {
    PLATFORM_STATE_UNAVAILABLE,
    DEVICE_LOCATION_STATUS_UNAVAILABLE,
    SETTINGS_INTENT_UNAVAILABLE,
    UNKNOWN
}

enum class AttendanceOptionalCapabilitySummary { READY, DEGRADED, NOT_REQUIRED }

data class AttendancePermissionInspectionIssue(
    val failure: AttendancePermissionFailure,
    val affectedAccesses: Set<AttendanceAccess>
) {
    val blocksManualAttendance: Boolean
        get() = affectedAccesses.any {
            it.requirement == AttendanceAccessRequirement.REQUIRED
        }
}

data class AttendanceAccessReadiness(
    val access: AttendanceAccess,
    val status: AttendanceAccessStatus,
    val reason: AttendanceAccessReason = AttendanceAccessReason.NONE,
    val recovery: AttendanceAccessRecovery = AttendanceAccessRecovery.NONE
)
```

Implement `AttendancePermissionReadiness` with these exact invariants:

- `requiredTotalCount` is derived from the three required enum values, not list size.
- `requiredReadyCount` counts required entries whose status is `READY`.
- `canEnterAttendance` is true only when every required enum value is present and `READY`, and no inspection issue affects a required access.
- Optional statuses never affect those three properties.
- `optionalCapabilitySummary` is `NOT_REQUIRED` when every optional entry is `NOT_REQUIRED_ON_DEVICE`, `DEGRADED` when an optional access has an inspection issue or any optional entry is not ready/not-required, and `READY` otherwise.
- `statusOf(access)` returns `ACTION_REQUIRED` when an entry is missing, so malformed data fails closed.
- `applyingRequestOutcomes(outcomes)` never turns a callback `GRANTED` into truth; it only overlays `DENIED` or `PERMANENTLY_DENIED` on a base entry that is not already `READY`/`NOT_REQUIRED_ON_DEVICE`.
- A permanent-denial overlay changes recovery to `OPEN_APPLICATION_SETTINGS`.
- `unavailable(failure)` creates all required entries as `ACTION_REQUIRED`, optional entries as `DEGRADED`, and carries the typed failure.

Use computed properties rather than storing duplicate booleans. The aggregate body should follow this complete shape:

```kotlin
data class AttendancePermissionReadiness(
    val entries: List<AttendanceAccessReadiness>,
    val inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
) {
    private val requiredAccesses: List<AttendanceAccess>
        get() = AttendanceAccess.entries.filter {
            it.requirement == AttendanceAccessRequirement.REQUIRED
        }

    val requiredReadyCount: Int
        get() = requiredAccesses.count { statusOf(it) == AttendanceAccessStatus.READY }

    val requiredTotalCount: Int
        get() = requiredAccesses.size

    val canEnterAttendance: Boolean
        get() = inspectionIssues.none { it.blocksManualAttendance } &&
            requiredAccesses.all { access ->
            entryOf(access)?.status == AttendanceAccessStatus.READY
        }

    val optionalCapabilitySummary: AttendanceOptionalCapabilitySummary
        get() {
            val optional = entries.filter {
                it.access.requirement == AttendanceAccessRequirement.OPTIONAL
            }
            return when {
                inspectionIssues.any { issue ->
                    issue.affectedAccesses.any {
                        it.requirement == AttendanceAccessRequirement.OPTIONAL
                    }
                } -> AttendanceOptionalCapabilitySummary.DEGRADED
                optional.isNotEmpty() && optional.all {
                    it.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
                } -> AttendanceOptionalCapabilitySummary.NOT_REQUIRED
                optional.any {
                    it.status != AttendanceAccessStatus.READY &&
                        it.status != AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
                } -> AttendanceOptionalCapabilitySummary.DEGRADED
                else -> AttendanceOptionalCapabilitySummary.READY
            }
        }

    fun entryOf(access: AttendanceAccess): AttendanceAccessReadiness? =
        entries.firstOrNull { it.access == access }

    fun statusOf(access: AttendanceAccess): AttendanceAccessStatus =
        entryOf(access)?.status ?: AttendanceAccessStatus.ACTION_REQUIRED

    fun applyingRequestOutcomes(
        outcomes: Map<AttendanceAccess, AttendancePermissionRequestOutcome>
    ): AttendancePermissionReadiness = copy(
        entries = entries.map { entry ->
            if (
                entry.status == AttendanceAccessStatus.READY ||
                entry.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
            ) {
                entry
            } else {
                when (outcomes[entry.access]) {
                    AttendancePermissionRequestOutcome.DENIED ->
                        entry.copy(status = AttendanceAccessStatus.DENIED)
                    AttendancePermissionRequestOutcome.PERMANENTLY_DENIED -> entry.copy(
                        status = AttendanceAccessStatus.PERMANENTLY_DENIED,
                        recovery = AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS
                    )
                    AttendancePermissionRequestOutcome.GRANTED,
                    null -> entry
                }
            }
        }
    )

    fun applyingInspectionIssues(
        issues: List<AttendancePermissionInspectionIssue>
    ): AttendancePermissionReadiness = copy(
        entries = entries.map { entry ->
            if (issues.any { entry.access in it.affectedAccesses }) {
                entry.copy(
                    status = if (
                        entry.access.requirement == AttendanceAccessRequirement.REQUIRED
                    ) {
                        AttendanceAccessStatus.ACTION_REQUIRED
                    } else {
                        AttendanceAccessStatus.DEGRADED
                    },
                    recovery = AttendanceAccessRecovery.NONE
                )
            } else {
                entry
            }
        },
        inspectionIssues = issues
    )

    companion object {
        fun unavailable(
            failure: AttendancePermissionFailure,
            affectedAccesses: Set<AttendanceAccess> = AttendanceAccess.entries
                .filter { it.requirement == AttendanceAccessRequirement.REQUIRED }
                .toSet()
        ) =
            AttendancePermissionReadiness(
                entries = AttendanceAccess.entries.map { access ->
                    AttendanceAccessReadiness(
                        access = access,
                        status = if (
                            access.requirement == AttendanceAccessRequirement.REQUIRED
                        ) {
                            AttendanceAccessStatus.ACTION_REQUIRED
                        } else {
                            AttendanceAccessStatus.DEGRADED
                        },
                        recovery = AttendanceAccessRecovery.NONE
                    )
                },
                inspectionIssues = listOf(
                    AttendancePermissionInspectionIssue(
                        failure = failure,
                        affectedAccesses = affectedAccesses
                    )
                )
            )
    }
}
```

- [ ] **Step 4: Run the focused test and confirm all invariants pass.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermissionReadinessTest'
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the domain aggregate.**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/model/attendance/permission/AttendancePermissionReadiness.kt app/src/test/java/com/example/infinite_track/domain/model/attendance/permission/AttendancePermissionReadinessTest.kt
git commit -m "feat: model attendance permission readiness"
```

### Task 2: Add next-action policy, repository contract, and use cases

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/permission/AttendancePermissionAction.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/repository/AttendancePermissionRepository.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/permission/ObserveAttendancePermissionReadinessUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/permission/RefreshAttendancePermissionReadinessUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/permission/ResolveNextAttendancePermissionActionUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/permission/ResolveNextAttendancePermissionActionUseCaseTest.kt`

**Interfaces:**

- Consumes: `AttendancePermissionReadiness` from Task 1.
- Produces: stable observation/refresh repository contract and primary/item action resolution.

- [ ] **Step 1: Write the failing resolver tests.**

Assert the exact primary order `PRECISE_LOCATION -> CAMERA -> DEVICE_LOCATION -> CONTINUE`, required inspection issue -> `RetryRefresh`, optional-only inspection issue -> normal required action/Continue, permanent denial -> application Settings, GPS disabled -> device Settings, API-unsupported optional item -> `None`, and optional degradation never enters the primary sequence.

- [ ] **Step 2: Run the resolver test and confirm it fails on missing classes.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveNextAttendancePermissionActionUseCaseTest'
```

Expected result: compilation failure for `ResolveNextAttendancePermissionActionUseCase`.

- [ ] **Step 3: Implement the action and repository contracts.**

```kotlin
sealed interface AttendancePermissionNextAction {
    data class RequestPermission(val access: AttendanceAccess) : AttendancePermissionNextAction
    data class OpenApplicationSettings(val access: AttendanceAccess) : AttendancePermissionNextAction
    data object OpenDeviceLocationSettings : AttendancePermissionNextAction
    data object ContinueToWorkMode : AttendancePermissionNextAction
    data object RetryRefresh : AttendancePermissionNextAction
    data object None : AttendancePermissionNextAction
}

interface AttendancePermissionRepository {
    fun observeReadiness(): Flow<AttendancePermissionReadiness>
    suspend fun refreshReadiness()
}
```

Each use case uses `@Inject constructor`; do not add it to `UseCaseModule`.

- [ ] **Step 4: Implement primary and per-item resolution.**

Expose these two methods:

```kotlin
class ResolveNextAttendancePermissionActionUseCase @Inject constructor() {
    private val requiredOrder = listOf(
        AttendanceAccess.PRECISE_LOCATION,
        AttendanceAccess.CAMERA,
        AttendanceAccess.DEVICE_LOCATION
    )

    fun forPrimary(
        readiness: AttendancePermissionReadiness
    ): AttendancePermissionNextAction {
        if (readiness.inspectionIssues.any { it.blocksManualAttendance }) {
            return AttendancePermissionNextAction.RetryRefresh
        }
        val nextAccess = requiredOrder.firstOrNull {
            readiness.statusOf(it) != AttendanceAccessStatus.READY
        } ?: return AttendancePermissionNextAction.ContinueToWorkMode
        return forAccess(readiness, nextAccess)
    }

    fun forAccess(
        readiness: AttendancePermissionReadiness,
        access: AttendanceAccess
    ): AttendancePermissionNextAction {
        val entry = readiness.entryOf(access)
            ?: return if (access.requirement == AttendanceAccessRequirement.REQUIRED) {
                AttendancePermissionNextAction.RetryRefresh
            } else {
                AttendancePermissionNextAction.None
            }
        if (
            entry.status == AttendanceAccessStatus.READY ||
            entry.status == AttendanceAccessStatus.NOT_REQUIRED_ON_DEVICE
        ) {
            return AttendancePermissionNextAction.None
        }
        return when (entry.recovery) {
            AttendanceAccessRecovery.REQUEST_PERMISSION ->
                AttendancePermissionNextAction.RequestPermission(access)
            AttendanceAccessRecovery.OPEN_APPLICATION_SETTINGS ->
                AttendancePermissionNextAction.OpenApplicationSettings(access)
            AttendanceAccessRecovery.OPEN_DEVICE_LOCATION_SETTINGS ->
                AttendancePermissionNextAction.OpenDeviceLocationSettings
            AttendanceAccessRecovery.NONE ->
                AttendancePermissionNextAction.RetryRefresh
        }
    }
}
```

`forPrimary` first checks for an inspection issue affecting required access, then iterates only the required enum values in the approved order. `forAccess` resolves from `AttendanceAccessRecovery`: request, app Settings, device Settings, or no action. A ready required set returns `ContinueToWorkMode` even when optional entries are degraded or have an optional-only inspection issue.

- [ ] **Step 5: Run the focused test and all domain permission tests.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermission*'
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the domain policy.**

```powershell
git add app/src/main/java/com/example/infinite_track/domain app/src/test/java/com/example/infinite_track/domain
git commit -m "feat: resolve attendance permission actions"
```

### Task 3: Read Android permission state, map SDK capabilities, publish repository state, and wire Hilt

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/data/soucre/local/permission/AttendancePermissionPlatformSnapshot.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/soucre/local/permission/AttendancePermissionDataSource.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/soucre/local/permission/AndroidAttendancePermissionDataSource.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/mapper/attendance/AttendancePermissionMapper.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/repository/attendance/AttendancePermissionRepositoryImpl.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/mapper/attendance/AttendancePermissionMapperTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/repository/attendance/AttendancePermissionRepositoryImplTest.kt`

**Interfaces:**

- Consumes: Android application context and the Task 1/2 domain contracts.
- Produces: fresh typed snapshots, pure SDK mapping, serialized refresh, last-trustworthy-content retention, and Hilt bindings.

- [ ] **Step 1: Write failing pure mapper tests.**

Cover SDK 28/29/30/33/34, fine versus coarse, camera, notification support, background support/recovery, GPS enabled/disabled/unavailable, and optional non-blocking behavior. The snapshot vocabulary is:

```kotlin
enum class DeviceLocationPlatformStatus { ENABLED, DISABLED, UNAVAILABLE }

data class AttendancePermissionPlatformSnapshot(
    val sdkInt: Int,
    val fineLocationGranted: Boolean,
    val coarseLocationGranted: Boolean,
    val cameraGranted: Boolean,
    val notificationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val deviceLocationStatus: DeviceLocationPlatformStatus,
    val inspectionIssues: List<AttendancePermissionInspectionIssue> = emptyList()
)
```

- [ ] **Step 2: Write failing repository tests with a manual fake data source.**

Use a sealed source result so exceptions never cross the boundary:

```kotlin
sealed interface AttendancePermissionSnapshotResult {
    data class Success(
        val snapshot: AttendancePermissionPlatformSnapshot
    ) : AttendancePermissionSnapshotResult

    data class Failure(
        val failure: AttendancePermissionFailure,
        val affectedAccesses: Set<AttendanceAccess>
    ) : AttendancePermissionSnapshotResult
}

interface AttendancePermissionDataSource {
    fun readSnapshot(): AttendancePermissionSnapshotResult
}
```

Assert refresh publication, concurrent refresh serialization, typed required failure without prior content, required failure retaining prior entries while blocking entry, optional-only failure retaining prior entries without blocking entry, and a later successful refresh clearing prior issues.

- [ ] **Step 3: Run the tests and confirm missing implementations fail.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermissionMapperTest' --tests '*AttendancePermissionRepositoryImplTest'
```

Expected result: compilation failure naming mapper/repository/data-source types.

- [ ] **Step 4: Implement the pure mapper.**

Map exact platform rules. Apply `snapshot.inspectionIssues` after ordinary grant mapping so every affected required entry becomes `ACTION_REQUIRED` with no native recovery, every affected optional entry becomes `DEGRADED`, and unaffected entries remain available:

- Fine granted -> precise `READY`; coarse-only -> precise `ACTION_REQUIRED` with `APPROXIMATE_LOCATION_ONLY` and `REQUEST_PERMISSION`; neither -> `ACTION_REQUIRED` and `REQUEST_PERMISSION`.
- Camera granted -> `READY`; otherwise `ACTION_REQUIRED` and `REQUEST_PERMISSION`.
- Device location enabled -> `READY`; disabled -> `DEVICE_LOCATION_DISABLED` and `OPEN_DEVICE_LOCATION_SETTINGS`; unavailable -> fail-closed device-location entry plus an issue scoped to `DEVICE_LOCATION` with `DEVICE_LOCATION_STATUS_UNAVAILABLE`.
- SDK below 33 -> notification `NOT_REQUIRED_ON_DEVICE`; SDK 33+ granted -> `READY`; otherwise `DEGRADED` and `REQUEST_PERMISSION`.
- SDK below 29 -> background `NOT_REQUIRED_ON_DEVICE`; SDK 29 missing -> `DEGRADED` and `REQUEST_PERMISSION`; SDK 30+ missing -> `DEGRADED` and `OPEN_APPLICATION_SETTINGS`.

- [ ] **Step 5: Implement the Android data source.**

Read fine and coarse independently with `ContextCompat.checkSelfPermission`, camera, API-gated notification/background permissions, and `LocationManagerCompat.isLocationEnabled`. Isolate per-access reads: a notification/background inspection problem produces an issue scoped only to that optional access and a degraded entry, while a fine/camera/device-location problem is scoped to that required access and fails closed. Reserve top-level `Failure` for a snapshot that cannot be constructed, scope it to all required accesses, map `SecurityException` to `PLATFORM_STATE_UNAVAILABLE`, and map other non-cancellation failures to `UNKNOWN`. Never log permission-state exceptions with user data.

- [ ] **Step 6: Implement the repository with a `MutableStateFlow` and `Mutex`.**

```kotlin
@Singleton
class AttendancePermissionRepositoryImpl @Inject constructor(
    private val dataSource: AttendancePermissionDataSource
) : AttendancePermissionRepository {
    private val state = MutableStateFlow<AttendancePermissionReadiness?>(null)
    private val refreshMutex = Mutex()

    override fun observeReadiness(): Flow<AttendancePermissionReadiness> =
        state.filterNotNull()

    override suspend fun refreshReadiness() = refreshMutex.withLock {
        when (val result = dataSource.readSnapshot()) {
            is AttendancePermissionSnapshotResult.Success ->
                state.value = result.snapshot.toAttendancePermissionReadiness()
            is AttendancePermissionSnapshotResult.Failure -> {
                val issue = AttendancePermissionInspectionIssue(
                    failure = result.failure,
                    affectedAccesses = result.affectedAccesses
                )
                state.value = state.value
                    ?.applyingInspectionIssues(listOf(issue))
                    ?: AttendancePermissionReadiness.unavailable(
                        failure = result.failure,
                        affectedAccesses = result.affectedAccesses
                    )
            }
        }
    }
}
```

- [ ] **Step 7: Add only the required Hilt providers.**

Add `@ApplicationContext` data-source and repository providers to `RepositoryModule`. Do not alter `UseCaseModule` and do not add dependencies.

- [ ] **Step 8: Run Phase 1 verification.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermission*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected result: both commands end with `BUILD SUCCESSFUL`; the current route still behaves as before because the new stack is not wired to UI yet.

- [ ] **Step 9: Commit Phase 1.**

```powershell
git add app/src/main/java/com/example/infinite_track/data app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt app/src/test/java/com/example/infinite_track/data
git commit -m "feat: publish Android permission readiness"
```

---

## Phase 2 — ViewModel State and Effects

### Task 4: Define immutable presentation models and pure UI mapping

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessContract.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessUiState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessUiMapper.kt`
- Replace: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessUiStateTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessUiMapperTest.kt`

**Interfaces:**

- Consumes: domain readiness and next action.
- Produces: Compose-free UI models, typed events/effects, Indonesian copy, semantic keys, and a temporary compatibility projection for the old Screen until Task 8.

- [ ] **Step 1: Write failing mapper tests.**

Assert default loading, partial progress, all-required-ready with optional degradation, approximate-only copy, denied copy, permanently-denied `Buka pengaturan`, unsupported optional copy, GPS disabled, required inspection issue with Retry, optional-only issue as non-blocking contextual guidance, and icon keys. Assert the mapper never includes optional items in `requiredTotalCount`.

- [ ] **Step 2: Define the contract without Compose runtime objects.**

Include:

```kotlin
enum class PermissionIconKey { LOCATION, CAMERA, DEVICE_LOCATION, NOTIFICATION, BACKGROUND_LOCATION }
enum class AttendanceFeedbackDuration { SHORT, LONG }
enum class AttendanceSettingsDestination { APPLICATION, DEVICE_LOCATION }
enum class AttendancePermissionFeedbackAction { RETRY_REFRESH, OPEN_APPLICATION_SETTINGS }
enum class PermissionGuidanceAction { RETRY_REFRESH }

data class PermissionItemUiModel(
    val access: AttendanceAccess,
    val title: String,
    val supportingText: String,
    val requirementLabel: String,
    val statusLabel: String,
    val actionLabel: String?,
    val iconKey: PermissionIconKey,
    val semantic: InfiniteSemantic,
    val stateDescription: String,
    val isReady: Boolean
)

data class AttendancePermissionFeedback(
    val id: String,
    val message: String,
    val semantic: InfiniteSemantic,
    val duration: AttendanceFeedbackDuration,
    val action: AttendancePermissionFeedbackAction? = null,
    val actionLabel: String? = null
)

data class PermissionGuidanceUiModel(
    val title: String,
    val message: String,
    val semantic: InfiniteSemantic,
    val actionLabel: String? = null,
    val action: PermissionGuidanceAction? = null
)
```

Define the approved minimum events and effects plus lifecycle completion events needed for idempotency:

```kotlin
sealed interface AttendancePermissionReadinessEvent {
    data object ScreenResumed : AttendancePermissionReadinessEvent
    data object PrimaryActionClicked : AttendancePermissionReadinessEvent
    data class PermissionItemClicked(val access: AttendanceAccess) : AttendancePermissionReadinessEvent
    data class PermissionResultReceived(
        val access: AttendanceAccess,
        val outcome: AttendancePermissionRequestOutcome
    ) : AttendancePermissionReadinessEvent
    data object ReturnedFromSettings : AttendancePermissionReadinessEvent
    data object RetryRefresh : AttendancePermissionReadinessEvent
    data class SettingsLaunchFailed(
        val destination: AttendanceSettingsDestination
    ) : AttendancePermissionReadinessEvent
    data class SnackbarFinished(val feedbackId: String) : AttendancePermissionReadinessEvent
    data class SnackbarActionClicked(
        val feedbackId: String,
        val action: AttendancePermissionFeedbackAction
    ) : AttendancePermissionReadinessEvent
    data object NavigationHandled : AttendancePermissionReadinessEvent
}

sealed interface AttendancePermissionReadinessEffect {
    data object RequestPreciseLocation : AttendancePermissionReadinessEffect
    data object RequestCamera : AttendancePermissionReadinessEffect
    data object RequestNotification : AttendancePermissionReadinessEffect
    data object RequestBackgroundLocation : AttendancePermissionReadinessEffect
    data class OpenApplicationSettings(
        val access: AttendanceAccess
    ) : AttendancePermissionReadinessEffect
    data object OpenDeviceLocationSettings : AttendancePermissionReadinessEffect
    data object NavigateToWorkMode : AttendancePermissionReadinessEffect
    data class ShowSnackbar(
        val feedback: AttendancePermissionFeedback
    ) : AttendancePermissionReadinessEffect
}
```

- [ ] **Step 3: Replace primitive UI state with immutable target state.**

The target state contains `isLoading`, `isRefreshing`, `requiredItems`, `optionalItems`, `requiredReadyCount`, `requiredTotalCount`, `canContinue`, `primaryActionLabel`, `primaryActionEnabled`, `contextualGuidance: PermissionGuidanceUiModel?`, and `recoverableFailure: PermissionGuidanceUiModel?`. Give every field a default so `AttendancePermissionReadinessUiState()` is a valid loading state.

Keep the old `toAttendancePermissionReadinessUiState(foregroundLocationGranted, cameraGranted, deviceLocationEnabled, notificationGranted, backgroundLocationGranted)` factory and the exact legacy projections `foregroundLocationGranted`, `cameraGranted`, `deviceLocationEnabled`, `notificationGranted`, `backgroundLocationGranted`, `canContinueToWorkMode`, `nextRequiredAction`, `progressCopy`, and `warningMessage` so the current stateful Screen and its feature-local children compile unchanged until Task 8. Mark the bridge in a named `LegacyPermissionReadinessProjection` section; delete it in Task 8.

- [ ] **Step 4: Implement a pure `AttendancePermissionReadinessUiMapper`.**

Declare it as `class AttendancePermissionReadinessUiMapper @Inject constructor()` so Hilt can construct the Task 5 ViewModel without a provider. The mapper owns copy, icon keys, requirement/status labels, `InfiniteSemantic`, actions, and coherent `stateDescription`. It accepts effective readiness after request evidence is applied. It never reads Context/resources and never returns `ImageVector`.

- [ ] **Step 5: Run mapper and legacy-regression tests.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermissionReadinessUiMapperTest' --tests '*AttendancePermissionReadinessUiStateTest'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected result: `BUILD SUCCESSFUL`; current screen remains source-compatible.

- [ ] **Step 6: Commit the presentation contract.**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission
git commit -m "feat: define permission readiness presentation contract"
```

### Task 5: Implement ViewModel observation, refresh, request evidence, and one-time effects

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt`
- Create: `app/src/test/java/com/example/infinite_track/testing/MainDispatcherRule.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModelTest.kt`

**Interfaces:**

- Consumes: three permission use cases and pure UI mapper.
- Produces: immutable `StateFlow<AttendancePermissionReadinessUiState>` and one-time `SharedFlow<AttendancePermissionReadinessEffect>`.

- [ ] **Step 1: Add the shared Main dispatcher rule and failing ViewModel tests.**

Cover initial loading/refresh, observation mapping, primary action ordering, per-item optional action, denial overlay, permanent denial -> app Settings, GPS -> device Settings, all required plus one `PrimaryActionClicked` -> one navigation, observing all-ready without a click -> no navigation, optional degradation or optional-only inspection issue plus CTA click -> navigation, required inspection issue -> Retry, callback/Settings/manual refresh, failed Settings launch -> long error feedback, last trustworthy content, rapid double-tap suppression, duplicate resume refresh suppression, one-shot effect delivery, and feedback de-duplication until `SnackbarFinished`.

Collect effects without Turbine:

```kotlin
val effect = async(UnconfinedTestDispatcher(testScheduler)) {
    viewModel.effects.first()
}
viewModel.onEvent(AttendancePermissionReadinessEvent.PrimaryActionClicked)
assertEquals(expectedEffect, effect.await())
```

- [ ] **Step 2: Run the test and confirm the ViewModel is missing.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermissionReadinessViewModelTest'
```

Expected result: compilation failure for `AttendancePermissionReadinessViewModel`.

- [ ] **Step 3: Implement ViewModel state/effect ownership.**

Use this public shape:

```kotlin
@HiltViewModel
class AttendancePermissionReadinessViewModel @Inject constructor(
    private val observeReadiness: ObserveAttendancePermissionReadinessUseCase,
    private val refreshReadiness: RefreshAttendancePermissionReadinessUseCase,
    private val resolveNextAction: ResolveNextAttendancePermissionActionUseCase,
    private val uiMapper: AttendancePermissionReadinessUiMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttendancePermissionReadinessUiState())
    val uiState: StateFlow<AttendancePermissionReadinessUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AttendancePermissionReadinessEffect>(
        extraBufferCapacity = 1
    )
    val effects: SharedFlow<AttendancePermissionReadinessEffect> = _effects.asSharedFlow()

    fun onEvent(event: AttendancePermissionReadinessEvent)
}
```

Implementation rules:

- Start observing once in `init`, then request an initial refresh.
- Store the latest repository aggregate and a map of known request outcomes. Apply outcomes only for presentation/action resolution; repository snapshots remain OS-derived.
- Remove request evidence when a fresh base entry becomes `READY` or `NOT_REQUIRED_ON_DEVICE`.
- Track one `refreshJob`; ignore a new resume/return/retry request while it is active.
- Track one native/navigation action in flight; clear it on callback, Settings return/failure, or when readiness changes so the action is no longer applicable.
- Emit navigation only in response to `PrimaryActionClicked` while the latest effective readiness is valid. Observing an all-ready state must never navigate automatically. Suppress a rapid repeated CTA event until Route sends `NavigationHandled` after invoking the callback, or until readiness changes; reset the guard if readiness later becomes blocked.
- Keep feedback IDs stable and suppress the same active feedback until Route reports `SnackbarFinished`.
- Never retain `Activity`, `Context`, launchers, `NavController`, or Compose values.

- [ ] **Step 4: Run ViewModel and all permission unit tests.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermission*'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit Phase 2.**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt app/src/test/java/com/example/infinite_track/testing/MainDispatcherRule.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModelTest.kt
git commit -m "feat: coordinate permission readiness state and effects"
```

---

## Phase 3 — Stateless UI and Unified Feedback

### Task 6: Build semantic feedback tokens, glass surface, and Infinite snackbar

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteFeedbackTokens.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteFeedbackGlassSurface.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteSnackbar.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/design/tokens/InfiniteFeedbackTokensTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/design/components/status/InfiniteSnackbarVisualsTest.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/design/components/status/InfiniteSnackbarTest.kt`

**Interfaces:**

- Consumes: existing `InfiniteSemantic`, colors, spacing, radius, elevation, motion, and registered SF Compact family.
- Produces: shared semantic palette/typography, API-26-safe glass atom, `InfiniteSnackbarVisuals`, visual snackbar, and host.

- [ ] **Step 1: Write failing JVM token/duration tests.**

Assert all seven `InfiniteSemantic` values resolve a complete palette, feedback typography uses only Medium/Bold with the approved line heights, Success/Info/Primary/Secondary/Neutral map to `Short`, Warning/Error map to `Long`, and explicit duration remains preserved in visuals.

- [ ] **Step 2: Implement `InfiniteFeedbackTokens`.**

Define `InfiniteFeedbackPalette` with `surfaceStart`, `surfaceEnd`, `content`, `supportingContent`, `border`, `accent`, `glow`, `shadow`, and `topHighlight`. Define centralized text styles for snackbar/title `14-16sp / 18-20sp`, supporting body `14sp / 20sp`, pill/action `11-12sp / 14-16sp`, dialog title `20sp / 24sp`, and dialog body `14sp / 20sp`.

- [ ] **Step 3: Implement the specialized glass surface.**

```kotlin
@Composable
fun InfiniteFeedbackGlassSurface(
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(InfiniteRadius.Large),
    shadowElevation: Dp = InfiniteElevation.Soft,
    showAccentRail: Boolean = true,
    showTopHighlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit
)
```

Render ordinary `shadow`, gradient background, 1 dp border, optional semantic rail, and top highlight. Content/text remains opaque. Do not call `Modifier.blur`, `RenderEffect`, `dropShadow`, `innerShadow`, or mutate generic `InfiniteSurface`.

- [ ] **Step 4: Implement snackbar data and duration mapping.**

```kotlin
@Immutable
data class InfiniteSnackbarVisuals(
    override val message: String,
    val semantic: InfiniteSemantic,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = semantic.defaultSnackbarDuration()
) : SnackbarVisuals

fun InfiniteSemantic.defaultSnackbarDuration(): SnackbarDuration = when (this) {
    InfiniteSemantic.Warning,
    InfiniteSemantic.Error -> SnackbarDuration.Long
    InfiniteSemantic.Success,
    InfiniteSemantic.Info,
    InfiniteSemantic.Primary,
    InfiniteSemantic.Secondary,
    InfiniteSemantic.Neutral -> SnackbarDuration.Short
}
```

Implement `InfiniteSnackbarHost(hostState, modifier)` by delegating to Material `SnackbarHost`, and `InfiniteSnackbar(data, modifier)` with a safe Info fallback when `data.visuals` is not `InfiniteSnackbarVisuals`. Limit the message to two lines, one action, and an optional explicit dismiss target of at least 48 dp. Do not implement a custom delay; Material owns queuing, timeout, live-region, and accessibility timeout adjustment.

- [ ] **Step 5: Run JVM tests and confirm they pass.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*InfiniteFeedbackTokensTest' --tests '*InfiniteSnackbarVisualsTest'
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Write and compile snackbar Compose tests.**

Test semantic variants, fallback visuals, action `performAction`, explicit dismiss, merged readable semantics, 48 dp targets, and two `showSnackbar` calls appearing sequentially through the same host state. Do not assert Material's internal millisecond timer; duration mapping is covered by JVM tests.

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit the snackbar foundation.**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/design app/src/test/java/com/example/infinite_track/presentation/design app/src/androidTest/java/com/example/infinite_track/presentation/design
git commit -m "feat: add Infinite glass snackbar foundation"
```

### Task 7: Unify inline, pill, embedded state, and dialog visuals with compatibility adapters

**Files:**

- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteFeedback.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteStatusPill.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/components/state/InfiniteStateComponents.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/status/StatusStateTokens.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/status/InfiniteTrackInlineAlert.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/status/InfiniteTrackConfirmDialog.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/status/InfiniteTrackStatusDialog.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/status/StatusStatePreview.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/preview/InfiniteComponentGallery.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/design/components/status/InfiniteStateStatusComponentsTest.kt`

**Interfaces:**

- Consumes: Task 6 palette, typography, and glass atom.
- Produces: canonical design-system implementations plus source-compatible legacy wrappers.

- [ ] **Step 1: Write failing component behavior/accessibility tests.**

Cover persistent inline alert remaining after `mainClock.advanceTimeBy(15_000)`, inline Retry action, dismiss target, compact pill label, empty/loading/error embedded states, recoverable error action, status dialog confirmation, confirm dialog explicit cancel/confirm, 320 dp width, font scale 2.0, coherent state description, and no clipped buttons.

- [ ] **Step 2: Make `InfiniteFeedback.kt` canonical.**

Implement `InfiniteInlineAlert`, `InfiniteStatusDialog`, and `InfiniteConfirmDialog` directly with `InfiniteFeedbackGlassSurface`; remove imports of `InfiniteTrack*`. Keep the current inline-alert overload exactly unchanged so existing positional and trailing-lambda calls still bind dismissal correctly. Add an action overload whose required `actionLabel` and `onAction` make overload resolution unambiguous:

```kotlin
@Composable
fun InfiniteInlineAlert(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
)
```

Keep status dialog modal behavior and confirm dialog explicit-choice behavior. Keep the existing public dialog parameter orders unchanged. Route status dialog through an internal `InfiniteStatusDialogContent` whose parameter list is the public values plus `@DrawableRes imageRes: Int?`; route confirmation dialog through an internal `InfiniteConfirmDialogContent` whose parameter list is the public values plus `isDestructive: Boolean`. Canonical public calls supply their existing semantic defaults; legacy wrappers supply their explicit `imageRes`/`isDestructive` values. This preserves illustrations and destructive-button behavior without breaking positional or trailing-lambda calls.

- [ ] **Step 3: Invert legacy adapters toward the canonical components.**

Map `StatusStateSpec` to `InfiniteSemantic` in `StatusStateTokens`; preserve `StatusStates` and all public legacy signatures. Each `InfiniteTrack*` function delegates to the matching canonical function. The canonical design package must not import legacy components after this step.

- [ ] **Step 4: Restyle pill and embedded states without semantic changes.**

- `InfiniteStatusPill`: use shared palette, compact Medium typography, no rail/heavy shadow; preserve `useSharedRequestPalette` and `colorOverride`.
- `InfiniteEmptyState`, `InfiniteLoadingState`, `InfiniteErrorState`: use embedded glass treatment. Add optional `actionLabel`/`onAction` only as trailing default parameters.
- Inline/dismiss controls and actions meet 48 dp targets; decorative icons have null semantics; parent nodes expose the coherent description.
- Dialog actions reflow vertically when width or font scale cannot fit two horizontal buttons.

- [ ] **Step 5: Expand previews/gallery.**

Show all semantic variants, snackbar action/dismiss, persistent inline recovery, compact pills, embedded states, confirmation/status dialogs, 320 dp width, and large-font examples. Remove SemiBold requests from every touched status component.

- [ ] **Step 6: Compile and run component instrumentation tests on an attached emulator.**

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
adb devices -l
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.design.components.status.InfiniteStateStatusComponentsTest
```

Expected result when a device is attached: test task `BUILD SUCCESSFUL`. If no device is available, record this task as `Needs Verification`; compilation alone is not the runtime verdict.

- [ ] **Step 7: Commit the unified family.**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/design app/src/main/java/com/example/infinite_track/presentation/components/status app/src/androidTest/java/com/example/infinite_track/presentation/design
git commit -m "feat: unify Infinite feedback component visuals"
```

### Task 8: Split Route from stateless Screen and wire stable navigation

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessRoute.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessUiState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionGlassCard.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionHeroCard.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionMissionRow.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionProgressHeader.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionReadinessInfoBox.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreenTest.kt`

**Interfaces:**

- Consumes: Task 5 state/effects and Task 6/7 feedback components.
- Produces: Activity-bound Route, exact approved stateless Screen signature, and unchanged navigation identity.

- [ ] **Step 1: Write failing stateless Screen tests.**

Render default loading, partial, all-required-ready, all-ready, denied, permanently denied, optional degraded, unsupported, GPS disabled, and recoverable-error states. Assert required progress appears exactly once, optional rows do not change its denominator, Retry emits `RetryRefresh`, row/CTA events are typed, small width remains usable, font scale 2.0 does not clip actions, and row semantics announce requirement/state/action.

- [ ] **Step 2: Implement the exact stateless Screen contract.**

```kotlin
@Composable
fun AttendancePermissionReadinessScreen(
    uiState: AttendancePermissionReadinessUiState,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

The Screen must contain no `Context`, lifecycle observer, launcher, permission string, Settings intent, repository/ViewModel lookup, navigation, or snackbar host. Render one progress summary, required section, optional section, persistent guidance/recovery, and primary CTA. Map `PermissionIconKey` to UI icons locally. Replace decorative `.blur(32.dp/36.dp)` with radial gradients so API 26 visual correctness is stable.

- [ ] **Step 3: Refactor feature-local permission components.**

`PermissionMissionRow` consumes one `PermissionItemUiModel`, uses `InfiniteInfoRow`/`InfiniteStatusPill`/compact action, provides a 48 dp target, merges semantics, and stacks its action on narrow layouts. `PermissionReadinessInfoBox` delegates to canonical `InfiniteInlineAlert`. Do not duplicate required progress in hero and header.

- [ ] **Step 4: Implement Route ownership.**

Use:

```kotlin
@Composable
fun AttendancePermissionReadinessRoute(
    onBackClick: () -> Unit,
    onContinueToWorkMode: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendancePermissionReadinessViewModel = hiltViewModel()
)
```

Route requirements:

- Collect state with `collectAsStateWithLifecycle()`.
- Use one `RequestMultiplePermissions` launcher for the fine/coarse location pair, plus separate camera, notification, background-location, application-Settings, and device-location-Settings launchers.
- After a known request, classify permanent denial only when not granted and `ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)` is false.
- Fine permission result is granted only when `ACCESS_FINE_LOCATION` is true; coarse-only remains a typed denial/approximate condition.
- Report each callback as `PermissionResultReceived`, then let ViewModel refresh OS truth.
- Add a lifecycle observer that emits `ScreenResumed`; remove it in `onDispose`.
- Collect effects in `LaunchedEffect(viewModel)`. After invoking `onContinueToWorkMode` for `NavigateToWorkMode`, emit `NavigationHandled`. Wrap Settings launches in safe exception handling and emit `SettingsLaunchFailed` instead of crashing.
- Use `rememberUpdatedState` for latest `onContinueToWorkMode` and `onBackClick` callbacks captured by long-lived effects.
- Own one `SnackbarHostState`; translate `AttendanceFeedbackDuration` to Material `Short`/`Long`; construct `InfiniteSnackbarVisuals(message = feedback.message, semantic = feedback.semantic, actionLabel = feedback.actionLabel, duration = materialDuration)` and pass it to `showSnackbar`; send action/dismiss result back as typed events.
- Overlay `InfiniteSnackbarHost` in a `Box` with bottom/navigation/IME insets so it does not cover the primary CTA. Do not add a global scaffold.

- [ ] **Step 5: Replace the graph destination call, not the route identity.**

In `MainContentNavGraph`, call `AttendancePermissionReadinessRoute`; keep the existing destination string and `safeNavigate(Screen.Attendance.route)` callback. Add an explicit shell test assertion that readiness and Attendance both hide the bottom bar.

- [ ] **Step 6: Delete the temporary legacy projection.**

Remove the primitive boolean factory, old `AttendancePermissionAction` presentation enum, and compatibility properties from `AttendancePermissionReadinessUiState.kt`. Delete platform helper functions from the old Screen. Update previews/tests to construct typed UI models only.

- [ ] **Step 7: Run Phase 3 verification.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePermission*' --tests '*InfiniteSnackbar*' --tests '*InfiniteFeedback*'
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:assembleDebug
```

Expected result: all commands end with `BUILD SUCCESSFUL`.

- [ ] **Step 8: Run the Screen instrumentation test when an emulator is available.**

```powershell
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessScreenTest
```

Expected result: `BUILD SUCCESSFUL`, or record `Needs Verification` with `adb devices -l` output if no device is attached.

- [ ] **Step 9: Commit Route/Screen integration.**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt app/src/test/java/com/example/infinite_track/presentation/navigation/WfaShellNavigationContractTest.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/permission
git commit -m "feat: route typed permission readiness effects"
```

### Task 9: Migrate directly affected Attendance outcomes from passive modal to snackbar

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceTransientFeedback.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenFaceResultRescueTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceTransientFeedbackTest.kt`

**Interfaces:**

- Consumes: canonical Infinite snackbar and existing Attendance result events.
- Produces: one-time success/error/location feedback with correct transient semantics.

- [ ] **Step 1: Write failing feedback mapping and face-result rescue tests.**

Define success as semantic Success + Short + optional `BERANDA` action; recoverable attendance/location/face errors as Error + Long with no modal. Update instrumentation expectations from `activeDialog` state to one visible snackbar event, cleared saved-state result, and no replay after navigating away/back. Add a failure carrying a sentinel exception message and assert the sentinel never appears in emitted feedback.

- [ ] **Step 2: Replace `DialogState` with a one-time feedback stream.**

```kotlin
data class AttendanceTransientFeedback(
    val id: Long,
    val message: String,
    val semantic: InfiniteSemantic,
    val duration: AttendanceTransientFeedbackDuration,
    val actionLabel: String? = null,
    val action: AttendanceTransientFeedbackAction? = null
)

enum class AttendanceTransientFeedbackDuration { SHORT, LONG }
enum class AttendanceTransientFeedbackAction { NAVIGATE_HOME }
```

In `AttendanceViewModel`, expose an immutable `SharedFlow<AttendanceTransientFeedback>` with `replay = 0`, generate monotonic IDs from a private ViewModel-owned counter, and replace all `DialogState.Success`, `DialogState.Error`, and `DialogState.LocationError` assignments with effect emission. Remove `activeDialog` and dialog-dismiss mutation from persistent `AttendanceScreenState`. Preserve approved static user copy, but replace every exception-derived/interpolated message with a fixed category-safe Indonesian message such as `Absensi belum berhasil. Silakan coba lagi.` or `Lokasi belum dapat dibaca. Periksa GPS lalu coba lagi.` Never copy `Throwable.message`, server internals, identifiers, or a raw unknown result into feedback.

- [ ] **Step 3: Install the snackbar host in the existing bottom-sheet scaffold.**

Use `scaffoldState.snackbarHostState` and:

```kotlin
snackbarHost = {
    InfiniteSnackbarHost(hostState = scaffoldState.snackbarHostState)
}
```

Collect the ViewModel feedback once in `LaunchedEffect(viewModel)`, construct `InfiniteSnackbarVisuals(message = feedback.message, semantic = feedback.semantic, actionLabel = feedback.actionLabel, duration = feedback.duration.toMaterialDuration())`, pass it to `showSnackbar`, and navigate home only when the user performs the `BERANDA` action. Auto-dismiss must leave the user on the usable Attendance screen. Remove only the three passive `InfiniteTrackStatusDialog` blocks; confirmation dialogs elsewhere remain modal.

- [ ] **Step 4: Run focused Attendance tests and compile.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendanceTransientFeedbackTest'
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:assembleDebug
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Run face-result rescue instrumentation when a device is attached.**

```powershell
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.AttendanceScreenFaceResultRescueTest
```

Expected result: `BUILD SUCCESSFUL`, or `Needs Verification` with device evidence.

- [ ] **Step 6: Commit the bounded semantic migration.**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test/java/com/example/infinite_track/presentation/screen/attendance app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance
git commit -m "feat: surface attendance outcomes as snackbars"
```

Do not migrate Login/Auth/Profile/WFA modal semantics in this task. Record the currently invisible Login snackbar host as a separate existing defect; it is not INF-230 scope.

---

## Phase 4 — Defensive Fallback Cleanup

### Task 10: Harden downstream revocation recovery, then remove dead normal-request ownership behind a runtime gate

**Files:**

- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/maps/AttendanceMap.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofencePermissionContract.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceManager.kt`
- Delete after gate passes: `app/src/main/java/com/example/infinite_track/utils/LocationPermissionHelper.kt`
- Delete after gate passes: `app/src/main/java/com/example/infinite_track/utils/CompositionLocals.kt`
- Delete after gate passes: `app/src/main/java/com/example/infinite_track/presentation/components/dialog/LocationPermissionDialog.kt`
- Modify after gate passes: `app/src/main/java/com/example/infinite_track/presentation/main/MainActivity.kt`
- Modify after gate passes: `app/src/main/java/com/example/infinite_track/presentation/main/InfiniteTrackApp.kt`
- Modify after gate passes: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify after gate passes: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/AttendancePermissionRevocationTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/geofencing/GeofencePermissionContractTest.kt`

**Interfaces:**

- Consumes: readiness route as normal owner and current downstream defensive checks.
- Produces: explicit non-requesting fallback after revocation, camera Settings recovery, precise geofence wording/guards, and removal of the unused Activity helper only after proof.

- [ ] **Step 1: Write failing downstream contract tests.**

Assert `AttendanceMap` renders a passive fallback without constructing a MapView when precise location is false; `AttendanceScreen` owns the persistent recovery action and invokes `navigatePermissionReadiness`; no downstream component auto-launches a normal permission request; Face Scanner only performs exceptional post-revocation recovery after a user action and opens app Settings after permanent denial; geofence foreground policy means precise location, background absence is degraded/non-blocking for manual attendance, and guard messages do not claim coarse is sufficient.

- [ ] **Step 2: Hoist map permission availability to its Attendance caller.**

Change `AttendanceMap` to accept `hasPreciseLocationPermission: Boolean`; remove `LocalContext`, `ContextCompat`, and the unkeyed `remember` permission read. In the Attendance route-level composable, refresh a defensive precise-location boolean on `ON_RESUME`, do not request permission there, skip `startLocationUpdates()` when false, and render canonical persistent inline recovery with action `Kembali ke kesiapan`. Append `navigatePermissionReadiness: () -> Unit = {}` to the Attendance screen boundary, wire the production graph callback to the existing readiness route, and keep previews/tests source-compatible through the default.

- [ ] **Step 3: Keep and improve explicit Face Scanner recovery.**

Do not auto-request camera on entry and do not present this path during ordinary ready entry. Preserve a user-triggered camera request only as exceptional recovery when camera was revoked after readiness or a direct-entry edge bypassed current readiness; label that condition as recovery, track whether this flow requested camera, use rationale state to classify permanent denial, and offer an explicit application Settings action when permanent. Refresh camera state after returning.

- [ ] **Step 4: Preserve geofence TOCTOU guards and correct semantics.**

Keep fine/background checks immediately before Play Services calls. Extract a pure permission contract used by the guard and JVM test. Foreground geofence requires precise location; background absence degrades automatic monitoring but never changes a successful manual attendance result. Replace `FINE/COARSE` wording with precise-location wording. Do not make GeofenceManager the owner of basic Attendance eligibility.

- [ ] **Step 5: Compile and run the pre-cleanup runtime gate on a disposable emulator.**

```powershell
.\gradlew.bat --no-daemon app:installDebug
adb devices -l
adb shell pm clear com.example.infinite_track
```

Run `pm clear` only against the disposable emulator selected in `adb devices -l`; it intentionally erases that emulator app's local session/data to create a fresh-install state. Never run this matrix against the user's daily physical device.

Manually verify and record:

1. Every first-time/ordinary location/camera/notification/background request originates from Permission Readiness; any Face Scanner camera request is separately evidenced as explicit post-revocation/direct-entry recovery.
2. Coarse-only location remains blocked.
3. Required-ready navigation reaches Attendance once.
4. Revoking precise location while downstream causes inline recovery and never an automatic request.
5. Revoking camera before Face Scanner shows explicit user-triggered recovery.
6. Missing background location degrades geofence monitoring but does not invalidate manual Attendance.

If any item fails, stop here, keep `LocationPermissionHelper` and its adapters, record `Needs Verification`, and fix the owning Phase 1-3 behavior before cleanup.

- [ ] **Step 6: Remove the dead helper only after all six gate checks pass.**

Remove `LocationPermissionHelper` construction/callback from `MainActivity`; remove its parameter/provider from `InfiniteTrackApp`; delete `LocalLocationPermissionHelper` and `LocationPermissionDialog`; remove `showPermissionDialog`, `permissionResult`, and `permissionMessage` from `AttendanceScreenState`; remove `onPermissionDialogResult`/dismiss code from `AttendanceViewModel`; remove helper/dialog imports and blocks from `AttendanceScreen`.

Before deleting, prove the exact references:

```powershell
rg -n "LocationPermissionHelper|LocalLocationPermissionHelper|LocationPermissionDialog|showPermissionDialog|permissionResult|permissionMessage|onPermissionDialogResult" app/src/main app/src/test app/src/androidTest
```

After deleting, the same command must return exit code 1 with no matches.

- [ ] **Step 7: Run Phase 4 tests.**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofencePermissionContractTest' --tests '*AttendancePermission*'
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.AttendancePermissionRevocationTest
.\gradlew.bat --no-daemon app:assembleDebug
```

Expected result: all attached-environment checks end with `BUILD SUCCESSFUL`. Missing device/runtime remains `Needs Verification`, and in that case the helper deletion must not be committed.

- [ ] **Step 8: Commit verified defensive cleanup.**

```powershell
git add -A app/src/main/java/com/example/infinite_track app/src/test/java/com/example/infinite_track app/src/androidTest/java/com/example/infinite_track
git commit -m "refactor: centralize attendance permission ownership"
```

### Task 11: Run full quality gates and record runtime evidence

**Files:**

- Create: `docs/linear-sync/INF-230-runtime-verification.md`
- Modify if implementation diverged intentionally: `docs/superpowers/specs/2026-07-22-inf-230-permission-readiness-architecture-and-feedback-design.md`

**Interfaces:**

- Consumes: all four implemented phases.
- Produces: reproducible verification evidence and a clear Done versus Needs Verification verdict.

- [ ] **Step 1: Run the full local gate with fresh output.**

```powershell
.\gradlew.bat --no-daemon app:test
.\gradlew.bat --no-daemon app:assembleDebug
.\gradlew.bat --no-daemon app:lint
```

Expected result: each command exits 0. If lint fails, separate introduced findings from pre-existing findings with exact file/line evidence; do not report a blanket pass.

- [ ] **Step 2: Run instrumentation suites on an attached emulator.**

```powershell
adb devices -l
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.example.infinite_track.presentation.screen.attendance.permission
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.example.infinite_track.presentation.design.components.status
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.AttendanceScreenFaceResultRescueTest
```

- [ ] **Step 3: Execute the approved OS/runtime matrix.**

Use disposable emulator images covering below Android 10, Android 10, Android 11+, and Android 13+. Record fresh install, location denied, coarse-only, camera denied, permanent denial, GPS off, notification skipped, background skipped, return from both Settings destinations, and downstream revocation. Capture core readiness states, inline Retry, Infinite snackbar, confirmation/status dialog styling, and evidence that downstream paths do not originate normal requests.

- [ ] **Step 4: Write the evidence document.**

For each matrix row record emulator/API, build commit, exact setup, expected result, observed result, pass/fail, and evidence link/path. Do not commit credentials, personal data, tokens, raw auth logs, or large screen recordings. When an environment is unavailable, write `Needs Verification` and the missing emulator/API instead of marking Done.

- [ ] **Step 5: Perform scope and architecture audits.**

```powershell
rg -n "LocalContext|rememberLauncherForActivityResult|shouldShowRequestPermissionRationale|ACTION_APPLICATION_DETAILS_SETTINGS|ACTION_LOCATION_SOURCE_SETTINGS|NavController|SnackbarHostState" app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt
rg -n "Context|Activity|NavController|ImageVector|SnackbarDuration|Manifest.permission" app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt app/src/main/java/com/example/infinite_track/domain/model/attendance/permission app/src/main/java/com/example/infinite_track/domain/use_case/attendance/permission
rg -n "\.blur\(|RenderEffect|dropShadow|innerShadow|FontWeight\.SemiBold" app/src/main/java/com/example/infinite_track/presentation/design/components/status app/src/main/java/com/example/infinite_track/presentation/design/components/state app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission
git diff origin/develop -- app/src/main/java/com/example/infinite_track/di/NetworkModule.kt app/src/main/res/xml/network_security_config.xml
```

Expected result: the first two architectural scans and visual forbidden-API scan return no matches; the sensitive-file diff is empty.

- [ ] **Step 6: Request code review before integration.**

Use `superpowers:requesting-code-review`. Require reviewers to inspect domain invariants, Activity boundary ownership, effect idempotency, coarse-only behavior, API-26 glass fallback, snackbar semantics, accessibility, runtime evidence, and the conditional helper deletion.

- [ ] **Step 7: Commit verification evidence.**

```powershell
git add docs/linear-sync/INF-230-runtime-verification.md docs/superpowers/specs/2026-07-22-inf-230-permission-readiness-architecture-and-feedback-design.md
git diff --cached --quiet; if ($LASTEXITCODE -ne 0) { git commit -m "docs: record INF-230 runtime verification" }
```

Do not update Linear to Done or promote toward `develop` until diff/commit evidence, fresh gates, runtime evidence, and review verdict all exist. Use `superpowers:finishing-a-development-branch` only after those conditions are satisfied.

## Final Acceptance Checklist

- [ ] Flow follows `Screen -> ViewModel -> UseCase -> Repository -> RepositoryImpl -> Android data source` in reverse observation direction and forward event direction.
- [ ] Required access is precise location, camera, and enabled device location; optional capability never blocks.
- [ ] Route owns native launchers, rationale, Settings, snackbar, and navigation; Screen is stateless.
- [ ] ViewModel has immutable state plus one-time effects and suppresses duplicate native/navigation/feedback actions.
- [ ] Infinite snackbar is designed, queued, accessibility-timeout-aware, Short/Long by semantics, and never uses a component-local delay.
- [ ] Inline/pill/state/dialog components share tokens but retain distinct behavior.
- [ ] Glass renders correctly on API 26 without blur and text remains high contrast.
- [ ] Status typography uses registered Medium/Bold with corrected component line heights.
- [ ] Permission screen is usable at 320 dp and font scale 2.0 with coherent semantics and 48 dp targets.
- [ ] Legacy feedback APIs remain source-compatible; semantic migration is bounded to Permission Readiness and Attendance.
- [ ] Downstream revocation has explicit recovery; first-time/ordinary requests originate only from readiness, while Face Scanner camera request is limited to explicit exceptional recovery.
- [ ] Helper cleanup occurred only after runtime gate evidence.
- [ ] Unit, build, lint, instrumentation, runtime matrix, and review verdict are recorded, or remaining gaps are explicitly `Needs Verification`.
