# INF-201 Face Verification Single-PR Design

## Status

Approved product and architecture design for Linear issue INF-201.

Linear: https://linear.app/infinite-track-palu/issue/INF-201/android-implement-face-verification-ui-state-system-and-result

## Goal

Implement the complete approved Face Verification experience in one bounded pull request while preserving Clean Architecture and Attendance semantics.

Face verification produces evidence that allows Attendance submission to begin. It never marks check-in or checkout successful by itself.

```text
Attendance Ready
→ VerifyingFace
→ Face Verification
→ Verified evidence
→ Attendance Submitting
→ backend check-in/check-out result
```

## Scope

The pull request implements:

- one nested Face Verification graph and one flow owner;
- no-face, alignment, low-light, blink, smile, verifying, mismatch, verified, timeout, camera-unavailable, and technical-failure states;
- state-driven scanner composition rather than one destination per visual state;
- typed camera, quality, liveness, matching, and result contracts;
- CameraX and ML Kit performance hardening;
- typed result handoff to Attendance;
- release-safe biometric diagnostics;
- unit, Compose, integration, build, lint, and device evidence.

The pull request does not include:

- unrelated Attendance redesign;
- geofence changes;
- auth/session rewrite beyond a bounded embedding-readiness adapter;
- assumed backend API changes;
- FCM or notification work;
- four-step liveness challenges;
- GPU/NPU acceleration or model quantization without evidence.

## Product decisions

### One flow, not many destinations

The visual concepts are state surfaces inside one Face Verification host:

```text
No Face
Face Misaligned
Low Light
Blink
Smile
Verifying
Face Not Matched
Identity Verified
Timeout
Technical Failure
```

They are not separate navigation destinations. Camera lifecycle, timeout, liveness progress, matching, retry generation, and result publication have one owner.

### Liveness policy

Initial production policy uses exactly one challenge per attempt:

```text
Blink OR Smile
```

The challenge is selected by a testable policy. UI copy such as `Challenge 2 of 4` is forbidden unless four real challenges are implemented and approved.

### Honest result semantics

Allowed verified copy:

```text
Identity verified
Face verification successful
Continue to attendance submission
```

Forbidden verified copy:

```text
Check-in successful
Attendance recorded
```

Mismatch, timeout, and recoverable technical failure stay retryable inside the scanner. Cancel returns to Attendance without submitting.

### Diagnostic visibility

Similarity score, threshold, model version, and quality metrics are debug/test evidence by default.

Release UI, logs, analytics, crash reports, navigation arguments, and saved state must not expose biometric-derived diagnostics or embeddings.

## Navigation architecture

```text
MainActivity
└── MainContentNavGraph
    └── AttendanceGraph
        ├── Attendance
        └── FaceVerificationGraph
            └── FaceVerificationHost
```

Only semantic arguments cross the route boundary:

```kotlin
enum class AttendanceActionIntent {
    CHECK_IN,
    CHECK_OUT
}

data class FaceVerificationRouteArgs(
    val intent: AttendanceActionIntent,
    val sessionId: String
)
```

Bitmap, ImageProxy, ML Kit `Face`, embedding, captured image, score, and threshold are never navigation arguments.

## Layer ownership

### Presentation

Owns:

- `FaceVerificationRoute`;
- `FaceVerificationScreen`;
- `FaceVerificationViewModel`;
- persistent UI state;
- user events;
- one-time effects;
- localized text mapping;
- reusable scanner components;
- accessibility announcements;
- result publication callback supplied by navigation.

Rules:

- Screen renders state and sends events only.
- Screen and ViewModel do not hold `NavController`.
- Compose is not the business-state source of truth.
- Navigation/result handoff is a one-time effect.

### Domain

Owns project types and policies:

```text
FaceObservation
FaceQuality
FaceGuidanceReason
LivenessChallenge
LivenessEvaluation
FaceSample
FaceMatchResult
FaceVerificationFailure
FaceVerificationRepository
FaceRuntimePolicy
```

Rules:

- no `Bitmap`, `ImageProxy`, ML Kit `Face`, CameraX, Context, or Compose;
- mismatch and technical failure are different results;
- threshold values are policy/config data, not scattered constants;
- domain types do not contain localized copy.

### Data/platform

Owns:

- CameraX preview and image analysis;
- ML Kit detector lifecycle;
- ML Kit-to-project observation mapping;
- Y-plane quality evaluation;
- face sample extraction and normalization;
- LiteRT/TensorFlow embedding inference;
- local embedding access and secure lifecycle;
- repository implementation;
- debug-only diagnostic construction.

## State model

```kotlin
data class FaceVerificationState(
    val sessionId: String,
    val attemptGeneration: Long,
    val actionIntent: AttendanceActionIntent,
    val stage: FaceVerificationStage,
    val camera: FaceCameraState,
    val remainingSeconds: Int,
    val overlay: FaceOverlayState,
    val canRetry: Boolean,
    val canExit: Boolean
)
```

```kotlin
sealed interface FaceVerificationStage {
    data object Initializing : FaceVerificationStage

    data class Guidance(
        val reason: FaceGuidanceReason
    ) : FaceVerificationStage

    data class PerformingLiveness(
        val challenge: LivenessChallengeState
    ) : FaceVerificationStage

    data class Verifying(
        val step: FaceVerificationProcessingStep
    ) : FaceVerificationStage

    data class Result(
        val decision: FaceVerificationDecision
    ) : FaceVerificationStage

    data class Unavailable(
        val failure: FaceVerificationFailure,
        val recovery: FaceVerificationRecovery
    ) : FaceVerificationStage
}
```

Guidance reasons:

```text
NoFace
MultipleFaces
MoveCloser
MoveFarther
CenterFace
LowLight
Blurry
HoldStill
```

Final decisions:

```text
Matched
NotMatched
Timeout
Cancelled
```

Technical failures:

```text
CameraUnavailable
DetectorUnavailable
ExtractionFailed
InvalidFaceSample
EmbeddingMissing
EmbeddingGenerationFailed
ModelFailure
```

## Events and effects

```kotlin
sealed interface FaceVerificationEvent {
    data object ScreenStarted : FaceVerificationEvent
    data object RetryRequested : FaceVerificationEvent
    data object ExitRequested : FaceVerificationEvent
    data object VerifyRequested : FaceVerificationEvent
    data object ApplicationSettingsReturned : FaceVerificationEvent
    data class FrameObserved(
        val observation: FaceObservation
    ) : FaceVerificationEvent
}
```

```kotlin
sealed interface FaceVerificationEffect {
    data class Finish(
        val result: FaceVerificationResultContract
    ) : FaceVerificationEffect

    data object OpenApplicationSettings : FaceVerificationEffect

    data class AnnounceAccessibility(
        val announcement: FaceVerificationAnnouncement
    ) : FaceVerificationEffect
}
```

## Camera and analysis pipeline

```text
CameraX ImageAnalysis
→ direct YUV ML Kit detection
→ FaceObservationMapper
→ temporal stability evaluator
→ quality evaluator
→ liveness evaluator
→ lock one best sample
→ crop/normalize once
→ embedding once
→ matching once
→ typed decision
```

### CameraX baseline

```text
front camera
640 × 480
YUV_420_888
STRATEGY_KEEP_ONLY_LATEST
Preview + ImageAnalysis bound together
```

Use one in-flight detector guard and one in-flight matcher guard. Every `ImageProxy` closes exactly once.

Full-frame Bitmap conversion per analyzed frame is forbidden. Bitmap/sample conversion happens only after a valid liveness-completed frame is locked.

### ML Kit baseline

```kotlin
FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
    .setMinFaceSize(0.20f)
    .enableTracking()
```

Tracking ID is continuity evidence only, never identity.

One detector lives for one Face Verification graph lifetime. Retry resets attempt state and reuses the detector unless the detector itself failed.

## Temporal stability

Single-frame decisions are not sufficient.

Initial policy:

```kotlin
data class FaceRuntimePolicy(
    val readyFramesRequired: Int = 3,
    val lowLightFramesRequired: Int = 3,
    val challengeFramesRequired: Int = 2,
    val faceLostGraceFrames: Int = 2,
    val timeoutMillis: Long = 20_000,
    val livenessHoldMillis: Long = 750,
    val inferenceThreads: Int = 2
)
```

These are starting values. INF-81 supplies regression evidence and INF-82 governs final thresholds.

Tracking ID change resets challenge progress. Multiple faces pause or reset the challenge. Brief face loss uses the grace window; sustained loss returns to guidance.

## Quality evaluation

Quality gates run before matching:

```text
face count valid
face size valid
face centered
lighting acceptable
stability achieved
liveness complete
sample extracted
```

Low-light evaluation should use sampled luminance from the Y plane, not full RGB conversion.

Blur/sharpness may be evaluated only with bounded cadence after face position becomes ready. It must not add expensive per-frame processing.

## Liveness

```kotlin
interface LivenessEvaluator {
    fun evaluate(
        observation: FaceObservation,
        challenge: LivenessChallenge
    ): LivenessEvaluation
}
```

```kotlin
sealed interface LivenessEvaluation {
    data object Waiting : LivenessEvaluation
    data class Progress(val value: Float) : LivenessEvaluation
    data object Completed : LivenessEvaluation
    data class Invalid(val reason: LivenessInvalidReason) : LivenessEvaluation
}
```

Blink and smile share one reusable UI and evaluator contract.

## Matching

```kotlin
interface FaceVerificationRepository {
    suspend fun verify(
        sample: FaceSample,
        identity: FaceIdentityReference
    ): FaceMatchResult
}
```

```kotlin
sealed interface FaceMatchResult {
    data class Matched(
        val evidence: FaceMatchEvidence
    ) : FaceMatchResult

    data class NotMatched(
        val reason: FaceMismatchReason
    ) : FaceMatchResult

    data class Failed(
        val failure: FaceMatchFailure
    ) : FaceMatchResult
}
```

`VerifyFaceUseCase` must not accept Android `Bitmap` or return `Result<Boolean>`.

One exact sample is locked after liveness success. One embedding and one comparison run per attempt.

LiteRT baseline is CPU with two threads. GPU/NPU delegates and quantization remain follow-ups until model/device benchmarks and threshold recalibration are approved.

Input and output buffers should be reused. Persisted embedding format changes require migration and compatibility tests.

## UI composition

### Shared structure

```text
FaceVerificationTopBar
FaceCameraStage
├── CameraPreview
├── FaceGuideOverlay
├── FaceStatusChip
├── LivenessChallengeProgress
└── FaceGuidanceMessage
FaceVerificationBottomSurface
```

### Guidance surface

Renders no-face, positioning, low-light, blur, blink, smile, and hold-still states.

### Processing surface

Renders extraction, embedding, and matching progress. Duplicate verify, retry, frame lock, and matching requests are disabled.

### Result surface

Renders matched, not matched, timeout, and technical failure. Mismatch and timeout offer retry. Verified offers Continue to Attendance.

Raw score and threshold are absent in release UI. A debug-only diagnostic card may display approved evidence.

## UI emission efficiency

Compose state is not updated for every camera frame.

Separate high-frequency overlay coordinates from low-frequency semantic state. Persistent state updates only when:

- semantic state changes;
- progress changes meaningfully;
- countdown changes;
- result/recovery changes.

Minor bounding-box jitter must not trigger full-screen recomposition.

## Result handoff

```kotlin
sealed interface FaceVerificationResultContract {
    data class Verified(
        val sessionId: String,
        val evidenceId: String
    ) : FaceVerificationResultContract

    data object Failed : FaceVerificationResultContract
    data object Timeout : FaceVerificationResultContract
    data object Cancelled : FaceVerificationResultContract
}
```

Default flow:

```text
Verified
→ publish once
→ return to Attendance
→ VerifyingFace → Submitting

NotMatched / Timeout
→ remain in scanner for retry
→ no Attendance submission

Cancelled
→ return to Attendance
→ restore Ready
```

