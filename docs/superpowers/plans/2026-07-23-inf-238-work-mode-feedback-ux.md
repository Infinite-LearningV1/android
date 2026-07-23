# INF-238 Work Mode, Target Location, WFA Recommendation, and Feedback UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a truthful, cohesive Attendance preparation experience for WFO, WFH, and WFA while finishing the shared timed-feedback and non-modal login/logout UX in one pull request.

**Architecture:** Keep `AttendanceViewModel` as the single screen lifecycle owner, but replace duplicated target fields with one `AttendancePreparationState`. Pure domain use cases resolve authoritative target identity, WFA booking lifecycle, range, and eligibility; presentation mappers derive map/UI state. Auth result feedback is emitted as semantic one-shot events to one root snackbar host.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Coroutines/StateFlow, Hilt, Retrofit, Room/DataStore, provider-neutral location contracts, JUnit 4, kotlinx-coroutines-test, Compose UI tests.

## Global Constraints

- Deliver all tasks on `codex/inf-238-work-mode-feedback-ux` in `E:\skrisi\android\.worktrees\inf-238-work-mode-feedback-ux`.
- One worktree, one branch, and one pull request cover INF-238 plus the approved Feedback UX scope.
- Include only the minimum INF-242 subset required by INF-238; do not claim full INF-242 completion.
- WFO authority is `status-today.active_location`.
- WFH authority is `/api/auth/me`, mandatory, admin-provisioned, and not employee-editable.
- WFA authority is an approved booking for the Attendance date.
- Search, pick-on-map, recommendation, and preview-marker state never become authoritative automatically.
- Preserve existing Attendance app bar and draggable bottom-sheet mechanics.
- Keep Google Maps/Places, Retrofit DTOs, Room entities, and `NavController` out of domain and ViewModel contracts.
- Use existing Infinite design-system primitives and typography; do not add another design system or gradients.
- Transient Success/Info feedback uses 4 seconds; Warning/Error uses 8 seconds; X and timer appear together.
- Active recovery state stays persistent without X or timer.
- Keep logout confirmation modal; remove login/logout loading and result modals.
- Do not update Gradle, AGP, Kotlin, Compose, or dependency versions.
- Use TDD: prove the focused test fails before implementation, then make the smallest coherent change.
- Use this PowerShell environment for every Gradle command:

```powershell
$env:JAVA_HOME='D:\Java_Home\java 1.8.2'
$env:ANDROID_HOME='C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

---

## File Structure

### Domain target and preparation

- Create `app/src/main/java/com/example/infinite_track/domain/model/attendance/AuthoritativeTargetLocation.kt` for stable target identity, source, resolution, and typed recovery/failure.
- Create `app/src/main/java/com/example/infinite_track/domain/model/attendance/TargetRangeStatus.kt` for range evidence independent of target identity.
- Create `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendancePreparationEligibility.kt` for one typed preparation outcome.
- Create `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaBookingForDate.kt` for not-requested/pending/rejected/approved lifecycle.
- Create `app/src/main/java/com/example/infinite_track/domain/use_case/booking/ResolveTodayWfaBookingStateUseCase.kt` for date-specific lifecycle lookup.
- Create `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveAuthoritativeTargetLocationUseCase.kt` for WFO/WFH/WFA authority.
- Create `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateTargetRangeUseCase.kt` for provider-neutral distance/range evaluation.
- Create `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateAttendancePreparationUseCase.kt` for readiness/recovery.
- Delete `app/src/main/java/com/example/infinite_track/domain/model/attendance/SelectedTargetLocation.kt` after all consumers migrate.
- Delete `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt` after all consumers migrate.
- Delete `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateWorkModeEligibilityUseCase.kt` after all consumers migrate.

### WFA data

- Modify `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt` to expose stable typed recommendation fields.
- Modify `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt` to preserve backend distance in meters and derive a stable preview key.
- Modify `app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt` to use suitability terminology and the new model.

### Presentation preparation

- Create `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationState.kt` for persistent preparation and WFA discovery state.
- Create `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiModel.kt` for render-only cards, target summary, recommendations, and actions.
- Create `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt` for typed domain-to-copy/action mapping.
- Create `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationReducer.kt` for pure preview-only recommendation/search selection updates.
- Create `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/LatestSelectionGuard.kt` for request identity.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt` so `preparation` is the only mutable target/preparation owner.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt` for cancellation, request identity, typed resolution, and semantic effects.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt` and `AttendanceCheckInRequestFactory.kt` to consume preparation truth.
- Modify `app/src/main/java/com/example/infinite_track/presentation/map/model/MapMarkerUiModel.kt` and `AttendanceMapUiMapper.kt` for the four locked marker roles.

### Attendance UI

- Replace `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WorkModeSelector.kt` with card-based options using the existing file path.
- Replace `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/LocationInfoRow.kt` with authoritative target summary composition using the existing file path.
- Create `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationOption.kt` for one compact recommendation row.
- Create `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationSection.kt` for loading/content/empty/failure/selected state.
- Modify `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt` and `AttendanceScreen.kt` to render the preparation UI model.

### Feedback and auth

- Modify `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteFeedbackTokens.kt` for central Bold title typography.
- Modify `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteFeedback.kt` for 4/8-second accessible timers and loading confirm action.
- Modify `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteSnackbar.kt` to support optional title plus message.
- Create `app/src/main/java/com/example/infinite_track/presentation/feedback/AppFeedback.kt` for semantic one-shot feedback and root mapping.
- Modify `app/src/main/java/com/example/infinite_track/presentation/main/MainActivity.kt` and `InfiniteTrackApp.kt` to host global feedback.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/auth/LoginViewModel.kt`, `LoginScreen.kt`, and `AppNavGraph.kt` for non-modal login.
- Modify `app/src/main/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCase.kt`, `ProfileViewModel.kt`, and `ProfileScreen.kt` for typed logout outcome and one confirmation modal.

---

### Task 1: Introduce the Authoritative Target Contract and Pure Resolver

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AuthoritativeTargetLocation.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaBookingForDate.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveAuthoritativeTargetLocationUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveAuthoritativeTargetLocationUseCaseTest.kt`

