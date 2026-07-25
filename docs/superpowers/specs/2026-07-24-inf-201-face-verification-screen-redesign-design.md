# INF-201 Face Verification Screen Redesign Design

## Status

Approved product & UI design for the Face Verification screen redesign (INF-201 follow-up), based on user-provided mockups for three states (Liveness Detection, Verification Successful, No Face Detected) plus derived states.

- Linear: https://linear.app/infinite-track-palu/issue/INF-201/android-implement-face-verification-ui-state-system-and-result
- Builds on and partly supersedes: `docs/superpowers/specs/2026-07-24-inf-201-face-verification-result-state-design.md`.

## Goal

Redesign the existing single-route face verification screen to match the approved mockups: a full-screen camera with a neon face frame, a 4-step liveness challenge with a numbered progress rail, status pills, guidance text, a diagnostics card, and a phase-aware bottom sheet — reusing existing Infinite components wherever possible.

Face verification still produces evidence only; the backend remains the sole authority for attendance success.

## Governance overrides (explicit, user-approved)

This redesign intentionally overrides three decisions locked in the prior INF-201 spec. Each was explicitly chosen by the product owner:

1. **Four real liveness challenges** replace the previous "one challenge (BLINK or SMILE) per attempt". `Challenge N of 4` copy and the 1–4 rail are now honest.
2. **Similarity score and threshold are shown in the UI in all build variants** (release included), not debug-only. Rationale: demonstration/thesis transparency. Raw embedding vectors are still never displayed or logged.
3. **`VerifyFaceUseCase` return type changes** from `Result<Boolean>` to a typed result carrying `similarity`, `threshold`, and `isMatch`, so the UI can render the diagnostics card.

These overrides touch the spirit of INF-80 (remove sensitive logging) and INF-82 (threshold governance); this document is the record of the deliberate deviation. Raw embeddings remain out of all logs, navigation args, and saved state.

## Scope

In scope:
- Redesign of the scanner screen visuals and phase flow to match mockups.
- Expanded liveness state model for a fixed 4-challenge sequence with per-challenge timeout.
- New `FaceVerificationFrame` composable (replaces the visual role of `FaceBoundingBox`).
- Numbered progress rail, status pills, guidance text, diagnostics card, phase-aware bottom sheet.
- Typed match result exposing similarity/threshold to the UI.
- Head-angle liveness detection (`headEulerAngleY`) added to `FaceDetectorHelper`.

Out of scope:
- Nested navigation graph, new repository, or removing the existing single route.
- New ML models, GPU/NPU, quantization, CameraX pipeline rewrite.
- Backend contract changes.

## Reuse inventory (verified)

| Need | Reuse | Path |
|---|---|---|
| Top bar (card, back, centered title) | `InfiniteTopBar` (extend for subtitle + right pill) | `presentation/design/components/navigation/InfiniteTopBar.kt` |
| Status pills | `InfiniteStatusPill` (leading icon, variants, `colorOverride`) | `presentation/design/components/status/InfiniteStatusPill.kt` |
| Buttons | `StatefulButton` (Elevated/Outlined; Default/Info/Error) and/or `InfiniteButton` | `presentation/components/button/StatefulButton.kt`, `presentation/design/components/button/InfiniteButton.kt` |
| Bottom sheet | Material3 `BottomSheetScaffold` glass pattern | as used in `presentation/screen/attendance/AttendanceScreen.kt` |
| Tokens/colors | `InfiniteColors` (Primary purple `Blue_500`, `Success`, `Warning`, `Accent` cyan), theme `Color.kt` | `presentation/design/tokens/InfiniteColors.kt` |
| Icons | `InfiniteIcons` + material-icons-core (CheckCircle, WarningAmber, AccessTime, ArrowBack) | `presentation/design/tokens/InfiniteIcons.kt` |
| Detection frame | `FaceBoundingBox` — **redesign** into `FaceVerificationFrame` | `presentation/components/cameras/FaceBoundingBox.kt` |

## Screen anatomy

Single route `Screen.FaceScanner` (`face_scanner?action={action}`), full-screen `Box`:

```text
CameraPreview (existing CameraX front camera, KEEP_ONLY_LATEST)
FaceVerificationOverlay
├── FaceVerificationTopBar        (top, floating card)
├── StatusPillRow                 (left status pill, right context pill)
├── FaceVerificationFrame         (center neon frame + numbered rail + badge)
├── FaceGuidanceText              (challenge title, hint, "Time remaining: Ns", Tips)
└── (processing overlay when Verifying)
FaceVerificationBottomSheet       (phase-aware)
```

