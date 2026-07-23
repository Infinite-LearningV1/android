# INF-238 Work Mode and Target Location Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the existing Attendance bottom sheet around provider-neutral Work Mode, authoritative Target Location, and backend-driven WFA recommendations without changing bottom-sheet mechanics or attendance business semantics.

**Architecture:** Build on INF-140 provider-neutral location contracts. Keep target resolution and eligibility in domain/use cases, persistent state in `AttendancePreparationViewModel`, map rendering in presentation adapters, and navigation/provider execution in the Route. Reuse existing Infinite design-system components and derive recommendation UI only from typed backend fields.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Coroutines, StateFlow, Hilt, Retrofit, JUnit, Turbine, Compose UI tests, existing Infinite Track design system.

## Global Constraints

- Base all work on `develop` after INF-140 / GitHub #94 is implemented and verified.
- Preserve existing Attendance bottom-sheet behavior.
- WFO target source is `status-today.active_location`.
- WFH target source is `/api/auth/me`; it is mandatory and admin-provisioned.
- WFA target source is an approved booking for the Attendance date.
- WFA recommendations and marker selections remain preview/request state.
- Do not add venue photos, image dependencies, or assumed backend fields.
- Reuse existing Infinite components before creating feature wrappers.
- Domain and ViewModel must contain no Mapbox or Google Maps SDK types.
- `SelectedTargetLocation` remains the only authoritative downstream target.
- One state exposes one primary action.
- Use TDD and commit after every task.

---

## File Structure

### Domain and mapping

- Modify: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/attendance/WorkModeEligibility.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveAttendancePreparationUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt`
- Modify the existing authenticated-profile mapper/repository path that consumes `ApiService.getUserProfile()`.

### Presentation state and mapper

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEvent.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEffect.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- Create or complete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`

### Reusable feature compositions

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WorkModeOptionCard.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/TargetLocationSummary.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WfaRecommendationOption.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WfaRecommendationSection.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/maps/AttendanceMap.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt`

### Tests

- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCaseTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveAttendancePreparationUseCaseTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapperTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModelTest.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt`

---

### Task 1: Lock Authoritative Target Sources in Domain

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCase.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCaseTest.kt`

**Interfaces:**
- Consumes: provider-neutral `GeoCoordinate`, WFO `active_location`, authenticated profile WFH location, approved WFA booking.
- Produces: `SelectedTargetLocation` with explicit source and target identity.

- [ ] **Step 1: Write failing source-resolution tests**

Add tests proving:

```kotlin
@Test
fun `WFO uses status today active location`() = runTest {
    val result = useCase.resolve(
        mode = WorkMode.WFO,
        todayStatus = todayStatus(activeLocation = officeLocation),
        profile = profile(homeLocation = homeLocation),
        approvedWfaBooking = null
    )

    assertEquals(TargetLocationSource.STATUS_TODAY, result.getOrThrow().source)
    assertEquals(officeLocation.id, result.getOrThrow().targetId)
}

@Test
fun `WFH uses admin provisioned profile location from me`() = runTest {
    val result = useCase.resolve(
        mode = WorkMode.WFH,
        todayStatus = todayStatus(activeLocation = officeLocation),
        profile = profile(homeLocation = homeLocation),
        approvedWfaBooking = null
    )

    assertEquals(TargetLocationSource.PROFILE_ADMIN_ASSIGNED, result.getOrThrow().source)
    assertEquals(homeLocation.coordinate, result.getOrThrow().coordinate)
}

@Test
fun `WFH missing profile target is contract failure`() = runTest {
    val result = useCase.resolve(
        mode = WorkMode.WFH,
        todayStatus = todayStatus(activeLocation = officeLocation),
        profile = profile(homeLocation = null),
        approvedWfaBooking = null
    )

    assertEquals(TargetResolutionFailure.WfhProfileTargetMissing, result.exceptionOrNull())
}

@Test
fun `WFA requires approved booking for attendance date`() = runTest {
    val result = useCase.resolve(
        mode = WorkMode.WFA,
        todayStatus = todayStatus(activeLocation = officeLocation),
        profile = profile(homeLocation = homeLocation),
        approvedWfaBooking = approvedBooking
    )

    assertEquals(TargetLocationSource.APPROVED_WFA_BOOKING, result.getOrThrow().source)
    assertEquals(approvedBooking.bookingId.toString(), result.getOrThrow().targetId)
}
```

- [ ] **Step 2: Run the focused tests and verify failure**

Run:

```bash
./gradlew app:testDebugUnitTest --tests '*ResolveSelectedTargetLocationUseCaseTest'
```

Expected: FAIL because explicit target sources and WFH contract failure are not fully implemented.

- [ ] **Step 3: Implement minimal typed source and failure contracts**

Introduce or align:

```kotlin
enum class TargetLocationSource {
    STATUS_TODAY,
    PROFILE_ADMIN_ASSIGNED,
    APPROVED_WFA_BOOKING
}