**Interfaces:**
- Consumes: `WorkMode`, `TodayStatus?`, `UserModel?`, `WfaBookingForDate`.
- Produces: `TargetLocationResolution` and non-null `AuthoritativeTargetLocation`.

- [ ] **Step 1: Write failing resolver tests**

Create tests for WFO source, WFH profile source, missing WFH, approved WFA, wrong-date WFA, and invalid radius. The first test must contain this contract:

```kotlin
@Test
fun `WFO resolves only from status today active location`() {
    val result = resolver(
        mode = WorkMode.WFO,
        todayStatus = todayStatus(activeLocation = office),
        profile = user(homeLatitude = -0.90, homeLongitude = 119.88, radius = 100),
        wfaBooking = WfaBookingForDate.NotRequested
    ) as TargetLocationResolution.Resolved

    assertEquals(TargetLocationSource.STATUS_TODAY, result.target.source)
    assertEquals(TargetLocationId("status:1"), result.target.targetId)
    assertEquals(office.coordinate, result.target.coordinate)
}

@Test
fun `recommendation type is not accepted by authoritative resolver`() {
    val parameters = ResolveAuthoritativeTargetLocationUseCase::class.java.methods
        .single { it.name == "invoke" }
        .parameterTypes

    assertFalse(parameters.any { it.simpleName == "WfaRecommendation" })
}
```

- [ ] **Step 2: Run the focused test and confirm the red state**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveAuthoritativeTargetLocationUseCaseTest' --console=plain
```

Expected: FAIL because the new contract and resolver do not exist.

- [ ] **Step 3: Implement the minimum typed contract and resolver**

Implement these exact public shapes:

```kotlin
@JvmInline
value class TargetLocationId(val value: String)

enum class TargetLocationSource { STATUS_TODAY, ADMIN_PROFILE, APPROVED_WFA_BOOKING }

data class ApprovedWfaTargetContext(val bookingId: Int, val scheduleDate: String)

data class AuthoritativeTargetLocation(
    val targetId: TargetLocationId,
    val mode: WorkMode,
    val source: TargetLocationSource,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val displayName: String,
    val approvedWfaContext: ApprovedWfaTargetContext? = null
)

sealed interface TargetLocationResolution {
    data class Resolving(val mode: WorkMode) : TargetLocationResolution
    data class Resolved(val target: AuthoritativeTargetLocation) : TargetLocationResolution
    data class Unavailable(
        val mode: WorkMode,
        val reason: TargetUnavailableReason,
        val recovery: TargetRecoveryAction
    ) : TargetLocationResolution
    data class Failed(val mode: WorkMode, val failure: TargetResolutionFailure) : TargetLocationResolution
}

enum class TargetUnavailableReason {
    WFO_NOT_ASSIGNED,
    WFH_PROFILE_CONTRACT_VIOLATION,
    WFA_NOT_REQUESTED,
    WFA_PENDING,
    WFA_REJECTED,
    WFA_APPROVAL_MISSING_FOR_DATE
}

enum class TargetRecoveryAction {
    REFRESH_STATUS,
    REFRESH_PROFILE,
    CONTACT_ADMIN,
    OPEN_WFA_BOOKING,
    OPEN_WFA_REQUESTS
}

enum class TargetResolutionFailure {
    STATUS_REFRESH_FAILED,
    PROFILE_REFRESH_FAILED,
    BOOKING_REFRESH_FAILED,
    INVALID_COORDINATE,
    INVALID_RADIUS
}
```

Use stable IDs `status:<locationId>`, `profile:<userId>`, and `booking:<bookingId>`. Reject a null/non-positive radius; do not default WFH or WFA radius to 100.

- [ ] **Step 4: Run the resolver tests and the previous resolver suite**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveAuthoritativeTargetLocationUseCaseTest' --tests '*ResolveSelectedTargetLocationUseCaseTest' --console=plain
```

Expected: new tests PASS; existing tests still PASS until migration removes the old resolver.

- [ ] **Step 5: Commit the contract**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/model/attendance/AuthoritativeTargetLocation.kt app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaBookingForDate.kt app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveAuthoritativeTargetLocationUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveAuthoritativeTargetLocationUseCaseTest.kt
git commit -m "feat: add authoritative attendance target contract"
```

---

### Task 2: Resolve WFA Booking Lifecycle and Align Recommendation Mapping

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/booking/ResolveTodayWfaBookingStateUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/booking/ResolveTodayWfaBookingStateUseCaseTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaMapperTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/model/location/ProviderNeutralLocationModelTest.kt`

**Interfaces:**
- Consumes: paged `BookingRepository` history and `RecommendationItem` DTO.
- Produces: `WfaBookingForDate` plus a stable `WfaRecommendation` preview model.

- [ ] **Step 1: Write failing booking and mapper tests**

Use these assertions:

```kotlin
@Test
fun `latest approved booking for attendance date preserves target evidence`() = runTest {
    val state = useCase("2026-07-23")
    val approved = state as WfaBookingForDate.Approved

    assertEquals(88, approved.booking.bookingId)
    assertEquals("2026-07-23", approved.booking.scheduleDateRaw)
    assertEquals(-0.89, approved.booking.latitude!!, 0.0)
}

@Test
fun `mapper preserves backend suitability and distance in meters`() {
    val result = recommendationItem(distanceFromCenter = 1250.0).toDomain()

    assertEquals(0.91, result.suitabilityScore, 0.0)
    assertEquals("Sangat sesuai", result.suitabilityLabel)
    assertEquals(1250.0, result.distanceMeters.value, 0.0)
    assertEquals("cafe@-0.900000,119.880000", result.stableKey)
}
```

