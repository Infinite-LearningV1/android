# Attendance Preparation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate Permission Readiness, Work Mode, Target Location, and Preparation Eligibility into one tested Attendance Preparation owner while preserving existing Attendance execution, checkout, geofence, Face Recognition, and backend semantics.

**Architecture:** Introduce typed domain contracts plus `AttendancePreparationViewModel` as the single persistent state owner for preparation. Migrate behavior through a temporary bridge into the existing `AttendanceViewModel`, then remove duplicate target and blocker policy after tests prove parity. Platform, navigation, and map actions are emitted as one-time effects and executed by the Route/UI layer.

**Tech Stack:** Kotlin, Android, Jetpack Compose, StateFlow/SharedFlow, coroutines, Hilt, JUnit, kotlinx-coroutines-test, existing domain/repository/use-case patterns.

## Global Constraints

- Repository: `Infinite-LearningV1/android`.
- Integration branch: `develop`.
- Primary issue: INF-238.
- Preserve `Screen → ViewModel → UseCase → Repository Interface → RepositoryImpl → API/Room/platform`.
- Backend remains the final Attendance validation authority.
- Required camera, foreground location, and GPS readiness may block preparation.
- Optional notification and background-location access must not block manual attendance.
- WFO target comes from `status-today.activeLocation`.
- WFH target comes from the registered profile home location.
- Approved WFA booking is the authoritative WFA Attendance target.
- WFA recommendations are discovery/request inputs only.
- Checkout must not require work-mode or target reselection.
- Work Mode selection must not register or remove active monitoring geofences.
- Face verification success is not final Attendance success.
- No ViewModel may hold `NavController`, `Activity`, DTO, Entity, Retrofit service, Room DAO, or Compose dependency.
- Use TDD, bounded commits, and preserve current behavior during migration.

---

## File Structure

### New domain files

- `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationEligibility.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationBlockReason.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationRecoveryAction.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationSnapshot.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/preparation/EvaluateAttendancePreparationUseCase.kt`

### New presentation files

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEvent.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEffect.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationSection.kt`

### Existing files to modify

- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WorkModeSelector.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateWorkModeEligibilityUseCase.kt`
- `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`

### Tests

- `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/preparation/EvaluateAttendancePreparationUseCaseTest.kt`
- `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModelTest.kt`
- `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolverPreparationTest.kt`
- Update existing target and eligibility tests where signatures change.

---

### Task 1: Add typed Attendance Preparation domain contracts

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationEligibility.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationBlockReason.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationRecoveryAction.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation/AttendancePreparationSnapshot.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/preparation/EvaluateAttendancePreparationUseCaseTest.kt`

**Interfaces:**
- Consumes: existing `WorkMode`, `SelectedTargetLocation`, and permission readiness domain values introduced by INF-230.
- Produces: `AttendancePreparationEligibility`, `AttendancePreparationBlockReason`, `AttendancePreparationRecoveryAction`, and `AttendancePreparationSnapshot` used by all later tasks.

- [ ] **Step 1: Write the failing contract test**

```kotlin
class EvaluateAttendancePreparationUseCaseTest {
    @Test
    fun `optional permissions do not block manual attendance`() {
        val snapshot = readySnapshot(
            notificationGranted = false,
            backgroundLocationGranted = false
        )

        val result = EvaluateAttendancePreparationUseCase()(snapshot)

        assertTrue(result is AttendancePreparationEligibility.Ready)
    }

