# INF-230 Permission and History Timeline Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine Attendance permission readiness into a transparent sequential timeline that exits automatically when required access is ready, and rebuild the History attendance timeline as individually lazy, continuously center-focused pills.

**Architecture:** Keep permission truth and one-shot navigation in `AttendancePermissionReadinessViewModel`; the route owns Android launchers and effect collection, while the NavGraph owns back-stack removal. Keep History scroll behavior entirely in presentation: pure viewport math is unit-tested, `HistoryScreen` maps one outer `LazyColumn` into stable record items, and the stateless design-system pill receives normalized focus and rail progress values.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose 2.8.x, Hilt ViewModel, Kotlin Coroutines/Flow, JUnit 4, Compose UI tests.

## Global Constraints

- Work only in `E:\skrisi\android\.worktrees\inf-230-timeline-scroll-refinement` on `codex/inf-230-timeline-scroll-refinement`.
- Do not touch the user-owned `develop` changes in `NetworkModule.kt` or `network_security_config.xml`.
- Expose exactly two new public visual composables: `PermissionTimelinePill` and `AttendanceHistoryTimelinePill`.
- Do not redesign `InfiniteStatusPill`; reuse it inside the History pill.
- Permission and History surfaces use translucent solid theme colors; do not add `Brush`, gradients, or radial backdrops.
- Use existing `headline4`, `body1`, and `body2` typography without call-site `fontSize`, `lineHeight`, or `fontFamily` overrides.
- Motion uses the official Material 3 v0_103 duration/easing values resolved with Material 3 1.2.1; do not upgrade Gradle dependencies and do not invent a separate motion language.
- Center-focus is directly scroll-linked and reversible; do not use delayed entrance, replay, bounce loops, or one-time reveal state.
- Keep one vertical scroll owner per screen. Permission uses one `verticalScroll`; History uses its existing single top-level `LazyColumn`.
- Required access controls navigation. Optional notification/background-location state never blocks Attendance.
- Runtime permission dialogs, Settings return, navigation, font-scale behavior, and scroll smoothness are verified on `develop`; feature-worktree completion must keep them marked Needs Verification.
- Follow strict red-green-refactor for unit-testable behavior. Compose instrumentation tests are written first and compiled in the feature worktree; their device execution is deferred to `develop` per the runtime gate and must not be reported as passing before that run.

## File Structure

### Permission completion and navigation

- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessContract.kt` — add effect-collector lifecycle events.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt` — emit ready navigation automatically and suppress/redeliver it safely.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessRoute.kt` — notify the ViewModel only after the `SharedFlow` collector is subscribed.
- Modify `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt` — navigate with inclusive readiness `popUpTo` and `launchSingleTop`.
- Modify `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModelTest.kt` — cover initial ready, transitions, duplicate suppression, optional degradation, and inspection failure.
- Create `app/src/test/java/com/example/infinite_track/presentation/navigation/AttendancePermissionNavigationTest.kt` — verify the exact `NavOptions` contract.

### Permission presentation

- Modify `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteMotion.kt` — expose the resolved Material 3 v0_103 state/enter/exit specs.
- Create `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionTimelinePill.kt` — public stateless required-permission pill.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionMissionRow.kt` — retain it only for optional rows and replace hardcoded glass colors with semantic theme colors.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionReadinessInfoBox.kt` — use a compact solid translucent semantic surface instead of the gradient feedback surface.
- Rewrite `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt` — transparent Scaffold/top bar, required timeline, optional disclosure, no ready alert.
- Delete `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionHeroCard.kt` — obsolete hero.
- Delete `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionProgressHeader.kt` — obsolete progress card.
- Delete `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionGlassCard.kt` — obsolete screen-local glass wrapper.
- Rewrite `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreenTest.kt` — new disclosure, timeline, semantics, and responsive contracts.

### History viewport math and timeline

- Create `app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineViewport.kt` — pure keys, focus, progress, transform, connector, and load-more calculations.
- Create `app/src/main/java/com/example/infinite_track/presentation/design/components/data/HistoryTimelineConnectorAccent.kt` — public immutable connector value consumed by the public History pill.
- Create `app/src/test/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineViewportTest.kt` — exhaustive JVM coverage for those calculations.
- Create `app/src/main/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePill.kt` — public stateless History timeline pill consuming `HistoryTimelineConnectorAccent`.
- Create `app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineLazyItems.kt` — internal `LazyListScope` builder with stable record keys.
- Modify `app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteAttendanceTimelineSection.kt` — expose existing connector/status mapping helpers as `internal` for reuse; keep Home behavior unchanged.
- Modify `app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryScreen.kt` — replace the eager timeline section with header + individually lazy records, and collect one viewport snapshot per scroll update.
- Create `app/src/androidTest/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePillTest.kt` — content, status-pill reuse, semantics, and large-font layout.
- Create `app/src/androidTest/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineLazyItemsTest.kt` — individual scrolling/stable item behavior.

---

### Task 1: Automatic permission completion and back-stack removal

**Files:**
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModelTest.kt:205-338`
- Create: `app/src/test/java/com/example/infinite_track/presentation/navigation/AttendancePermissionNavigationTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessContract.kt:42-60`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt:45-150`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessRoute.kt:125-245`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt:160-166`

**Interfaces:**
- Consumes: `AttendancePermissionReadiness.canEnterAttendance`, required `inspectionIssues`, existing `navigationPending`, and `AttendancePermissionReadinessEffect.NavigateToWorkMode`.
- Produces: `EffectCollectorStarted`, `EffectCollectorStopped`, `attendanceReadyNavOptions(): NavOptions`, and automatic one-shot navigation for trustworthy required-ready state.

- [ ] **Step 1: Replace CTA-only navigation tests with failing automatic-navigation tests**

Add these behaviors to `AttendancePermissionReadinessViewModelTest.kt`. Start the collector before sending `EffectCollectorStarted`, matching the route subscription contract.

```kotlin
@Test
fun `initial trustworthy ready state navigates when effect collector starts`() = runTest {
    val viewModel = viewModel(FakeRepository(allRequiredReady()))
    advanceUntilIdle()
    val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effects.collect { effects.send(it) }
    }

    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
    advanceUntilIdle()

    assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
}

@Test
fun `repeated ready observations do not duplicate pending navigation`() = runTest {
    val repository = FakeRepository(partialReadiness())
    val viewModel = viewModel(repository)
    val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effects.collect { effects.send(it) }
    }
    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
    advanceUntilIdle()

    repository.readiness.value = allRequiredReady()
    advanceUntilIdle()
    assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())

    repository.readiness.value = allRequiredReady(notification = AttendanceAccessStatus.DEGRADED)
    advanceUntilIdle()
    assertFalse(effects.tryReceive().isSuccess)
}