Cover `NotRequested`, `Pending`, `Rejected`, `Approved`, and repository failure. When multiple records exist for the date, select the highest `bookingId` as the latest backend record.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveTodayWfaBookingStateUseCaseTest' --tests '*WfaMapperTest' --console=plain
```

Expected: FAIL because the lifecycle use case and aligned fields do not exist.

- [ ] **Step 3: Implement lifecycle lookup and typed recommendation fields**

Use this lifecycle:

```kotlin
sealed interface WfaBookingForDate {
    data object NotRequested : WfaBookingForDate
    data class Pending(val booking: BookingHistoryItem) : WfaBookingForDate
    data class Rejected(val booking: BookingHistoryItem) : WfaBookingForDate
    data class Approved(val booking: BookingHistoryItem) : WfaBookingForDate
    data class Failed(val cause: Throwable) : WfaBookingForDate
}
```

Use this recommendation contract and update all constructor/property call sites in `MarkerViewWfa.kt` and existing tests in the same step:

```kotlin
data class WfaRecommendation(
    val stableKey: String,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val category: String,
    val suitabilityScore: Double,
    val suitabilityLabel: String,
    val distanceMeters: DistanceMeters
)
```

Build `stableKey` from the normalized lowercase name plus coordinates formatted to six decimals: `"${name.trim().lowercase(Locale.ROOT)}@${String.format(Locale.US, "%.6f,%.6f", latitude, longitude)}"`. Do not map `scoreDetails` into the compact model and do not add an image URL.

- [ ] **Step 4: Run mapper, booking, and provider-neutral tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveTodayWfaBookingStateUseCaseTest' --tests '*WfaMapperTest' --tests '*ProviderNeutralLocationModelTest' --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit WFA contracts**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/model/wfa app/src/main/java/com/example/infinite_track/domain/use_case/booking/ResolveTodayWfaBookingStateUseCase.kt app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt app/src/test/java/com/example/infinite_track/domain/use_case/booking app/src/test/java/com/example/infinite_track/data/mapper/wfa app/src/test/java/com/example/infinite_track/domain/model/location/ProviderNeutralLocationModelTest.kt
git commit -m "refactor: type WFA booking and recommendation state"
```

---

### Task 3: Separate Range Evaluation from Preparation Eligibility

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/TargetRangeStatus.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendancePreparationEligibility.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateTargetRangeUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateAttendancePreparationUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/EvaluateTargetRangeUseCaseTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/EvaluateAttendancePreparationUseCaseTest.kt`

**Interfaces:**
- Consumes: `TargetLocationResolution`, `CurrentLocationResult?`, current epoch time.
- Produces: `TargetRangeStatus` and `AttendancePreparationEligibility` without UI copy.

- [ ] **Step 1: Write failing range and preparation matrix tests**

Cover inside, outside, unavailable current location, stale location older than 60 seconds, target unavailable, WFA lifecycle recovery, and ready state:

```kotlin
@Test
fun `outside range retains authoritative target`() {
    val status = rangeUseCase(
        target = target(radiusMeters = 100.0),
        current = currentLocation(latitude = -0.91, longitude = 119.89, capturedAt = NOW),
        nowEpochMillis = NOW
    ) as TargetRangeStatus.Outside

    assertTrue(status.distance.value > 100.0)
}

@Test
fun `WFH profile violation maps to refresh profile recovery`() {
    val result = preparationUseCase(
        resolution = TargetLocationResolution.Unavailable(
            WorkMode.WFH,
            TargetUnavailableReason.WFH_PROFILE_CONTRACT_VIOLATION,
            TargetRecoveryAction.REFRESH_PROFILE
        ),
        rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
    )

    assertEquals(
        AttendancePreparationEligibility.Blocked(
            AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT,
            AttendancePreparationRecovery.REFRESH_PROFILE
        ),
        result
    )
}
```

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*EvaluateTargetRangeUseCaseTest' --tests '*EvaluateAttendancePreparationUseCaseTest' --console=plain
```

Expected: FAIL because the models/use cases do not exist.

- [ ] **Step 3: Implement pure evaluation**

Define:

```kotlin
sealed interface TargetRangeStatus {
    data class Inside(val distance: DistanceMeters) : TargetRangeStatus
    data class Outside(val distance: DistanceMeters) : TargetRangeStatus
    data class Unknown(val reason: TargetRangeUnknownReason) : TargetRangeStatus
}

enum class TargetRangeUnknownReason { CURRENT_LOCATION_UNAVAILABLE, CURRENT_LOCATION_STALE }

sealed interface AttendancePreparationEligibility {
    data object Resolving : AttendancePreparationEligibility
    data class Ready(
        val target: AuthoritativeTargetLocation,
        val range: TargetRangeStatus.Inside
    ) : AttendancePreparationEligibility
    data class Blocked(
        val reason: AttendancePreparationBlockReason,
        val recovery: AttendancePreparationRecovery
    ) : AttendancePreparationEligibility
}

enum class AttendancePreparationRecovery {
    REFRESH_STATUS,
    REFRESH_PROFILE,
    REFRESH_LOCATION,
    FOCUS_TARGET,
    OPEN_WFA_BOOKING,
    OPEN_WFA_REQUESTS,
    CONTACT_ADMIN
}
```

