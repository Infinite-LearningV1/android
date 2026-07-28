# INF-265 WFA Request Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the WFA Request Form, Review, and Result visuals from the approved reference using transparent page roots, shared Infinite Track components, and a real read-only Google Maps preview.

**Architecture:** Keep the existing graph-scoped `WfaRequestViewModel`, events, normalized draft, and backend-confirmed result unchanged. Extend the provider-neutral map presentation model with explicit permission and interaction policies, then add reusable read-only location and result molecules under shared presentation components. Screens remain orchestration-only and compose those shared components in scrollable transparent layouts.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Google Maps Compose, Navigation Compose, JUnit4, Compose UI tests, Hilt, Gradle/JBR 17.

## Global Constraints

- The first and second reference panels are one Form destination and one vertically scrollable `LazyColumn`.
- Form and Review use a real Google Maps preview with pan, zoom, rotate, tilt, toolbar, and map-click interaction disabled.
- WFA page roots do not paint a background; the existing root/Home background remains visible.
- No API, repository, domain request, or backend contract changes.
- Radius remains server-authoritative and read-only.
- Do not claim that schedule-conflict validation passed because the current state does not contain that evidence.
- Screens render visual content through shared components; do not create `wfa_request/components`.
- Typography, colors, spacing, radius, density, and surface styles come from existing core/design tokens.
- Preserve existing WFA test tags and navigation behavior.
- Use JBR 17 at `C:\Users\Febriyadi\.jdks\jbr-17.0.14` and Gradle `--no-daemon -P kotlin.compiler.execution.strategy=in-process`.
- Report Google Maps rendering, gesture behavior, screenshots, and authenticated submission as Needs Verification when no online device is available.

## File Structure

### New files

- `app/src/main/java/com/example/infinite_track/presentation/components/map/ReadOnlyLocationMap.kt` — reusable map molecule and map-state builder.
- `app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteChecklistCard.kt` — reusable semantic checklist molecule.
- `app/src/main/java/com/example/infinite_track/presentation/design/components/state/InfiniteResultHero.kt` — reusable result hero molecule.
- `app/src/test/java/com/example/infinite_track/presentation/components/map/ReadOnlyLocationMapModelTest.kt` — pure map-model coverage.
- `app/src/test/java/com/example/infinite_track/presentation/map/adapter/MapPresentationPolicyTest.kt` — permission and gesture-policy coverage.

### Modified files

- `app/src/main/java/com/example/infinite_track/presentation/map/model/MapUiState.kt`
- `app/src/main/java/com/example/infinite_track/presentation/map/adapter/GoogleAttendanceMap.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/textfield/InfiniteTrackDropDown.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/textfield/InfiniteTrackTextArea.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestReviewScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestResultScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/strings.xml`
- `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`
- `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`

---

### Task 1: Add provider-neutral read-only map policies

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/model/MapUiState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/map/adapter/GoogleAttendanceMap.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/map/adapter/MapPresentationPolicyTest.kt`

**Interfaces:**
- Produces: `MapPermissionRequirement`, `MapInteractionMode`, and `MapInteractionMode.toMapUiSettings()`.
- Preserves: existing `MapUiState()` callers default to precise-location permission and interactive gestures.

- [ ] **Step 1: Write failing policy tests**

```kotlin
class MapPresentationPolicyTest {
    @Test
    fun `existing map defaults remain permission gated and interactive`() {
        val state = MapUiState()
        assertEquals(MapPermissionRequirement.PreciseLocation, state.permissionRequirement)
        assertEquals(MapInteractionMode.Interactive, state.interactionMode)
        assertTrue(state.interactionMode.toMapUiSettings().scrollGesturesEnabled)
    }

    @Test
    fun `read only mode disables all map gestures and toolbar`() {
        val settings = MapInteractionMode.ReadOnly.toMapUiSettings()
        assertFalse(settings.scrollGesturesEnabled)
        assertFalse(settings.zoomGesturesEnabled)
        assertFalse(settings.rotationGesturesEnabled)
        assertFalse(settings.tiltGesturesEnabled)
        assertFalse(settings.mapToolbarEnabled)
    }
}
```

- [ ] **Step 2: Run the tests and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*MapPresentationPolicyTest" --console=plain
```

Expected: compilation fails because the two policy enums and mapper do not exist.

- [ ] **Step 3: Add policy types and backward-compatible defaults**

