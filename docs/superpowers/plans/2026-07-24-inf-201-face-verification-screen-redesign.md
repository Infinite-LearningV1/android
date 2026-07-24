# INF-201 Face Verification Screen Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the single-route face verification screen to match the approved mockups — 4-step liveness (Blink → Smile → Turn Left → Turn Right) with a numbered progress rail, neon `FaceVerificationFrame`, status pills, guidance text, a similarity/threshold diagnostics card, and a phase-aware bottom sheet — reusing existing Infinite components.

**Architecture:** Evolve the existing scanner in place. Extract pure, JVM-testable logic (challenge sequencer, head-turn evaluator, rail node states, diagnostics formatting, match-result mapping) and keep Compose as thin renderers. Expand `FaceScannerViewModel` for the 4-challenge phase model with per-challenge timeout. Change `VerifyFaceUseCase` to return a typed result carrying similarity/threshold. Keep the single `FaceScanner` route + `savedStateHandle` handoff; backend stays the only attendance authority.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt, StateFlow, CameraX, ML Kit Face Detection (eye-open, smiling, `headEulerAngleY`), TensorFlow Lite, coroutines, JUnit4 + kotlinx-coroutines-test (no Mockito/Robolectric — use hand-written fakes), Compose UI Test.

**Design spec:** `docs/superpowers/specs/2026-07-24-inf-201-face-verification-screen-redesign-design.md`

## Global Constraints

- Work only in the isolated worktree for branch `codex/inf-201-face-verification-spec-plan`. Do not edit the main checkout.
- Build/test via the canonical CLI command: PowerShell with `$env:JAVA_HOME="D:\Java_Home\java 1.8.2"`, `$env:ANDROID_HOME="C:\Users\Febriyadi\AppData\Local\Android\Sdk"`, PATH prepend, `Set-Location <worktree>`, then `.\gradlew.bat --no-daemon <task> --console=plain`. If kapt fails with `IllegalAccessError ... com.sun.tools.javac`, stop stale Kotlin daemons (`.\gradlew.bat --stop` + kill `KotlinCompileDaemon`) and rerun. Never edit `gradle.properties`.
- Keep the single `Screen.FaceScanner` route + `savedStateHandle` handoff; `FaceVerificationResult` enum transport unchanged. Face success ≠ attendance success.
- Reuse `InfiniteTopBar`, `InfiniteStatusPill`, `StatefulButton`/`InfiniteButton`, `BottomSheetScaffold` glass pattern, `InfiniteColors`, `InfiniteIcons`.
- No new ML model, no CameraX pipeline rewrite, no nested graph, no new repository.
- Similarity/threshold are shown in UI (approved override); raw embedding vectors are never displayed, logged, in nav args, or saved state.
- No Mockito/Robolectric: pure logic gets JVM unit tests; Compose gets androidTest (compile-checked, device run = Needs Verification if no device).
- Required verification: `app:testDebugUnitTest`, `app:compileDebugAndroidTestKotlin`, `app:lintDebug`, `app:assembleDebug`.

---

## File Structure

### Create

```text
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencer.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/HeadTurnEvaluator.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRail.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationTopBar.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceDiagnosticsCard.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationBottomSheet.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencerTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/HeadTurnEvaluatorTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRailStateTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceDiagnosticsUiModelTest.kt
app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt
```

### Modify

```text
app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt
app/src/main/java/com/example/infinite_track/domain/use_case/auth/FaceMatchDiagnostics.kt
app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceOutcomeMapper.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultSurface.kt
app/src/test/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCaseLoggingTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceOutcomeMapperTest.kt
app/src/main/res/values/strings.xml
```

Reconciliation note: `LivenessChallenge` already exists in `FaceScannerViewModel.kt` with `{ BLINK, SMILE }`; this plan extends it. `FaceOutcomeMapper`/`FaceMatchOutcome` (Task-4 prior work) are updated to carry similarity/threshold.

---

