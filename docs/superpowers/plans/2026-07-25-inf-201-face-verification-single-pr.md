# INF-201 Face Verification Single-PR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the approved Face Verification state system, optimized camera pipeline, typed matching contract, and Attendance handoff in one bounded pull request.

**Architecture:** One nested `FaceVerificationGraph` owns one scanner session and one shared ViewModel. Presentation renders state-driven guidance, processing, and result surfaces; domain owns project types and policies; data/platform adapts CameraX, ML Kit, LiteRT, and secure embedding storage without leaking Android types into domain.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Navigation Compose, Hilt, StateFlow/SharedFlow, CameraX, ML Kit Face Detection, LiteRT/TensorFlow Lite, coroutines, JUnit, Turbine, Compose UI Test.

## Global Constraints

- Implement the approved scope in one PR linked to Linear INF-201.
- Preserve `Screen → ViewModel → UseCase → Repository Interface → RepositoryImpl → platform/data`.
- Face success is evidence for backend submission, never final Attendance success.
- One liveness challenge per attempt: Blink OR Smile.
- Use `640×480`, `YUV_420_888`, `KEEP_ONLY_LATEST`, ML Kit FAST mode, classification enabled, landmark/contour disabled.
- No full-frame Bitmap conversion per analyzed frame.
- No Android, CameraX, ML Kit, or Compose types in domain.
- No raw embeddings, similarity score, or thresholds in release UI/logs/navigation.
- One detector and one matching operation at a time; every `ImageProxy` closes exactly once.
- Reuse Infinite design tokens and reusable components.
- Required verification: `app:testDebugUnitTest`, `app:compileDebugAndroidTestKotlin`, `app:lintDebug`, `app:assembleDebug`.

---

## File Structure

### Create

```text
app/src/main/java/com/example/infinite_track/domain/model/face/FaceVerificationModels.kt
app/src/main/java/com/example/infinite_track/domain/model/face/FaceRuntimePolicy.kt
app/src/main/java/com/example/infinite_track/domain/repository/FaceVerificationRepository.kt
app/src/main/java/com/example/infinite_track/domain/use_case/face/EvaluateFaceQualityUseCase.kt
app/src/main/java/com/example/infinite_track/domain/use_case/face/EvaluateLivenessUseCase.kt
app/src/main/java/com/example/infinite_track/domain/use_case/face/VerifyFaceIdentityUseCase.kt
app/src/main/java/com/example/infinite_track/data/face/MlKitFaceFrameAnalyzer.kt
app/src/main/java/com/example/infinite_track/data/face/MlKitFaceObservationMapper.kt
app/src/main/java/com/example/infinite_track/data/face/YPlaneFaceQualityEvaluator.kt
app/src/main/java/com/example/infinite_track/data/face/AndroidFaceSampleExtractor.kt
app/src/main/java/com/example/infinite_track/data/face/FaceVerificationRepositoryImpl.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationContract.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModel.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRoute.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationScreen.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/components/FaceVerificationTopBar.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/components/FaceCameraStage.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/components/FaceVerificationBottomSurface.kt
app/src/test/java/com/example/infinite_track/domain/use_case/face/EvaluateFaceQualityUseCaseTest.kt
app/src/test/java/com/example/infinite_track/domain/use_case/face/EvaluateLivenessUseCaseTest.kt
app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModelTest.kt
app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationScreenTest.kt
```

### Modify

```text
app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt
app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/FaceVerificationResult.kt
app/src/main/java/com/example/infinite_track/data/face/FaceProcessor.kt
app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt
app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt
app/src/main/res/values/strings.xml
```

### Remove after migration

```text
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt
app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt
```

Delete only after all callers and tests use the new contracts.

---