Use a provider-neutral Haversine calculation. Treat `nowEpochMillis - capturedAtEpochMillis > 60_000L` as stale. Do not mutate or return a replacement target from range evaluation.

- [ ] **Step 4: Run focused tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*EvaluateTargetRangeUseCaseTest' --tests '*EvaluateAttendancePreparationUseCaseTest' --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit evaluation contracts**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/model/attendance app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateTargetRangeUseCase.kt app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateAttendancePreparationUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/attendance/EvaluateTargetRangeUseCaseTest.kt app/src/test/java/com/example/infinite_track/domain/use_case/attendance/EvaluateAttendancePreparationUseCaseTest.kt
git commit -m "feat: evaluate attendance preparation and range"
```

---

### Task 4: Add Cohesive Preparation State and Provider-Neutral UI Projection

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiModel.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/LatestSelectionGuard.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/model/MapMarkerUiModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapper.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapperTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/LatestSelectionGuardTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapperTest.kt`

**Interfaces:**
- Consumes: typed target resolution, range, WFA discovery, and eligibility.
- Produces: one persistent `AttendancePreparationState`, one render-only model, and `MapUiState`.

- [ ] **Step 1: Write failing projection and request-identity tests**

Prove one selected mode, one primary action, WFA-only secondary search, separate markers, and stale request rejection:

```kotlin
@Test
fun `WFA recommendation preview never replaces authoritative marker`() {
    val mapped = AttendanceMapUiMapper.map(preparationWithApprovedTargetAndPreview(), true)

    assertEquals(1, mapped.markers.count { it.role == MapMarkerRole.AUTHORITATIVE_TARGET })
    assertEquals(1, mapped.markers.count { it.role == MapMarkerRole.WFA_RECOMMENDATION })
    assertEquals(approvedTarget.coordinate, mapped.markers.single {
        it.role == MapMarkerRole.AUTHORITATIVE_TARGET
    }.coordinate)
}

@Test
fun `guard rejects result from previous mode selection`() {
    val guard = LatestSelectionGuard()
    val wfaRequest = guard.next(WorkMode.WFA)
    guard.next(WorkMode.WFO)

    assertFalse(guard.isCurrent(wfaRequest, WorkMode.WFA))
}
```

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePreparationUiMapperTest' --tests '*LatestSelectionGuardTest' --tests '*AttendanceMapUiMapperTest' --console=plain
```

Expected: FAIL on missing preparation contracts and marker roles.

- [ ] **Step 3: Implement state, render models, copy mapper, and map projection**

Use these core shapes:

```kotlin
data class AttendancePreparationState(
    val selectedMode: WorkMode = WorkMode.WFO,
    val targetResolution: TargetLocationResolution = TargetLocationResolution.Resolving(WorkMode.WFO),
    val currentLocation: CurrentLocationResult? = null,
    val rangeStatus: TargetRangeStatus? = null,
    val wfaDiscovery: WfaDiscoveryState = WfaDiscoveryState.Hidden,
    val eligibility: AttendancePreparationEligibility = AttendancePreparationEligibility.Resolving
)

sealed interface WfaDiscoveryState {
    data object Hidden : WfaDiscoveryState
    data object Loading : WfaDiscoveryState
    data object Empty : WfaDiscoveryState
    data class Failure(val retryable: Boolean = true) : WfaDiscoveryState
    data class Content(
        val recommendations: List<WfaRecommendation>,
        val selectedKey: String? = null,
        val searchPreview: LocationResult? = null
    ) : WfaDiscoveryState
}

enum class MapMarkerRole {
    CURRENT_LOCATION,
    AUTHORITATIVE_TARGET,
    WFA_RECOMMENDATION,
    SEARCH_PREVIEW
}

```

Change `AttendanceMapUiMapper.map` to accept exactly `preparation: AttendancePreparationState` and `hasPreciseLocationPermission: Boolean`; remove its `AttendanceScreenState` parameter. Project the authoritative marker/circle only from `TargetLocationResolution.Resolved`, recommendation markers from `WfaDiscoveryState.Content.recommendations`, and the search marker from `Content.searchPreview`.

`AttendancePreparationUiMapper` must map typed reasons to Indonesian copy and an `AttendancePreparationPrimaryAction` enum. It must produce exactly one `primaryAction` and at most one WFA-only `secondaryAction`.

- [ ] **Step 4: Run focused tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePreparationUiMapperTest' --tests '*LatestSelectionGuardTest' --tests '*AttendanceMapUiMapperTest' --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit presentation contracts**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation app/src/main/java/com/example/infinite_track/presentation/map app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation app/src/test/java/com/example/infinite_track/presentation/map/mapper/AttendanceMapUiMapperTest.kt
git commit -m "feat: add cohesive attendance preparation state"
```

---

### Task 5: Migrate AttendanceViewModel with Latest-Selection-Wins

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationReducer.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceCheckInRequestFactory.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendancePreparationReducerTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolverTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceCheckInRequestFactoryTest.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenFaceResultRescueTest.kt`

**Interfaces:**
- Consumes: Tasks 1-4 contracts and current repositories/use cases.
- Produces: an `AttendanceScreenState` whose single preparation owner is `preparation: AttendancePreparationState`, semantic camera/navigation effects, and a check-in request derived from the resolved target.

- [ ] **Step 1: Write failing reducer, stale-selection, checkout, and request tests**

Add tests proving:

```kotlin
@Test
fun `recommendation selection changes preview only`() {
    val next = reducer.selectRecommendation(stateWithApprovedTarget(), recommendation)

    assertEquals(approvedTarget, (next.targetResolution as Resolved).target)
    assertEquals(recommendation.stableKey, (next.wfaDiscovery as Content).selectedKey)
}