```kotlin
enum class MapPermissionRequirement { PreciseLocation, None }
enum class MapInteractionMode { Interactive, ReadOnly }

data class MapUiState(
    val markers: List<MapMarkerUiModel> = emptyList(),
    val circles: List<MapCircleUiModel> = emptyList(),
    val hasPreciseLocationPermission: Boolean = false,
    val contentDescription: String = "Peta lokasi attendance",
    val permissionRequirement: MapPermissionRequirement = MapPermissionRequirement.PreciseLocation,
    val interactionMode: MapInteractionMode = MapInteractionMode.Interactive
)
```

Add an internal mapper in `GoogleAttendanceMap.kt`:

```kotlin
internal fun MapInteractionMode.toMapUiSettings() = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = this == MapInteractionMode.Interactive,
    scrollGesturesEnabled = this == MapInteractionMode.Interactive,
    tiltGesturesEnabled = false,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = this == MapInteractionMode.Interactive
)
```

Gate the existing fallback only when:

```kotlin
state.permissionRequirement == MapPermissionRequirement.PreciseLocation &&
    !state.hasPreciseLocationPermission
```

Remember UI settings by `state.interactionMode`.

- [ ] **Step 4: Run policy and existing map tests**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*MapPresentationPolicyTest" --tests "*GoogleAttendanceMap*" --tests "*AttendanceMap*" --console=plain
```

Expected: PASS with existing Attendance maps still interactive by default.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/map/model/MapUiState.kt app/src/main/java/com/example/infinite_track/presentation/map/adapter/GoogleAttendanceMap.kt app/src/test/java/com/example/infinite_track/presentation/map/adapter/MapPresentationPolicyTest.kt
git commit -m "feat(map): support read-only presentation mode"
```

---

### Task 2: Build the reusable read-only location map molecule

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/components/map/ReadOnlyLocationMap.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/components/map/ReadOnlyLocationMapModelTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`

**Interfaces:**
- Consumes: `MapInteractionMode.ReadOnly` and `MapPermissionRequirement.None` from Task 1.
- Produces:

```kotlin
@Composable
fun ReadOnlyLocationMap(
    coordinate: GeoCoordinate?,
    radiusMeters: Int,
    title: String,
    address: String,
    modifier: Modifier = Modifier,
    contentDescription: String
)

internal fun buildReadOnlyLocationMapState(
    coordinate: GeoCoordinate,
    radiusMeters: Int,
    title: String,
    address: String,
    contentDescription: String
): MapUiState
```

- [ ] **Step 1: Write failing map-model tests**

```kotlin
class ReadOnlyLocationMapModelTest {
    @Test
    fun `preview contains selected WFA marker server radius and read only policies`() {
        val state = buildReadOnlyLocationMapState(
            coordinate = GeoCoordinate(-0.89, 119.87),
            radiusMeters = 100,
            title = "Kafe Taman",
            address = "Jl. Merdeka 10, Palu",
            contentDescription = "Peta Kafe Taman"
        )

        assertEquals(MapPermissionRequirement.None, state.permissionRequirement)
        assertEquals(MapInteractionMode.ReadOnly, state.interactionMode)
        assertEquals(MapMarkerRole.AUTHORITATIVE_TARGET, state.markers.single().role)
        assertEquals(MapMarkerCategory.WFA, state.markers.single().category)
        assertEquals(DistanceMeters(100.0), state.circles.single().radius)
    }
}
```

- [ ] **Step 2: Run the map-model test and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*ReadOnlyLocationMapModelTest" --console=plain
```

Expected: compilation fails because the builder does not exist.

- [ ] **Step 3: Implement the builder and composable**

Build one authoritative WFA marker and one radius circle. Render
`AttendanceMap` at a token-derived fixed height, apply rounded clipping, and
send a stable `MapCameraEffect.Focus` centered on the candidate.

For invalid/null coordinates, render `InfiniteErrorState` with localized map
copy. Track `AttendanceMapEvent.Ready`; if the map does not become ready within
eight seconds, replace only the map region with the same shared fallback.

The map must not request precise permission or enable a my-location layer.

- [ ] **Step 4: Run the model tests and compile Android tests**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*ReadOnlyLocationMapModelTest" app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/components/map/ReadOnlyLocationMap.kt app/src/test/java/com/example/infinite_track/presentation/components/map/ReadOnlyLocationMapModelTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml
git commit -m "feat(map): add read-only location preview"
```

---

### Task 3: Make shared request inputs controlled and reference-ready

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/textfield/InfiniteTrackDropDown.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/textfield/InfiniteTrackTextArea.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`

**Interfaces:**
- Produces:

```kotlin
@Composable
fun InfiniteTrackDropDown(
    selectedValue: String?,
    onSelected: (String) -> Unit,
    items: List<String>,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    enabled: Boolean = true
)
```

Extends:

```kotlin
fun InfiniteTrackTextArea(
    ...,
    maxLength: Int? = null,
    showCharacterCount: Boolean = false
)
```

- [ ] **Step 1: Add failing Compose assertions**

Add a Form test that clicks `wfaReasonDropdown`, selects `Lainnya`, asserts the
dropdown displays `Lainnya`, and asserts `wfaOtherReason` becomes visible. Add
an assertion that notes display `18/250` for the existing fixture.

- [ ] **Step 2: Compile the Android test and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:compileDebugAndroidTestKotlin --console=plain
```

Expected: compilation or semantic assertion setup fails because the controlled
dropdown tag and character counter do not exist.

- [ ] **Step 3: Refactor the shared controls**

Remove local selection ownership from `InfiniteTrackDropDown`; display
`selectedValue ?: placeholder` and invoke only `onSelected`. Use core `body1`
and `body2`, shared theme colors, `InfiniteSpacing`, and a minimum 48 dp target.

In `InfiniteTrackTextArea`, enforce:

```kotlin
val acceptedValue = if (maxLength == null) newValue else newValue.take(maxLength)
```

Render the count with `InfiniteSupportingText` aligned to the end when
`showCharacterCount && maxLength != null`.

- [ ] **Step 4: Recompile Android tests and run WFA unit tests**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*WfaRequest*" app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/components/textfield/InfiniteTrackDropDown.kt app/src/main/java/com/example/infinite_track/presentation/components/textfield/InfiniteTrackTextArea.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt
git commit -m "refactor(ui): control shared request inputs"
```

---

### Task 4: Add reusable checklist and result hero molecules

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteChecklistCard.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/components/state/InfiniteResultHero.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`

**Interfaces:**
- Produces:

```kotlin
data class InfiniteChecklistItem(
    val text: String,
    val semantic: InfiniteSemantic = InfiniteSemantic.Success
)

@Composable
fun InfiniteChecklistCard(
    title: String,
    items: List<InfiniteChecklistItem>,
    modifier: Modifier = Modifier
)