sealed interface TargetResolutionFailure {
    data object WfoTargetUnavailable : TargetResolutionFailure
    data object WfhProfileTargetMissing : TargetResolutionFailure
    data object WfhProfileTargetInvalid : TargetResolutionFailure
    data object WfaApprovedBookingMissing : TargetResolutionFailure
}
```

Ensure `SelectedTargetLocation` contains project-owned coordinate, radius, description, target ID, mode, and source.

- [ ] **Step 4: Run focused tests and verify pass**

Run the same Gradle test command.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveSelectedTargetLocationUseCaseTest.kt
git commit -m "feat: lock authoritative work mode target sources"
```

---

### Task 2: Create Typed Attendance Preparation Evaluation

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveAttendancePreparationUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/attendance/WorkModeEligibility.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveAttendancePreparationUseCaseTest.kt`

**Interfaces:**
- Consumes: selected mode, selected authoritative target, current location state, WFA booking state.
- Produces: one typed preparation result with one recovery action.

- [ ] **Step 1: Write failing preparation matrix tests**

Cover at minimum:

```kotlin
@Test fun `WFO inside range is ready`()
@Test fun `WFO outside range requests focus target`()
@Test fun `WFH missing profile target requests refresh profile`()
@Test fun `WFA not requested requests booking`()
@Test fun `WFA pending requests view status`()
@Test fun `WFA approved inside range is ready`()
@Test fun `current location unavailable requests refresh location`()
```

Expected result shape:

```kotlin
sealed interface AttendancePreparationResult {
    data class Ready(
        val mode: WorkMode,
        val target: SelectedTargetLocation,
        val range: RangeEvidence
    ) : AttendancePreparationResult

    data class Blocked(
        val reason: AttendancePreparationBlockReason,
        val recovery: AttendancePreparationRecovery
    ) : AttendancePreparationResult
}
```

- [ ] **Step 2: Run the focused tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*ResolveAttendancePreparationUseCaseTest'
```

Expected: FAIL because the evaluator does not exist.

- [ ] **Step 3: Implement the evaluator without UI copy**

Implement typed reasons and recoveries:

```kotlin
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

Do not place strings, colors, icons, or navigation routes in domain.

- [ ] **Step 4: Run focused tests and verify pass**

Run the same Gradle test command.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain app/src/test/java/com/example/infinite_track/domain/use_case/attendance/ResolveAttendancePreparationUseCaseTest.kt
git commit -m "feat: add typed attendance preparation evaluation"
```

---

### Task 3: Align WFA Recommendation Mapping with Backend-Supported Fields

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/model/wfa/WfaModels.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/mapper/wfa/WfaMapper.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/network/response/WfaRecommendationResponse.kt`
- Test: create or extend `app/src/test/java/com/example/infinite_track/data/mapper/wfa/WfaMapperTest.kt`

**Interfaces:**
- Consumes: backend recommendation DTO from `/api/wfa/recommendations`.
- Produces: stable project-owned recommendation model for compact card and marker preview.

- [ ] **Step 1: Write failing mapper tests**

Assert mapping for:

```kotlin
@Test
fun `maps backend recommendation fields without image assumptions`() {
    val result = dto.toDomain()

    assertEquals(dto.name, result.name)
    assertEquals(dto.address, result.address)
    assertEquals(dto.category, result.category)
    assertEquals(dto.suitabilityScore, result.suitabilityScore)
    assertEquals(dto.suitabilityLabel, result.suitabilityLabel)
    assertEquals(dto.distanceFromCenter, result.distanceMeters)
    assertEquals(GeoCoordinate(dto.latitude, dto.longitude), result.coordinate)
}
```

Also assert that no `imageUrl`, public rating, or provider DTO exists in the domain model.

- [ ] **Step 2: Run mapper tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*WfaMapperTest'
```

Expected: FAIL until names/types are aligned.

- [ ] **Step 3: Implement the compact domain model**

Use:

```kotlin
data class WfaRecommendation(
    val stableKey: String,
    val name: String,
    val address: String,
    val coordinate: GeoCoordinate,
    val category: String,
    val suitabilityScore: Double,
    val suitabilityLabel: String,
    val distanceMeters: Double
)
```

Derive `stableKey` deterministically from normalized name and coordinate until the backend provides a stable place ID. Document that it is UI-selection identity only.