### Task 1: Liveness challenge sequencer (pure)

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencer.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencerTest.kt`
- Modify: `FaceScannerViewModel.kt` (move/extend `LivenessChallenge` enum to include the two new challenges)

**Interfaces:** `LivenessChallenge { BLINK, SMILE, TURN_LEFT, TURN_RIGHT }`, `CHALLENGE_ORDER`, `LivenessSequencer` with `current`, `index` (1-based), `passedCount`, `isComplete`, `pass()`, `reset()`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LivenessSequencerTest {
    @Test fun `starts at first challenge`() {
        val s = LivenessSequencer()
        assertEquals(LivenessChallenge.BLINK, s.current)
        assertEquals(1, s.index)
        assertEquals(0, s.passedCount)
        assertFalse(s.isComplete)
    }

    @Test fun `advances through fixed order`() {
        val s = LivenessSequencer()
        s.pass(); assertEquals(LivenessChallenge.SMILE, s.current); assertEquals(2, s.index)
        s.pass(); assertEquals(LivenessChallenge.TURN_LEFT, s.current)
        s.pass(); assertEquals(LivenessChallenge.TURN_RIGHT, s.current)
        assertFalse(s.isComplete)
        s.pass(); assertEquals(4, s.passedCount); assertTrue(s.isComplete)
    }

    @Test fun `reset returns to start`() {
        val s = LivenessSequencer()
        s.pass(); s.pass(); s.reset()
        assertEquals(LivenessChallenge.BLINK, s.current)
        assertEquals(0, s.passedCount)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*LivenessSequencerTest" --console=plain`
Expected: FAIL (`LivenessSequencer` missing; `TURN_LEFT/TURN_RIGHT` missing).

- [ ] **Step 3: Implement**

In `FaceScannerViewModel.kt`, extend the enum:

```kotlin
enum class LivenessChallenge { BLINK, SMILE, TURN_LEFT, TURN_RIGHT }
```

Create `LivenessSequencer.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

val CHALLENGE_ORDER: List<LivenessChallenge> = listOf(
    LivenessChallenge.BLINK,
    LivenessChallenge.SMILE,
    LivenessChallenge.TURN_LEFT,
    LivenessChallenge.TURN_RIGHT
)

class LivenessSequencer(private val order: List<LivenessChallenge> = CHALLENGE_ORDER) {
    var passedCount: Int = 0
        private set

    val current: LivenessChallenge
        get() = order[passedCount.coerceIn(0, order.lastIndex)]
    val index: Int get() = (passedCount + 1).coerceAtMost(order.size)
    val total: Int get() = order.size
    val isComplete: Boolean get() = passedCount >= order.size

    fun pass() { if (passedCount < order.size) passedCount++ }
    fun reset() { passedCount = 0 }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*LivenessSequencerTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencer.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencerTest.kt
git commit -m "feat(face): add 4-challenge liveness sequencer"
```

---

### Task 2: Head-turn evaluator (pure) + FaceDetectorHelper wiring

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/HeadTurnEvaluator.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/HeadTurnEvaluatorTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt`

**Interfaces:** `HeadTurnEvaluator.evaluate(eulerY: Float, direction: LivenessChallenge, threshold: Float = 20f): LivenessResult`; `FaceDetectorHelper.verifyHeadTurn(face, direction): LivenessResult`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

import com.example.infinite_track.data.face.LivenessResult
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadTurnEvaluatorTest {
    // Front camera: positive eulerY = user turns to their right; negative = their left.
    @Test fun `turn right success when eulerY beyond positive threshold`() {
        assertEquals(LivenessResult.SUCCESS, HeadTurnEvaluator.evaluate(28f, LivenessChallenge.TURN_RIGHT))
    }
    @Test fun `turn left success when eulerY beyond negative threshold`() {
        assertEquals(LivenessResult.SUCCESS, HeadTurnEvaluator.evaluate(-28f, LivenessChallenge.TURN_LEFT))
    }
    @Test fun `in progress near threshold`() {
        assertEquals(LivenessResult.IN_PROGRESS, HeadTurnEvaluator.evaluate(14f, LivenessChallenge.TURN_RIGHT))
    }
    @Test fun `failure when centered`() {
        assertEquals(LivenessResult.FAILURE, HeadTurnEvaluator.evaluate(2f, LivenessChallenge.TURN_RIGHT))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*HeadTurnEvaluatorTest" --console=plain`
Expected: FAIL.

- [ ] **Step 3: Implement**

`HeadTurnEvaluator.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

import com.example.infinite_track.data.face.LivenessResult
import kotlin.math.abs

object HeadTurnEvaluator {
    private const val SUCCESS_DEGREES = 22f
    private const val PROGRESS_DEGREES = 10f

    fun evaluate(
        eulerY: Float,
        direction: LivenessChallenge,
        threshold: Float = SUCCESS_DEGREES
    ): LivenessResult {
        val signed = when (direction) {
            LivenessChallenge.TURN_RIGHT -> eulerY
            LivenessChallenge.TURN_LEFT -> -eulerY
            else -> return LivenessResult.FAILURE
        }
        return when {
            signed >= threshold -> LivenessResult.SUCCESS
            signed >= PROGRESS_DEGREES -> LivenessResult.IN_PROGRESS
            else -> LivenessResult.FAILURE
        }
    }
}
```