@Composable
fun InfiniteResultHero(
    title: String,
    message: String,
    semantic: InfiniteSemantic,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 1: Add screen-test assertions for supported checklist copy and result hero**

Assert that the Form shows the three localized supported facts and does not show
`Tidak ada jadwal WFA yang bentrok`. Assert that success shows the localized
hero title and message.

- [ ] **Step 2: Compile Android tests and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:compileDebugAndroidTestKotlin --console=plain
```

Expected: RED because the new copy/components are absent.

- [ ] **Step 3: Implement both shared molecules**

Compose `InfiniteChecklistCard` from `InfiniteCard`, `InfiniteSectionHeader`,
and shared semantic icon/text atoms. Compose `InfiniteResultHero` from a large
semantic feedback icon surface, core `headline3`/`body1`, and theme tokens.
Do not import feature WFA models into either component.

- [ ] **Step 4: Compile and run focused tests**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*WfaRequest*" app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteChecklistCard.kt app/src/main/java/com/example/infinite_track/presentation/design/components/state/InfiniteResultHero.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt
git commit -m "feat(ui): add checklist and result hero components"
```

---

### Task 5: Redesign the transparent one-page Form

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`

**Interfaces:**
- Consumes: `ReadOnlyLocationMap`, controlled dropdown/textarea, and
  `InfiniteChecklistCard`.
- Extends:

```kotlin
fun WfaRequestFormScreen(
    uiState: WfaRequestUiState,
    onEvent: (WfaRequestEvent) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 1: Write failing Form structure tests**

Assert tags `wfaLocationMap`, `wfaEmployeeCard`, `wfaRequestDetails`,
`wfaEligibilityCard`, and `wfaReviewAction` exist in one Form destination.
Use `performScrollTo()` on the final action to prove the second reference panel
is reachable in the same scroll container. Assert no screen-owned opaque
background semantic marker is present.

- [ ] **Step 2: Compile tests and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:compileDebugAndroidTestKotlin --console=plain
```

Expected: RED because new section tags and Close behavior are absent.

- [ ] **Step 3: Recompose the Form**

Remove `background(InfiniteColors.AttendanceReportBackground)` from the root.
Use `InfiniteTopBar` with `Icons.Default.Close` and `onClose`.

Build one `LazyColumn` in this order:

1. selected location card and `ReadOnlyLocationMap`;
2. server radius/status row;
3. employee card and profile-source supporting text;
4. request detail card with date picker, controlled reason dropdown,
   conditional other reason, and 250-character notes;
5. supported-facts checklist;
6. Continue action.

Retain all existing field-error placement and test tags. Do not add a schedule
conflict claim.

- [ ] **Step 4: Run focused WFA gates**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*WfaRequest*" app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestFormScreen.kt app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml
git commit -m "feat(wfa): redesign transparent request form"
```

---

### Task 6: Redesign Review and Result with shared components

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestReviewScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestResultScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt`
- Modify: `app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`

**Interfaces:**
- Consumes: shared read-only map and result hero.
- Extends Review and Result with `onClose` callbacks defaulting to their current
  back behavior.

- [ ] **Step 1: Write failing Review/Result and Close tests**

Assert Review contains `wfaReviewLocationMap`, `wfaReviewDetailsCard`,
`wfaReviewEmployeeCard`, Confirm, and Edit. Assert Result contains
`wfaSuccessHero`, the backend-confirmed detail card, and both existing exit
actions. Add navigation coverage proving Close returns Review to Form and
Result to Attendance when not submitting.

- [ ] **Step 2: Compile Android tests and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:compileDebugAndroidTestKotlin --console=plain
```

Expected: RED because the new sections and Close actions do not exist.

- [ ] **Step 3: Implement transparent Review and Result**

Remove both page backgrounds. Add Close to each `InfiniteTopBar`.

Review order:

1. review heading/guidance;
2. location card and read-only map;
3. request detail card;
4. employee card;
5. radius policy card;
6. Submit and Edit actions.

Result order:

1. shared result hero;
2. backend-confirmed request detail card;
3. Attendance/status outlined action;
4. Home primary action.

Keep existing submitting locks, typed failure mapping, scrolling, and stable
test tags.

- [ ] **Step 4: Run focused WFA and navigation gates**

Run:

```powershell
.\gradlew.bat --no-daemon -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest --tests "*WfaRequest*" app:compileDebugAndroidTestKotlin --console=plain
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestReviewScreen.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestResultScreen.kt app/src/main/java/com/example/infinite_track/presentation/navigation/WfaRequestNavGraph.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/wfa_request/WfaRequestScreensTest.kt app/src/androidTest/java/com/example/infinite_track/presentation/navigation/WfaRequestNavigationTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml
git commit -m "feat(wfa): redesign review and result states"
```

---

### Task 7: Audit component-only rendering and run complete verification

**Files:**
- Modify only files implicated by verification failures.
- Verify all files listed in Tasks 1–6.

**Interfaces:**
- Produces: a clean, reviewable branch ready for push and PR.

- [ ] **Step 1: Audit WFA screens for raw visual primitives**

Run:

```powershell
rg -n "\b(Text|Icon|Button|OutlinedButton|CircularProgressIndicator)\s*\(|MaterialTheme|Color\(|[0-9]+\.dp|\.background\(" app/src/main/java/com/example/infinite_track/presentation/screen/attendance/wfa_request
```

Expected: no raw visual components, no page background, and no hardcoded visual
tokens in WFA screen files. Layout primitives and token-based modifiers are
allowed.

- [ ] **Step 2: Run formatting and diff checks**

Run:

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors and only intended paths changed.

- [ ] **Step 3: Run the full Android gate**

Run:

```powershell
.\gradlew.bat --no-daemon --console=plain -P kotlin.compiler.execution.strategy=in-process app:testDebugUnitTest app:compileDebugAndroidTestKotlin app:lintDebug app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Check device availability**

Run:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices -l
```

If no online device is listed, record Google Maps rendering, read-only gesture
behavior, screenshots, large-font runtime, and authenticated submission as
Needs Verification.

- [ ] **Step 5: Inspect final diff and commit any verification-only corrections**

```powershell
git diff develop...HEAD --stat
git log --oneline develop..HEAD
git status --short
```

If verification required corrections:

```powershell
git add <exact-corrected-paths>
git commit -m "fix(wfa): complete visual redesign verification"
```

- [ ] **Step 6: Publish after user-authorized delivery**

```powershell
git push -u origin codex/inf-265-wfa-request-visual-redesign
```

Create a ready-for-review PR targeting `develop` with:

- the reference-driven Form/Review/Result redesign;
- transparent page ownership;
- component reuse/adaptations;
- read-only Google Maps behavior;
- full local verification;
- explicit device/runtime Needs Verification evidence.