- [ ] **Step 4: Run mapper tests and verify pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data app/src/main/java/com/example/infinite_track/domain/model/wfa app/src/test/java/com/example/infinite_track/data/mapper/wfa
git commit -m "refactor: align WFA recommendations with backend contract"
```

---

### Task 4: Add Preparation UI State, Effects, and Mapper

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEvent.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationEffect.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapper.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationUiMapperTest.kt`

**Interfaces:**
- Consumes: typed domain preparation result, target, recommendation state, and marker projection.
- Produces: render-only state and semantic one-time effects.

- [ ] **Step 1: Write failing mapper tests for every visible state**

Cover:

```text
WFO resolving / ready / unavailable
WFH resolving / ready / profile-contract failure
WFA not requested / pending / rejected / approved
recommendation loading / empty / failure / content / selected
inside / outside / unknown range
```

Assert one primary action per state.

- [ ] **Step 2: Run mapper tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*AttendancePreparationUiMapperTest'
```

- [ ] **Step 3: Implement render-only models**

Use explicit models such as:

```kotlin
data class AttendancePreparationUiState(
    val selectedMode: WorkMode,
    val modeOptions: List<WorkModeOptionUiModel>,
    val targetSummary: TargetLocationSummaryUiModel?,
    val recommendationSection: WfaRecommendationSectionUiModel?,
    val map: MapUiState,
    val primaryAction: AttendancePreparationPrimaryAction,
    val secondaryAction: AttendancePreparationSecondaryAction? = null,
    val isResolving: Boolean = false
)
```

Effects remain semantic:

```kotlin
sealed interface AttendancePreparationEffect {
    data class FocusTarget(val coordinate: GeoCoordinate) : AttendancePreparationEffect
    data class FocusRecommendation(val coordinate: GeoCoordinate) : AttendancePreparationEffect
    data object OpenWfaBooking : AttendancePreparationEffect
    data object OpenWfaRequests : AttendancePreparationEffect
    data object OpenFaceVerification : AttendancePreparationEffect
    data object ContactAdmin : AttendancePreparationEffect
}
```

Do not put raw routes, provider camera objects, or `NavController` in state/effects.

- [ ] **Step 4: Run mapper tests and verify pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation
git commit -m "feat: add attendance preparation UI contract"
```

---

### Task 5: Implement Latest-Selection-Wins Preparation ViewModel

**Files:**
- Create or modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModel.kt`
- Modify DI wiring in the existing module that provides attendance/location use cases.
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation/AttendancePreparationViewModelTest.kt`

**Interfaces:**
- Consumes: target resolver, preparation evaluator, `/me` profile use case, status-today use case, WFA recommendations and booking resolver.
- Produces: `StateFlow<AttendancePreparationUiState>` and `SharedFlow<AttendancePreparationEffect>`.

- [ ] **Step 1: Write failing ViewModel tests**

Required tests:

```kotlin
@Test fun `rapid WFO to WFH switch ignores stale WFO result`()
@Test fun `rapid WFA to WFO switch ignores stale recommendation result`()
@Test fun `recommendation row selection updates marker preview`()
@Test fun `recommendation marker selection updates selected row`()
@Test fun `WFH refresh reloads me profile`()
@Test fun `ready primary action emits face verification once`()
@Test fun `duplicate primary taps do not emit duplicate navigation`()
```

- [ ] **Step 2: Run ViewModel tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*AttendancePreparationViewModelTest'
```

- [ ] **Step 3: Implement cancellable mode resolution**

Use one owned job or `flatMapLatest` for mode-dependent resolution. Recommendation fetching must be cancelled or ignored when the user leaves WFA.

State remains persistent in `StateFlow`; navigation/map commands are one-time effects with replay zero.

- [ ] **Step 4: Run ViewModel tests and verify pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/preparation app/src/main/java/com/example/infinite_track/di app/src/test/java/com/example/infinite_track/presentation/screen/attendance/preparation
git commit -m "feat: implement attendance preparation orchestration"
```

---