### Top bar

Extend `InfiniteTopBar` (or a thin `FaceVerificationTopBar` wrapper) to support:
- centered title + subtitle (two lines), and
- a right-side status pill instead of an icon action.

Title: `Liveness Detection` while challenges run; `Face Verification` for detecting/no-face/result. Subtitle: `<Check In|Check Out> · <WorkMode>` (e.g. `Check In · WFO`). Right pill: `● <Check-in|Check-out>` in Primary purple.

### Status pills (reuse `InfiniteStatusPill`)

| Phase | Left pill | Right pill |
|---|---|---|
| Detecting / no face | `No face detected` (Warning) | `Position your face` (Neutral) |
| Liveness | `Face detected` (Success, CheckCircle) | `Challenge {index} of 4` (Neutral) |
| Verified | `Face matched` (Success) | `Similarity verified` (Neutral) |
| Not matched / technical | `Not matched` / `Verification failed` (Error) | contextual |

## Liveness model

```kotlin
enum class LivenessChallenge { BLINK, SMILE, TURN_LEFT, TURN_RIGHT }

// Fixed order, index 1..4 shown in the rail and "Challenge N of 4".
val CHALLENGE_ORDER = listOf(BLINK, SMILE, TURN_LEFT, TURN_RIGHT)
```

Phase model (expanded `LivenessState` or a new sealed `FacePhase`):

```text
DetectingFace          → no face / positioning / low light / multiple faces guidance
Liveness(index 1..4)   → current challenge, per-challenge 20s countdown
ReadyToVerify          → all 4 challenges passed; Verify enabled
Verifying              → matching in progress (processing overlay)
Result:
  Verified(evidence, similarity, threshold)
  NotMatched(similarity, threshold)
  Timeout
  TechnicalFailure(reason)
```

Detection signals (ML Kit, no new model):
- BLINK: `leftEyeOpenProbability`/`rightEyeOpenProbability` below threshold (existing logic).
- SMILE: `smilingProbability` above threshold (existing logic).
- TURN_LEFT / TURN_RIGHT: `headEulerAngleY` beyond ±threshold (new; sign chosen for front-camera mirroring).

Rules:
- **Timeout is per challenge: 20s, reset when a challenge is passed.** Expiry → `Timeout` result (retryable).
- Challenges are sequential and auto-detected; a brief face-loss grace applies (reuse existing behavior). Multiple faces pause progress with guidance.
- After challenge 4 passes → `ReadyToVerify`. The **Verify** button is disabled during challenges and enabled in `ReadyToVerify`.

## Verify action & matching

- User taps **Verify** in `ReadyToVerify` → `Verifying` → lock the current best frame → `VerifyFaceUseCase`.
- New typed result:

```kotlin
data class VerifyFaceMatch(
    val isMatch: Boolean,
    val similarity: Float,
    val threshold: Float
)
// VerifyFaceUseCase now returns Result<VerifyFaceMatch> (was Result<Boolean>).
```

- `isMatch` → `Verified`; `!isMatch` → `NotMatched` (both carry similarity+threshold for the card); exceptions → `TechnicalFailure`.
- The score/threshold flow to the approved UI diagnostics intentionally. The domain use case does not log them or raw embeddings.

## FaceVerificationFrame (redesign)

A new composable parameterized by a `FrameStyle` derived from phase:

```kotlin
data class FrameStyle(
    val color: Color,        // purple (liveness/detect), cyan (success), red (fail)
    val dashed: Boolean,     // true only for no-face
    val showRail: Boolean,   // true during liveness
    val showCheckBadge: Boolean, // true on success (top-center ✓)
    val showSilhouette: Boolean  // true on no-face (center silhouette + scan corners)
)
```

- Liveness: solid purple neon, segmented corner ticks, **numbered rail 1–4** overlaid on the frame edges. Node states: passed = cyan with ✓, active = orange, pending = white outline. Rail connectors mirror the mockup (left nodes 1–2, right nodes 3–4).
- Verified: solid cyan neon, top-center circular ✓ badge, no rail.
- No face: dashed purple neon, centered person silhouette + scan-corner marks.
- Keep the existing image→preview coordinate mapping and front-camera mirroring from `FaceBoundingBox` when a real bounding box is available; the frame is a fixed centered guide when no face is present.

