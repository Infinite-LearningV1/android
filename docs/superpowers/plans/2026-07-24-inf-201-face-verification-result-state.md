# INF-201 Face Verification Result & UI State Implementation Plan (Code-Accurate Revision)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the existing face verification flow up to INF-201's acceptance criteria — an honest, differentiated result experience (captured photo, success/failure, specific reason, Retry/Continue/Cancel, loading, error handling) with release-safe biometric diagnostics — by enhancing the current components in place.

**Architecture:** Keep the single `FaceScanner` route and `savedStateHandle` handoff. Extend `FaceScannerViewModel`/`FaceScannerState` with a typed failure reason, face count, and a transient captured preview; map `VerifyFaceUseCase(Bitmap): Result<Boolean>` outcomes to those reasons; gate similarity logging behind `BuildConfig.DEBUG`; enrich the result surface with reused Infinite components. No navigation graph, no deletions, no new repository.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, Hilt, StateFlow, CameraX, ML Kit Face Detection, TensorFlow Lite, coroutines, JUnit, Compose UI Test.

**Design spec:** `docs/superpowers/specs/2026-07-24-inf-201-face-verification-result-state-design.md`

## Global Constraints

- Work only in the isolated worktree for branch `codex/inf-201-face-verification-spec-plan`. Do not edit the main checkout `E:\skrisi\android`.
- Preserve the existing flow: single `Screen.FaceScanner` route, `FaceScannerScreen`, `FaceScannerViewModel`, `FaceDetectorHelper`, `FaceProcessor`, `VerifyFaceUseCase`. Delete nothing.
- Face success is evidence for backend submission, never final Attendance success.
- One liveness challenge per attempt (BLINK or SMILE). No multi-step expansion.
- Do not expose raw similarity/threshold/embedding in release logs, analytics, navigation arguments, or `savedStateHandle`.
- Do not change `VerifyFaceUseCase`'s public signature or the `FaceVerificationResult` enum transport protocol.
- Do not change ML Kit `LANDMARK_MODE_ALL`, the CameraX per-frame pipeline, thresholds, or embedding format in this issue (tracked by INF-81/INF-82/INF-237).
- Reuse `StatefulButton`, `ButtonStyle`, `ButtonStateType`, `FaceBoundingBox`, `LoadingAnimation`, and `InfiniteColors`/`InfiniteSemantic` tokens.
- Required verification: `app:testDebugUnitTest`, `app:compileDebugAndroidTestKotlin`, `app:lintDebug`, `app:assembleDebug`.

---

## File Structure

### Modify

```text
app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt
app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt
app/src/main/res/values/strings.xml
```

### Create

```text
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationReason.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultCopy.kt
app/src/test/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCaseLoggingTest.kt
app/src/test/java/com/example/infinite_track/data/face/FaceDetectorHelperFaceCountTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModelReasonTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultCopyTest.kt
app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerResultSurfaceTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceFaceResultHandoffTest.kt
```

Notes:
- `FaceVerificationReason.kt` holds `FaceVerificationFailureReason` so it can be referenced by the ViewModel and copy mapper without a circular dependency.
- `FaceResultCopy.kt` is a pure mapper (reason → string-res id + semantic role) so copy/colour selection is unit-testable and never colour-only.

---

### Task 1: Gate similarity/threshold logging behind BuildConfig.DEBUG

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCaseLoggingTest.kt`

**Interfaces:**
- No public signature change. Introduces an injectable/overridable debug flag and a `diagnosticSink` seam so logging can be asserted in tests.

- [ ] **Step 1: Write the failing test**

Extract the debug decision into a testable pure function so no Android `Log`/`BuildConfig` is needed in a JVM unit test.

```kotlin
class VerifyFaceUseCaseLoggingTest {
    @Test
    fun `diagnostic payload is null in release`() {
        val payload = FaceMatchDiagnostics.build(
            isDebug = false, similarity = 0.83f, threshold = 0.15f, isMatch = true
        )
        assertNull(payload)
    }