### Task 6: Build Existing-Component Work Mode and Recommendation UI

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WorkModeOptionCard.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/TargetLocationSummary.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WfaRecommendationOption.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/components/WfaRecommendationSection.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt`

**Interfaces:**
- Consumes: render-only UI models and callbacks.
- Produces: redesign inside the existing bottom sheet with no business logic.

- [ ] **Step 1: Write failing Compose tests**

Cover:

```text
three Work Mode cards render
selected mode has selected semantics
WFH card says admin-provisioned and has no search/edit action
WFA row has category icon and no image semantics
score is announced as suitability score
one primary action renders
large font does not hide primary action
recommendation loading, empty, error, content, and selected states render
```

- [ ] **Step 2: Run Compose test compilation and focused tests**

```bash
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.WorkModeTargetLocationScreenTest
```

Expected before implementation: compile/test failure.

- [ ] **Step 3: Implement components using existing primitives**

Use `InfiniteCard`/`InfiniteSurface`, `InfiniteStatusPill`, `InfiniteButton`, `InfiniteIconButton`, `InfiniteInlineAlert`, and existing Material icons.

`WfaRecommendationOption` must render:

```text
category icon
name
category + formatted distance
suitability score + label
selection indicator
```

No `Image`, Coil request, remote URL, or random drawable representing a venue photo.

- [ ] **Step 4: Run Compose tests and verify pass**

Expected: PASS on configured emulator/device.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/WorkModeTargetLocationScreenTest.kt
git commit -m "feat: redesign work mode and WFA recommendation surface"
```

---

### Task 7: Synchronize Provider-Neutral Markers and Route-Owned Effects

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/maps/AttendanceMap.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/maps/MarkerViewWfa.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify the Attendance Route/navigation owner used by `AttendanceScreen`.
- Add or extend mapper and UI tests.

**Interfaces:**
- Consumes: `MapUiState`, marker roles, and semantic effects.
- Produces: synchronized row/marker selection and Route-owned camera/navigation execution.

- [ ] **Step 1: Write failing marker synchronization tests**

Assert:

```text
row selection selects the matching WFA_RECOMMENDATION marker
marker selection selects the matching row
AUTHORITATIVE_TARGET marker remains unchanged
SEARCH_PREVIEW never becomes authoritative
FocusTarget effect is executed once
OpenWfaBooking effect navigates once
```

- [ ] **Step 2: Run focused unit/UI tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*AttendancePreparation*'
./gradlew app:compileDebugAndroidTestKotlin
```

- [ ] **Step 3: Implement provider-neutral marker projection and Route execution**

The ViewModel emits semantic effects. The Route maps them to the current Mapbox adapter during this issue. Do not introduce Google Maps code here; that follows the INF-140 migration seam.

- [ ] **Step 4: Run tests and verify pass**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/components/maps app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test app/src/androidTest
git commit -m "feat: synchronize attendance recommendation markers"
```

---

### Task 8: Remove Duplicate Legacy State and Run Final Verification

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Update relevant docs if the source-of-truth contract changes.

**Interfaces:**
- Consumes: completed preparation state/effect architecture.
- Produces: no duplicate target, marker, recommendation, or legacy CTA source of truth.

- [ ] **Step 1: Add regression tests for removed duplicate state**

Prove that UI no longer relies on parallel fields such as:

```text
targetLocation
selectedTargetLocation
targetLocationMarker
selectedMarkerInfo
selectedWfaLocation
selectedWfaMarkerInfo
buttonText
isButtonEnabled
isCheckInMode
```

where a typed preparation/action contract already owns the same truth.

- [ ] **Step 2: Remove duplicate fields and legacy derivation**

Keep a temporary compatibility mapper only when required by an un-migrated consumer. Document its removal boundary and do not create new writes to legacy fields.

- [ ] **Step 3: Run all relevant unit tests**

```bash
./gradlew app:testDebugUnitTest
```

Expected: PASS with zero failures.

- [ ] **Step 4: Run Android test compilation and lint**

```bash
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:lintDebug
```

Expected: PASS or documented non-blocking baseline findings unrelated to this change.

- [ ] **Step 5: Build the debug APK**

```bash
./gradlew app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Capture runtime evidence**

Verify on emulator/device:

```text
WFO ready and outside range
WFH ready from /me
WFH invalid profile recovery
WFA recommendation loading/content/empty/failure
row-marker synchronized selection
booking handoff
WFA pending/rejected/approved
approved target marker and radius
large font and narrow screen
no fabricated venue images
existing bottom-sheet behavior unchanged
```

- [ ] **Step 7: Commit**

```bash
git add app docs
git commit -m "refactor: complete INF-238 work mode target redesign"
```

---

## Self-Review Results

- Spec coverage: every locked product, UX, architecture, component, marker, and verification requirement is assigned to a task.
- Placeholder scan: no implementation step depends on TBD backend fields or fabricated images.
- Type consistency: `SelectedTargetLocation`, `GeoCoordinate`, `TargetLocationSource`, `AttendancePreparationResult`, `AttendancePreparationUiState`, and semantic effects are defined before consumers use them.
- Scope protection: Google Maps implementation, permission redesign, geofence lifecycle changes, and bottom-sheet rewrites remain excluded.

## Execution Gate

Do not execute this plan until INF-140 / #94 is implemented and verified. Execution should use an isolated worktree based on the latest `develop`.
