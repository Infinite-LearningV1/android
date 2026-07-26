# INF-201 Face Frame / Progress-Rail State Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `FaceVerificationFrame` render the mockup-correct visual for every `LivenessState` and attach the 4-node liveness rail to the frame border, always in sync with `LivenessSequencer` progress.

**Architecture:** Presentation-only revision. `frameStyleFor()` becomes an exhaustive `when` returning a typed `FrameStyle` (RailMode/FrameBadge/FrameInnerContent enums replace booleans). Rail nodes are placed on the frame border via fraction-based offsets inside `BoxWithConstraints`. Timeout renders inner content in the frame plus a full-stage scrim owned by `FaceScannerScreen`.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), JUnit4 JVM tests, Compose androidTest. Spec: `docs/superpowers/specs/2026-07-26-inf-201-face-frame-state-sync-design.md`.

## Global Constraints

- Work happens in an isolated worktree branch off `develop` (create via superpowers:using-git-worktrees). Never commit directly on `develop`.
- Presentation-only: `FaceScannerViewModel`, `LivenessSequencer`, `FaceScannerTransitionPolicy`, `FaceDetectorHelper`, domain/data contracts must not change.
- Frame colors stay local to `FaceVerificationFrame.kt`: `FramePurple = Color(0xFF8A3DFF)`, `FrameCyan = Color(0xFF38F9F5)`, `FrameRed = Color(0xFFFF5C5C)` (red = CROSS badge only).
- All new user-visible copy goes through `res/values/strings.xml` (English) + `res/values-in/strings.xml` (Indonesian). No literals in composables.
- Rail progress data derives only from `state.challengeIndex` / `state.challengeTotal` — never introduce parallel progress state.
- Gradle needs JDK 17 (project verified locally with JBR 17). Run all commands from the worktree root.
- Commit message style follows the repo: `test(face): …`, `feat(face): …`, `docs(face): …`.

---

### Task 0: Worktree + docs commit

**Files:**
- Create: worktree branch `codex/inf-201-face-frame-state-sync` from `develop`
- Add: `docs/superpowers/specs/2026-07-26-inf-201-face-frame-state-sync-design.md` (copy from main checkout if untracked there)
- Add: `docs/superpowers/plans/2026-07-26-inf-201-face-frame-state-sync.md` (this file)

**Interfaces:**
- Consumes: nothing
- Produces: isolated branch containing the approved spec and this plan

- [ ] **Step 1: Create the worktree** (superpowers:using-git-worktrees)

```bash
git worktree add ../android-inf201-frame-sync -b codex/inf-201-face-frame-state-sync develop
```

- [ ] **Step 2: Copy both docs into the worktree and commit**

```bash
git add docs/superpowers/specs/2026-07-26-inf-201-face-frame-state-sync-design.md docs/superpowers/plans/2026-07-26-inf-201-face-frame-state-sync.md
git commit -m "docs(face): add INF-201 frame state sync spec and plan"
```

---