In `FaceDetectorHelper.kt`, add (near `verifySmile`):

```kotlin
fun verifyHeadTurn(
    face: com.google.mlkit.vision.face.Face,
    direction: com.example.infinite_track.presentation.screen.attendance.face.LivenessChallenge
): LivenessResult =
    com.example.infinite_track.presentation.screen.attendance.face.HeadTurnEvaluator
        .evaluate(face.headEulerAngleY, direction)
```

- [ ] **Step 4: Run tests + compile**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*HeadTurnEvaluatorTest" app:compileDebugKotlin --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/HeadTurnEvaluator.kt app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/HeadTurnEvaluatorTest.kt
git commit -m "feat(face): add head-turn liveness evaluator"
```

---

### Task 3: Typed match result (similarity + threshold)

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/FaceMatchDiagnostics.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceOutcomeMapper.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCaseLoggingTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceOutcomeMapperTest.kt`

**Interfaces:** `VerifyFaceMatch(isMatch, similarity, threshold)`; `VerifyFaceUseCase.invoke(Bitmap): Result<VerifyFaceMatch>`; `FaceOutcomeMapper.fromMatch(Result<VerifyFaceMatch>): FaceMatchOutcome` where `FaceMatchOutcome` gains `similarity`/`threshold` nullable.

- [ ] **Step 1: Write failing tests**

Replace the body of `FaceOutcomeMapperTest.kt` match cases:

```kotlin
@Test fun `match maps to success carrying score`() {
    val outcome = FaceOutcomeMapper.fromMatch(Result.success(VerifyFaceMatch(true, 0.42f, 0.15f)))
    assertEquals(LivenessState.SUCCESS, outcome.livenessState)
    assertEquals(0.42f, outcome.similarity)
    assertEquals(0.15f, outcome.threshold)
}
@Test fun `no match carries score and NOT_MATCHED`() {
    val outcome = FaceOutcomeMapper.fromMatch(Result.success(VerifyFaceMatch(false, 0.05f, 0.15f)))
    assertEquals(FaceVerificationFailureReason.NOT_MATCHED, outcome.failureReason)
    assertEquals(0.05f, outcome.similarity)
}
@Test fun `failure maps to TECHNICAL_FAILURE without score`() {
    val outcome = FaceOutcomeMapper.fromMatch(Result.failure(IllegalStateException("x")))
    assertEquals(FaceVerificationFailureReason.TECHNICAL_FAILURE, outcome.failureReason)
    assertNull(outcome.similarity)
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*FaceOutcomeMapperTest" --console=plain`
Expected: FAIL (`VerifyFaceMatch` missing; `similarity` field missing).

- [ ] **Step 3: Implement**

In `VerifyFaceUseCase.kt`, add the result type and change the return:

```kotlin
data class VerifyFaceMatch(
    val isMatch: Boolean,
    val similarity: Float,
    val threshold: Float
)
```

Change `invoke` to compute `similarity`, build `VerifyFaceMatch(isMatch, similarity, SIMILARITY_THRESHOLD)`, and `Result.success(match)`. Keep the debug diagnostics log (no raw embedding). Update `FaceMatchDiagnostics.kt` only if needed for the new field names (keep `FaceMatchDiagnosticsFactory` as-is).

In `FaceOutcomeMapper.kt`, extend `FaceMatchOutcome`:

```kotlin
data class FaceMatchOutcome(
    val livenessState: LivenessState,
    val failureReason: FaceVerificationFailureReason?,
    val similarity: Float? = null,
    val threshold: Float? = null
)

object FaceOutcomeMapper {
    fun fromMatch(result: Result<VerifyFaceMatch>): FaceMatchOutcome = result.fold(
        onSuccess = { m ->
            if (m.isMatch) FaceMatchOutcome(LivenessState.SUCCESS, null, m.similarity, m.threshold)
            else FaceMatchOutcome(LivenessState.FAILURE, FaceVerificationFailureReason.NOT_MATCHED, m.similarity, m.threshold)
        },
        onFailure = { FaceMatchOutcome(LivenessState.FAILURE, FaceVerificationFailureReason.TECHNICAL_FAILURE) }
    )
}
```