@Test
fun `optional degradation still auto navigates`() = runTest {
    val viewModel = viewModel(
        FakeRepository(allRequiredReady(notification = AttendanceAccessStatus.DEGRADED))
    )
    val effect = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.first() }

    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
    advanceUntilIdle()

    assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effect.await())
}

@Test
fun `required inspection failure never auto navigates from stale ready content`() = runTest {
    val repository = FakeRepository(allRequiredReady())
    val viewModel = viewModel(repository)
    val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effects.collect { effects.send(it) }
    }
    repository.readiness.value = readiness(
        camera = AttendanceAccessStatus.ACTION_REQUIRED,
        issues = listOf(requiredIssue())
    )
    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
    advanceUntilIdle()

    assertFalse(effects.tryReceive().isSuccess)
    assertFalse(viewModel.uiState.value.canContinue)
}

@Test
fun `pending navigation is redelivered after collector restart`() = runTest {
    val viewModel = viewModel(FakeRepository(allRequiredReady()))
    val effects = Channel<AttendancePermissionReadinessEffect>(Channel.UNLIMITED)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effects.collect { effects.send(it) }
    }
    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
    advanceUntilIdle()
    assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())

    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStopped)
    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
    advanceUntilIdle()

    assertEquals(AttendancePermissionReadinessEffect.NavigateToWorkMode, effects.receive())
}
```

In the existing optional-action tests, replace `allRequiredReady(notification = AttendanceAccessStatus.DEGRADED)` with:

```kotlin
readiness(
    camera = AttendanceAccessStatus.ACTION_REQUIRED,
    notification = AttendanceAccessStatus.DEGRADED
)
```

This keeps the readiness destination open while exercising the optional action; a screen with all required access ready now navigates immediately.

- [ ] **Step 2: Run the focused ViewModel tests and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessViewModelTest"
```

Expected: FAIL because `EffectCollectorStarted` and `EffectCollectorStopped` do not exist and ready observations do not emit navigation.

- [ ] **Step 3: Add collector lifecycle events and minimal ready-navigation logic**

Add to `AttendancePermissionReadinessEvent`:

```kotlin
data object EffectCollectorStarted : AttendancePermissionReadinessEvent
data object EffectCollectorStopped : AttendancePermissionReadinessEvent
```

Add to `AttendancePermissionReadinessViewModel`:

```kotlin
private var effectCollectorActive = false

private fun onEffectCollectorStarted() {
    effectCollectorActive = true
    val readiness = effectiveReadiness() ?: return
    val requiredInspectionFailed = readiness.inspectionIssues.any { it.blocksManualAttendance }
    if (navigationPending && readiness.canEnterAttendance && !requiredInspectionFailed) {
        emit(AttendancePermissionReadinessEffect.NavigateToWorkMode)
    } else {
        maybeNavigateToWorkMode(readiness)
    }
}

private fun maybeNavigateToWorkMode(readiness: AttendancePermissionReadiness) {
    val requiredInspectionFailed = readiness.inspectionIssues.any { it.blocksManualAttendance }
    if (
        effectCollectorActive &&
        readiness.canEnterAttendance &&
        !requiredInspectionFailed &&
        !navigationPending &&
        inFlightAction == null
    ) {
        navigationPending = true
        emit(
            AttendancePermissionReadinessEffect.NavigateToWorkMode,
            trackInFlight = true
        )
    }
}
```

Wire events into `onEvent`:

```kotlin
AttendancePermissionReadinessEvent.EffectCollectorStarted -> onEffectCollectorStarted()
AttendancePermissionReadinessEvent.EffectCollectorStopped -> effectCollectorActive = false
```

At the end of `onReadinessObserved`, after `render()`, call:

```kotlin
maybeNavigateToWorkMode(effective)
```

Keep `PrimaryActionClicked` as a fallback for incomplete/error flows, but its ready branch must continue to respect `navigationPending` and `inFlightAction`.

- [ ] **Step 4: Subscribe before announcing collector readiness**

In `AttendancePermissionReadinessRoute.kt`, replace the opening line `viewModel.effects.collect { effect ->` with:

```kotlin
try {
    viewModel.effects
        .onSubscription {
            viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStarted)
        }
        .collect { effect ->
```

Keep every existing exhaustive `when (effect)` branch inside that collector. Immediately after the collector's current closing brace, close the `try` with:

```kotlin
} finally {
    viewModel.onEvent(AttendancePermissionReadinessEvent.EffectCollectorStopped)
}
```

Import `kotlinx.coroutines.flow.onSubscription`. Preserve the existing `NavigateToWorkMode` order exactly: invoke `latestOnContinueToWorkMode()`, then dispatch `NavigationHandled`.

- [ ] **Step 5: Write the failing NavOptions contract test**

Create `AttendancePermissionNavigationTest.kt`:

```kotlin
package com.example.infinite_track.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionNavigationTest {
    @Test
    fun `ready navigation removes readiness destination and is single top`() {
        val options = attendanceReadyNavOptions()

        assertEquals(Screen.AttendancePermissionReadiness.route, options.popUpToRoute)
        assertTrue(options.isPopUpToInclusive())
        assertTrue(options.shouldLaunchSingleTop())
    }
}
```

- [ ] **Step 6: Run the navigation test and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "com.example.infinite_track.presentation.navigation.AttendancePermissionNavigationTest"
```

Expected: FAIL because `attendanceReadyNavOptions()` does not exist.

- [ ] **Step 7: Add exact ready-navigation options and use them**

In `MainContentNavGraph.kt`, add:

```kotlin
import androidx.navigation.NavOptions
import androidx.navigation.navOptions

internal fun attendanceReadyNavOptions(): NavOptions = navOptions {
    popUpTo(Screen.AttendancePermissionReadiness.route) {
        inclusive = true
    }
    launchSingleTop = true
}
```

Replace the existing callback with:

```kotlin
onContinueToWorkMode = {
    navController.navigate(
        Screen.Attendance.route,
        attendanceReadyNavOptions()
    )
}
```

Do not use `safeNavigate` for this transition because it pops to the graph start destination rather than inclusively removing the readiness destination.

- [ ] **Step 8: Run focused tests and verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.permission.AttendancePermissionReadinessViewModelTest" --tests "com.example.infinite_track.presentation.navigation.AttendancePermissionNavigationTest"
```

Expected: PASS with no duplicate navigation effects.

- [ ] **Step 9: Commit Task 1**