@Test
fun `WFA request uses approved booking id from target context`() {
    val request = AttendanceCheckInRequestFactory.create(
        workMode = WorkMode.WFA,
        authoritativeTarget = approvedWfaTarget(bookingId = 88)
    )

    assertEquals(88, request.bookingId)
}

@Test
fun `checkout readiness ignores selected work mode`() {
    val state = checkedInState(preparation = unresolvedWfaPreparation())
    assertTrue(AttendanceActionResolver.resolve(state) is AttendanceActionState.Ready)
}
```

Extend the instrumentation fixture with a controllable fake resolver so WFA can complete after WFO and prove WFO remains selected.

- [ ] **Step 2: Run focused unit tests and Android test compilation**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePreparationReducerTest' --tests '*AttendanceActionResolverTest' --tests '*AttendanceCheckInRequestFactoryTest' app:compileDebugAndroidTestKotlin --console=plain
```

Expected: FAIL until the ViewModel/state migration is implemented.

- [ ] **Step 3: Migrate state and orchestration**

Make `AttendanceScreenState` own:

```kotlin
val preparation: AttendancePreparationState = AttendancePreparationState()
```

In `AttendanceViewModel`, add separate `modeResolutionJob` and `recommendationJob`, cancel both on mode changes, call `LatestSelectionGuard.next(mode)`, and check `isCurrent` before every async state write. Replace `onLocationSelected` and `applyPickedLocation` so they update `WfaDiscoveryState.Content.searchPreview`; they must never construct an authoritative target.

Implement the pure reducer with this exact surface:

```kotlin
object AttendancePreparationReducer {
    fun selectRecommendation(
        state: AttendancePreparationState,
        recommendation: WfaRecommendation
    ): AttendancePreparationState

    fun selectSearchPreview(
        state: AttendancePreparationState,
        preview: LocationResult
    ): AttendancePreparationState
}
```

Both functions may update only `wfaDiscovery`; they must preserve `selectedMode`, `targetResolution`, `currentLocation`, `rangeStatus`, and `eligibility` unchanged.

Replace the WFA check-in booking re-query with:

```kotlin
val bookingId = resolvedTarget.approvedWfaContext?.bookingId
```

Keep checkout on the active backend session path. Emit semantic `MapCameraEffect` only after a current request passes the guard.

- [ ] **Step 4: Run preparation, action, request, and Android compilation tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePreparation*' --tests '*AttendanceActionResolverTest' --tests '*AttendanceCheckInRequestFactoryTest' app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit orchestration**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenFaceResultRescueTest.kt
git commit -m "refactor: make preparation state authoritative"
```

---

### Task 6: Build the Work Mode, Target Summary, and WFA Recommendation UI

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WorkModeSelector.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/LocationInfoRow.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationOption.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/WfaRecommendationSection.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt`

**Interfaces:**
- Consumes: `AttendancePreparationUiModel` and event callbacks only.
- Produces: reference-aligned bottom-sheet presentation with no business rules.

- [ ] **Step 1: Write failing Compose tests for the visible state matrix**

Test these exact semantics/copy rules:

```kotlin
@Test
fun workModeCardsExposeOneSelectedModeAndWfaOnlySearch() {
    composeRule.setContent {
        WorkModePreparationContent(model = wfaReadyUiModel(), onEvent = {})
    }

    composeRule.onNodeWithText("Work From Office").assertIsDisplayed()
    composeRule.onNodeWithText("Work From Home").assertIsDisplayed()
    composeRule.onNodeWithText("Work From Anywhere").assertIsDisplayed()
    composeRule.onAllNodes(hasStateDescription("Dipilih")).assertCountEquals(1)
    composeRule.onNodeWithText("Cari lokasi WFA").assertIsDisplayed()
}

@Test
fun WFHHasAdminCopyAndNoSearchOrEditAction() {
    composeRule.setContent {
        WorkModePreparationContent(model = wfhReadyUiModel(), onEvent = {})
    }

    composeRule.onNodeWithText("Ditetapkan oleh admin").assertIsDisplayed()
    composeRule.onNodeWithText("Cari lokasi WFA").assertDoesNotExist()
    composeRule.onNodeWithText("Ubah lokasi").assertDoesNotExist()
}
```

Also cover WFA loading/content/empty/failure/selected, target source/radius/distance, one primary action, 320 dp width, and font scale 2.0.