### Task 1: Add typed domain contracts and policy

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/model/face/FaceVerificationModels.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/face/FaceRuntimePolicy.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/repository/FaceVerificationRepository.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/model/face/FaceVerificationModelsTest.kt`

**Interfaces:**
- Produces: `FaceObservation`, `FaceGuidanceReason`, `FaceQuality`, `LivenessChallenge`, `LivenessEvaluation`, `FaceSample`, `FaceMatchResult`, `FaceVerificationFailure`, `FaceVerificationRepository`, `FaceRuntimePolicy`.

- [ ] **Step 1: Write the failing contract tests**

```kotlin
class FaceVerificationModelsTest {
    @Test
    fun `mismatch and technical failure are distinct results`() {
        val mismatch: FaceMatchResult = FaceMatchResult.NotMatched(
            FaceMismatchReason.BELOW_THRESHOLD
        )
        val failure: FaceMatchResult = FaceMatchResult.Failed(
            FaceMatchFailure.MODEL_FAILURE
        )

        assertTrue(mismatch is FaceMatchResult.NotMatched)
        assertTrue(failure is FaceMatchResult.Failed)
    }

    @Test
    fun `runtime policy uses approved baseline`() {
        val policy = FaceRuntimePolicy()
        assertEquals(3, policy.readyFramesRequired)
        assertEquals(3, policy.lowLightFramesRequired)
        assertEquals(2, policy.challengeFramesRequired)
        assertEquals(20_000L, policy.timeoutMillis)
        assertEquals(2, policy.inferenceThreads)
    }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationModelsTest"
```

Expected: FAIL because the new domain types do not exist.

- [ ] **Step 3: Implement the domain models**

```kotlin
sealed interface FaceMatchResult {
    data class Matched(val evidence: FaceMatchEvidence) : FaceMatchResult
    data class NotMatched(val reason: FaceMismatchReason) : FaceMatchResult
    data class Failed(val failure: FaceMatchFailure) : FaceMatchResult
}

interface FaceVerificationRepository {
    suspend fun verify(
        sample: FaceSample,
        identity: FaceIdentityReference
    ): FaceMatchResult
}
```

Keep all domain files free of Android and ML Kit imports.

- [ ] **Step 4: Run the tests and architecture import check**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationModelsTest"
grep -R "android\.\|androidx\.camera\|com.google.mlkit" app/src/main/java/com/example/infinite_track/domain/model/face app/src/main/java/com/example/infinite_track/domain/repository/FaceVerificationRepository.kt
```

Expected: tests PASS; grep returns no matches.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain app/src/test/java/com/example/infinite_track/domain/model/face
git commit -m "feat(face): add typed verification domain contracts"
```

---

### Task 2: Add pure quality and liveness evaluators

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/face/EvaluateFaceQualityUseCase.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/face/EvaluateLivenessUseCase.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/face/EvaluateFaceQualityUseCaseTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/domain/use_case/face/EvaluateLivenessUseCaseTest.kt`

**Interfaces:**
- Consumes: `FaceObservation`, `FaceRuntimePolicy`, `LivenessChallenge`.
- Produces: `FaceQualityEvaluation`, `LivenessEvaluation`.

- [ ] **Step 1: Write failing temporal-stability tests**

```kotlin
@Test
fun `face becomes ready only after required stable frames`() {
    val useCase = EvaluateFaceQualityUseCase(FaceRuntimePolicy(readyFramesRequired = 3))
    val observation = readyObservation(trackingId = 7)

    assertFalse(useCase(observation).isReady)
    assertFalse(useCase(observation).isReady)
    assertTrue(useCase(observation).isReady)
}

@Test
fun `tracking id change resets liveness progress`() {
    val evaluator = EvaluateLivenessUseCase(FaceRuntimePolicy(challengeFramesRequired = 2))
    evaluator.evaluate(blinkObservation(trackingId = 1), LivenessChallenge.BLINK)
    val result = evaluator.evaluate(blinkObservation(trackingId = 2), LivenessChallenge.BLINK)

    assertTrue(result is LivenessEvaluation.Progress)
    assertEquals(0.5f, result.value)
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*EvaluateFaceQualityUseCaseTest" --tests "*EvaluateLivenessUseCaseTest"
```

Expected: FAIL because evaluator classes do not exist.

- [ ] **Step 3: Implement deterministic evaluators**

Implement counters keyed by tracking ID, explicit reset methods, low-light consensus, face-lost grace frames, and challenge progress. Do not include UI strings.

```kotlin
class EvaluateLivenessUseCase(
    private val policy: FaceRuntimePolicy
) {
    private var activeTrackingId: Int? = null
    private var successFrames = 0

    fun evaluate(
        observation: FaceObservation,
        challenge: LivenessChallenge
    ): LivenessEvaluation {
        if (activeTrackingId != observation.trackingId) {
            activeTrackingId = observation.trackingId
            successFrames = 0
        }
        val frameMatches = challenge.matches(observation)
        successFrames = if (frameMatches) successFrames + 1 else 0
        return if (successFrames >= policy.challengeFramesRequired) {
            LivenessEvaluation.Completed
        } else {
            LivenessEvaluation.Progress(
                successFrames.toFloat() / policy.challengeFramesRequired
            )
        }
    }

    fun reset() {
        activeTrackingId = null
        successFrames = 0
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew app:testDebugUnitTest --tests "*EvaluateFaceQualityUseCaseTest" --tests "*EvaluateLivenessUseCaseTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/domain/use_case/face app/src/test/java/com/example/infinite_track/domain/use_case/face
git commit -m "feat(face): add quality and liveness evaluators"
```

---

### Task 3: Build the optimized CameraX and ML Kit adapter

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/data/face/MlKitFaceFrameAnalyzer.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/face/MlKitFaceObservationMapper.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/face/YPlaneFaceQualityEvaluator.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/face/MlKitFaceObservationMapperTest.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/face/YPlaneFaceQualityEvaluatorTest.kt`

**Interfaces:**
- Produces: `FaceFrameAnalyzer.start(onObservation, onFailure)`, `FaceFrameAnalyzer.analyze(imageProxy)`, `FaceFrameAnalyzer.close()`.
- Emits project-owned `FaceObservation`; never exposes ML Kit `Face`.

- [ ] **Step 1: Write mapper and luminance tests**

```kotlin
@Test
fun `mapper preserves only project-owned observation fields`() {
    val observation = mapper.map(fakeMlKitFace())
    assertEquals(1, observation.faceCount)
    assertNotNull(observation.bounds)
    assertNotNull(observation.smileProbability)
}

@Test
fun `dark y plane is low light`() {
    val y = ByteBuffer.wrap(ByteArray(16 * 16) { 12 })
    val result = evaluator.evaluate(y, rowStride = 16, pixelStride = 1, crop = fullCrop())
    assertEquals(FaceQuality.LOW_LIGHT, result)
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*MlKitFaceObservationMapperTest" --tests "*YPlaneFaceQualityEvaluatorTest"
```

- [ ] **Step 3: Implement detector configuration and in-flight guard**

```kotlin
private val detector = FaceDetection.getClient(
    FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.20f)
        .enableTracking()
        .build()
)

private val detecting = AtomicBoolean(false)
```

`analyze(imageProxy)` must:

1. close immediately when another task is in flight;
2. use `InputImage.fromMediaImage()` directly;
3. map all detected faces to one project observation including face count;
4. calculate luminance from the Y plane;
5. reset the guard and close the proxy in one completion path.

Use an `AtomicBoolean` close guard in tests/debug assertions so double-close is detectable.

- [ ] **Step 4: Run tests and compile**

```bash
./gradlew app:testDebugUnitTest --tests "*MlKitFaceObservationMapperTest" --tests "*YPlaneFaceQualityEvaluatorTest"
./gradlew app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/face app/src/test/java/com/example/infinite_track/data/face
git commit -m "refactor(face): isolate optimized ML Kit frame analysis"
```

---

### Task 4: Add sample extraction and typed matching repository

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/data/face/AndroidFaceSampleExtractor.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/face/FaceVerificationRepositoryImpl.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/face/VerifyFaceIdentityUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/face/FaceProcessor.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/face/FaceVerificationRepositoryImplTest.kt`

**Interfaces:**
- Consumes: locked platform frame snapshot and normalized bounds in data layer.
- Produces: domain `FaceSample` and `FaceMatchResult`.

- [ ] **Step 1: Write failing typed-result tests**

```kotlin
@Test
fun `score below threshold returns not matched`() = runTest {
    whenever(processor.generateEmbedding(any())).thenReturn(Result.success(capturedEmbedding))
    whenever(identityStore.currentReference()).thenReturn(referenceWith(storedEmbedding))

    val result = repository.verify(sample, identityReference)

    assertEquals(
        FaceMatchResult.NotMatched(FaceMismatchReason.BELOW_THRESHOLD),
        result
    )
}

@Test
fun `missing embedding is technical failure`() = runTest {
    whenever(identityStore.currentReference()).thenReturn(referenceWithoutEmbedding())
    assertEquals(
        FaceMatchResult.Failed(FaceMatchFailure.EMBEDDING_MISSING),
        repository.verify(sample, identityReference)
    )
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationRepositoryImplTest"
```

- [ ] **Step 3: Implement one-sample, one-embedding, one-match path**

Use one `Mutex` or atomic attempt token to prevent duplicate matching. Reuse direct input and output buffers in `FaceProcessor`. Gate diagnostics:

```kotlin
if (BuildConfig.DEBUG) {
    diagnosticSink.record(
        FaceVerificationDiagnostics(score, thresholdVersion, modelVersion, decision)
    )
}
```

Release logging must not include score, threshold, embedding, or identity details.

- [ ] **Step 4: Run tests and inspect release-path logging**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationRepositoryImplTest"
grep -R "similarity score\|threshold:" app/src/main/java/com/example/infinite_track/data app/src/main/java/com/example/infinite_track/domain
```

Expected: tests PASS; any diagnostic output is inside an explicit debug-only path.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/face app/src/main/java/com/example/infinite_track/domain/use_case/face app/src/main/java/com/example/infinite_track/di app/src/test/java/com/example/infinite_track/data/face
git commit -m "refactor(face): add typed identity matching repository"
```

---

### Task 5: Add state, events, effects, and ViewModel reducer

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationContract.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModel.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModelTest.kt`

**Interfaces:**
- Consumes: quality/liveness use cases, sample extractor coordinator, `VerifyFaceIdentityUseCase`, monotonic clock.
- Produces: `StateFlow<FaceVerificationState>` and `SharedFlow<FaceVerificationEffect>`.

- [ ] **Step 1: Write failing state-machine tests**

```kotlin
@Test
fun `low light blocks liveness and matching`() = runTest {
    viewModel.onEvent(FaceVerificationEvent.FrameObserved(lowLightObservation()))
    assertEquals(
        FaceVerificationStage.Guidance(FaceGuidanceReason.LowLight),
        viewModel.state.value.stage
    )
    verifyNoInteractions(verifyFaceIdentityUseCase)
}

@Test
fun `matched publishes finish effect once`() = runTest {
    completeLivenessAndReturn(FaceMatchResult.Matched(evidence))

    assertEquals(
        FaceVerificationEffect.Finish(
            FaceVerificationResultContract.Verified(sessionId, evidence.id)
        ),
        viewModel.effect.first()
    )
    assertNull(viewModel.effect.replayCache.firstOrNull())
}

@Test
fun `retry increments generation and cancels old attempt`() = runTest {
    val first = viewModel.state.value.attemptGeneration
    viewModel.onEvent(FaceVerificationEvent.RetryRequested)
    assertEquals(first + 1, viewModel.state.value.attemptGeneration)
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationViewModelTest"
```

- [ ] **Step 3: Implement reducer and orchestration**

Use one persistent state and one effect stream. Store only project-owned types. Use monotonic deadline:

```kotlin
private fun startDeadline() {
    val deadline = clock.elapsedRealtimeMillis() + policy.timeoutMillis
    timeoutJob = viewModelScope.launch {
        while (isActive) {
            val remaining = deadline - clock.elapsedRealtimeMillis()
            if (remaining <= 0) {
                transitionToTimeout()
                break
            }
            _state.update { it.copy(remainingSeconds = ceil(remaining / 1000.0).toInt()) }
            delay(250)
        }
    }
}
```

Retry cancels timeout, liveness, extraction, and matching jobs; clears locked sample; resets evaluators; increments generation; starts a new deadline.

- [ ] **Step 4: Run ViewModel tests**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationViewModelTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationContract.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModel.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModelTest.kt
git commit -m "feat(face): add state-driven verification session"
```

---

### Task 6: Build reusable scanner UI surfaces

**Files:**
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationRoute.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationScreen.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/components/FaceVerificationTopBar.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/components/FaceCameraStage.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/components/FaceVerificationBottomSurface.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationScreenTest.kt`

**Interfaces:**
- Consumes: immutable `FaceVerificationState` and `onEvent` callback.
- Produces: visual states and accessibility semantics; no navigation or repository calls.

- [ ] **Step 1: Write Compose tests for every approved state**

```kotlin
@Test
fun lowLight_showsWarningAndRetry_withoutVerifiedCopy() {
    compose.setContent {
        FaceVerificationScreen(
            state = previewState(FaceVerificationStage.Guidance(FaceGuidanceReason.LowLight)),
            onEvent = {}
        )
    }

    compose.onNodeWithText("Pencahayaan belum memadai").assertIsDisplayed()
    compose.onNodeWithText("Coba lagi").assertIsDisplayed()
    compose.onNodeWithText("Identitas terverifikasi").assertDoesNotExist()
}

@Test
fun verified_usesIdentityCopy_notAttendanceSuccess() {
    compose.setContent {
        FaceVerificationScreen(
            state = previewState(matchedStage()),
            onEvent = {}
        )
    }

    compose.onNodeWithText("Identitas terverifikasi").assertIsDisplayed()
    compose.onNodeWithText("Absensi berhasil").assertDoesNotExist()
}
```

Add equivalent tests for no-face, alignment, blink, smile, verifying, mismatch, timeout, and technical failure.

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:compileDebugAndroidTestKotlin
```

Expected: FAIL because UI components do not exist.

- [ ] **Step 3: Implement state-driven composition**

```kotlin
@Composable
fun FaceVerificationScreen(
    state: FaceVerificationState,
    onEvent: (FaceVerificationEvent) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        FaceCameraStage(state = state)
        FaceVerificationTopBar(
            intent = state.actionIntent,
            onBack = { onEvent(FaceVerificationEvent.ExitRequested) }
        )
        FaceVerificationBottomSurface(
            stage = state.stage,
            remainingSeconds = state.remainingSeconds,
            onRetry = { onEvent(FaceVerificationEvent.RetryRequested) },
            onContinue = { onEvent(FaceVerificationEvent.ContinueRequested) },
            onCancel = { onEvent(FaceVerificationEvent.ExitRequested) }
        )
    }
}
```

Use Infinite tokens/components. Provide content descriptions and text for every semantic state. Keep raw diagnostics behind a debug-only parameter that is absent in release state.

- [ ] **Step 4: Compile and run Compose tests**

```bash
./gradlew app:compileDebugAndroidTestKotlin
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.face.FaceVerificationScreenTest
```

Expected: compile PASS; device test PASS when a device is available.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face app/src/main/res/values/strings.xml app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/face
git commit -m "feat(face): implement verification guidance and result surfaces"
```

---

### Task 7: Add nested graph and typed Attendance handoff

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/Screen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/FaceVerificationResult.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Test: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/FaceVerificationResultContractTest.kt`
- Test: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenFaceResultRescueTest.kt`

**Interfaces:**
- Produces: `FaceVerificationResultContract.Verified/Failed/Timeout/Cancelled` consumed exactly once by Attendance.

- [ ] **Step 1: Write failing handoff tests**

```kotlin
@Test
fun `verified moves attendance from verifying to submitting`() {
    viewModel.onFaceVerificationResult(
        FaceVerificationResultContract.Verified("session-1", "evidence-1")
    )
    assertTrue(viewModel.uiState.value.actionState is AttendanceActionState.Submitting)
}

@Test
fun `cancelled returns to ready and does not submit`() {
    viewModel.onFaceVerificationResult(FaceVerificationResultContract.Cancelled)
    assertTrue(viewModel.uiState.value.actionState is AttendanceActionState.Ready)
    verifyNoInteractions(checkInUseCase, checkOutUseCase)
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationResultContractTest"
```

- [ ] **Step 3: Implement graph ownership and callback handoff**

```kotlin
navigation(
    route = Screen.FaceVerificationGraph.route,
    startDestination = Screen.FaceVerificationHost.route
) {
    composable(Screen.FaceVerificationHost.route) { entry ->
        val graphEntry = remember(entry) {
            navController.getBackStackEntry(Screen.FaceVerificationGraph.route)
        }
        val viewModel: FaceVerificationViewModel = hiltViewModel(graphEntry)
        FaceVerificationRoute(
            viewModel = viewModel,
            onFinish = { result ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(FACE_VERIFICATION_RESULT_KEY, result.savedStateValue)
                navController.popBackStack(
                    Screen.FaceVerificationGraph.route,
                    inclusive = true
                )
            }
        )
    }
}
```

`FaceVerificationRoute` and ViewModel do not receive `NavController`.

Keep Attendance lifecycle-aware `getStateFlow()` consumption and remove the result immediately after handling.

- [ ] **Step 4: Run result and Attendance tests**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationResultContractTest"
./gradlew app:compileDebugAndroidTestKotlin
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/navigation app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test/java/com/example/infinite_track/presentation/screen/attendance app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance
git commit -m "feat(attendance): wire typed face verification handoff"
```

---

### Task 8: Add tracing, performance guards, and release-safe diagnostics

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/data/face/MlKitFaceFrameAnalyzer.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/face/AndroidFaceSampleExtractor.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/face/FaceVerificationRepositoryImpl.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceVerificationViewModel.kt`
- Test: `app/src/test/java/com/example/infinite_track/data/face/FaceVerificationDiagnosticsPolicyTest.kt`

**Interfaces:**
- Produces trace sections and debug-only diagnostics without altering domain/UI contracts.

- [ ] **Step 1: Write failing diagnostics policy test**

```kotlin
@Test
fun `release policy hides score and threshold`() {
    val payload = FaceVerificationDiagnosticsPolicy(isDebug = false)
        .build(score = 0.83f, threshold = 0.15f, decision = MATCHED)
    assertNull(payload)
}
```

- [ ] **Step 2: Verify failure**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationDiagnosticsPolicyTest"
```

- [ ] **Step 3: Add trace sections**

```kotlin
trace("FaceVerification.detect") { submitDetection() }
trace("FaceVerification.quality") { evaluateQuality() }
trace("FaceVerification.liveness") { evaluateLiveness() }
trace("FaceVerification.extract") { extractSample() }
trace("FaceVerification.embedding") { generateEmbedding() }
trace("FaceVerification.match") { compareEmbedding() }
trace("FaceVerification.result") { publishDecision() }
```

Add debug counters for analyzed, dropped, and matched frames without coordinates or identity data.

- [ ] **Step 4: Run tests and lint-sensitive grep**

```bash
./gradlew app:testDebugUnitTest --tests "*FaceVerificationDiagnosticsPolicyTest"
grep -R "Log\.[dievw].*similarity\|Log\.[dievw].*embedding\|Log\.[dievw].*threshold" app/src/main/java
```

Expected: tests PASS; no ungated sensitive logging.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/data/face app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face app/src/test/java/com/example/infinite_track/data/face
git commit -m "perf(face): add traces and release-safe diagnostics"
```

---

### Task 9: Remove legacy scanner implementation and finish verification

**Files:**
- Remove: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt`
- Remove: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt`
- Remove: `app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt`
- Modify: imports/callers discovered by repository search.
- Update: `docs/superpowers/specs/2026-07-25-inf-201-face-verification-single-pr-design.md` only if implementation decisions changed.

**Interfaces:**
- Final deliverable: no legacy scanner path remains; one graph and one typed flow compile.

- [ ] **Step 1: Confirm no remaining callers before deletion**

```bash
grep -R "FaceScannerScreen\|FaceScannerViewModel\|FaceDetectorHelper" app/src/main app/src/test app/src/androidTest
```

Expected: only legacy declarations or migration tests remain.

- [ ] **Step 2: Delete legacy files and fix imports**

```bash
git rm \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerViewModel.kt \
  app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/FaceScannerScreen.kt \
  app/src/main/java/com/example/infinite_track/data/face/FaceDetectorHelper.kt
```

- [ ] **Step 3: Run focused unit tests**

```bash
./gradlew app:testDebugUnitTest \
  --tests "*FaceVerification*" \
  --tests "*EvaluateFaceQualityUseCaseTest" \
  --tests "*EvaluateLivenessUseCaseTest"
```

Expected: PASS.

- [ ] **Step 4: Run required project verification**

```bash
./gradlew app:testDebugUnitTest
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:lintDebug
./gradlew app:assembleDebug
git diff --check
```

Expected: all commands PASS. Any environment-only instrumentation blocker is documented without claiming device success.

- [ ] **Step 5: Perform device matrix and attach evidence**

Record:

```text
- camera bind and first detection latency;
- no face and alignment guidance;
- low-light detection and automatic recovery;
- blink attempt;
- smile attempt;
- mismatch and retry;
- verified → Attendance Submitting;
- timeout and retry;
- cancel → Attendance Ready;
- camera permission denied/settings recovery;
- background/foreground behavior;
- low-end and mid-range jank/resource review;
- no camera/analyzer after graph exit.
```

- [ ] **Step 6: Commit final cleanup**

```bash
git add -A
git commit -m "test(face): complete verification flow coverage and cleanup"
```

---

## PR Checklist

The PR description must answer:

```text
Presentation: which screen/state/event/effect and reusable components changed?
Domain: which typed quality, liveness, matching, and failure contracts changed?
Data: how CameraX, ML Kit, sample extraction, inference, and secure diagnostics changed?
Navigation: who owns the Face Verification graph and how is the result published once?
Source of truth: where does one scanner session live?
Verification: which tests, builds, traces, and device scenarios passed?
```

Include:

- Linear INF-201 link;
- spec and plan paths;
- before/after screenshots for all approved states;
- build/test/lint evidence;
- device and performance evidence;
- explicit note that face verification is not final Attendance success.

## Plan Self-Review

- Spec coverage: navigation, state model, Clean Architecture, all visual states, CameraX/ML Kit performance, typed matching, security, handoff, tests, and device evidence are each mapped to a task.
- Placeholder scan: no TBD/TODO or unspecified implementation steps remain.
- Type consistency: `FaceVerificationResultContract`, `FaceVerificationStage`, `FaceMatchResult`, `FaceVerificationRepository`, and `FaceRuntimePolicy` names are consistent across tasks.