    @Test
    fun `missing foreground location blocks preparation`() {
        val snapshot = readySnapshot(foregroundLocationGranted = false)

        val result = EvaluateAttendancePreparationUseCase()(snapshot)

        assertEquals(
            AttendancePreparationBlockReason.ForegroundLocationPermissionMissing,
            (result as AttendancePreparationEligibility.Blocked).reason
        )
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "*EvaluateAttendancePreparationUseCaseTest"
```

Expected: compilation failure because the preparation contracts and use case do not exist.

- [ ] **Step 3: Add the typed contracts**

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

```kotlin
data class AttendancePreparationSnapshot(
    val cameraGranted: Boolean,
    val foregroundLocationGranted: Boolean,
    val notificationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val deviceLocationEnabled: Boolean,
    val selectedMode: WorkMode,
    val selectedTarget: SelectedTargetLocation?
)
```

- [ ] **Step 4: Commit the contracts and failing test scaffold**

```bash
git add app/src/main/java/com/example/infinite_track/domain/model/attendance/preparation \
  app/src/test/java/com/example/infinite_track/domain/use_case/attendance/preparation/EvaluateAttendancePreparationUseCaseTest.kt
git commit -m "test: define attendance preparation contracts"
```

---

### Task 2: Implement preparation eligibility evaluation

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/preparation/EvaluateAttendancePreparationUseCase.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/preparation/EvaluateAttendancePreparationUseCaseTest.kt`

**Interfaces:**
- Consumes: `AttendancePreparationSnapshot`.
- Produces: `operator fun invoke(snapshot: AttendancePreparationSnapshot): AttendancePreparationEligibility`.

- [ ] **Step 1: Complete the eligibility matrix tests**

Add cases for:

```kotlin
@Test fun `camera missing blocks with request permission recovery`()
@Test fun `gps disabled blocks with device settings recovery`()
@Test fun `WFO without target blocks as WfoTargetUnavailable`()
@Test fun `WFH without target blocks as WfhLocationMissing`()
@Test fun `WFA without approved target blocks as WfaApprovedTargetMissing`()
@Test fun `ready target returns Ready with same target`()
```

Use explicit expected reason and recovery-action assertions.

- [ ] **Step 2: Run the tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*EvaluateAttendancePreparationUseCaseTest"
```

Expected: tests fail because evaluation is not implemented.

- [ ] **Step 3: Implement the minimal evaluator**

```kotlin
class EvaluateAttendancePreparationUseCase @Inject constructor() {
    operator fun invoke(
        snapshot: AttendancePreparationSnapshot
    ): AttendancePreparationEligibility {
        if (!snapshot.cameraGranted) {
            return blocked(
                AttendancePreparationBlockReason.CameraPermissionMissing,
                AttendancePreparationRecoveryAction.RequestRequiredPermission
            )
        }
        if (!snapshot.foregroundLocationGranted) {
            return blocked(
                AttendancePreparationBlockReason.ForegroundLocationPermissionMissing,
                AttendancePreparationRecoveryAction.RequestRequiredPermission
            )
        }
        if (!snapshot.deviceLocationEnabled) {
            return blocked(
                AttendancePreparationBlockReason.DeviceLocationDisabled,
                AttendancePreparationRecoveryAction.OpenDeviceLocationSettings
            )
        }

        val target = snapshot.selectedTarget
        if (target?.location == null) {
            val reason = when (snapshot.selectedMode) {
                WorkMode.WFO -> AttendancePreparationBlockReason.WfoTargetUnavailable
                WorkMode.WFH -> AttendancePreparationBlockReason.WfhLocationMissing
                WorkMode.WFA -> AttendancePreparationBlockReason.WfaApprovedTargetMissing
            }
            val recovery = when (snapshot.selectedMode) {
                WorkMode.WFO -> AttendancePreparationRecoveryAction.RefreshTargets
                WorkMode.WFH -> AttendancePreparationRecoveryAction.OpenWfhLocationSettings
                WorkMode.WFA -> AttendancePreparationRecoveryAction.OpenWfaRequest
            }
            return blocked(reason, recovery)
        }

        return AttendancePreparationEligibility.Ready(target)
    }

    private fun blocked(
        reason: AttendancePreparationBlockReason,
        recoveryAction: AttendancePreparationRecoveryAction
    ) = AttendancePreparationEligibility.Blocked(reason, recoveryAction)
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew app:testDebugUnitTest --tests "*EvaluateAttendancePreparationUseCaseTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/attendance/preparation \
  app/src/test/java/com/example/infinite_track/domain/use_case/attendance/preparation
git commit -m "feat: evaluate attendance preparation readiness"
```

---

### Task 3: Make approved WFA booking the authoritative Attendance target

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/ResolveTodayApprovedWfaBookingUseCase.kt`
- Test: existing `ResolveSelectedTargetLocationUseCaseTest.kt`
- Test: existing `ResolveTodayApprovedWfaBookingIdUseCaseTest.kt`

**Interfaces:**
- Consumes: selected mode, WFO location, WFH location, approved WFA booking target.
- Produces: one `SelectedTargetLocation` where WFA uses approved-booking location metadata rather than recommendation defaults.

- [ ] **Step 1: Add failing WFA target tests**

```kotlin
@Test
fun `WFA target uses approved booking location id radius and coordinates`() {
    val approved = approvedBooking(
        bookingId = 77,
        locationId = 88,
        latitude = -0.9,
        longitude = 119.8,
        radiusMeters = 250f
    )

    val result = useCase(
        mode = WorkMode.WFA,
        wfoLocation = null,
        wfhLocation = null,
        approvedWfaTarget = approved
    )

    assertEquals(88, result.location?.locationId)
    assertEquals(250, result.location?.radius)
}
```

Also assert that a recommendation without approved booking returns unavailable.

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*ResolveSelectedTargetLocationUseCaseTest"
```

Expected: signature/assertion failure because current WFA mapping uses recommendation data and defaults.

- [ ] **Step 3: Change the resolver input and WFA branch**

Replace `selectedWfaLocation` as Attendance truth with a typed approved target input. Preserve recommendation selection in the WFA Request feature only.

```kotlin
WorkMode.WFA -> approvedWfaTarget?.toSelectedTargetLocation()
    ?: SelectedTargetLocation.unavailable(
        mode = WorkMode.WFA,
        reason = SelectedTargetUnavailableReason.WfaApprovedTargetMissing
    )
```

- [ ] **Step 4: Run target and booking tests**

```bash
./gradlew app:testDebugUnitTest \
  --tests "*ResolveSelectedTargetLocationUseCaseTest" \
  --tests "*ResolveTodayApprovedWfaBookingIdUseCaseTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt \
  app/src/main/java/com/example/infinite_track/domain/use_case/booking \
  app/src/test/java/com/example/infinite_track/domain/use_case
git commit -m "feat: use approved WFA booking as attendance target"
```

---

### Task 4: Add immutable preparation UI state, events, effects, and mapper

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEvent.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEffect.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapperTest.kt`

**Interfaces:**
- Consumes: typed domain eligibility and target.
- Produces: immutable `AttendancePreparationUiState` and semantic one-time effects.

- [ ] **Step 1: Write mapper tests**

```kotlin
@Test
fun `WFH missing maps to safe localized recovery content`() {
    val ui = mapper.mapEligibility(
        AttendancePreparationEligibility.Blocked(
            reason = AttendancePreparationBlockReason.WfhLocationMissing,
            recoveryAction = AttendancePreparationRecoveryAction.OpenWfhLocationSettings
        )
    )

    assertEquals("Lokasi WFH belum tersedia", ui.title)
    assertEquals("Perbarui lokasi WFH", ui.recoveryLabel)
}
```

Add tests that no mapper output contains raw exception text.

- [ ] **Step 2: Run the mapper tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*AttendancePreparationUiMapperTest"
```

Expected: compilation failure because presentation contracts do not exist.

- [ ] **Step 3: Implement presentation contracts**

```kotlin
data class AttendancePreparationUiState(
    val permissionReadiness: AttendancePermissionReadinessUiModel,
    val selectedMode: WorkMode = WorkMode.WFO,
    val availableModes: List<WorkModeOptionUiModel> = emptyList(),
    val selectedTarget: SelectedTargetLocationUiModel? = null,
    val eligibility: AttendancePreparationEligibilityUiModel = AttendancePreparationEligibilityUiModel.Resolving,
    val isResolving: Boolean = true,
    val error: AttendancePreparationErrorUiModel? = null
)
```

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

- [ ] **Step 4: Run tests**

```bash
./gradlew app:testDebugUnitTest --tests "*AttendancePreparationUiMapperTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation
git commit -m "feat: add attendance preparation presentation contracts"
```

---

### Task 5: Implement AttendancePreparationViewModel with latest-selection-wins

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModelTest.kt`

**Interfaces:**
- Consumes: permission readiness observer/use case from INF-230, target sources, approved WFA booking resolver, target resolver, and `EvaluateAttendancePreparationUseCase`.
- Produces: `StateFlow<AttendancePreparationUiState>` and `SharedFlow<AttendancePreparationEffect>`.

- [ ] **Step 1: Write ViewModel tests**

Required tests:

```kotlin
@Test fun `initial entry rechecks permission and target sources`()
@Test fun `selecting mode resolves target and eligibility atomically`()
@Test fun `rapid WFA then WFO ignores stale WFA result`()
@Test fun `optional permission absence keeps ready state`()
@Test fun `recovery click emits semantic effect once`()
@Test fun `focus target is emitted as effect not persistent state`()
```

For the race test, suspend the WFA resolver, select WFO, complete WFA, and assert final mode/target remain WFO.

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*AttendancePreparationViewModelTest"
```

Expected: compilation failure because the ViewModel does not exist.

- [ ] **Step 3: Implement the ViewModel using a cancellable job**

```kotlin
@HiltViewModel
class AttendancePreparationViewModel @Inject constructor(
    private val observePermissionReadiness: ObserveAttendancePermissionReadinessUseCase,
    private val resolveSelectedTarget: ResolveSelectedTargetLocationUseCase,
    private val resolveApprovedWfaTarget: ResolveTodayApprovedWfaBookingUseCase,
    private val evaluatePreparation: EvaluateAttendancePreparationUseCase,
    private val mapper: AttendancePreparationUiMapper
) : ViewModel() {
    private val _uiState = MutableStateFlow(AttendancePreparationUiState())
    val uiState: StateFlow<AttendancePreparationUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AttendancePreparationEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AttendancePreparationEffect> = _effects.asSharedFlow()

    private var resolutionJob: Job? = null

    fun onEvent(event: AttendancePreparationEvent) {
        when (event) {
            is AttendancePreparationEvent.WorkModeSelected -> resolve(event.mode)
            AttendancePreparationEvent.Entered,
            AttendancePreparationEvent.RefreshRequested,
            AttendancePreparationEvent.PermissionStateChanged -> resolve(_uiState.value.selectedMode)
            AttendancePreparationEvent.RecoveryActionClicked -> emitRecoveryEffect()
        }
    }

    private fun resolve(mode: WorkMode) {
        resolutionJob?.cancel()
        resolutionJob = viewModelScope.launch {
            _uiState.update { it.copy(selectedMode = mode, isResolving = true) }
            val result = resolvePreparation(mode)
            if (_uiState.value.selectedMode != mode) return@launch
            _uiState.value = mapper.map(result)
            result.selectedTarget?.location?.let {
                _effects.emit(AttendancePreparationEffect.FocusSelectedTarget(it))
            }
        }
    }
}
```

Keep source-loading helpers focused; do not copy navigation or geofence behavior into this ViewModel.

- [ ] **Step 4: Run ViewModel tests**

```bash
./gradlew app:testDebugUnitTest --tests "*AttendancePreparationViewModelTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModel.kt \
  app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModelTest.kt
git commit -m "feat: add attendance preparation state owner"
```

---

### Task 6: Bridge preparation state into Attendance execution

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolverPreparationTest.kt`

**Interfaces:**
- Consumes: typed `AttendancePreparationEligibility` or a small execution-facing snapshot.
- Produces: `AttendanceActionState.Ready(CHECK_IN)` or `AttendanceActionState.Blocked` without re-evaluating per-mode rules.

- [ ] **Step 1: Write action resolver tests**

```kotlin
@Test
fun `ready preparation allows check in`() {
    val state = attendanceState(
        preparationEligibility = readyPreparation()
    )

    assertEquals(
        AttendanceActionState.Ready(AttendanceActionIntent.CHECK_IN, "Check In"),
        AttendanceActionResolver.resolve(state)
    )
}

@Test
fun `blocked preparation maps directly to blocked action`() {
    val state = attendanceState(
        preparationEligibility = blockedPreparation(
            AttendancePreparationBlockReason.WfhLocationMissing
        )
    )

    val result = AttendanceActionResolver.resolve(state)
    assertTrue(result is AttendanceActionState.Blocked)
}

@Test
fun `checkout ignores preparation mode selection`() {
    val state = checkedInState(preparationEligibility = blockedPreparation())
    val result = AttendanceActionResolver.resolve(state)
    assertEquals(AttendanceActionIntent.CHECK_OUT, (result as AttendanceActionState.Ready).intent)
}
```

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*AttendanceActionResolverPreparationTest"
```

Expected: failure because resolver still reads old fields and fallback rules.

- [ ] **Step 3: Add a temporary bridge field**

Add one typed preparation field to `AttendanceScreenState`:

```kotlin
val preparationEligibility: AttendancePreparationEligibility = AttendancePreparationEligibility.Resolving
```

Do not add a second copy of mode/target values.

- [ ] **Step 4: Simplify AttendanceActionResolver**

For check-in, consume `preparationEligibility` only. Remove manual WFO/WFH/WFA fallback checks after migration tests pass.

```kotlin
private fun resolvePreparationBlocker(
    eligibility: AttendancePreparationEligibility
): AttendanceActionState.Blocked? = when (eligibility) {
    AttendancePreparationEligibility.Resolving -> loadingBlock()
    is AttendancePreparationEligibility.Ready -> null
    is AttendancePreparationEligibility.Blocked -> eligibility.toActionBlock()
}
```

- [ ] **Step 5: Run resolver and existing action-state tests**

```bash
./gradlew app:testDebugUnitTest \
  --tests "*AttendanceActionResolverPreparationTest" \
  --tests "*AttendanceActionResolverTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt \
  app/src/test/java/com/example/infinite_track/presentation/screen/attendance
git commit -m "refactor: consume typed attendance preparation result"
```

---

### Task 7: Integrate the preparation owner into AttendanceScreen and effects

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationSection.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WorkModeSelector.kt`
- Test: relevant Compose/UI tests under `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/`

**Interfaces:**
- Consumes: `AttendancePreparationUiState`, events, and effects.
- Produces: Compose rendering and Route-owned platform/navigation/map actions.

- [ ] **Step 1: Add UI contract tests**

Cover:

```text
WFO/WFH/WFA all visible
required permission blocker shown
optional permission absence does not disable manual attendance
selected target card changes with mode
WFH missing shows recovery action
WFA without approval shows WFA request action
```

- [ ] **Step 2: Run UI tests and verify failure**

```bash
./gradlew app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.AttendancePreparationSectionTest
```

Expected: failure because the new section is not integrated.

- [ ] **Step 3: Render the unified preparation section**

Compose hierarchy:

```text
Permission readiness summary
Work Mode options
Selected Target Location card
Eligibility/recovery alert
Contextual supporting action
```

The section receives immutable state and event lambdas only.

- [ ] **Step 4: Collect one-time effects in AttendanceScreen/Route**

Handle:

```kotlin
when (effect) {
    is AttendancePreparationEffect.RequestPermissions -> permissionLauncher.launch(...)
    AttendancePreparationEffect.OpenApplicationSettings -> openAppSettings()
    AttendancePreparationEffect.OpenDeviceLocationSettings -> openLocationSettings()
    is AttendancePreparationEffect.FocusSelectedTarget -> mapController.focus(effect.location)
    AttendancePreparationEffect.OpenWfaRequest -> navigateToWfaRequest()
    AttendancePreparationEffect.OpenWfaRequests -> navigateToWfaRequests()
    AttendancePreparationEffect.OpenWfhLocationSettings -> navigateToWfhLocationSettings()
    AttendancePreparationEffect.ProceedToFaceVerification -> navigateToFaceScanner()
}
```

Do not construct routes in either ViewModel.

- [ ] **Step 5: Run UI tests**

```bash
./gradlew app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.AttendancePreparationSectionTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance \
  app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance
git commit -m "feat: integrate unified attendance preparation UI"
```

---

### Task 8: Remove duplicate legacy state and verify boundaries

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt`
- Modify: tests affected by deleted bridge fields.

**Interfaces:**
- Consumes: stable preparation state and effects from Tasks 1–7.
- Produces: final ownership with no duplicate target or preparation policy.

- [ ] **Step 1: Search for duplicate fields and policy**

```bash
grep -R "targetLocation\|targetLocationMarker\|isWfaModeActive\|workModeEligibility" \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

Classify every result as authoritative, explicit preview-only, or removable bridge.

- [ ] **Step 2: Delete obsolete preparation ownership from AttendanceViewModel**

Remove:

```text
permission readiness orchestration
target source mapping
mode eligibility orchestration
WFA target truth
map animation persistent state
raw WFA booking route construction
```

Keep Attendance execution, Face Recognition handoff, submit, final result, and active-session checkout logic.

- [ ] **Step 3: Delete duplicate state fields**

Remove fields whose values are derivable from `AttendancePreparationUiState.selectedTarget` or typed preparation eligibility. Keep WFA preview fields only inside the WFA Request/recommendation owner and name them explicitly as preview.

- [ ] **Step 4: Run unit tests**

```bash
./gradlew app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Run lint and build**

```bash
./gradlew app:lintDebug app:assembleDebug
```

Expected: PASS.

- [ ] **Step 6: Verify architecture searches**

```bash
grep -R "NavController\|Screen\.WfaBooking\|GeofenceManager" \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation
```

Expected: no matches.

```bash
grep -R "removeAllGeofences\|setupGeofence\|addGeofence" \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation
```

Expected: no matches.

- [ ] **Step 7: Capture runtime evidence**

Record or screenshot:

```text
required permission missing
GPS disabled
optional permission missing but manual attendance available
WFO ready
WFH ready
WFH missing target blocked
WFA missing approval blocked
WFA approved ready
rapid WFA → WFO switch without stale result
checkout without mode reselection
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main app/src/test app/src/androidTest
git commit -m "refactor: finalize attendance preparation ownership"
```

---

## Final Verification

Run:

```bash
./gradlew app:testDebugUnitTest app:lintDebug app:assembleDebug
```

Expected: all tasks complete successfully.

Review the final diff:

```bash
git diff develop...HEAD --check
git diff develop...HEAD --stat
```

Confirm the PR description answers:

```text
Presentation, Domain, Data, and Navigation changed what?
Where is the source of truth now?
How were permission, mode, target, and eligibility verified?
Why do geofence, Face Recognition, and WFA Request remain separate?
What bridge fields were removed?
What runtime evidence is attached?
```