- [ ] **Step 2: Compile/run the focused Compose test and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
```

When an emulator is available, run:

```powershell
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.WorkModeTargetLocationScreenTest --console=plain
```

Expected before implementation: compile/test failure on missing UI compositions.

- [ ] **Step 3: Implement reference-aligned compositions using existing primitives**

Use `InfiniteCard` with `InfiniteSurfaceVariant.Default` or `Outlined` so the cards contain no gradient. Use `InfiniteStatusPill`, `InfiniteButton`, `InfiniteIconButton`, existing Material icons, and existing typography. Selected state must combine border, light tint, check icon, and semantics.

Use these render-only entry points:

```kotlin
@Composable
fun WorkModeSelector(
    options: List<WorkModeOptionUiModel>,
    onModeSelected: (WorkMode) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun TargetLocationSummary(
    model: TargetLocationSummaryUiModel,
    modifier: Modifier = Modifier
)

@Composable
fun WfaRecommendationOption(
    model: WfaRecommendationUiModel,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
)
```

`AttendanceBottomSheetContent` receives one `AttendancePreparationUiModel` and one `onEvent(AttendancePreparationEvent)` callback. Search/Pick appears only for WFA. Do not render venue images or a rating star.

Keep the existing `AttendanceScreen` app bar, map viewport, and bottom-sheet drag/peek mechanics unchanged; this task replaces only the content inside the existing bottom sheet and the models/events feeding it.

- [ ] **Step 4: Compile Android tests and run the focused UI test when a device exists**

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS. Connected test must PASS when the configured device/emulator is online; otherwise record it as runtime verification pending.

- [ ] **Step 5: Commit Attendance UI**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/components/button/attendance app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt
git commit -m "feat: redesign work mode and WFA preparation UI"
```

---

### Task 7: Finish the Shared Feedback Component Contract

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteFeedbackTokens.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteFeedback.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteSnackbar.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/status/InfiniteTrackConfirmDialog.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/design/tokens/InfiniteFeedbackTokensTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/design/components/status/InfiniteInlineAlertPolicyTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/design/components/status/InfiniteSnackbarVisualsTest.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/design/components/status/InfiniteStateStatusComponentsTest.kt`

**Interfaces:**
- Consumes: existing Infinite typography, surface, motion, and semantic tokens.
- Produces: Bold titles, accessible 4/8-second transient policy, persistent recovery, titled snackbar, and loading confirm action.

- [ ] **Step 1: Change tests first**

Lock the policy:

```kotlin
@Test
fun `transient semantic durations are four and eight seconds`() {
    assertEquals(4_000, InfiniteInlineAlertDuration.Short.timeoutMillis)
    assertEquals(8_000, InfiniteInlineAlertDuration.Long.timeoutMillis)
    assertEquals(InfiniteInlineAlertDuration.Long, InfiniteSemantic.Warning.defaultInlineAlertDuration())
    assertEquals(InfiniteInlineAlertDuration.Long, InfiniteSemantic.Error.defaultInlineAlertDuration())
    assertEquals(
        InfiniteInlineAlertDuration.Persistent,
        InfiniteSemantic.Warning.defaultInlineAlertDuration(hasAction = true)
    )
}

@Test
fun `inline and snackbar title tokens use body1 metrics with bold weight`() {
    assertEquals(body1.copy(fontWeight = FontWeight.Bold), InfiniteFeedbackTypography.inlineTitle)
    assertEquals(body1.copy(fontWeight = FontWeight.Bold), InfiniteFeedbackTypography.snackbarTitle)
}
```

Compose tests must assert a transient Warning renders `INLINE_ALERT_TIMER_TAG` and `Dismiss alert`, then disappears after 8,250 ms with one callback. Persistent actionable recovery must render neither timer nor dismiss X.

- [ ] **Step 2: Run feedback unit tests and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*InfiniteFeedbackTokensTest' --tests '*InfiniteInlineAlertPolicyTest' --tests '*InfiniteSnackbarVisualsTest' --console=plain
```

Expected: FAIL because warning/error are currently persistent and titles are Medium.

- [ ] **Step 3: Implement central typography, accessible timers, snackbar title, and loading confirmation**

Set:

```kotlin
val inlineTitle = body1.copy(fontWeight = FontWeight.Bold)
val snackbarTitle = body1.copy(fontWeight = FontWeight.Bold)
```

Obtain the actual transient timeout before animating the timer with:

```kotlin
val actualTimeoutMillis = LocalAccessibilityManager.current
    ?.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = duration.timeoutMillis,
        containsIcons = true,
        containsText = true,
        containsControls = onDismiss != null
    )
    ?: duration.timeoutMillis
```

Keep `hasAction = true` persistent. Extend `InfiniteSnackbarVisuals` with `title: String?` and render title/body with the shared tokens.

Extend confirm dialog wrappers with:

```kotlin
confirmLoading: Boolean = false
```

When loading, disable dismiss/confirm and render an 18 dp progress indicator inside the confirm button. Do not add another dialog.

- [ ] **Step 4: Run feedback tests and Android test compilation**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*InfiniteFeedback*' --tests '*InfiniteInlineAlertPolicyTest' --tests '*InfiniteSnackbarVisualsTest' app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit feedback components**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/design app/src/main/java/com/example/infinite_track/presentation/components/status app/src/test/java/com/example/infinite_track/presentation/design app/src/androidTest/java/com/example/infinite_track/presentation/design/components/status
git commit -m "feat: finish timed state feedback components"
```

---

### Task 8: Add Root Semantic Feedback and Replace Login Popups

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/feedback/AppFeedback.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/main/MainActivity.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/main/InfiniteTrackApp.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/auth/LoginViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/auth/LoginScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/AppNavGraph.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/feedback/AppFeedbackMapperTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/auth/LoginViewModelTest.kt`

**Interfaces:**
- Consumes: semantic `AppFeedbackEvent` from auth ViewModels.
- Produces: replay-zero root feedback plus `LoginEffect.NavigateHome`.

- [ ] **Step 1: Write failing feedback mapper and LoginViewModel tests**

Use:

```kotlin
@Test
fun `login success emits navigation and semantic feedback once`() = runTest {
    val feedback = FakeAppFeedbackController()
    val viewModel = createViewModel(loginResult = Result.success(user()), feedback = feedback)
    val navigation = async { viewModel.effects.first() }

    viewModel.login("user@example.com", "password")
    advanceUntilIdle()

    assertEquals(listOf(AppFeedbackEvent.LOGIN_SUCCESS), feedback.events)
    assertEquals(LoginEffect.NavigateHome, navigation.await())
    assertEquals(LoginUiState.Idle, viewModel.uiState.value)
}

@Test
fun `login failure stays inline and emits no root snackbar`() = runTest {
    val viewModel = createViewModel(loginResult = Result.failure(Exception("Invalid credentials")))
    viewModel.login("user@example.com", "bad")
    advanceUntilIdle()

    assertEquals(LoginUiState.Failure("Invalid credentials"), viewModel.uiState.value)
}
```

Mapper tests lock title, message, semantic, and `SnackbarDuration.Short` for login success.

- [ ] **Step 2: Run focused tests and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AppFeedbackMapperTest' --tests '*LoginViewModelTest' --console=plain
```

Expected: FAIL because app feedback and effect contracts do not exist.

- [ ] **Step 3: Implement root feedback and non-modal Login**

Create:

```kotlin
enum class AppFeedbackEvent { LOGIN_SUCCESS, LOGOUT_SUCCESS, LOGOUT_REMOTE_WARNING }

interface AppFeedbackEmitter {
    fun emit(event: AppFeedbackEvent)
}

@Singleton
class AppFeedbackController @Inject constructor() : AppFeedbackEmitter {
    private val _events = MutableSharedFlow<AppFeedbackEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<AppFeedbackEvent> = _events.asSharedFlow()
    override fun emit(event: AppFeedbackEvent) { _events.tryEmit(event) }
}

sealed interface LoginEffect { data object NavigateHome : LoginEffect }
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Failure(val message: String) : LoginUiState
}
```

Inject `AppFeedbackController` into `MainActivity` and pass it to `InfiniteTrackApp`. Collect events once at the root, map to `InfiniteSnackbarVisuals`, and render one `InfiniteSnackbarHost` with safe drawing/IME padding.

In Login, replace `InfiniteTrackButton` with `InfiniteButton(state = Loading)` and replace status dialogs with `InfiniteTrackInlineAlert` for failure. Delete `LoginLoadingDialog`, `LoginStatusDialog`, and the `Complete your Profile` popup. Collect `LoginEffect.NavigateHome` once and navigate through the existing callback.

- [ ] **Step 4: Run Login/feedback tests and compile Android tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AppFeedbackMapperTest' --tests '*LoginViewModel*Test' app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit Login feedback**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/feedback app/src/main/java/com/example/infinite_track/presentation/main app/src/main/java/com/example/infinite_track/presentation/screen/auth app/src/main/java/com/example/infinite_track/presentation/navigation/AppNavGraph.kt app/src/test/java/com/example/infinite_track/presentation/feedback app/src/test/java/com/example/infinite_track/presentation/screen/auth
git commit -m "feat: replace login popups with transient feedback"
```