### Task 1: Typed `FrameStyle` model + exhaustive state mapping

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrameStyleTest.kt` (full rewrite)

**Interfaces:**
- Consumes: `FaceScannerState`, `LivenessState` (existing, unchanged)
- Produces: `enum class RailMode { HIDDEN, PROGRESS, ALL_PASSED }`, `enum class FrameBadge { NONE, CHECK, CROSS }`, `enum class FrameInnerContent { NONE, SILHOUETTE, TIMEOUT_INFO }`, `data class FrameStyle(color: Color, dashed: Boolean, railMode: RailMode, badge: FrameBadge, inner: FrameInnerContent)`, `fun frameStyleFor(state: FaceScannerState): FrameStyle`. Tasks 2–4 consume `style.railMode`, `style.badge`, `style.inner`.

- [ ] **Step 1: Rewrite the JVM test with the new contract (failing)**

Replace the entire content of `FaceVerificationFrameStyleTest.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class FaceVerificationFrameStyleTest {

    private fun styleOf(
        state: LivenessState,
        readyToVerify: Boolean = false
    ): FrameStyle = frameStyleFor(
        FaceScannerState(livenessState = state, readyToVerify = readyToVerify)
    )

    @Test
    fun `idle and detecting are dashed purple with silhouette and no rail`() {
        listOf(LivenessState.IDLE, LivenessState.DETECTING_FACE).forEach { s ->
            val style = styleOf(s)
            assertEquals(true, style.dashed)
            assertEquals(Color(0xFF8A3DFF), style.color)
            assertEquals(RailMode.HIDDEN, style.railMode)
            assertEquals(FrameBadge.NONE, style.badge)
            assertEquals(FrameInnerContent.SILHOUETTE, style.inner)
        }
    }

    @Test
    fun `waiting for liveness shows progress rail on solid purple frame`() {
        val style = styleOf(LivenessState.WAITING_FOR_LIVENESS)
        assertEquals(false, style.dashed)
        assertEquals(Color(0xFF8A3DFF), style.color)
        assertEquals(RailMode.PROGRESS, style.railMode)
        assertEquals(FrameBadge.NONE, style.badge)
        assertEquals(FrameInnerContent.NONE, style.inner)
    }

    @Test
    fun `low light hides the rail`() {
        val style = styleOf(LivenessState.LOW_LIGHT)
        assertEquals(false, style.dashed)
        assertEquals(RailMode.HIDDEN, style.railMode)
        assertEquals(FrameInnerContent.NONE, style.inner)
    }

    @Test
    fun `liveness detected shows progress until ready to verify`() {
        assertEquals(
            RailMode.PROGRESS,
            styleOf(LivenessState.LIVENESS_DETECTED).railMode
        )
        assertEquals(
            RailMode.ALL_PASSED,
            styleOf(LivenessState.LIVENESS_DETECTED, readyToVerify = true).railMode
        )
    }

    @Test
    fun `verifying keeps the all-passed rail`() {
        val style = styleOf(LivenessState.VERIFYING_FACE)
        assertEquals(RailMode.ALL_PASSED, style.railMode)
        assertEquals(FrameBadge.NONE, style.badge)
    }

    @Test
    fun `success is cyan with check badge and no rail`() {
        val style = styleOf(LivenessState.SUCCESS)
        assertEquals(Color(0xFF38F9F5), style.color)
        assertEquals(FrameBadge.CHECK, style.badge)
        assertEquals(RailMode.HIDDEN, style.railMode)
        assertEquals(false, style.dashed)
    }

    @Test
    fun `failure is cyan with cross badge not a red frame`() {
        val style = styleOf(LivenessState.FAILURE)
        assertEquals(Color(0xFF38F9F5), style.color)
        assertEquals(FrameBadge.CROSS, style.badge)
        assertEquals(RailMode.HIDDEN, style.railMode)
    }

    @Test
    fun `timeout is purple with timeout inner content`() {
        val style = styleOf(LivenessState.TIMEOUT)
        assertEquals(Color(0xFF8A3DFF), style.color)
        assertEquals(FrameInnerContent.TIMEOUT_INFO, style.inner)
        assertEquals(FrameBadge.NONE, style.badge)
        assertEquals(RailMode.HIDDEN, style.railMode)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.face.FaceVerificationFrameStyleTest"`
Expected: compilation FAILURE (`RailMode` unresolved) — that is the failing state for this cycle.

- [ ] **Step 3: Implement the model and mapping**

In `FaceVerificationFrame.kt`, replace the `FrameStyle` data class and `frameStyleFor` with:

```kotlin
enum class RailMode { HIDDEN, PROGRESS, ALL_PASSED }
enum class FrameBadge { NONE, CHECK, CROSS }
enum class FrameInnerContent { NONE, SILHOUETTE, TIMEOUT_INFO }

/**
 * Visual style of the face frame for a given phase. Derived purely so it can be unit-tested.
 */
data class FrameStyle(
    val color: Color,
    val dashed: Boolean,
    val railMode: RailMode,
    val badge: FrameBadge,
    val inner: FrameInnerContent
)

fun frameStyleFor(state: FaceScannerState): FrameStyle = when (state.livenessState) {
    LivenessState.IDLE,
    LivenessState.DETECTING_FACE -> FrameStyle(
        color = FramePurple, dashed = true,
        railMode = RailMode.HIDDEN, badge = FrameBadge.NONE,
        inner = FrameInnerContent.SILHOUETTE
    )

    LivenessState.WAITING_FOR_LIVENESS -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.PROGRESS, badge = FrameBadge.NONE,
        inner = FrameInnerContent.NONE
    )

    LivenessState.LOW_LIGHT -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.NONE,
        inner = FrameInnerContent.NONE
    )

    LivenessState.LIVENESS_DETECTED -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = if (state.readyToVerify) RailMode.ALL_PASSED else RailMode.PROGRESS,
        badge = FrameBadge.NONE, inner = FrameInnerContent.NONE
    )

    LivenessState.VERIFYING_FACE -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.ALL_PASSED, badge = FrameBadge.NONE,
        inner = FrameInnerContent.NONE
    )

    LivenessState.SUCCESS -> FrameStyle(
        color = FrameCyan, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.CHECK,
        inner = FrameInnerContent.NONE
    )

    LivenessState.FAILURE -> FrameStyle(
        color = FrameCyan, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.CROSS,
        inner = FrameInnerContent.NONE
    )

    LivenessState.TIMEOUT -> FrameStyle(
        color = FramePurple, dashed = false,
        railMode = RailMode.HIDDEN, badge = FrameBadge.NONE,
        inner = FrameInnerContent.TIMEOUT_INFO
    )
}
```

No `else` branch — the `when` must be exhaustive over `LivenessState`.

Then adapt the `FaceVerificationFrame` composable body minimally so the file compiles (final visuals arrive in Tasks 2–4):

```kotlin
        if (style.inner == FrameInnerContent.SILHOUETTE) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = FramePurple,
                modifier = Modifier.size(96.dp)
            )
        }

        if (style.railMode != RailMode.HIDDEN) {
            val passed = if (style.railMode == RailMode.ALL_PASSED) {
                state.challengeTotal
            } else {
                (state.challengeIndex - 1).coerceAtLeast(0)
            }
            val active = if (style.railMode == RailMode.ALL_PASSED) 0 else state.challengeIndex
            LivenessProgressRail(
                passedCount = passed,
                activeIndex = active,
                total = state.challengeTotal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }

        if (style.badge == FrameBadge.CHECK) {
            // Keep the existing badge Box body exactly as it is today
            // (Surface-background circle + purple check). Task 3 restyles it
            // and adds the CROSS variant.
        }
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.face.FaceVerificationFrameStyleTest"`
Expected: PASS (8 tests)

- [ ] **Step 5: Compile the full app + run the whole face test package**

Run: `./gradlew app:compileDebugKotlin app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL (other face tests unaffected)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrameStyleTest.kt
git commit -m "feat(face): replace frame style booleans with typed exhaustive state mapping"
```

---

### Task 2: Rail nodes attached to the frame border

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRail.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRailStateTest.kt` (add placement tests)
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt` (add border-rail test)

**Interfaces:**
- Consumes: `RailMode`, `FrameStyle` from Task 1; existing `railNodeStates(passedCount, activeIndex, total)` and `RailNode(number, state)` (visibility widened to `internal`)
- Produces: `enum class RailSide { LEFT, RIGHT }`, `data class RailNodePlacement(val side: RailSide, val heightFraction: Float)`, `fun railNodePlacements(total: Int): List<RailNodePlacement>`. Task 5 relies on `LivenessProgressRail` (the Row composable) being deleted.

- [ ] **Step 1: Write failing JVM placement tests**

Append to `LivenessProgressRailStateTest.kt`:

```kotlin
    @Test
    fun `four challenge placements follow the mockup border positions`() {
        val placements = railNodePlacements(4)

        assertEquals(
            listOf(
                RailNodePlacement(RailSide.LEFT, 0.20f),
                RailNodePlacement(RailSide.LEFT, 0.45f),
                RailNodePlacement(RailSide.RIGHT, 0.35f),
                RailNodePlacement(RailSide.RIGHT, 0.55f)
            ),
            placements
        )
    }

    @Test
    fun `non-standard totals alternate sides with even spacing`() {
        val placements = railNodePlacements(2)

        assertEquals(RailSide.LEFT, placements[0].side)
        assertEquals(RailSide.RIGHT, placements[1].side)
        assertEquals(1f / 3f, placements[0].heightFraction, 0.0001f)
        assertEquals(2f / 3f, placements[1].heightFraction, 0.0001f)
    }
```

(Use the existing imports in that file; add `org.junit.Assert.assertEquals` if missing.)

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.face.LivenessProgressRailStateTest"`
Expected: compilation FAILURE (`railNodePlacements` unresolved)

- [ ] **Step 3: Implement placements + widen `RailNode`, delete the Row composable**

In `LivenessProgressRail.kt`:

1. Change `private fun RailNode` to `internal fun RailNode`.
2. Delete the `LivenessProgressRail` composable (Row version) entirely.
3. Add below `railNodeStates`:

```kotlin
enum class RailSide { LEFT, RIGHT }

data class RailNodePlacement(val side: RailSide, val heightFraction: Float)

/**
 * Border positions for the liveness nodes, as fractions of the frame height. The 4-challenge
 * layout mirrors the approved mockups (1-2 on the left edge, 3-4 on the right); other totals
 * alternate sides with even spacing. Pure for JVM unit testing.
 */
fun railNodePlacements(total: Int): List<RailNodePlacement> =
    if (total == 4) {
        listOf(
            RailNodePlacement(RailSide.LEFT, 0.20f),
            RailNodePlacement(RailSide.LEFT, 0.45f),
            RailNodePlacement(RailSide.RIGHT, 0.35f),
            RailNodePlacement(RailSide.RIGHT, 0.55f)
        )
    } else {
        (1..total).map { n ->
            RailNodePlacement(
                side = if (n % 2 == 1) RailSide.LEFT else RailSide.RIGHT,
                heightFraction = n.toFloat() / (total + 1)
            )
        }
    }
```

- [ ] **Step 4: Render border nodes in `FaceVerificationFrame`**

Replace the Task 1 temporary rail block (the `LivenessProgressRail(...)` call) with border placement. The frame's root `Box` becomes `BoxWithConstraints`:

```kotlin
@Composable
fun FaceVerificationFrame(
    state: FaceScannerState,
    modifier: Modifier = Modifier
) {
    val style = frameStyleFor(state)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val frameInset = 8.dp
        val nodeSize = 28.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            // ... existing rounded-rect stroke drawing, unchanged ...
        }

        // ... silhouette block from Task 1, unchanged ...

        if (style.railMode != RailMode.HIDDEN) {
            val nodeStates = if (style.railMode == RailMode.ALL_PASSED) {
                railNodeStates(state.challengeTotal, 0, state.challengeTotal)
            } else {
                railNodeStates(
                    (state.challengeIndex - 1).coerceAtLeast(0),
                    state.challengeIndex,
                    state.challengeTotal
                )
            }
            val placements = railNodePlacements(state.challengeTotal)

            placements.forEachIndexed { i, placement ->
                val nodeCenterY = maxHeight * placement.heightFraction
                val nodeX = when (placement.side) {
                    RailSide.LEFT -> frameInset - nodeSize / 2
                    RailSide.RIGHT -> maxWidth - frameInset - nodeSize / 2
                }
                val tickX = when (placement.side) {
                    RailSide.LEFT -> nodeX + nodeSize
                    RailSide.RIGHT -> nodeX - 12.dp
                }

                // Short horizontal tick connecting the node to the frame interior.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = tickX, y = nodeCenterY - 1.dp)
                        .size(width = 12.dp, height = 2.dp)
                        .background(style.color)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = nodeX, y = nodeCenterY - nodeSize / 2)
                ) {
                    RailNode(number = i + 1, state = nodeStates[i])
                }
            }
        }

        // ... badge block, unchanged in this task ...
    }
}
```

New imports needed: `androidx.compose.foundation.layout.BoxWithConstraints`, `androidx.compose.foundation.layout.offset`, `androidx.compose.foundation.layout.width` is not needed (use `size(width, height)` overload from `androidx.compose.foundation.layout.size`).

- [ ] **Step 5: Run JVM tests**

Run: `./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.face.*"`
Expected: PASS (placement tests + all Task 1 tests)

- [ ] **Step 6: Add the Compose border-rail test (androidTest)**

Append to `FaceVerificationRedesignScreenTest.kt`:

```kotlin
    @Test
    fun livenessFrame_showsBorderRailNodesMatchingProgress() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(
                    livenessState = LivenessState.WAITING_FOR_LIVENESS,
                    challengeIndex = 2,
                    challengeTotal = 4
                )
            )
        }

        // Node 1 passed -> renders a check icon, not the number.
        composeTestRule.onAllNodesWithText("1").assertCountEquals(0)
        // Active node 2 and pending nodes 3, 4 render their numbers.
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun readyToVerifyFrame_showsAllNodesPassed() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(
                    livenessState = LivenessState.LIVENESS_DETECTED,
                    challengeIndex = 4,
                    challengeTotal = 4,
                    readyToVerify = true
                )
            )
        }

        listOf("1", "2", "3", "4").forEach { n ->
            composeTestRule.onAllNodesWithText(n).assertCountEquals(0)
        }
    }
```

- [ ] **Step 7: Compile androidTest sources**

Run: `./gradlew app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL (execution happens on-device in Task 5)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRail.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRailStateTest.kt app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt
git commit -m "feat(face): attach liveness rail nodes to the frame border"
```

---

### Task 3: CHECK / CROSS badges with accessible descriptions

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-in/strings.xml`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt`

**Interfaces:**
- Consumes: `FrameBadge` from Task 1
- Produces: string resources `face_frame_badge_verified`, `face_frame_badge_not_matched`; badge rendering keyed by `style.badge`

- [ ] **Step 1: Add string resources**

`res/values/strings.xml` (next to the other `face_` strings):

```xml
    <string name="face_frame_badge_verified">Face verified</string>
    <string name="face_frame_badge_not_matched">Face not matched</string>
```

`res/values-in/strings.xml`:

```xml
    <string name="face_frame_badge_verified">Wajah terverifikasi</string>
    <string name="face_frame_badge_not_matched">Wajah tidak cocok</string>
```

- [ ] **Step 2: Write failing androidTest assertions**

Append to `FaceVerificationRedesignScreenTest.kt` (imports: `androidx.compose.ui.test.onNodeWithContentDescription`):

```kotlin
    @Test
    fun successFrame_showsCheckBadgeWithDescription() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(livenessState = LivenessState.SUCCESS)
            )
        }

        composeTestRule.onNodeWithContentDescription("Face verified").assertIsDisplayed()
    }

    @Test
    fun failureFrame_showsCrossBadgeWithDescription() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(livenessState = LivenessState.FAILURE)
            )
        }

        composeTestRule.onNodeWithContentDescription("Face not matched").assertIsDisplayed()
    }
```

- [ ] **Step 3: Implement the badge block**

Replace the Task 1 badge block in `FaceVerificationFrame.kt`:

```kotlin
        if (style.badge != FrameBadge.NONE) {
            val badgeColor = when (style.badge) {
                FrameBadge.CHECK -> FramePurple
                FrameBadge.CROSS -> FrameRed
                FrameBadge.NONE -> Color.Transparent
            }
            val badgeIcon = when (style.badge) {
                FrameBadge.CROSS -> Icons.Filled.Close
                else -> Icons.Filled.Check
            }
            val badgeDescription = when (style.badge) {
                FrameBadge.CROSS -> stringResource(R.string.face_frame_badge_not_matched)
                else -> stringResource(R.string.face_frame_badge_verified)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = badgeDescription,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
```

New imports: `androidx.compose.material.icons.filled.Close`, `androidx.compose.ui.res.stringResource`, `com.example.infinite_track.R`. The old Surface-background/purple-check badge is gone (`InfiniteColors.Surface` import may drop out if now unused).

- [ ] **Step 4: Compile + JVM regression**

Run: `./gradlew app:compileDebugKotlin app:compileDebugAndroidTestKotlin app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all JVM tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt
git commit -m "feat(face): restyle success badge and add failure cross badge with a11y copy"
```

---

### Task 4: TIMEOUT inner content + full-stage scrim

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt` (scrim layer between camera and frame, around line 300–316)
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-in/strings.xml`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt`

**Interfaces:**
- Consumes: `FrameInnerContent.TIMEOUT_INFO` from Task 1
- Produces: string resources `face_frame_timeout_title`, `face_frame_timeout_message`, `face_frame_timeout_countdown`, `face_frame_timeout_times_up`; private composable `TimeoutInnerContent()`

- [ ] **Step 1: Add string resources**

`res/values/strings.xml`:

```xml
    <string name="face_frame_timeout_title">Verification Timeout</string>
    <string name="face_frame_timeout_message">You didn\'t complete the verification in time. Please try again.</string>
    <string name="face_frame_timeout_countdown">00:00</string>
    <string name="face_frame_timeout_times_up">Time\'s up</string>
```

`res/values-in/strings.xml`:

```xml
    <string name="face_frame_timeout_title">Waktu Verifikasi Habis</string>
    <string name="face_frame_timeout_message">Verifikasi tidak selesai tepat waktu. Silakan coba lagi.</string>
    <string name="face_frame_timeout_countdown">00:00</string>
    <string name="face_frame_timeout_times_up">Waktu habis</string>
```

- [ ] **Step 2: Write failing androidTest**

Append to `FaceVerificationRedesignScreenTest.kt`:

```kotlin
    @Test
    fun timeoutFrame_showsTimeoutContent() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(livenessState = LivenessState.TIMEOUT)
            )
        }

        composeTestRule.onNodeWithText("Verification Timeout").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Time's up").assertIsDisplayed()
    }