`VerifyFaceMatch` is in the `auth` use-case package; add the import to `FaceOutcomeMapper.kt` and `FaceOutcomeMapperTest.kt`.

Update `VerifyFaceUseCaseLoggingTest.kt` only if the diagnostics factory signature changed (it does not; keep as-is).

- [ ] **Step 4: Run tests**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*FaceOutcomeMapperTest" --tests "*VerifyFaceUseCaseLoggingTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt app/src/main/java/com/example/infinite_track/domain/use_case/auth/FaceMatchDiagnostics.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceOutcomeMapper.kt app/src/test/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCaseLoggingTest.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceOutcomeMapperTest.kt
git commit -m "feat(face): return similarity and threshold from verification"
```

---

### Task 4: Expand ViewModel phase model + per-challenge timeout

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt`
- Test: extend `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessSequencerTest.kt` is not enough — add pure reducer coverage inline where feasible.

**Interfaces:** `FaceScannerState` gains `challengeIndex: Int`, `challengeTotal: Int = 4`, `readyToVerify: Boolean`, `similarity: Float?`, `threshold: Float?`. New public `onVerifyClicked()`. Per-challenge 20s timeout via monotonic deadline reset on each `pass()`.

- [ ] **Step 1: Write failing test (pure per-challenge timeout policy)**

Add to a new pure helper `ChallengeTimeout` and test it (JVM-safe):

```kotlin
// FaceScannerViewModelReasonTest additions or a new ChallengeTimeoutTest
@Test fun `remaining seconds ceils from millis`() {
    assertEquals(20, ChallengeTimeout.remainingSeconds(deadlineMillis = 20_000, nowMillis = 0))
    assertEquals(1, ChallengeTimeout.remainingSeconds(deadlineMillis = 20_000, nowMillis = 19_500))
    assertEquals(0, ChallengeTimeout.remainingSeconds(deadlineMillis = 20_000, nowMillis = 20_000))
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*ChallengeTimeout*" --console=plain`
Expected: FAIL.

- [ ] **Step 3: Implement**

Add pure helper in `FaceScannerViewModel.kt` (top-level `object`):

```kotlin
object ChallengeTimeout {
    const val PER_CHALLENGE_MILLIS = 20_000L
    fun remainingSeconds(deadlineMillis: Long, nowMillis: Long): Int =
        (((deadlineMillis - nowMillis).coerceAtLeast(0)) / 1000.0).let { kotlin.math.ceil(it).toInt() }
}
```

Then wire the ViewModel: hold a `LivenessSequencer`; on a passed challenge, `sequencer.pass()`, reset the per-challenge deadline, update `challengeIndex`; when `sequencer.isComplete`, set `readyToVerify = true` (do not auto-verify). Replace the single random challenge in `initializeScanner()` with `sequencer.reset()` + first challenge. Route detection to the current challenge: `BLINK`→`verifyBlink`, `SMILE`→`verifySmile`, `TURN_LEFT`/`TURN_RIGHT`→`verifyHeadTurn`. Add `onVerifyClicked()` that guards on `readyToVerify` then runs `proceedWithFaceVerification()`; map result via `FaceOutcomeMapper.fromMatch` and store `similarity`/`threshold` into state. Timeout uses `clock.elapsedRealtime()`-style deadline (reuse existing coroutine countdown but reset on each pass).

- [ ] **Step 4: Run tests + compile**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*ChallengeTimeout*" --tests "*FaceOutcomeMapperTest" app:compileDebugKotlin --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face
git commit -m "feat(face): 4-challenge phase model with per-challenge timeout"
```

---

### Task 5: LivenessProgressRail (rail node states)

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRail.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRailStateTest.kt`

**Interfaces:** pure `railNodeStates(passedCount: Int, activeIndex: Int, total: Int = 4): List<RailNodeState>` where `RailNodeState { PASSED, ACTIVE, PENDING }`; composable `LivenessProgressRail(passedCount, activeIndex, modifier)`.

- [ ] **Step 1: Write failing test**

```kotlin
class LivenessProgressRailStateTest {
    @Test fun `node states reflect progress`() {
        val states = railNodeStates(passedCount = 1, activeIndex = 2)
        assertEquals(RailNodeState.PASSED, states[0])
        assertEquals(RailNodeState.ACTIVE, states[1])
        assertEquals(RailNodeState.PENDING, states[2])
        assertEquals(RailNodeState.PENDING, states[3])
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*LivenessProgressRailStateTest" --console=plain`
Expected: FAIL.