---

### Task 9: Make Manual Logout Typed, Non-Modal, and Re-auth Safe

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/manager/SessionManager.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileScreen.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCaseTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/manager/SessionManagerTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/profile/ProfileViewModelLogoutTest.kt`

**Interfaces:**
- Consumes: best-effort remote logout, mandatory local runtime cleanup, `SessionManager`, root `AppFeedbackEmitter`.
- Produces: typed `LogoutOutcome`, `ProfileEffect.NavigateToLogin`, and persistent local-cleanup failure.

- [ ] **Step 1: Write failing logout outcome and stale re-auth tests**

Lock these outcomes:

```kotlin
@Test
fun `remote failure plus local success clears reauth and returns warning`() = runTest {
    sessionManager.triggerForcedReauth(ReauthReason.UNKNOWN)
    val outcome = useCase(remoteResult = Result.failure(Exception("offline")))()

    assertEquals(LogoutOutcome.SuccessWithRemoteWarning, outcome)
    assertNull(sessionManager.reauthReason.value)
    assertFalse(sessionManager.sessionExpired.value)
}

@Test
fun `local cleanup failure does not navigate or claim logout`() = runTest {
    val effects = mutableListOf<ProfileEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effects.collect(effects::add)
    }

    viewModel.confirmLogout()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value is ProfileLogoutUiState.LocalCleanupFailure)
    assertTrue(effects.isEmpty())
    assertTrue(feedback.events.isEmpty())
}
```

Also prove cancellation is rethrown and success/warning navigation effects are emitted once.

- [ ] **Step 2: Run focused auth/profile tests and confirm failure**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*LogoutUseCaseTest' --tests '*SessionManagerTest' --tests '*ProfileViewModelLogoutTest' --console=plain
```

Expected: FAIL because typed logout outcome and profile effect do not exist.

- [ ] **Step 3: Implement typed outcome and one-modal Profile flow**

Use:

```kotlin
sealed interface LogoutOutcome {
    data object Success : LogoutOutcome
    data object SuccessWithRemoteWarning : LogoutOutcome
    data class LocalCleanupFailed(val cause: Throwable) : LogoutOutcome
}
```

`LogoutUseCase` must:

1. store `authRepository.logoutRemote()` result;
2. always attempt local cleanup unless cancelled;
3. return `LocalCleanupFailed` without navigating when local cleanup fails;
4. call `sessionManager.resetSessionExpired()` after successful local cleanup;
5. return Success or SuccessWithRemoteWarning based on the stored remote result.

`ProfileViewModel` emits root `LOGOUT_SUCCESS` or `LOGOUT_REMOTE_WARNING`, then emits `ProfileEffect.NavigateToLogin`. `ProfileScreen` collects the effect and navigates immediately. Keep `InfiniteTrackConfirmDialog`, pass `confirmLoading = true` while submitting, remove the artificial 2-second delay, `ProfileLoadingDialog`, and result status dialogs. Render local cleanup failure as a persistent inline recovery with Retry.

- [ ] **Step 4: Run logout/profile tests and Android compilation**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*LogoutUseCaseTest' --tests '*SessionManagerTest' --tests '*ProfileViewModelLogoutTest' --tests '*LoginViewModelReauthTest' app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS; existing forced re-auth tests still PASS.