```

- [ ] **Step 3: Implement `TimeoutInnerContent` and wire it**

In `FaceVerificationFrame.kt`, inside the `BoxWithConstraints` content add:

```kotlin
        if (style.inner == FrameInnerContent.TIMEOUT_INFO) {
            TimeoutInnerContent(modifier = Modifier.align(Alignment.Center))
        }
```

And at file scope:

```kotlin
@Composable
private fun TimeoutInnerContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(InfiniteColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = FramePurple,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = stringResource(R.string.face_frame_timeout_title),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.face_frame_timeout_message),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(4.dp, InfiniteColors.Secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.face_frame_timeout_countdown),
                    color = InfiniteColors.Secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = stringResource(R.string.face_frame_timeout_times_up),
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}
```

New imports: `androidx.compose.foundation.border`, `androidx.compose.foundation.layout.Arrangement`, `androidx.compose.foundation.layout.Column`, `androidx.compose.material.icons.outlined.Timer`, `androidx.compose.material3.Text`, `androidx.compose.ui.text.font.FontWeight`, `androidx.compose.ui.text.style.TextAlign`, `androidx.compose.ui.unit.sp`.

- [ ] **Step 4: Add the full-stage scrim in `FaceScannerScreen`**

Between the `CameraPreview(...)` call and the `FaceVerificationFrame(...)` call (currently lines ~300–316):

```kotlin
        // Layer 1.5: Dim the whole stage when the verification timed out (mockup behavior).
        if (uiState.livenessState == LivenessState.TIMEOUT) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        }
