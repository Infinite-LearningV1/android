# INF-201 Face Verification Result & UI State Design (Code-Accurate Revision)

## Status

Revised product and architecture design for Linear issue INF-201, re-grounded against the **actual** Android codebase on `develop`.

- Linear: https://linear.app/infinite-track-palu/issue/INF-201/android-implement-face-verification-ui-state-system-and-result
- Supersedes the aspirational single-PR rewrite drafted on branch `djangosuryaa/inf-201-face-verification-single-pr-spec-plan` (GitHub issue #102). See [Divergence from issue #102](#divergence-from-issue-102).

## Why this revision exists

The earlier draft proposed a big-bang rewrite: a nested `FaceVerificationGraph`/`FaceVerificationHost`, a new `FaceVerificationRepository`, typed `FaceMatchResult` replacing `Result<Boolean>`, new domain models (`FaceObservation`, `FaceQuality`, …), and deletion of `FaceScannerScreen`, `FaceScannerViewModel`, and `FaceDetectorHelper`.

A verification pass against the real code shows that architecture does not exist and the flow it wanted to delete is the active, working implementation. This revision keeps INF-201's genuine acceptance criteria but re-expresses them as **incremental, test-driven enhancements** to the existing face flow. No component is deleted, no parallel navigation graph is introduced.

## Goal

Bring the existing face verification flow up to the INF-201 acceptance criteria: an honest, differentiated result experience (success/failure with a specific reason), clear Retry / Continue / Cancel actions, loading and error handling, and release-safe biometric diagnostics — while preserving Clean Architecture and Attendance semantics.

Face verification produces evidence that lets Attendance submission begin. It never marks check-in/check-out successful by itself; the backend remains the only final authority.

```text
Attendance Ready
→ VerifyingFace (scanner)
→ liveness + match
→ typed result back to Attendance
→ Submitting → backend check-in/check-out result
```

## Current architecture (verified)

### Navigation

- `presentation/navigation/Screen.kt`: `data object FaceScanner : Screen("face_scanner?action={action}")` with `createRoute(action: String = "checkin")`.
- `presentation/navigation/MainContentNavGraph.kt`: a **single** `composable(Screen.FaceScanner.route)` with `navArgument("action")` (default `"checkin"`) rendering `FaceScannerScreen(action, navController)`. There is **no** nested graph and **no** `FaceVerificationHost`.
- Result handoff uses the Navigation `savedStateHandle`: `FaceScannerScreen` sets `previousBackStackEntry.savedStateHandle[FACE_VERIFICATION_RESULT_KEY] = result.savedStateValue` then `popBackStack()`.

### Presentation — face scanner

- `presentation/screen/attendance/face/FaceScannerScreen.kt` (~1064 lines): owns camera permission recovery UI, CameraX preview + analysis, overlay, instruction/result surface, and result publication. It **does** receive `navController` and publishes results itself via a local `publishResultOnce` guard.
- `presentation/screen/attendance/face/FaceScannerViewModel.kt` (~469 lines):
  - `enum LivenessChallenge { BLINK, SMILE }`
  - `enum LivenessState { IDLE, DETECTING_FACE, WAITING_FOR_LIVENESS, LOW_LIGHT, LIVENESS_DETECTED, VERIFYING_FACE, SUCCESS, FAILURE, TIMEOUT }`
  - `data class FaceScannerState(livenessState, currentChallenge, instructionText, boundingBox, progress, errorMessage, isProcessing, timeRemaining, showCountdown, imageSize)`
  - Exposes `StateFlow<FaceScannerState>`; drives detection via `FaceDetectorHelper`; calls `VerifyFaceUseCase(faceBitmap): Result<Boolean>`.
  - Timeout: 20 s via a decrement counter (`startTimeout()` with `repeat` + `delay(1000)`). Liveness hold: 1.5 s. Random single challenge (BLINK or SMILE) per attempt.
- `presentation/screen/attendance/face/CameraPermissionRecoveryContract.kt`: `enum CameraPermissionRecoveryUi { CAMERA, RECOVERY_REQUIRED, RATIONALE, APPLICATION_SETTINGS }` + `resolve(isCameraGranted, hasRequestedRecovery, shouldShowRationale)`.
- `presentation/screen/attendance/face/FaceLightingQuality.kt`: `internal enum LightingQuality { ACCEPTABLE, LOW_LIGHT }` + `evaluate(pixels: IntArray)` over RGB luminance.

### Data / platform

- `data/face/FaceDetectorHelper.kt` (~302 lines): ML Kit `FaceDetectorOptions` = `PERFORMANCE_MODE_FAST`, `LANDMARK_MODE_ALL`, `CLASSIFICATION_MODE_ALL`, `setMinFaceSize(0.15f)`, `enableTracking()`. `detect(imageProxy, onResult)` returns the **largest** face only (silently ignoring extra faces) and closes the `ImageProxy` in `addOnCompleteListener`. Also `verifyBlink`, `verifySmile`, `extractFaceBitmap`, `isFaceWellPositioned`, `reinitialize`, `release`.
- `data/face/FaceProcessor.kt` (~312 lines): TFLite `face_recognition_metadata.tflite`, 112×112 input, 128-dim embedding. `generateEmbedding(photoUrl): Result<ByteArray>` and `generateEmbeddingFromBitmap(bitmap): Result<ByteArray>`.

### Domain

- `domain/use_case/auth/VerifyFaceUseCase.kt` (~145 lines): `suspend operator fun invoke(capturedFaceBitmap: Bitmap): Result<Boolean>`. Threshold `SIMILARITY_THRESHOLD = 0.15f`. Cosine similarity vs stored embedding. **Logs the raw similarity score and threshold with `Log.d` unconditionally** (release-path leak).
- `domain/repository/AuthRepository.kt`: `getLoggedInUser(): Flow<UserModel?>` and `saveFaceEmbedding(userId, embedding)`. There is **no** dedicated face repository.
- `domain/model/auth/UserModel.kt`: carries `faceEmbedding: ByteArray?`.

### Attendance integration

- `presentation/screen/attendance/FaceVerificationResult.kt`: an **enum** `FaceVerificationResult(savedStateValue, submitsAttendance, returnsImmediatelyToAttendance, allowsRetryOnScanner, attendanceErrorMessage)` with values `SUCCESS`, `FAILED`, `TIMEOUT`, `CANCELLED`; plus `fromSavedState(String)` and `fromScannerExitState(LivenessState)`. Key: `FACE_VERIFICATION_RESULT_KEY`.
- `presentation/screen/attendance/AttendanceActionState.kt`: `sealed interface AttendanceActionState { Loading, Ready, Blocked, VerifyingFace, Submitting, Success, RetryableFailure, Completed }` (INF-239 already landed the honest state machine).
- `AttendanceViewModel`: `onAttendanceButtonClicked()` → `VerifyingFace(intent)` + `NavigationTarget.FaceScanner(intent)`; `onFaceVerificationResult(result)`; `onUnexpectedFaceVerificationResult()`; `checkInUseCase` / `checkOutUseCase`.
- `AttendanceScreen`: observes `savedStateHandle.getStateFlow(FACE_VERIFICATION_RESULT_KEY, null)`, parses with `FaceVerificationResult.fromSavedState`, calls the ViewModel, then removes the key.

### Reusable UI already in use

`StatefulButton`, `ButtonStyle`, `ButtonStateType` (`presentation/components/button`), `FaceBoundingBox` (`presentation/components/cameras`), `LoadingAnimation` (`presentation/components/loading`), and design tokens `InfiniteColors` / `InfiniteSemantic` plus `InfiniteInlineAlert` / `InfiniteSnackbar*` (`presentation/design`).

## Gap analysis vs INF-201 acceptance criteria

| INF-201 criterion | Current state | Action in this design |
|---|---|---|
| Show captured photo on result | Not shown | Hold the extracted face bitmap in `FaceScannerState` (transient) and render it on the result surface. |
| Show confidence / similarity | Not shown | Keep raw score **debug-only** (security). Release shows qualitative pass/fail, not the number. Documented divergence. |
| Clear Success (green) / Failed (red) | Partly (icon/colour by state) | Formalize a result surface with token-based colour **and** text/icon (never colour-only). |
| Specific failure reason (no face, multiple faces, low confidence, poor lighting) | Collapsed into one `FAILURE` + separate `LOW_LIGHT` guidance | Introduce a typed `FaceVerificationFailureReason` and differentiate reasons. |
| Retry / Continue / Cancel buttons | Retry + Close only; success auto-returns | Keep Retry/Cancel; add explicit result-state actions; success continues to Attendance submission. |
| Loading state during verification | `isProcessing` overlay | Preserve; ensure it is announced for accessibility. |
| Error handling for API/processing failure | Collapsed into `FAILURE` | Separate `TECHNICAL_FAILURE` from `NOT_MATCHED`. |
| Navigate to check-in on success | Works via `SUCCESS` result → Attendance `Submitting` | Preserve; add regression tests locking the handoff. |
| Multiple faces | `detect` silently picks the largest | Expose face count; surface `MULTIPLE_FACES` guidance. |

## Product decisions

### One flow, no new graph

Visual concepts (no face, misaligned, low light, blink, smile, verifying, not matched, verified, timeout, technical failure) remain **states inside the existing single `FaceScannerScreen`**, not separate destinations and not a nested graph. This matches the working implementation and keeps camera lifecycle, timeout, liveness, matching, and result publication under one owner.

### Honest result semantics

Allowed verified copy: "Identitas terverifikasi", "Verifikasi wajah berhasil", "Melanjutkan ke pengiriman absensi".

Forbidden verified copy: "Check-in berhasil", "Absensi tercatat" (backend is the only authority; already respected by the `SUCCESS → Submitting` handoff).

Mismatch, timeout, low light, no face, multiple faces, and recoverable technical failures stay retryable inside the scanner. Cancel returns to Attendance without submitting.

### Differentiated failure reasons

Replace the single generic `FAILURE` message with a typed reason so the UI can explain what went wrong:

```text
NO_FACE            → no face detected within the frame/time
MULTIPLE_FACES     → more than one face in frame
LOW_LIGHT          → lighting insufficient for reliable matching
NOT_MATCHED        → liveness ok, embedding below threshold
TECHNICAL_FAILURE  → embedding/model/extraction/no-stored-embedding error
```

`TIMEOUT` remains its own terminal outcome (distinct from `NO_FACE`, which may occur before timeout). `LOW_LIGHT` already exists as a live guidance state and also becomes a valid terminal reason if unresolved.

### Confidence privacy (divergence-resolving decision)

INF-201 asks to "show confidence score". The locked security rule forbids exposing biometric-derived diagnostics in release. Resolution:

- **Release UI**: no raw similarity number; show only qualitative pass/fail and reason copy.
- **Debug builds** (`BuildConfig.DEBUG`): a diagnostic line may show the similarity score and threshold to support tuning (INF-157 style).
- Similarity/threshold/embedding never enter navigation arguments, `savedStateHandle`, analytics, crash reports, or release logs.

### Captured-photo handling

The result surface may render the extracted face bitmap held in `FaceScannerState`. It is transient (kept in ViewModel memory for the current attempt, cleared on reset/`onCleared`), never persisted, never logged, and never passed as a navigation argument.

## State model changes (incremental)

Extend the existing types rather than replacing them.

`FaceScannerViewModel`:

```kotlin
enum class FaceVerificationFailureReason {
    NO_FACE,
    MULTIPLE_FACES,
    LOW_LIGHT,
    NOT_MATCHED,
    TECHNICAL_FAILURE
}
```

`FaceScannerState` gains (defaults preserve current behaviour):

```kotlin
data class FaceScannerState(
    // ...existing fields...
    val failureReason: FaceVerificationFailureReason? = null,
    val detectedFaceCount: Int = 0,
    val capturedFacePreview: Bitmap? = null,
    val debugSimilarity: Float? = null // populated only in debug builds
)
```

`LivenessState` is unchanged in shape; `FAILURE`/`TIMEOUT` now carry meaning through `failureReason`.

## Result contract changes

`FaceVerificationResult` (enum) stays the transport for Attendance because the `savedStateHandle` string protocol is already wired end-to-end. The scanner keeps deciding when to publish `SUCCESS`; `FAILED`/`TIMEOUT` remain on-screen for retry; `CANCELLED` returns to Attendance.

Rationale for keeping the enum (not a sealed `Verified(sessionId, evidenceId)`): the differentiated reasons are UI/guidance concerns that live **inside** the scanner and never need to cross the route boundary. Attendance only needs "did this submit or not", which the enum's `submitsAttendance` flag already answers. Adding session/evidence identifiers would introduce unused ceremony and risk carrying biometric-derived data across the boundary.

## Camera and analysis pipeline (as-is, with targeted change)

The existing CameraX pipeline (front camera, `STRATEGY_KEEP_ONLY_LATEST`, `imageProxyToBitmap` per analyzed frame) is retained. INF-201's aspirational "direct YUV, no per-frame Bitmap" optimization is **out of scope** here and tracked separately (see divergence). The only pipeline change:

- `FaceDetectorHelper.detect` reports the number of detected faces (in addition to the largest face) so the ViewModel can surface `MULTIPLE_FACES` without changing detection performance characteristics.

ML Kit config stays `FAST` + `CLASSIFICATION_MODE_ALL` + tracking. `LANDMARK_MODE_ALL` is left unchanged in this issue to avoid liveness regressions; any change to `LANDMARK_MODE_NONE` must be justified by INF-81 evidence and is out of scope.

## Liveness

Unchanged: one challenge per attempt (BLINK or SMILE), progressive feedback via `LivenessResult { SUCCESS, IN_PROGRESS, FAILURE }`, 1.5 s hold before verification. No multi-step / "challenge N of 4" expansion.

## Matching

`VerifyFaceUseCase(Bitmap): Result<Boolean>` is retained. The ViewModel maps its outcome to differentiated reasons:

- `Result.success(true)` → `LivenessState.SUCCESS`.
- `Result.success(false)` → `FAILURE` with `NOT_MATCHED`.
- `Result.failure(...)` → `FAILURE` with `TECHNICAL_FAILURE` (including the existing "no stored embedding" failure).

The only change inside `VerifyFaceUseCase` is **gating the similarity/threshold logs behind `BuildConfig.DEBUG`**. Its public signature does not change (no `Result<Boolean>` → typed-result refactor in this issue).

## Security and privacy

- Similarity score, threshold, and embeddings must not appear in release logs, analytics, crash reports, navigation arguments, or `savedStateHandle`.
- `VerifyFaceUseCase` similarity/threshold logging is gated by `BuildConfig.DEBUG`.
- `capturedFacePreview` and `debugSimilarity` are transient, released on reset/`onCleared`, and never persisted or logged.
- Existing embedding storage (Room via `AuthRepository`) is unchanged; logout clearing continues via `AuthRuntimeCleaner`.

## UI composition

Reuse existing components and tokens. The result surface (inside `InstructionSection` / a dedicated result composable) must:

- Show success as green + check icon + verified copy; failure as red + error icon + reason copy; never colour alone.
- Render the captured face thumbnail when available.
- Offer **Continue** on success (or auto-return, matching current SUCCESS behaviour), **Retry** for retryable reasons (`NOT_MATCHED`, `TIMEOUT`, `NO_FACE`, `MULTIPLE_FACES`, `LOW_LIGHT`, recoverable `TECHNICAL_FAILURE`), and **Cancel/Close** always.
- Provide content descriptions / accessibility announcements for each terminal state.
- In release, omit any raw score/threshold; a debug-only diagnostic line may show `debugSimilarity`.

## Test strategy

### Unit (`app/src/test`)

- `VerifyFaceUseCase`: similarity/threshold logging is invoked only when debug; match/no-match/failure mapping.
- `FaceScannerViewModel`: reason mapping (no face / multiple faces / low light / not matched / technical failure / timeout), retryability per reason, single-publish success, captured preview cleared on reset.
- `FaceVerificationResult`: `SUCCESS`, `FAILED`, `TIMEOUT`, and `CANCELLED` round-trip through `fromScannerExitState` / `fromSavedState`. Scanner-local failure reasons are covered by `FaceOutcomeMapper` and UI-copy tests; they are never persisted or navigated.
- Reason → copy/colour mapping is pure and unit-tested.

### Compose (`app/src/androidTest`)

- Each terminal state renders the right copy, icon, colour, and buttons.
- Success uses identity copy, not attendance-success copy.
- Release variant hides raw score; retry/cancel behaviour.

### Integration

- `Attendance Ready → scanner → matched → SUCCESS → Attendance Submitting`.
- `FAILED`/`TIMEOUT`/`CANCELLED` never submit Attendance (lock existing `AttendanceViewModel` behaviour).

### Device

Front-camera lifecycle, no-face, misalignment, low-light recovery, blink, smile, not-matched, verified, timeout, retry, cancel, permission denial/settings recovery, background/foreground, resource release.

## Acceptance criteria

### Behaviour

- Result surface shows captured photo, clear success/failure with text+icon+colour, and a specific reason on failure.
- Retry / Continue / Cancel available per the rules above; loading state during verification.
- Success continues to Attendance submission; face success is never attendance success.

### Architecture

- No new navigation graph; existing single `FaceScanner` route and `savedStateHandle` handoff preserved.
- `FaceScannerScreen`, `FaceScannerViewModel`, `FaceDetectorHelper`, `FaceProcessor`, `VerifyFaceUseCase` retained (no deletions).
- Android/ML Kit types stay out of `VerifyFaceUseCase`'s domain signature (already true).

### Security

- No raw similarity/threshold/embedding in release logs, analytics, nav args, or saved state.
- `VerifyFaceUseCase` logging gated by `BuildConfig.DEBUG`.

### Verification

The following pass, or blockers are documented with evidence:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:test
./gradlew app:compileDebugKotlin
./gradlew app:compileDebugAndroidTestKotlin
./gradlew app:lint
./gradlew app:lintDebug
./gradlew app:assembleDebug
```

The face-verification flow also requires emulator/device evidence. If a connected runtime is
unavailable, report that gate as **Needs Verification**.

## Divergence from issue #102

Grounded in the code verification above, these parts of the original single-PR draft are intentionally **not** adopted here:

1. **Nested `FaceVerificationGraph`/`FaceVerificationHost`** — the flow is a single flat route with `savedStateHandle` handoff. Introducing a graph would be a navigation rewrite with no behavioural benefit for INF-201.
2. **Deleting `FaceScannerScreen`/`FaceScannerViewModel`/`FaceDetectorHelper`** — these are the active implementation; deletion is neither necessary nor safe.
3. **New `FaceVerificationRepository` + typed `FaceMatchResult` replacing `Result<Boolean>`** — face data is served through `AuthRepository` + `UserModel.faceEmbedding`; a new repository is unused ceremony for this issue.
4. **New domain models (`FaceObservation`, `FaceQuality`, `FaceRuntimePolicy`, …)** — not present; the differentiated-reason enum inside the scanner covers INF-201's needs.
5. **Direct-YUV / no-per-frame-Bitmap pipeline, GPU/NPU, threshold governance, `LANDMARK_MODE_NONE`** — performance/model concerns tracked by INF-81/INF-82/INF-237; out of INF-201 scope.
6. **Sealed `Verified(sessionId, evidenceId)` result** — the enum + `submitsAttendance` already satisfies the boundary; identifiers would risk carrying biometric-derived data across routes.

Items 1–6 remain legitimate future refactors; if the team wants the target architecture, it should be its own spec/issue with its own runtime evidence, not folded into INF-201.

## Definition of Done

INF-201 is complete when the existing scanner presents an honest, differentiated result experience (captured photo, success/failure, specific reason, Retry/Continue/Cancel, loading, error handling), raw biometric diagnostics are absent from release paths, the Attendance handoff still gates submission behind the backend, unit/Compose/build/lint pass, and device evidence is attached.