- [ ] **Step 3: Implement**

`LivenessProgressRail.kt` (pure fn + composable). Pure fn:

```kotlin
enum class RailNodeState { PASSED, ACTIVE, PENDING }

fun railNodeStates(passedCount: Int, activeIndex: Int, total: Int = 4): List<RailNodeState> =
    (1..total).map { n ->
        when {
            n <= passedCount -> RailNodeState.PASSED
            n == activeIndex -> RailNodeState.ACTIVE
            else -> RailNodeState.PENDING
        }
    }
```

Composable renders numbered circles (cyan+✓ passed, orange active, white pending) with connectors, using `InfiniteColors.Accent`/`Secondary`/`Surface` and `Icons.Filled.Check`. Positions mirror the mockup (nodes 1–2 left, 3–4 right).

- [ ] **Step 4: Run test + compile**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*LivenessProgressRailStateTest" app:compileDebugKotlin --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRail.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/LivenessProgressRailStateTest.kt
git commit -m "feat(face): add numbered liveness progress rail"
```

---

### Task 6: FaceVerificationFrame (neon frame styles)

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt`

**Interfaces:** `FrameStyle(color, dashed, showRail, showCheckBadge, showSilhouette)`; `frameStyleFor(state: FaceScannerState): FrameStyle` (pure); `FaceVerificationFrame(state, modifier)` drawing a rounded-rect neon border via `Canvas` (solid or dashed via `PathEffect.dashPathEffect`), optional top-center ✓ badge, optional centered silhouette + scan corners, and the rail when `showRail`.

- [ ] **Step 1: Write failing test (pure style mapping)**

```kotlin
class FaceVerificationFrameStyleTest {
    @Test fun `no face is dashed purple with silhouette`() {
        val s = frameStyleFor(FaceScannerState(livenessState = LivenessState.DETECTING_FACE))
        assertTrue(s.dashed); assertTrue(s.showSilhouette); assertFalse(s.showRail)
    }
    @Test fun `success is cyan with badge no rail`() {
        val s = frameStyleFor(FaceScannerState(livenessState = LivenessState.SUCCESS))
        assertTrue(s.showCheckBadge); assertFalse(s.showRail)
    }
}
```

Place in `app/src/test/java/.../face/FaceVerificationFrameStyleTest.kt`.

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*FaceVerificationFrameStyleTest" --console=plain`
Expected: FAIL.

- [ ] **Step 3: Implement**

Implement `frameStyleFor` (pure, JVM-safe — uses `Color` from `androidx.compose.ui.graphics`, which is JVM-available) mapping: `DETECTING_FACE`/no-face → dashed purple + silhouette; `WAITING_FOR_LIVENESS`/liveness → solid purple + rail; `SUCCESS` → cyan + badge; `FAILURE`/`TIMEOUT` → red. Implement `FaceVerificationFrame` composable drawing the border on `Canvas` and overlaying `LivenessProgressRail` when `showRail`.

- [ ] **Step 4: Run test + compile**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*FaceVerificationFrameStyleTest" app:compileDebugKotlin --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrame.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationFrameStyleTest.kt
git commit -m "feat(face): add neon FaceVerificationFrame with phase styles"
```

---

### Task 7: Top bar (subtitle + pill), status pills, guidance text

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationTopBar.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:** `FaceVerificationTopBar(title, subtitle, intentLabel, onBack)`; helper composables `FaceStatusPillRow(state)` and `FaceGuidanceText(state)` (may live in the same file or `FaceScannerScreen.kt`).

- [ ] **Step 1: Add challenge/guidance strings**

Add to `strings.xml`: `face_challenge_blink_title/hint`, `face_challenge_smile_title/hint`, `face_challenge_turn_left_title/hint`, `face_challenge_turn_right_title/hint`, `face_time_remaining` (with `%1$d`), `face_no_face_tips`, plus pill labels (`face_pill_face_detected`, `face_pill_no_face`, `face_pill_challenge` with `%1$d`/`%2$d`, `face_pill_position`, `face_pill_matched`, `face_pill_similarity_verified`).

- [ ] **Step 2: Implement top bar wrapper**

Build `FaceVerificationTopBar` as a floating glass card (reuse `InfiniteColors.AttendanceReportGlassSurface/Border`, `InfiniteTopBarActionButton` for back) with a two-line centered title/subtitle and a right-side `● {intentLabel}` pill in `InfiniteColors.Primary`. Reference `InfiniteTopBar.kt` for the glass styling.

- [ ] **Step 3: Implement pill row + guidance text**

`FaceStatusPillRow(state)` renders left/right `InfiniteStatusPill`s per the spec table using `leadingIcon` (CheckCircle/WarningAmber) and `colorOverride`. `FaceGuidanceText(state)` renders challenge title+hint (from strings by `state`’s current challenge), `Time remaining: {n}s` with `Icons.Filled.Schedule` when counting down, or the `Tips` block for no-face.

- [ ] **Step 4: Compile**

Run: `.\gradlew.bat --no-daemon app:compileDebugKotlin --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationTopBar.kt app/src/main/res/values/strings.xml
git commit -m "feat(face): add redesigned top bar, status pills, guidance text"
```

---

### Task 8: Diagnostics card (similarity/threshold/result)

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceDiagnosticsCard.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceDiagnosticsUiModelTest.kt`

**Interfaces:** pure `faceDiagnosticsUiModel(similarity: Float?, threshold: Float?, isMatch: Boolean?): FaceDiagnosticsUiModel?` (null when no score); composable `FaceDiagnosticsCard(model)`.

- [ ] **Step 1: Write failing test**

```kotlin
class FaceDiagnosticsUiModelTest {
    @Test fun `null when no score`() {
        assertNull(faceDiagnosticsUiModel(null, null, null))
    }
    @Test fun `formats matched`() {
        val m = faceDiagnosticsUiModel(0.197f, 0.15f, true)!!
        assertEquals("0.197", m.similarityText)
        assertEquals("0.15", m.thresholdText)
        assertTrue(m.matched)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*FaceDiagnosticsUiModelTest" --console=plain`
Expected: FAIL.

- [ ] **Step 3: Implement**

```kotlin
data class FaceDiagnosticsUiModel(
    val similarityText: String,
    val thresholdText: String,
    val matched: Boolean
)

fun faceDiagnosticsUiModel(similarity: Float?, threshold: Float?, isMatch: Boolean?): FaceDiagnosticsUiModel? {
    if (similarity == null || threshold == null || isMatch == null) return null
    fun fmt(v: Float) = (kotlin.math.round(v * 1000f) / 1000f).toString().trimEnd('0').trimEnd('.')
    return FaceDiagnosticsUiModel(fmt(similarity), fmt(threshold), isMatch)
}
```

Composable `FaceDiagnosticsCard` renders the dark rounded card with three labeled rows + trailing status icons (green check / red cross), shown whenever `model != null`. All build variants.