- [ ] **Step 5: Commit logout feedback**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCase.kt app/src/main/java/com/example/infinite_track/domain/manager/SessionManager.kt app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt app/src/main/java/com/example/infinite_track/presentation/screen/profile app/src/test/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCaseTest.kt app/src/test/java/com/example/infinite_track/domain/manager/SessionManagerTest.kt app/src/test/java/com/example/infinite_track/presentation/screen/profile/ProfileViewModelLogoutTest.kt
git commit -m "fix: separate manual logout from forced reauth"
```

---

### Task 10: Remove Legacy Target State and Complete Integration Verification

**Files:**
- Delete: `app/src/main/java/com/example/infinite_track/domain/model/attendance/SelectedTargetLocation.kt`
- Delete: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt`
- Delete: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/EvaluateWorkModeEligibilityUseCase.kt`
- Delete: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCaseTest.kt`
- Delete: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/EvaluateWorkModeEligibilityUseCaseTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendancePreparationStateOwnershipTest.kt`
- Create: `docs/linear-sync/INF-238-runtime-verification.md`

**Interfaces:**
- Consumes: completed contracts and UI from Tasks 1-9.
- Produces: no mutable legacy target copies and a verified debug build.

- [ ] **Step 1: Add a failing ownership regression test and source scan**

The unit test must verify `AttendanceScreenState` exposes `preparation` and does not expose these properties:

```kotlin
@Test
fun `screen state has one preparation owner`() {
    val names = AttendanceScreenState::class.members.map { it.name }.toSet()

    assertTrue("preparation" in names)
    assertTrue(
        setOf(
            "targetLocation",
            "wfoLocation",
            "wfhLocation",
            "approvedWfaLocation",
            "selectedTargetLocation",
            "targetLocationMarker",
            "selectedWfaLocation",
            "selectedWfaMarkerInfo",
            "pickedLocation"
        ).none(names::contains)
    )
}
```

Run this source scan and fail only when a legacy reference remains:

```powershell
$legacyHits = rg -n "SelectedTargetLocation|ResolveSelectedTargetLocationUseCase|EvaluateWorkModeEligibilityUseCase" app/src/main app/src/test app/src/androidTest
if ($LASTEXITCODE -eq 0) { $legacyHits; throw "Legacy target contract references remain" }
if ($LASTEXITCODE -gt 1) { throw "Legacy target contract scan failed" }
```

- [ ] **Step 2: Run the ownership test and confirm failure before deletion**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*AttendancePreparationStateOwnershipTest' --console=plain
```

Expected: FAIL while legacy properties still exist.

- [ ] **Step 3: Delete legacy contracts and remove all compatibility writes**

Remove the listed legacy fields and files. Update previews/tests to construct `AttendancePreparationState` and typed resolution. Remove the mutable `buttonText`, `isButtonEnabled`, and `isCheckInMode` constructor properties; update every consumer to read `actionState.ctaLabel`, `actionState.isCtaEnabled`, and `actionState.legacyIsCheckInMode` respectively.

Create `docs/linear-sync/INF-238-runtime-verification.md` with explicit rows for WFO, WFH, WFA lifecycle, recommendation row-marker sync, rapid switching, login, logout, timer, large font, and narrow screen. Each row uses `PASS`, `FAIL`, or `NEEDS DEVICE`; it must not mark an unexecuted runtime case as PASS.

- [ ] **Step 4: Run the full verification gate**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest app:compileDebugAndroidTestKotlin app:lintDebug app:assembleDebug --console=plain
git diff --check
$legacyHits = rg -n "SelectedTargetLocation|ResolveSelectedTargetLocationUseCase|EvaluateWorkModeEligibilityUseCase" app/src/main app/src/test app/src/androidTest
if ($LASTEXITCODE -eq 0) { $legacyHits; throw "Legacy target contract references remain" }
if ($LASTEXITCODE -gt 1) { throw "Legacy target contract scan failed" }
```

Expected:

- Gradle exits 0 with `BUILD SUCCESSFUL`.
- `git diff --check` prints nothing.
- the legacy-contract source scan prints nothing.
- unrelated existing compiler/deprecation warnings may remain documented baseline warnings.

When a device/emulator is available, additionally run:

```powershell
adb devices -l
.\gradlew.bat --no-daemon app:connectedDebugAndroidTest --console=plain
```

Update the runtime verification document with evidence from the actual run.

- [ ] **Step 5: Commit cleanup and verification evidence**

```powershell
git add -A app/src/main app/src/test app/src/androidTest docs/linear-sync/INF-238-runtime-verification.md
git commit -m "refactor: complete INF-238 preparation migration"
```

---

## Final Review Checklist

- [ ] Compare every acceptance criterion in `docs/superpowers/specs/2026-07-23-inf-238-work-mode-feedback-ux-design.md` to a task and test above.
- [ ] Confirm `local.properties`, `google-services.json`, keystores, and API keys are not staged.
- [ ] Confirm Gradle wrapper remains unchanged.
- [ ] Confirm all WFA search/recommendation/pick flows feed booking draft or preview state only.
- [ ] Confirm authoritative map marker/radius comes only from `TargetLocationResolution.Resolved`.
- [ ] Confirm WFH exposes no employee edit/search action.
- [ ] Confirm checkout does not require mode reselection.
- [ ] Confirm login/logout result dialogs and artificial logout delay are gone.
- [ ] Confirm manual logout clears stale re-auth state while genuine forced re-auth still works.
- [ ] Confirm transient feedback shows title, X, and progress timer with the locked 4/8-second policy.
- [ ] Confirm persistent recovery has no X/timer and stays visible until recovery.
- [ ] Confirm `git status --short` contains only intended source, test, and evidence files before publishing.

## Execution Order

Execute Tasks 1-10 sequentially. Tasks 1-6 form the Attendance authority and UI chain; Tasks 7-9 finish the shared feedback/auth chain; Task 10 removes compatibility state only after every consumer has migrated. Do not squash intermediate test evidence while implementing; each task ends with its own reviewable commit.