## Timeout and lifecycle

Timeout uses a monotonic clock, not a decrement-only counter.

```kotlin
val deadline = clock.elapsedRealtimeMillis() + policy.timeoutMillis
```

The scanner cancels the active attempt when the host leaves the foreground. Camera and analyzer work stop when the graph is no longer visible. No foreground service or battery-optimization exemption is used.

## Security and privacy

- No raw embedding in presentation, logs, analytics, crash reports, routes, or result payloads.
- Release logs omit similarity score, threshold, outcome internals, and processing details.
- Debug diagnostics are explicitly gated by `BuildConfig.DEBUG`.
- Local embedding is encrypted, account-scoped, cleared on logout, invalidated when identity reference changes, and excluded from backup.
- Captured frame/sample lifetime is bounded to the current attempt and released after result/reset.

## Performance observability

Add trace sections:

```text
FaceVerification.cameraBind
FaceVerification.detect
FaceVerification.quality
FaceVerification.liveness
FaceVerification.extract
FaceVerification.embedding
FaceVerification.match
FaceVerification.result
```

Record device evidence for:

- camera bind latency;
- first face detection latency;
- average and p95 detection duration;
- liveness completion duration;
- embedding inference p50/p95;
- dropped frames and allocations;
- time to result;
- Compose jank;
- resource release after graph exit.

## Error handling

Typed recovery examples:

```text
Camera permission denied
→ request permission / open settings

Detector unavailable
→ retry detector initialization

Embedding missing
→ refresh identity readiness / contact admin according to existing auth contract

Extraction or model failure
→ retry attempt when recoverable

Mismatch
→ retry; never classify as technical failure
```

## Test strategy

### Unit

- reducer/state-machine transitions;
- temporal stability;
- no-face, multiple-face, alignment, low-light, blur policy;
- blink/smile waiting, progress, completed, invalid;
- timeout using fake monotonic clock;
- match, mismatch, missing embedding, extraction failure, generation failure, model failure;
- publish-once result guard;
- debug/release diagnostics policy.

### Compose

- each visual state;
- actions and enabled/disabled states;
- accessibility labels and no color-only semantics;
- large font and narrow width;
- retry and cancel behavior.

### Integration

```text
Attendance Ready
→ scanner
→ liveness
→ matched
→ typed Verified result
→ Attendance Submitting
```

Cancel, mismatch, timeout, and technical failure must never submit Attendance.

### Device

Verify front-camera lifecycle, no-face, misalignment, low-light recovery, blink, smile, mismatch, verified, timeout, retry, background/foreground, permission denial, low-end performance, and resource release.

## Acceptance criteria

### Architecture

- One nested graph owns Face Verification.
- Visual states are not separate destinations.
- Screen and ViewModel do not hold `NavController`.
- State, events, and effects are separated.
- Android/ML Kit types do not leak into domain.
- Matching is typed and not `Result<Boolean>`.

### Runtime

- One analyzer and one matcher operation at a time.
- Every ImageProxy closes exactly once.
- Retry cancels old jobs and increments attempt generation.
- Final result publishes once.
- Low light blocks matching and recovers automatically.
- Camera/analyzer stop after leaving the graph.

### Performance

- 640×480 YUV baseline is used.
- Detection runs directly on YUV.
- ML Kit FAST/classification-only configuration is used.
- Temporal stability prevents state flicker.
- One sample, embedding, and match run per attempt.
- CPU two-thread inference baseline is measured.
- Trace and device metrics are attached.

### UI

- All approved visual states are implemented.
- Infinite components and tokens are reused.
- UI does not rely on color alone.
- Release UI hides raw diagnostic score/threshold.
- Verified copy does not claim Attendance success.

### Verification

The following commands pass, or blockers are documented with evidence:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:lintDebug
./gradlew app:assembleDebug
```

## Definition of Done

The single pull request is complete when the complete approved state system works end-to-end, Clean Architecture boundaries are restored, sensitive biometric data is absent from release paths, tests/builds pass, device evidence is attached, and backend submission remains the only final Attendance authority.