- [ ] **Step 4: Run test + compile**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --tests "*FaceDiagnosticsUiModelTest" app:compileDebugKotlin --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceDiagnosticsCard.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceDiagnosticsUiModelTest.kt
git commit -m "feat(face): add similarity/threshold diagnostics card"
```

---

### Task 9: Phase-aware bottom sheet + assemble the redesigned screen

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationBottomSheet.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultSurface.kt` (fold into the new sheet or keep for terminal copy)
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt`

**Interfaces:** `FaceVerificationBottomSheet(state, onVerify, onContinue, onTryAgain, onCancel)`; `FaceScannerScreen` composes top bar + pills + `FaceVerificationFrame` + guidance + diagnostics card + bottom sheet inside the existing `BottomSheetScaffold`, wiring events to the ViewModel (`onVerifyClicked`, `resetScanner`, `publishExitStateOnce`, SUCCESS publish).

- [ ] **Step 1: Write Compose tests for phases**

```kotlin
class FaceVerificationRedesignScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun liveness_showsVerifyDisabledAndChallengePill() {
        compose.setContent {
            FaceVerificationBottomSheet(
                state = FaceScannerState(livenessState = LivenessState.WAITING_FOR_LIVENESS, challengeIndex = 2, readyToVerify = false),
                onVerify = {}, onContinue = {}, onTryAgain = {}, onCancel = {}
            )
        }
        compose.onNodeWithText("Verify").assertIsDisplayed()
    }

    @Test fun verified_showsContinueAndIdentityCopy() {
        compose.setContent {
            FaceVerificationBottomSheet(
                state = FaceScannerState(livenessState = LivenessState.SUCCESS),
                onVerify = {}, onContinue = {}, onTryAgain = {}, onCancel = {}
            )
        }
        compose.onNodeWithText("Identity Verified").assertIsDisplayed()
        compose.onNodeWithText("Continue to Attendance").assertIsDisplayed()
        compose.onAllNodesWithText("Attendance recorded").assertCountEquals(0)
    }
}
```

- [ ] **Step 2: Verify compile fails**

Run: `.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain`
Expected: FAIL (`FaceVerificationBottomSheet` missing).

- [ ] **Step 3: Implement sheet + wire screen**

Implement `FaceVerificationBottomSheet` per the spec's phase table (titles/bodies/actions), reusing `StatefulButton`/`InfiniteButton`. `Verify` enabled only when `state.readyToVerify`. In `FaceScannerScreen`, replace the current `CameraContent` overlay stack with: `FaceVerificationTopBar`, `FaceStatusPillRow`, `FaceVerificationFrame(state)`, `FaceGuidanceText(state)`, `FaceDiagnosticsCard(faceDiagnosticsUiModel(state.similarity, state.threshold, state.livenessState == SUCCESS || failure-not-matched))`, and put `FaceVerificationBottomSheet` in the sheet slot. Keep permission-recovery branches and `publishResultOnce` intact; `onContinue`/success publishes `FaceVerificationResult.SUCCESS`; `onCancel` publishes exit state.

- [ ] **Step 4: Compile + run Compose tests**

Run: `.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin --console=plain`
Then device (if available): `.\gradlew.bat --no-daemon connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.face.FaceVerificationRedesignScreenTest --console=plain`
Expected: compile PASS; device PASS or mark Needs Verification.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRedesignScreenTest.kt
git commit -m "feat(face): assemble redesigned face verification screen"
```

---

### Task 10: Full verification + device evidence

**Files:** all above.

- [ ] **Step 1: Full unit tests**

Run: `.\gradlew.bat --no-daemon app:testDebugUnitTest --console=plain`
Expected: PASS.

- [ ] **Step 2: Release-leak grep (no raw embedding)**

```bash
grep -Rn "Log\..*embedding\|Log\..*ByteArray" app/src/main/java
```
Expected: no logging of raw embedding vectors (score/threshold display is allowed).

- [ ] **Step 3: Required verification**

Run each: `app:testDebugUnitTest`, `app:compileDebugAndroidTestKotlin`, `app:lintDebug`, `app:assembleDebug` (canonical command). Expected: PASS; document blockers.

- [ ] **Step 4: Device matrix**

Record: all 4 challenges in order; per-challenge 20s timeout; no-face; multiple faces; low light; matched (card 0.xxx/0.15); not matched (card); verified → Attendance Submitting; cancel → Ready; permission recovery; captured preview.

- [ ] **Step 5: Commit evidence notes**

```bash
git add -A
git commit -m "test(face): redesign verification coverage and device evidence"
```

---

## PR Checklist

Answer: which phases/components changed; ML Kit signals used (eye-open, smiling, `headEulerAngleY`); that `VerifyFaceUseCase` now returns similarity/threshold; that raw embeddings are never shown/logged; single-route handoff unchanged; tests/builds/device evidence. Include the governance-override note (4 challenges; score in release) and links to Linear INF-201 + spec/plan.

## Plan Self-Review

- **Spec coverage:** top bar/subtitle/pill (T7), status pills (T7), `FaceVerificationFrame` styles (T6), numbered rail (T5), 4-challenge sequence + per-challenge timeout (T1/T2/T4), head-turn via ML Kit (T2), typed similarity/threshold result (T3), diagnostics card verified & not-matched (T8), phase-aware bottom sheet + Verify/Continue/TryAgain/Cancel (T9), handoff unchanged + honest copy (T9), verification/device (T10). All mapped.
- **Placeholder scan:** no TBD/TODO deliverables; each code step shows concrete code or a precise edit against verified symbols.
- **Type consistency:** `LivenessChallenge{BLINK,SMILE,TURN_LEFT,TURN_RIGHT}`, `LivenessSequencer`, `VerifyFaceMatch`, `FaceMatchOutcome(+similarity/threshold)`, `RailNodeState`, `FrameStyle`/`frameStyleFor`, `FaceDiagnosticsUiModel`, `FaceScannerState(+challengeIndex/readyToVerify/similarity/threshold)` are consistent across tasks.