```

(`Color` and `background` are already imported in that file; verify and add if missing.)

- [ ] **Step 5: Compile + JVM regression**

Run: `./gradlew app:compileDebugKotlin app:compileDebugAndroidTestKotlin app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, JVM tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-in/strings.xml app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt
git commit -m "feat(face): render mockup timeout content inside the frame with stage scrim"
```

---

### Task 5: Full gate + cleanup + runtime evidence

**Files:**
- Verify: whole `:app` module; no source changes expected beyond fixes surfaced by lint/tests

**Interfaces:**
- Consumes: everything above
- Produces: green local gate; device evidence or explicit `Needs Verification`

- [ ] **Step 1: Grep for stale references**

Run: `grep -rn "LivenessProgressRail(" app/src --include=*.kt`
Expected: no call sites remain (the composable was deleted in Task 2; `railNodeStates` / `RailNode` / `railNodePlacements` references are fine). If any preview or screen still calls it, replace that call with `FaceVerificationFrame` usage or delete the preview block.

- [ ] **Step 2: Run the full local gate**

```bash
./gradlew app:test app:lint app:assembleDebug app:compileDebugAndroidTestKotlin
```

Expected: BUILD SUCCESSFUL on all four. Fix anything surfaced (unused imports in `FaceVerificationFrame.kt` / `LivenessProgressRail.kt` are likely candidates) and re-run.

- [ ] **Step 3: Device/emulator evidence (CLAUDE.md runtime gate)**

```bash
adb devices -l
./gradlew app:connectedDebugAndroidTest --tests "com.example.infinite_track.presentation.screen.attendance.face.*"
```

If a device is connected: capture screenshots of liveness (rail on border), low light (no rail), readyToVerify (all passed), success (purple check badge), failure (cyan frame + red cross), timeout (purple frame + timeout content + scrim). Redact any personal data before attaching evidence.

If no device: record the item as `Needs Verification` with the exact missing command — do not claim Done.

- [ ] **Step 4: Commit any gate fixes**

```bash
git add -A
git commit -m "test(face): green local gate for frame state sync"
```

(Skip if Step 2 required no changes.)

- [ ] **Step 5: Push branch + open PR to `develop`**

```bash
git push -u origin codex/inf-201-face-frame-state-sync
gh pr create --base develop --title "[INF-201] Sync face frame visuals and border rail with scanner states" --body "..."
```

PR body must list: state→visual table, test evidence, and the runtime `Needs Verification` items if any.

---

## Self-Review Notes

- Spec coverage: state table → Task 1; border rail → Task 2; badges → Task 3; timeout + scrim → Task 4; strings → Tasks 3–4; tests/gate/evidence → every task + Task 5. `LivenessProgressRail` deletion → Task 2 Step 3 + Task 5 Step 1.
- Type consistency: `RailMode`/`FrameBadge`/`FrameInnerContent`/`FrameStyle` defined in Task 1 and consumed with identical names in Tasks 2–4; `railNodePlacements(total)` defined and consumed as written.
- No placeholders: every code step carries the actual code; the only elisions are explicitly "unchanged" existing blocks.