```powershell
git add -- app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessContract.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModel.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessRoute.kt app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessViewModelTest.kt app/src/test/java/com/example/infinite_track/presentation/navigation/AttendancePermissionNavigationTest.kt
git commit -m "fix: complete attendance permission navigation"
```

### Task 2: Material 3 permission timeline and optional disclosure

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteMotion.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionTimelinePill.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionMissionRow.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionReadinessInfoBox.kt`
- Rewrite: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreen.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionHeroCard.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionProgressHeader.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission/PermissionGlassCard.kt`
- Rewrite test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreenTest.kt`

**Interfaces:**
- Consumes: `PermissionItemUiModel`, `AttendancePermissionReadinessUiState`, `InfiniteColors`, `infiniteSemanticColors`, `InfiniteSpacing`, typography tokens, and typed permission events.
- Produces: public `PermissionTimelinePill(...)`; internal optional disclosure and measured connector layout; M3-compatible `InfiniteMotion.stateChangeTween`, `enterTween`, and `exitTween`.

- [ ] **Step 1: Write new Compose contracts before changing production UI**

Rewrite the instrumentation test around these cases:

```kotlin
@Test
fun permissionTimelinePillExposesCompletedNodeAndCurrentAction() {
    val item = permissionItem(
        AttendanceAccess.CAMERA,
        status = "Perlu diatur",
        action = "Minta izin",
        semantic = InfiniteSemantic.Primary
    )
    composeRule.setContent {
        Infinite_TrackTheme {
            PermissionTimelinePill(
                item = item,
                stepNumber = 2,
                isCurrentAction = true,
                showTopConnector = true,
                showBottomConnector = true,
                topConnectorComplete = true,
                bottomConnectorComplete = false,
                onClick = {}
            )
        }
    }

    composeRule.onNodeWithTag("permission-step-camera").assertHasClickAction()
    composeRule.onNodeWithTag(
        "permission-step-node-camera",
        useUnmergedTree = true
    ).assertContentDescriptionEquals("Langkah 2")
}

@Test
fun loadingShellDoesNotRenderPermissionTimelineOrReadyAlert() {
    render(AttendancePermissionReadinessUiState())

    composeRule.onNodeWithText("Lokasi presisi").assertDoesNotExist()
    composeRule.onNodeWithText("Akses wajib sudah siap").assertDoesNotExist()
    composeRule.onNodeWithText("Attendance").assertIsDisplayed()
}

@Test
fun requiredTimelineShowsChecksAndOnlyCurrentStepIsClickable() {
    val events = mutableListOf<AttendancePermissionReadinessEvent>()
    render(
        partialState(
            requiredReadyCount = 1,
            requiredItems = listOf(
                permissionItem(AttendanceAccess.PRECISE_LOCATION, "Siap", null, InfiniteSemantic.Success, true),
                permissionItem(AttendanceAccess.CAMERA, "Perlu diatur", "Minta izin", InfiniteSemantic.Primary),
                permissionItem(AttendanceAccess.DEVICE_LOCATION, "Perlu diatur", "Buka pengaturan", InfiniteSemantic.Warning)
            )
        ),
        onEvent = events::add
    )

    composeRule.onNodeWithTag(
        "permission-step-node-precise_location",
        useUnmergedTree = true
    )
        .assertContentDescriptionEquals("Selesai")
    composeRule.onNodeWithTag("permission-step-camera").assertHasClickAction().performClick()
    composeRule.onNodeWithTag("permission-step-device_location").assertHasNoClickAction()
    assertEquals(
        listOf(AttendancePermissionReadinessEvent.PermissionItemClicked(AttendanceAccess.CAMERA)),
        events
    )
}

@Test
fun optionalPermissionsAreCollapsedUntilExplicitlyExpanded() {
    render(partialState())

    composeRule.onNodeWithText("Notifikasi pengingat").assertDoesNotExist()
    composeRule.onNodeWithTag("permission-optional-toggle")
        .assertStateDescriptionEquals("Diciutkan")
        .performClick()
    composeRule.onNodeWithText("Notifikasi pengingat").assertIsDisplayed()
    composeRule.onNodeWithTag("permission-optional-toggle")
        .assertStateDescriptionEquals("Diperluas")
}

@Test
fun readyAlertIsNeverRendered() {
    render(readyState(optionalReady = false))
    composeRule.onNodeWithText("Akses wajib sudah siap").assertDoesNotExist()
}
```

Retain and adapt the 320 dp/font-scale 2.0 test so the active pill action and primary button both remain within `screenHost` and are at least 48 dp high. Add imports for `assertDoesNotExist`, `assertHasClickAction`, `assertHasNoClickAction`, `assertContentDescriptionEquals`, and `assertStateDescriptionEquals`.

- [ ] **Step 2: Compile instrumentation tests and verify expected RED compile/failure state**

Run:

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

Expected: compilation fails because `PermissionTimelinePill` does not exist. Device execution remains deferred to `develop`; do not claim these tests pass in the feature worktree.

- [ ] **Step 3: Replace the unused local motion values with an M3 v0_103 adapter**

Update `InfiniteMotion.kt` to use the resolved Material 3 values exactly:

```kotlin
package com.example.infinite_track.presentation.design.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/** Public adapter for Material 3 v0_103 motion tokens, internal in M3 1.2.1. */
object InfiniteMotion {
    const val DurationShort4 = 200
    const val DurationMedium2 = 300

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> stateChangeTween() = tween<T>(
        durationMillis = DurationShort4,
        easing = Emphasized
    )

    fun <T> enterTween() = tween<T>(
        durationMillis = DurationMedium2,
        easing = EmphasizedDecelerate
    )

    fun <T> exitTween() = tween<T>(
        durationMillis = DurationShort4,
        easing = EmphasizedAccelerate
    )
}
```

Do not add or update a Gradle dependency.

- [ ] **Step 4: Implement the public required-permission pill**

Create `PermissionTimelinePill.kt` with this API:

```kotlin
@Composable
fun PermissionTimelinePill(
    item: PermissionItemUiModel,
    stepNumber: Int,
    isCurrentAction: Boolean,
    showTopConnector: Boolean,
    showBottomConnector: Boolean,
    topConnectorComplete: Boolean,
    bottomConnectorComplete: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
)
```

Implementation requirements, all in this file:

```kotlin
val colors = infiniteSemanticColors(item.semantic)
val enabled = isCurrentAction && onClick != null
val shape = RoundedCornerShape(18.dp)