    @Test
    fun `diagnostic payload is present in debug`() {
        val payload = FaceMatchDiagnostics.build(
            isDebug = true, similarity = 0.83f, threshold = 0.15f, isMatch = true
        )
        assertNotNull(payload)
        assertEquals(0.83f, payload!!.similarity)
        assertEquals(0.15f, payload.threshold)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew app:testDebugUnitTest --tests "*VerifyFaceUseCaseLoggingTest"
```

Expected: FAIL because `FaceMatchDiagnostics` does not exist.

- [ ] **Step 3: Implement the gated diagnostics**

Add a pure helper in `VerifyFaceUseCase.kt` (or an adjacent file in the same package) and use `BuildConfig.DEBUG` at the call site:

```kotlin
data class FaceMatchDiagnostics(
    val similarity: Float,
    val threshold: Float,
    val isMatch: Boolean
)

internal object FaceMatchDiagnosticsFactory {
    fun build(isDebug: Boolean, similarity: Float, threshold: Float, isMatch: Boolean):
        FaceMatchDiagnostics? =
        if (isDebug) FaceMatchDiagnostics(similarity, threshold, isMatch) else null
}
```

Replace the two unconditional `Log.d` calls (the "similarity score" and "verification result" lines) with:

```kotlin
FaceMatchDiagnosticsFactory
    .build(BuildConfig.DEBUG, similarity, SIMILARITY_THRESHOLD, isMatch)
    ?.let { Log.d(TAG, "Face match diagnostics: $it") }
```

Keep the `Log.e` error path (it must not include similarity/threshold/embedding values).

- [ ] **Step 4: Run the tests and the release-leak grep**

```bash
./gradlew app:testDebugUnitTest --tests "*VerifyFaceUseCaseLoggingTest"
grep -Rn "similarity score\|threshold: \|Log\.d.*similarity" app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt
```

Expected: tests PASS; grep shows any similarity/threshold logging only inside the debug-gated path.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/auth/VerifyFaceUseCaseLoggingTest.kt
git commit -m "fix(face): gate similarity diagnostics behind BuildConfig.DEBUG"
```

---

### Task 2: Add typed failure reason and copy mapper

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationReason.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultCopy.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultCopyTest.kt`

**Interfaces:**
- Produces: `enum class FaceVerificationFailureReason { NO_FACE, MULTIPLE_FACES, LOW_LIGHT, NOT_MATCHED, TECHNICAL_FAILURE }`.
- Produces: `FaceResultCopy.forFailure(reason): FaceResultCopyModel` and `FaceResultCopy.success(): FaceResultCopyModel`, where the model exposes a `@StringRes titleRes`, `@StringRes messageRes`, a semantic role (`SUCCESS`/`ERROR`/`WARNING`), and `retryable: Boolean`.

- [ ] **Step 1: Write the failing copy-mapper test**

```kotlin
class FaceResultCopyTest {
    @Test
    fun `not matched is retryable error`() {
        val copy = FaceResultCopy.forFailure(FaceVerificationFailureReason.NOT_MATCHED)
        assertEquals(FaceResultRole.ERROR, copy.role)
        assertTrue(copy.retryable)
    }

    @Test
    fun `low light is retryable warning`() {
        val copy = FaceResultCopy.forFailure(FaceVerificationFailureReason.LOW_LIGHT)
        assertEquals(FaceResultRole.WARNING, copy.role)
        assertTrue(copy.retryable)
    }

    @Test
    fun `success is non-retryable success role`() {
        val copy = FaceResultCopy.success()
        assertEquals(FaceResultRole.SUCCESS, copy.role)
        assertFalse(copy.retryable)
    }

    @Test
    fun `every failure reason maps to distinct string resources`() {
        val ids = FaceVerificationFailureReason.entries.map {
            FaceResultCopy.forFailure(it).titleRes to FaceResultCopy.forFailure(it).messageRes
        }
        assertEquals(ids.size, ids.toSet().size)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceResultCopyTest"
```

Expected: FAIL because the types do not exist.

- [ ] **Step 3: Implement the reason enum and pure copy mapper**

`FaceVerificationReason.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

enum class FaceVerificationFailureReason {
    NO_FACE,
    MULTIPLE_FACES,
    LOW_LIGHT,
    NOT_MATCHED,
    TECHNICAL_FAILURE
}
```

`FaceResultCopy.kt`:

```kotlin
package com.example.infinite_track.presentation.screen.attendance.face

import androidx.annotation.StringRes
import com.example.infinite_track.R

enum class FaceResultRole { SUCCESS, ERROR, WARNING }

data class FaceResultCopyModel(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    val role: FaceResultRole,
    val retryable: Boolean
)

object FaceResultCopy {
    fun success() = FaceResultCopyModel(
        titleRes = R.string.face_result_success_title,
        messageRes = R.string.face_result_success_message,
        role = FaceResultRole.SUCCESS,
        retryable = false
    )

    fun forFailure(reason: FaceVerificationFailureReason) = when (reason) {
        FaceVerificationFailureReason.NO_FACE -> FaceResultCopyModel(
            R.string.face_result_no_face_title, R.string.face_result_no_face_message,
            FaceResultRole.WARNING, retryable = true
        )
        FaceVerificationFailureReason.MULTIPLE_FACES -> FaceResultCopyModel(
            R.string.face_result_multiple_faces_title, R.string.face_result_multiple_faces_message,
            FaceResultRole.WARNING, retryable = true
        )
        FaceVerificationFailureReason.LOW_LIGHT -> FaceResultCopyModel(
            R.string.face_result_low_light_title, R.string.face_result_low_light_message,
            FaceResultRole.WARNING, retryable = true
        )
        FaceVerificationFailureReason.NOT_MATCHED -> FaceResultCopyModel(
            R.string.face_result_not_matched_title, R.string.face_result_not_matched_message,
            FaceResultRole.ERROR, retryable = true
        )
        FaceVerificationFailureReason.TECHNICAL_FAILURE -> FaceResultCopyModel(
            R.string.face_result_technical_title, R.string.face_result_technical_message,
            FaceResultRole.ERROR, retryable = true
        )
    }
}
```

Add the referenced strings to `strings.xml` (Indonesian copy consistent with the existing scanner strings), e.g. `face_result_success_title = "Identitas terverifikasi"`, `face_result_not_matched_message = "Wajah tidak cocok dengan data tersimpan. Silakan coba lagi."`, etc. Success copy must not claim attendance success.

- [ ] **Step 4: Run the tests**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceResultCopyTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationReason.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultCopy.kt app/src/main/res/values/strings.xml app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceResultCopyTest.kt
git commit -m "feat(face): add typed verification failure reasons and copy mapper"
```

---

### Task 3: Surface detected face count from FaceDetectorHelper

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/face/FaceDetectorHelperFaceCountTest.kt`

**Interfaces:**
- Produces: a small `DetectedFace` result carrying `face: Face` and `totalFaces: Int`, or an additive callback overload, without changing existing `detect` semantics for callers not yet migrated.

- [ ] **Step 1: Write the failing test for the face-count reducer**

Keep ML Kit out of the JVM test by extracting the selection into a pure function.

```kotlin
class FaceDetectorHelperFaceCountTest {
    @Test
    fun `selects largest face and reports count`() {
        val result = FaceSelection.select(
            listOf(area(10), area(40), area(25))
        )
        assertEquals(3, result!!.totalFaces)
        assertEquals(40, result.selected.areaProxy)
    }

    @Test
    fun `empty list yields null`() {
        assertNull(FaceSelection.select(emptyList()))
    }
}
```

Use a tiny test-only `FaceLike` abstraction (`areaProxy`) so `FaceSelection.select` is pure; the production `detect` adapts ML Kit `Face` bounding-box area to it.

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceDetectorHelperFaceCountTest"
```

Expected: FAIL because `FaceSelection` does not exist.

- [ ] **Step 3: Implement the pure selector and thread the count through `detect`**

Add `FaceSelection.select(...)` returning `{ selected, totalFaces }`. In `detect`'s `addOnSuccessListener`, compute the count from `faces.size` and the largest face via the selector, then emit both. Provide the count to callers with an additive callback (e.g. `onResult: (Result<DetectedFace>)`), and keep the largest-face behaviour identical. Do not change ML Kit options or the `imageProxy.close()` in `addOnCompleteListener`.

- [ ] **Step 4: Run tests and compile**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceDetectorHelperFaceCountTest"
./gradlew app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt app/src/test/java/com/example/infinite_track/data/face/FaceDetectorHelperFaceCountTest.kt
git commit -m "feat(face): report detected face count for multiple-face guidance"
```

---

### Task 4: Map outcomes to reasons in FaceScannerViewModel

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModelReasonTest.kt`

**Interfaces:**
- Consumes: `FaceDetectorHelper` detection with face count, `VerifyFaceUseCase(Bitmap): Result<Boolean>`.
- Produces: `FaceScannerState.failureReason`, `.detectedFaceCount`, `.capturedFacePreview`, `.debugSimilarity` (debug only).

- [ ] **Step 1: Write failing state-machine tests**

```kotlin
@Test
fun `no match maps to NOT_MATCHED failure`() = runTest {
    val vm = buildViewModel(verify = { Result.success(false) })
    vm.forceVerify(fakeFace(), fakeBitmap())
    advanceUntilIdle()
    assertEquals(LivenessState.FAILURE, vm.uiState.value.livenessState)
    assertEquals(FaceVerificationFailureReason.NOT_MATCHED, vm.uiState.value.failureReason)
}

@Test
fun `verification exception maps to TECHNICAL_FAILURE`() = runTest {
    val vm = buildViewModel(verify = { Result.failure(IllegalStateException("no embedding")) })
    vm.forceVerify(fakeFace(), fakeBitmap())
    advanceUntilIdle()
    assertEquals(FaceVerificationFailureReason.TECHNICAL_FAILURE, vm.uiState.value.failureReason)
}

@Test
fun `multiple faces sets MULTIPLE_FACES guidance and does not verify`() = runTest {
    val vm = buildViewModel(verify = { error("must not verify") })
    vm.onFacesDetected(count = 2, face = fakeFace(), bitmap = fakeBitmap())
    assertEquals(FaceVerificationFailureReason.MULTIPLE_FACES, vm.uiState.value.failureReason)
}

@Test
fun `reset clears captured preview and reason`() = runTest {
    val vm = buildViewModel(verify = { Result.success(false) })
    vm.forceVerify(fakeFace(), fakeBitmap())
    advanceUntilIdle()
    vm.resetScanner()
    assertNull(vm.uiState.value.capturedFacePreview)
    assertNull(vm.uiState.value.failureReason)
}
```

Expose narrow test seams (`forceVerify`, `onFacesDetected`) or use the existing public entry points if they are already reachable; keep production wiring unchanged otherwise.

- [ ] **Step 2: Run the tests to verify they fail**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceScannerViewModelReasonTest"
```

Expected: FAIL because the new fields/mapping do not exist.

- [ ] **Step 3: Implement reason mapping and preview capture**

Add the new fields to `FaceScannerState` (all with behaviour-preserving defaults). Update:

- Detection: when `count > 1`, set `failureReason = MULTIPLE_FACES` with guidance copy and skip liveness/verification for that frame; when `count == 0` after timeout, terminal `TIMEOUT` keeps its own state while `NO_FACE` is set for a no-face terminal path if detection never succeeded.
- Low light: the existing `LOW_LIGHT` state additionally records `failureReason = LOW_LIGHT` when it becomes the terminal outcome.
- `proceedWithFaceVerification()`: on the extracted `faceBitmap`, set `capturedFacePreview = faceBitmap`; then map `verifyFaceUseCase(...)`:
  - `success(true)` → `handleVerificationSuccess()`.
  - `success(false)` → `FAILURE` + `NOT_MATCHED`.
  - `failure(_)` → `FAILURE` + `TECHNICAL_FAILURE`.
- Populate `debugSimilarity` only when `BuildConfig.DEBUG` (reuse Task 1's gate; do not expose in release).
- `initializeScanner()`/`resetScanner()`/`onCleared()` clear `capturedFacePreview`, `failureReason`, and `debugSimilarity`.

- [ ] **Step 4: Run the tests**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceScannerViewModelReasonTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModelReasonTest.kt
git commit -m "feat(face): map verification outcomes to typed failure reasons"
```

---

### Task 5: Enrich the result surface UI

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerResultSurfaceTest.kt`

**Interfaces:**
- Consumes: `FaceScannerState` (with reason/preview), `FaceResultCopy`.
- Produces: result composable rendering captured photo, success/failure copy+icon+colour, and Retry/Continue/Cancel; no navigation or repository calls beyond the existing `publishResultOnce`.

- [ ] **Step 1: Write Compose tests for terminal states**

```kotlin
@Test
fun notMatched_showsReasonAndRetry_withoutRawScore() {
    composeTestRule.setContent {
        FaceResultSurface(
            state = previewState(
                livenessState = LivenessState.FAILURE,
                failureReason = FaceVerificationFailureReason.NOT_MATCHED
            ),
            onRetry = {}, onContinue = {}, onCancel = {}
        )
    }
    composeTestRule.onNodeWithText("Wajah tidak cocok", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Coba Lagi").assertIsDisplayed()
    composeTestRule.onNode(hasText("0.", substring = true)).assertDoesNotExist()
}

@Test
fun success_usesIdentityCopy_notAttendanceSuccess() {
    composeTestRule.setContent {
        FaceResultSurface(
            state = previewState(livenessState = LivenessState.SUCCESS),
            onRetry = {}, onContinue = {}, onCancel = {}
        )
    }
    composeTestRule.onNodeWithText("Identitas terverifikasi").assertIsDisplayed()
    composeTestRule.onNodeWithText("Absensi berhasil").assertDoesNotExist()
}

@Test
fun multipleFaces_showsGuidance() {
    composeTestRule.setContent {
        FaceResultSurface(
            state = previewState(
                livenessState = LivenessState.FAILURE,
                failureReason = FaceVerificationFailureReason.MULTIPLE_FACES
            ),
            onRetry = {}, onContinue = {}, onCancel = {}
        )
    }
    composeTestRule.onNodeWithText("Pastikan hanya ada satu wajah", substring = true)
        .assertIsDisplayed()
}
```

Add equivalent cases for `NO_FACE`, `LOW_LIGHT`, `TIMEOUT`, and `TECHNICAL_FAILURE`.

- [ ] **Step 2: Verify compile/failure**

```bash
./gradlew app:compileDebugAndroidTestKotlin
```

Expected: FAIL because `FaceResultSurface` does not exist.

- [ ] **Step 3: Implement the result surface**

Extract a `FaceResultSurface(state, onRetry, onContinue, onCancel)` composable used by `InstructionSection`/`CameraContent` for terminal states. It must:

- Resolve copy/role via `FaceResultCopy` (never colour-only: always render icon + text).
- Map `FaceResultRole` to `InfiniteColors`/`InfiniteSemantic` tokens.
- Render `state.capturedFacePreview` as a thumbnail when non-null.
- Show `StatefulButton` "Coba Lagi" when the reason is retryable, "Lanjutkan" on success (or keep the existing auto-return on `SUCCESS`), and always a "Tutup"/Cancel button (preserving the current `publishExitStateOnce`).
- In release, never render `state.debugSimilarity`; a debug-only diagnostic line may show it.
- Provide content descriptions / accessibility semantics per terminal state.

Keep the existing `publishResultOnce`/`finishFaceScanner` publication and the `SUCCESS → FaceVerificationResult.SUCCESS` mapping intact.

- [ ] **Step 4: Compile and run Compose tests**

```bash
./gradlew app:compileDebugAndroidTestKotlin
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.face.FaceScannerResultSurfaceTest
```

Expected: compile PASS; device test PASS when a device is available (otherwise mark Needs Verification with evidence).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt app/src/main/res/values/strings.xml app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerResultSurfaceTest.kt
git commit -m "feat(face): enrich result surface with reason, photo, and actions"
```

---

### Task 6: Lock the Attendance handoff contract

**Files:**
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceFaceResultHandoffTest.kt`
- Modify (only if a gap is proven): `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`

**Interfaces:**
- Consumes: `FaceVerificationResult` enum.
- Verifies: `SUCCESS → Submitting`; `FAILED`/`TIMEOUT`/`CANCELLED` never call backend.

- [ ] **Step 1: Write regression tests for the existing handoff**

```kotlin
@Test
fun `success moves attendance to submitting`() {
    val vm = buildAttendanceViewModel()
    vm.setActionStateForTest(AttendanceActionState.VerifyingFace(AttendanceActionIntent.CHECK_IN))
    vm.onFaceVerificationResult(FaceVerificationResult.SUCCESS)
    assertTrue(vm.uiState.value.actionState is AttendanceActionState.Submitting)
}

@Test
fun `cancelled does not submit`() {
    val vm = buildAttendanceViewModel()
    vm.setActionStateForTest(AttendanceActionState.VerifyingFace(AttendanceActionIntent.CHECK_IN))
    vm.onFaceVerificationResult(FaceVerificationResult.CANCELLED)
    verify(checkInUseCase, never()).invoke(any())
}
```

Use whatever ViewModel construction/test seam the existing attendance tests already use; if none, add a minimal internal test hook rather than changing production behaviour.

- [ ] **Step 2: Run the tests**

```bash
./gradlew app:testDebugUnitTest --tests "*AttendanceFaceResultHandoffTest"
```

Expected: PASS against current behaviour (INF-239 already implements it). If a test fails, it reveals a real regression — fix `onFaceVerificationResult` minimally to satisfy the contract, then re-run.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceFaceResultHandoffTest.kt
git commit -m "test(attendance): lock face verification handoff contract"
```

---

### Task 7: Full verification and device evidence

**Files:**
- All modified files above.

- [ ] **Step 1: Run focused unit tests**

```bash
./gradlew app:testDebugUnitTest --tests "*Face*" --tests "*AttendanceFaceResultHandoffTest"
```

Expected: PASS.

- [ ] **Step 2: Run release-leak grep**

```bash
grep -Rn "Log\.[dview].*similarity\|Log\.[dview].*threshold\|Log\.[dview].*embedding" app/src/main/java
```

Expected: any such logging is inside an explicit `BuildConfig.DEBUG` path only.

- [ ] **Step 3: Run required project verification**

```bash
./gradlew app:testDebugUnitTest
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:lintDebug
./gradlew app:assembleDebug
git diff --check
```

Expected: all PASS. Document any environment-only instrumentation blocker without claiming device success.

- [ ] **Step 4: Perform device matrix and attach evidence**

Record:

```text
- camera bind and first detection;
- no-face guidance and terminal NO_FACE;
- multiple-face guidance;
- misalignment guidance;
- low-light detection and automatic recovery;
- blink attempt; smile attempt;
- not matched → retry;
- verified → Attendance Submitting;
- timeout → retry;
- cancel → Attendance Ready;
- camera permission denied/settings recovery;
- background/foreground behaviour;
- captured photo shown on result;
- release build shows no raw similarity score.
```

- [ ] **Step 5: Commit final evidence notes**

```bash
git add -A
git commit -m "docs(face): attach INF-201 verification and device evidence"
```

---

## PR Checklist

The PR description must answer:

```text
Presentation: which scanner states/reasons/result-surface changes landed?
Domain: what changed in VerifyFaceUseCase (only debug gating; signature unchanged)?
Data: what changed in FaceDetectorHelper (face count only; ML Kit options unchanged)?
Navigation: confirm the single FaceScanner route and savedStateHandle handoff are unchanged.
Security: confirm no raw similarity/threshold/embedding in release logs/nav/saved state.
Verification: which tests, builds, and device scenarios passed?
```

Include: Linear INF-201 link; spec and plan paths; before/after screenshots for each terminal state; build/test/lint evidence; device evidence; explicit note that face verification is not final Attendance success; and a short note referencing the [Divergence from issue #102](#divergence-from-issue-102) so reviewers know the nested-graph rewrite was intentionally deferred.

## Plan Self-Review

- **Spec coverage:** captured photo (T4/T5), success/failure clarity (T2/T5), specific reasons (T2/T3/T4), Retry/Continue/Cancel (T5), loading (existing + T5), error handling as TECHNICAL_FAILURE (T4), navigate-on-success handoff (T6), release-safe diagnostics (T1/T4), multiple faces (T3/T4/T5) — each maps to a task.
- **Placeholder scan:** no TBD/TODO deliverables; every code step shows concrete code or an exact edit description tied to verified symbols.
- **Type consistency:** `FaceVerificationFailureReason`, `FaceResultCopyModel`, `FaceResultRole`, `FaceScannerState` fields, `LivenessState`, and `FaceVerificationResult` names match the design spec and the verified code.
- **No deletions / no new graph / no signature changes:** honored across all tasks, consistent with the global constraints and the divergence rationale.