The numbered rail is its own sub-composable (`LivenessProgressRail`) taking `passedCount`/`activeIndex` so it is testable and reusable.

## Guidance text

Center-bottom, above the sheet:
- Challenge title (e.g. `Smile to continue`) + hint (e.g. `Show a natural smile inside the frame`).
- `Time remaining: {n}s` with a clock icon (AccessTime), warning-colored seconds.
- No-face state shows a `Tips` block instead.

Per-challenge copy (strings.xml): Blink, Smile, Turn Left, Turn Right — title + hint each.

## Bottom sheet (phase-aware)

Reuse the `BottomSheetScaffold` glass pattern. Content by phase:

| Phase | Title | Body | Actions |
|---|---|---|---|
| No face | `No Face Detected` | can't find a face… | Try Again (primary) · Cancel |
| Liveness / ReadyToVerify | `Verify your liveness` | active challenge confirms presence. Attendance will be submitted next. | Verify (primary, enabled only in ReadyToVerify) · Try Again · Cancel |
| Verified | `Identity Verified` | Face verification was successful. Attendance will be submitted next. | Continue to Attendance (primary → publish SUCCESS) · Back |
| Not matched / timeout / technical | reason title | reason message | Try Again (primary) · Cancel |

Verified copy must not claim attendance success. `Continue to Attendance` publishes `FaceVerificationResult.SUCCESS`; Cancel/Back publish `CANCELLED`.

## Diagnostics card (success & not-matched)

A dark rounded card above the sheet with three rows: `Similarity Score: {value}`, `Threshold: {value}`, `Result: Matched|Not matched`, each with a leading icon and a trailing status check/cross. Shown for `Verified` and `NotMatched` (whenever a score was computed); absent for no-face/timeout/technical. Values come from `VerifyFaceMatch`. Present in all build variants (approved override).

## Navigation & handoff

Unchanged transport: `FaceVerificationResult` enum via `savedStateHandle` (`FACE_VERIFICATION_RESULT_KEY`), consumed once by Attendance. `SUCCESS → Submitting`; `FAILED`/`TIMEOUT`/`CANCELLED` never submit. Screen/ViewModel keep the existing `publishResultOnce` guard.

## Security & privacy

- Raw embedding vectors: never displayed, logged, in nav args, or saved state.
- Similarity/threshold: intentionally displayed in UI (approved); still never attach the embedding.
- Captured preview bitmap: transient in ViewModel state, cleared on reset/onCleared, never persisted/logged.

## Test strategy

- **Unit (JVM):** challenge sequencer (order, per-challenge timeout reset, advance on pass, restart), phase reducer transitions, ML-Kit signal → challenge-pass mapping (pure), match-result→phase mapping, rail node-state mapping, diagnostics formatting, honest verified copy.
- **Compose (androidTest):** each phase renders correct frame style/pills/guidance/sheet/actions; rail node states; diagnostics card shows values on verified & not-matched and is absent otherwise; Verify disabled until ReadyToVerify; verified copy ≠ attendance-success.
- **Integration/existing:** Attendance handoff still locked by `AttendanceScreenFaceResultRescueTest` (SUCCESS→Submitting; others no submit).
- **Device:** front-camera lifecycle, all 4 challenges, per-challenge timeout, no-face, multiple faces, low light, matched (card), not matched (card), cancel, permission recovery.

Required verification: `app:compileDebugKotlin`, `app:test`, `app:testDebugUnitTest`, `app:compileDebugAndroidTestKotlin`, `app:lint`, `app:lintDebug`, `app:assembleDebug`.

## Acceptance criteria

- Three mockup states plus derived states render matching the design; components reused where listed.
- 4 challenges detected in fixed order via ML Kit; per-challenge 20s timeout.
- `FaceVerificationFrame` renders liveness/success/no-face styles + numbered rail.
- Verify enabled only after all challenges; success continues to Attendance; face success ≠ attendance success.
- Diagnostics card shows similarity/threshold/result on verified & not-matched (all build variants); no raw embedding anywhere.
- Attendance handoff behavior unchanged; required Gradle verifications pass (device items marked Needs Verification if no device).

## Definition of Done

The redesigned screen implements the approved mockups end-to-end (4-challenge liveness, numbered rail, pills, guidance, diagnostics card, phase-aware bottom sheet) reusing Infinite components; `VerifyFaceUseCase` returns similarity/threshold; backend remains the only attendance authority; unit/Compose/build/lint pass; device evidence attached or explicitly marked Needs Verification.