Row(
    modifier = modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .testTag("permission-step-${item.access.name.lowercase()}")
        .semantics(mergeDescendants = true) {
            contentDescription = item.title
            stateDescription = item.stateDescription
            if (enabled) role = Role.Button
        }
) {
    PermissionTimelineRail(
        stepNumber = stepNumber,
        complete = item.isReady,
        showTop = showTopConnector,
        showBottom = showBottomConnector,
        topComplete = topConnectorComplete,
        bottomComplete = bottomConnectorComplete,
        accent = colors.accent,
        modifier = Modifier.fillMaxHeight()
    )
    Spacer(Modifier.width(InfiniteSpacing.Default.sm))
    Surface(
        modifier = Modifier
            .weight(1f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .sizeIn(minHeight = 48.dp),
        shape = shape,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = if (isCurrentAction) 3.dp else 0.dp
    ) {
        BoxWithConstraints(Modifier.padding(InfiniteSpacing.Default.lg)) {
            val compact = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.5f
            val copy: @Composable (Modifier) -> Unit = { copyModifier ->
                Column(
                    modifier = copyModifier,
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.xs)
                ) {
                    Text(item.title, style = headline4, color = colors.content)
                    Text(item.supportingText, style = body2, color = colors.content.copy(alpha = 0.74f))
                }
            }
            val label: @Composable () -> Unit = {
                Text(
                    text = item.actionLabel ?: item.statusLabel,
                    style = body1,
                    color = colors.accent,
                    modifier = Modifier.sizeIn(minHeight = 48.dp).wrapContentHeight()
                )
            }
            if (compact) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
                ) {
                    copy(Modifier.fillMaxWidth())
                    label()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.md)
                ) {
                    copy(Modifier.weight(1f))
                    label()
                }
            }
        }
    }
}
```

Add this private measured rail in the same file:

```kotlin
@Composable
private fun PermissionTimelineRail(
    stepNumber: Int,
    complete: Boolean,
    showTop: Boolean,
    showBottom: Boolean,
    topComplete: Boolean,
    bottomComplete: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    nodeTag: String
) {
    Box(
        modifier = modifier.width(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width / 2f
            val centerY = size.height / 2f
            val stroke = 2.dp.toPx()
            val muted = InfiniteColors.Neutral.copy(alpha = 0.22f)
            if (showTop) {
                drawLine(muted, Offset(x, 0f), Offset(x, centerY), stroke)
                if (topComplete) drawLine(accent, Offset(x, 0f), Offset(x, centerY), stroke)
            }
            if (showBottom) {
                drawLine(muted, Offset(x, centerY), Offset(x, size.height), stroke)
                if (bottomComplete) drawLine(accent, Offset(x, centerY), Offset(x, size.height), stroke)
            }
        }
        Surface(
            modifier = Modifier
                .size(32.dp)
                .testTag(nodeTag)
                .semantics {
                    contentDescription = if (complete) "Selesai" else "Langkah $stepNumber"
                },
            shape = CircleShape,
            color = accent.copy(alpha = if (complete) 0.20f else 0.12f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
        ) {
            AnimatedContent(
                targetState = complete,
                transitionSpec = {
                    (fadeIn(InfiniteMotion.stateChangeTween()) +
                        scaleIn(InfiniteMotion.stateChangeTween(), initialScale = 0.82f))
                        .togetherWith(
                            fadeOut(InfiniteMotion.stateChangeTween()) +
                                scaleOut(InfiniteMotion.stateChangeTween(), targetScale = 0.82f)
                        )
                },
                contentAlignment = Alignment.Center,
                label = "permission-step-state"
            ) { isComplete ->
                if (isComplete) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                } else {
                    Text(stepNumber.toString(), style = body1, color = accent)
                }
            }
        }
    }
}
```

Pass `nodeTag = "permission-step-node-${item.access.name.lowercase()}"`. Decorative icons inside merged semantics use `contentDescription = null`.

Do not use `InfiniteStatusPill` here; the compact action/status copy is part of the large permission pill, not a nested status component.

- [ ] **Step 5: Make optional rows and inspection alerts solid/translucent**

In `PermissionMissionRow.kt`:

- replace `Color(0x33FFFFFF)` and white borders with `infiniteSemanticColors(item.semantic).container/border`;
- retain the existing `InfiniteStatusPill` only for optional row status;
- keep `BoxWithConstraints`, 48 dp action targets, and merged semantics;
- do not add gradients or Brush imports.

Rewrite `PermissionReadinessInfoBox` with this solid/translucent implementation; do not call `InfiniteInlineAlert`, because that component owns the global gradient feedback style:

```kotlin
@Composable
internal fun PermissionReadinessInfoBox(
    guidance: PermissionGuidanceUiModel,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = infiniteSemanticColors(guidance.semantic)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(InfiniteSpacing.Default.lg),
            verticalArrangement = Arrangement.spacedBy(InfiniteSpacing.Default.sm)
        ) {
            Text(guidance.title, style = headline4, color = colors.content)
            Text(guidance.message, style = body2, color = colors.content.copy(alpha = 0.76f))
            if (guidance.actionLabel != null && onAction != null) {
                InfiniteButton(
                    text = guidance.actionLabel,
                    onClick = onAction,
                    variant = InfiniteButtonVariant.Ghost,
                    size = InfiniteSize.Medium,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 6: Rewrite the permission screen around transparent Scaffold**

Use this root structure:

```kotlin
@Composable
fun AttendancePermissionReadinessScreen(
    uiState: AttendancePermissionReadinessUiState,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var optionalExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("attendance-permission-screen"),
        containerColor = Color.Transparent,
        topBar = {
            InfiniteTopBar(
                title = "Attendance",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Kembali",
                onNavigationClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                InfiniteLoadingState(message = "Memeriksa kesiapan akses...")
            }
        } else {
            PermissionReadinessContent(
                uiState = uiState,
                optionalExpanded = optionalExpanded,
                onOptionalExpandedChange = { optionalExpanded = it },
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
```

Inside `PermissionReadinessContent`:

1. Render `recoverableFailure` first with `PermissionReadinessInfoBox` and Retry.
2. Render an `InfiniteSectionHeader` for required access.
3. Determine `currentIndex = requiredItems.indexOfFirst { !it.isReady }`.
4. Render the three pills in a normal `Column` without another scroll container.
5. For item `index`, enable only `index == currentIndex`; `topConnectorComplete = index > 0 && requiredItems[index - 1].isReady`; `bottomConnectorComplete = item.isReady`.
6. Render a 48 dp `Surface` toggle tagged `permission-optional-toggle`, with `stateDescription = "Diperluas"` or `"Diciutkan"` and `Role.Button`.
7. Render optional rows plus `contextualGuidance` only inside `AnimatedVisibility`; use `fadeIn(InfiniteMotion.enterTween()) + expandVertically(InfiniteMotion.enterTween())` and the corresponding M3 accelerate exit specs.
8. Keep the existing primary button below the required/optional content as a fallback for the current next action; it is never the success action because ready state auto-navigates.
9. Keep the bottom 112 dp spacer required by the shared shell/snackbar area.

Remove imports and code for `AttendanceReportBackground`, `PermissionBackdrop`, `Brush`, radial gradients, `statusBarsPadding`, the hero, the progress card, and `defaultGuidance`.

- [ ] **Step 7: Remove obsolete permission-only helpers**

Delete `PermissionHeroCard.kt`, `PermissionProgressHeader.kt`, and `PermissionGlassCard.kt`. Confirm no call sites remain:

```powershell
rg -n "PermissionHeroCard|PermissionProgressHeader|PermissionGlassCard|PermissionBackdrop|defaultGuidance" app/src
```

Expected: no matches.

- [ ] **Step 8: Compile production and instrumentation sources**

Run:

```powershell
.\gradlew.bat --no-daemon app:compileDebugKotlin app:compileDebugAndroidTestKotlin
```

Expected: BUILD SUCCESSFUL. This proves compilation only; the Compose tests remain Needs Verification until their `develop` device run.

- [ ] **Step 9: Commit Task 2**

```powershell
git add -- app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteMotion.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/permission/AttendancePermissionReadinessScreenTest.kt
git commit -m "feat: refine attendance permission timeline"
```

### Task 3: Pure History viewport and rail calculations

**Files:**
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineViewportTest.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineViewport.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/components/data/HistoryTimelineConnectorAccent.kt`

**Interfaces:**
- Consumes: viewport start/end offsets, visible History keys/indexes/centers, total record count, motion-enabled flag.
- Produces: `historyItemKey`, `historyRecordIdFromKey`, `calculateHistoryFocusFraction`, `calculateHistoryTimelineProgress`, `resolveHistoryFocusTransform`, `resolveHistoryConnectorAccent`, and `shouldLoadMoreHistory`.

- [ ] **Step 1: Write failing pure JVM tests**

Create `HistoryTimelineViewportTest.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.history

import com.example.infinite_track.presentation.design.components.data.HistoryFocusTransform
import com.example.infinite_track.presentation.design.components.data.resolveHistoryFocusTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryTimelineViewportTest {
    @Test
    fun `history key round trips a stable record id`() {
        assertEquals("history-71", historyItemKey(71))
        assertEquals(71, historyRecordIdFromKey("history-71"))
        assertEquals(null, historyRecordIdFromKey("summary"))
    }

    @Test
    fun `focus is one at center and clamps to zero outside range`() {
        assertEquals(1f, calculateHistoryFocusFraction(500f, 500f, 1000f), 0.0001f)
        assertEquals(0f, calculateHistoryFocusFraction(1200f, 500f, 1000f), 0.0001f)
        assertTrue(calculateHistoryFocusFraction(700f, 500f, 1000f) in 0f..1f)
    }

    @Test
    fun `timeline progress interpolates between visible record centers`() {
        val visible = listOf(
            VisibleHistoryItem(recordIndex = 2, center = 400f),
            VisibleHistoryItem(recordIndex = 3, center = 600f)
        )

        assertEquals(0.625f, calculateHistoryTimelineProgress(visible, 5, 500f), 0.0001f)
    }

    @Test
    fun `timeline progress handles empty and zero range`() {
        assertEquals(0f, calculateHistoryTimelineProgress(emptyList(), 0, 500f), 0f)
        assertEquals(
            0f,
            calculateHistoryTimelineProgress(listOf(VisibleHistoryItem(0, 500f)), 1, 500f),
            0f
        )
    }

    @Test
    fun `reduced motion keeps content fully readable`() {
        assertEquals(
            HistoryFocusTransform(alpha = 1f, scale = 1f, translationYDp = 0f, elevationDp = 0f),
            resolveHistoryFocusTransform(focusFraction = 0.2f, motionEnabled = false)
        )
    }

    @Test
    fun `center focus reaches approved transform envelope`() {
        assertEquals(
            HistoryFocusTransform(alpha = 1f, scale = 1f, translationYDp = -3f, elevationDp = 3f),
            resolveHistoryFocusTransform(focusFraction = 1f, motionEnabled = true)
        )
        assertEquals(
            HistoryFocusTransform(alpha = 0.70f, scale = 0.965f, translationYDp = 0f, elevationDp = 0f),
            resolveHistoryFocusTransform(focusFraction = 0f, motionEnabled = true)
        )
    }

    @Test
    fun `load more uses last visible record index not outer lazy index`() {
        assertTrue(shouldLoadMoreHistory(7, 10, canLoadMore = true, loading = false))
        assertFalse(shouldLoadMoreHistory(5, 10, canLoadMore = true, loading = false))
        assertFalse(shouldLoadMoreHistory(7, 10, canLoadMore = false, loading = false))
    }
}
```

Add these connector tests to the same class:

```kotlin
@Test
fun `half progress completes the first segment and middle node`() {
    val first = resolveHistoryConnectorAccent(0, 3, 0.5f)
    val middle = resolveHistoryConnectorAccent(1, 3, 0.5f)

    assertEquals(1f, first.bottomFraction, 0f)
    assertEquals(1f, middle.topFraction, 0f)
    assertTrue(middle.nodeComplete)
    assertEquals(0f, middle.bottomFraction, 0f)
}

@Test
fun `full progress accents every available connector half`() {
    val accents = (0..2).map { resolveHistoryConnectorAccent(it, 3, 1f) }

    assertEquals(0f, accents[0].topFraction, 0f)
    assertEquals(1f, accents[0].bottomFraction, 0f)
    assertEquals(1f, accents[1].topFraction, 0f)
    assertEquals(1f, accents[1].bottomFraction, 0f)
    assertEquals(1f, accents[2].topFraction, 0f)
    assertEquals(0f, accents[2].bottomFraction, 0f)
    assertTrue(accents.all { it.nodeComplete })
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.history.HistoryTimelineViewportTest"
```

Expected: FAIL because the viewport types and functions do not exist.

- [ ] **Step 3: Implement the pure viewport model and calculations**

Create `HistoryTimelineViewport.kt` with:

```kotlin
package com.example.infinite_track.presentation.screen.history

import kotlin.math.abs
import kotlin.math.min

internal const val HistoryKeyPrefix = "history-"
private const val HistoryFocusRangeFraction = 0.58f

internal data class VisibleHistoryItem(
    val recordIndex: Int,
    val center: Float
)

internal data class HistoryViewportSnapshot(
    val focusByKey: Map<String, Float> = emptyMap(),
    val timelineProgress: Float = 0f,
    val lastVisibleRecordIndex: Int? = null
)

internal fun historyItemKey(recordId: Int): String = "$HistoryKeyPrefix$recordId"

internal fun historyRecordIdFromKey(key: Any?): Int? =
    (key as? String)?.takeIf { it.startsWith(HistoryKeyPrefix) }
        ?.removePrefix(HistoryKeyPrefix)
        ?.toIntOrNull()

internal fun calculateHistoryFocusFraction(
    itemCenter: Float,
    viewportCenter: Float,
    viewportHeight: Float
): Float {
    val range = viewportHeight * HistoryFocusRangeFraction
    if (range <= 0f) return if (itemCenter == viewportCenter) 1f else 0f
    return (1f - min(abs(itemCenter - viewportCenter) / range, 1f)).coerceIn(0f, 1f)
}

internal fun calculateHistoryTimelineProgress(
    visibleItems: List<VisibleHistoryItem>,
    totalRecordCount: Int,
    viewportCenter: Float
): Float {
    if (totalRecordCount <= 1 || visibleItems.isEmpty()) return 0f
    val sorted = visibleItems.sortedBy { it.center }
    val lower = sorted.lastOrNull { it.center <= viewportCenter }
    val upper = sorted.firstOrNull { it.center >= viewportCenter }
    val fractionalIndex = when {
        lower == null -> upper!!.recordIndex.toFloat()
        upper == null -> lower.recordIndex.toFloat()
        lower.recordIndex == upper.recordIndex || lower.center == upper.center -> lower.recordIndex.toFloat()
        else -> {
            val fraction = ((viewportCenter - lower.center) / (upper.center - lower.center))
                .coerceIn(0f, 1f)
            lower.recordIndex + (upper.recordIndex - lower.recordIndex) * fraction
        }
    }
    return (fractionalIndex / (totalRecordCount - 1).toFloat()).coerceIn(0f, 1f)
}

internal fun shouldLoadMoreHistory(
    lastVisibleRecordIndex: Int?,
    recordCount: Int,
    canLoadMore: Boolean,
    loading: Boolean
): Boolean = lastVisibleRecordIndex != null &&
    recordCount > 0 &&
    lastVisibleRecordIndex >= recordCount - 3 &&
    canLoadMore &&
    !loading
```

Create `HistoryTimelineConnectorAccent.kt`:

```kotlin
package com.example.infinite_track.presentation.design.components.data

import androidx.compose.runtime.Immutable

@Immutable
data class HistoryTimelineConnectorAccent(
    val topFraction: Float,
    val nodeComplete: Boolean,
    val bottomFraction: Float
)

@Immutable
internal data class HistoryFocusTransform(
    val alpha: Float,
    val scale: Float,
    val translationYDp: Float,
    val elevationDp: Float
)

internal fun resolveHistoryFocusTransform(
    focusFraction: Float,
    motionEnabled: Boolean
): HistoryFocusTransform {
    if (!motionEnabled) return HistoryFocusTransform(1f, 1f, 0f, 0f)
    val focus = focusFraction.coerceIn(0f, 1f)
    return HistoryFocusTransform(
        alpha = 0.70f + 0.30f * focus,
        scale = 0.965f + 0.035f * focus,
        translationYDp = -3f * focus,
        elevationDp = 3f * focus
    )
}
```

Import that type in `HistoryTimelineViewport.kt` and add the complete connector calculation:

```kotlin
internal fun resolveHistoryConnectorAccent(
    recordIndex: Int,
    recordCount: Int,
    timelineProgress: Float
): HistoryTimelineConnectorAccent {
    if (recordCount <= 0 || recordIndex !in 0 until recordCount) {
        return HistoryTimelineConnectorAccent(0f, false, 0f)
    }
    if (recordCount == 1) {
        return HistoryTimelineConnectorAccent(0f, true, 0f)
    }

    val progress = timelineProgress.coerceIn(0f, 1f)
    val lastIndex = recordCount - 1
    fun nodePosition(index: Int): Float = index / lastIndex.toFloat()
    fun segmentProgress(startIndex: Int, endIndex: Int): Float {
        val start = nodePosition(startIndex)
        val end = nodePosition(endIndex)
        return ((progress - start) / (end - start)).coerceIn(0f, 1f)
    }

    val top = if (recordIndex == 0) {
        0f
    } else {
        ((segmentProgress(recordIndex - 1, recordIndex) - 0.5f) * 2f)
            .coerceIn(0f, 1f)
    }
    val bottom = if (recordIndex == lastIndex) {
        0f
    } else {
        (segmentProgress(recordIndex, recordIndex + 1) * 2f)
            .coerceIn(0f, 1f)
    }

    return HistoryTimelineConnectorAccent(
        topFraction = top,
        nodeComplete = progress >= nodePosition(recordIndex),
        bottomFraction = bottom
    )
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.history.HistoryTimelineViewportTest"
```

Expected: PASS.

- [ ] **Step 5: Commit Task 3**

```powershell
git add -- app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineViewport.kt app/src/main/java/com/example/infinite_track/presentation/design/components/data/HistoryTimelineConnectorAccent.kt app/src/test/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineViewportTest.kt
git commit -m "test: define history timeline viewport behavior"
```

### Task 4: Center-focused History pill and individually lazy records

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePill.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineLazyItems.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteAttendanceTimelineSection.kt:205-220`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryScreen.kt:40-245`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePillTest.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineLazyItemsTest.kt`

**Interfaces:**
- Consumes: Task 3 viewport functions, `AttendanceRecord`, report mapper extensions, `TimelineConnectorPosition`, `InfiniteStatusPill`, and existing History loading/export state.
- Produces: public `AttendanceHistoryTimelinePill(...)`; internal `LazyListScope.attendanceHistoryTimelineItems(...)`; a timeline-local `HistoryViewportSnapshot` collected once per scroll update.

- [ ] **Step 1: Write component and lazy-list instrumentation contracts first**

Create `AttendanceHistoryTimelinePillTest.kt` with this primary contract, then repeat the same render inside a 320 dp host with `Density(1f, 2f)` and assert the root bounds remain within the host:

```kotlin
@Test
fun pillRendersRecordCopyAndExistingStatusPillSemantics() {
    composeRule.setContent {
        Infinite_TrackTheme {
            AttendanceHistoryTimelinePill(
                nodeLabel = "22",
                dateLabel = "22 July 2026",
                timeRange = "08:00 - 17:00",
                supportingText = "Head Office",
                statusLabel = "On Time",
                statusVariant = InfiniteStatusVariant.OnTime,
                connectorPosition = TimelineConnectorPosition.Middle,
                connectorAccent = HistoryTimelineConnectorAccent(1f, true, 0.5f),
                focusFraction = 1f,
                motionEnabled = true,
                modeAccentColor = InfiniteColors.Primary,
                modifier = Modifier.testTag("history-pill")
            )
        }
    }

    composeRule.onNodeWithText("22 July 2026").assertIsDisplayed()
    composeRule.onNodeWithText("08:00 - 17:00").assertIsDisplayed()
    composeRule.onNodeWithText("Head Office").assertIsDisplayed()
    composeRule.onNodeWithContentDescription("On Time").assertIsDisplayed()
    composeRule.onNodeWithTag("history-pill")
        .assertStateDescriptionEquals("22 July 2026, 08:00 - 17:00, On Time")
}
```

Create `HistoryTimelineLazyItemsTest.kt` with a `LazyColumn` containing 20 `AttendanceRecord` values through the internal builder:

```kotlin
@Test
fun recordsAreIndependentOuterLazyItemsAndLastRecordCanScrollIntoView() {
    val records = (1..20).map { index -> attendanceRecord(id = index, date = index.toString()) }
    composeRule.setContent {
        Infinite_TrackTheme {
            LazyColumn(Modifier.testTag("history-list")) {
                attendanceHistoryTimelineItems(
                    records = records,
                    focusByKey = emptyMap(),
                    timelineProgress = 0f,
                    motionEnabled = true
                )
            }
        }
    }

    composeRule.onNodeWithText(records.last().toReportDateLabel())
        .performScrollTo()
        .assertIsDisplayed()
    assertEquals("history-20", historyItemKey(records.last().id))
}
```

The local `attendanceRecord` fixture must populate the real required constructor fields (`id`, `date`, `monthYear`, `timeIn`, `timeOut`, and `workHour`) without mocks.

- [ ] **Step 2: Compile instrumentation tests before production implementation**

Run:

```powershell
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

Expected before production changes: compilation fails because `AttendanceHistoryTimelinePill` and `attendanceHistoryTimelineItems` do not exist. Device execution remains deferred to `develop`.

- [ ] **Step 3: Implement the stateless public History pill**

Create `AttendanceHistoryTimelinePill.kt` with this API:

```kotlin
@Composable
fun AttendanceHistoryTimelinePill(
    nodeLabel: String,
    dateLabel: String,
    timeRange: String,
    supportingText: String?,
    statusLabel: String,
    statusVariant: InfiniteStatusVariant,
    connectorPosition: TimelineConnectorPosition,
    connectorAccent: HistoryTimelineConnectorAccent,
    focusFraction: Float,
    motionEnabled: Boolean,
    modeAccentColor: Color,
    modifier: Modifier = Modifier
)
```

The root is `Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min))`. The private rail fills the measured row height, draws the muted connector first, then the Task 3 top/bottom accent fractions, and places a circular `nodeLabel` surface over it. The content surface uses `InfiniteColors.AttendanceReportGlassSurface`, `AttendanceReportGlassBorder`, and a shape already used by report cards.

Apply focus only through a draw layer:

```kotlin
val transform = resolveHistoryFocusTransform(focusFraction, motionEnabled)
val density = LocalDensity.current

Modifier.graphicsLayer {
    alpha = transform.alpha
    scaleX = transform.scale
    scaleY = transform.scale
    translationY = with(density) { transform.translationYDp.dp.toPx() }
    shadowElevation = with(density) { transform.elevationDp.dp.toPx() }
    shape = RoundedCornerShape(18.dp)
    clip = false
}
```

Inside the surface:

- use `body1` for date/time and `body2` for supporting text;
- place the existing `InfiniteStatusPill(label, variant, size = InfiniteSize.Small, useSharedRequestPalette = true)` beside the content on normal width and below it at 320 dp/font scale 1.5+;
- merge semantic description as `"$dateLabel, $timeRange, $statusLabel"`;
- do not add click behavior, animation timers, gradients, or a fixed height.

- [ ] **Step 4: Add the internal lazy-list builder**

Create `HistoryTimelineLazyItems.kt`:

```kotlin
internal fun LazyListScope.attendanceHistoryTimelineItems(
    records: List<AttendanceRecord>,
    focusByKey: Map<String, Float>,
    timelineProgress: Float,
    motionEnabled: Boolean
) {
    itemsIndexed(
        items = records,
        key = { _, record -> historyItemKey(record.id) },
        contentType = { _, _ -> "history-record" }
    ) { index, record ->
        val status = record.toReportStatus()
        val key = historyItemKey(record.id)
        AttendanceHistoryTimelinePill(
            nodeLabel = record.date,
            dateLabel = record.toReportDateLabel(),
            timeRange = record.toReportTimeRangeLabel(),
            supportingText = record.location?.takeIf(String::isNotBlank)
                ?: record.toReportWorkHourLabel(),
            statusLabel = status.label,
            statusVariant = status.kind.toTimelineVariant(),
            connectorPosition = connectorPositionFor(index, records.size),
            connectorAccent = resolveHistoryConnectorAccent(index, records.size, timelineProgress),
            focusFraction = focusByKey[key] ?: 0f,
            motionEnabled = motionEnabled,
            modeAccentColor = attendanceModeColor(record.modeKey ?: record.modeLabel),
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}
```

Change only the visibility of `connectorPositionFor` and `AttendanceReportStatusKind.toTimelineVariant` in `InfiniteAttendanceTimelineSection.kt` from `private` to `internal`. Do not change the legacy Home timeline rendering.

- [ ] **Step 5: Replace the eager History timeline with viewport-driven lazy items**

In `HistoryScreen.kt`:

1. Build `recordIndexById = remember(uiState.records) { uiState.records.mapIndexed { index, record -> record.id to index }.toMap() }`.
2. Hold one `HistoryViewportSnapshot(focusByKey, timelineProgress, lastVisibleRecordIndex)` as Compose state.
3. Use one `LaunchedEffect(lazyListState, recordIndexById, uiState.records.size)` with `snapshotFlow { lazyListState.layoutInfo }`, map only visible keys beginning with `history-`, and update the snapshot atomically.
4. Calculate `viewportCenter` from `viewportStartOffset` and `viewportEndOffset`; calculate each item center from `offset + size / 2f`.
5. Use `calculateHistoryFocusFraction` for the map and `calculateHistoryTimelineProgress` for rail progress.
6. Replace the existing load-more `derivedStateOf` comparison against outer lazy indexes with `shouldLoadMoreHistory(snapshot.lastVisibleRecordIndex, records.size, canLoadMore, isLoadingMore || isLoading)`.
7. Keep all existing summary, distribution, actions, notices, loading, empty, error, and export behavior.
8. Replace the single `InfiniteAttendanceTimelineSection` item with a keyed header item followed by `attendanceHistoryTimelineItems(...)`.

Use this exact viewport snapshot collection after `lazyListState` is created:

```kotlin
val recordIndexById = remember(uiState.records) {
    uiState.records.mapIndexed { index, record -> record.id to index }.toMap()
}
var historyViewport by remember(recordIndexById) {
    mutableStateOf(HistoryViewportSnapshot())
}

LaunchedEffect(lazyListState, recordIndexById, uiState.records.size) {
    snapshotFlow {
        val layout = lazyListState.layoutInfo
        val viewportStart = layout.viewportStartOffset.toFloat()
        val viewportEnd = layout.viewportEndOffset.toFloat()
        val viewportCenter = (viewportStart + viewportEnd) / 2f
        val viewportHeight = (viewportEnd - viewportStart).coerceAtLeast(0f)
        val visible = layout.visibleItemsInfo.mapNotNull { info ->
            val recordId = historyRecordIdFromKey(info.key) ?: return@mapNotNull null
            val recordIndex = recordIndexById[recordId] ?: return@mapNotNull null
            val center = info.offset + info.size / 2f
            Triple(historyItemKey(recordId), recordIndex, center)
        }
        val visibleMathItems = visible.map { (_, index, center) ->
            VisibleHistoryItem(recordIndex = index, center = center)
        }
        HistoryViewportSnapshot(
            focusByKey = visible.associate { (key, _, center) ->
                key to calculateHistoryFocusFraction(center, viewportCenter, viewportHeight)
            },
            timelineProgress = calculateHistoryTimelineProgress(
                visibleItems = visibleMathItems,
                totalRecordCount = uiState.records.size,
                viewportCenter = viewportCenter
            ),
            lastVisibleRecordIndex = visibleMathItems.maxOfOrNull { it.recordIndex }
        )
    }
        .distinctUntilChanged()
        .collect { historyViewport = it }
}
```

Replace the old `shouldLoadMore` calculation with:

```kotlin
val shouldLoadMore = remember(
    historyViewport.lastVisibleRecordIndex,
    uiState.records.size,
    uiState.canLoadMore,
    uiState.isLoadingMore,
    uiState.isLoading
) {
    derivedStateOf {
        shouldLoadMoreHistory(
            lastVisibleRecordIndex = historyViewport.lastVisibleRecordIndex,
            recordCount = uiState.records.size,
            canLoadMore = uiState.canLoadMore,
            loading = uiState.isLoadingMore || uiState.isLoading
        )
    }
}
```

Replace the old single timeline item with:

```kotlin
item(key = "history-timeline-header", contentType = "history-header") {
    InfiniteSectionHeader(
        title = "Attendance Timeline",
        subtitle = "Scroll untuk memusatkan detail kehadiran",
        leadingIcon = Icons.Outlined.CalendarMonth
    )
}
if (uiState.records.isEmpty()) {
    item(key = "history-timeline-empty", contentType = "history-empty") {
        InfiniteGlassReportCard {
            InfiniteEmptyState(
                title = "No attendance records",
                message = "No attendance records found for this period."
            )
        }
    }
} else {
    attendanceHistoryTimelineItems(
        records = uiState.records,
        focusByKey = historyViewport.focusByKey,
        timelineProgress = historyViewport.timelineProgress,
        motionEnabled = motionEnabled
    )
}
```

Use this reduced-motion read once per composition:

```kotlin
val context = LocalContext.current
val motionEnabled = remember(context) {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) > 0f
}
```

The History focus values must be passed directly to `graphicsLayer`; do not wrap them in `animateFloatAsState`, because center focus must reverse immediately with scroll.

- [ ] **Step 6: Compile and run all JVM tests**

Run:

```powershell
.\gradlew.bat --no-daemon app:test app:compileDebugAndroidTestKotlin
```

Expected: all JVM tests PASS and Android test sources compile. Do not claim instrumentation runtime PASS yet.

- [ ] **Step 7: Run repository quality gates**

Run:

```powershell
.\gradlew.bat --no-daemon app:lint app:assembleDebug
```

Expected: BUILD SUCCESSFUL for lint and debug assembly. If lint reports pre-existing warnings without failing, record them exactly; do not silently broaden this task to unrelated cleanup.

- [ ] **Step 8: Perform final source and scope checks**

Run:

```powershell
rg -n "Brush|radialGradient|AttendanceReportBackground|PermissionBackdrop|defaultGuidance|statusBarsPadding" app/src/main/java/com/example/infinite_track/presentation/screen/attendance/permission
rg -n "fun PermissionTimelinePill|fun AttendanceHistoryTimelinePill" app/src/main/java
rg -n "height\(56\.dp\)" app/src/main/java/com/example/infinite_track/presentation/screen/history app/src/main/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePill.kt
git diff --check
git status --short
```

Expected:

- first command has no matches;
- second command has exactly two public timeline-pill definitions;
- third command has no matches in the new History path;
- `git diff --check` exits 0;
- status contains only Task 4 files before commit.

- [ ] **Step 9: Commit Task 4**

```powershell
git add -- app/src/main/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePill.kt app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteAttendanceTimelineSection.kt app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineLazyItems.kt app/src/main/java/com/example/infinite_track/presentation/screen/history/HistoryScreen.kt app/src/androidTest/java/com/example/infinite_track/presentation/design/components/data/AttendanceHistoryTimelinePillTest.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/history/HistoryTimelineLazyItemsTest.kt
git commit -m "feat: add scroll-linked attendance history timeline"
```

## Runtime verification on `develop`

After review/PR integration into `develop`, run the app on an emulator or physical device and verify:

1. Initially ready required permissions open Attendance without flashing incomplete timeline content.
2. Completing the final required permission navigates once; Back does not return to readiness.
3. Denial, permanent denial, device-location Settings, and Settings-return refresh remain correct.
4. Optional disclosure is collapsed on a fresh destination and does not block manual Attendance.
5. 320 dp width and font scale 2.0 preserve all copy and 48 dp targets.
6. History focus moves continuously to the viewport center in both scroll directions.
7. History rail accent starts only when History records enter the viewport and reverses when scrolling upward.
8. Animator duration scale 0 keeps all History content fully opaque, unscaled, and readable.

Keep INF-230 marked Needs Verification until this runtime checklist passes on `develop`.
