# INF-239 Attendance Action State Machine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement an explicit AttendanceActionState state machine so Android attendance check-in/check-out flow is honest, predictable, and no longer infers business action from button text.

**Architecture:** Add a presentation-layer state model as the source of truth for attendance CTA and transitions. Keep legacy bridge fields temporarily for existing bottom-sheet wiring, but derive them from AttendanceActionState instead of using them for business decisions. Face verification success transitions to Submitting only; backend mutation success is the only source of attendance Success.

**Tech Stack:** Kotlin, Jetpack Compose, MVVM ViewModel, kotlinx.coroutines StateFlow, existing InfiniteTrack status/button components, JUnit unit tests.

## Global Constraints

- Work only in isolated worktree: `C:\Users\Febriyadi\.claude\worktrees\android-attendance-action-state-machine`.
- Do not edit main checkout `E:\skrisi\android`.
- Android is a trusted data-capture client, not final attendance source of truth.
- Do not change backend contract.
- Do not redesign Attendance screen visuals.
- Do not infer check-in/check-out from `buttonText`.
- Face verification success is not attendance success.
- Backend check-in/check-out success is the only trigger for attendance success.
- Reuse existing `StatefulButton`, `ButtonStateType`, `InfiniteTrackInlineAlert`, `InfiniteTrackStatusDialog`, `LoadingAnimation`, and `StatusStates`.

---

## File Structure

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionState.kt`
  - Defines `AttendanceActionIntent`, `AttendanceBlockReason`, `AttendanceActionState`, and presentation bridge helpers.
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
  - Adds `actionState` while retaining legacy bridge fields.
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
  - Replaces dynamic triple resolver with explicit action resolver and transitions.
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
  - Renders terminal dialogs from action state and keeps navigation side effect intact.
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
  - Accepts action state and shows inline alert/loading guidance.
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceActionButtons.kt`
  - Uses action-state-derived labels/enabled/loading state with `StatefulButton`.
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionStateTest.kt`
  - Locks state model bridge semantics.
- Create/Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModelActionStateTest.kt`
  - Locks no-button-text intent detection and face/backend transition semantics where feasible.

## Task 1: Add explicit action model and bridge tests

**Files:**
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionStateTest.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`

**Interfaces:**
- Produces: `enum class AttendanceActionIntent { CHECK_IN, CHECK_OUT }`
- Produces: `enum class AttendanceBlockReason { PERMISSION_REQUIRED, TARGET_LOCATION_UNAVAILABLE, WFH_LOCATION_MISSING, WFA_BOOKING_REQUIRED, ALREADY_COMPLETED, SERVER_RESTRICTION, UNKNOWN }`
- Produces: `sealed interface AttendanceActionState`
- Produces bridge helpers: `ctaLabel`, `isCtaEnabled`, `legacyIsCheckInMode`

- [ ] **Step 1: Write failing tests for action-state bridge behavior**

Create `AttendanceActionStateTest.kt` with tests for:

```kotlin
@Test
fun readyCheckIn_exposesEnabledCheckInBridge() {
    val state = AttendanceActionState.Ready(
        intent = AttendanceActionIntent.CHECK_IN,
        label = "Check-in di sini"
    )

    assertEquals("Check-in di sini", state.ctaLabel)
    assertTrue(state.isCtaEnabled)
    assertTrue(state.legacyIsCheckInMode)
}

@Test
fun readyCheckOut_exposesEnabledCheckOutBridge() {
    val state = AttendanceActionState.Ready(
        intent = AttendanceActionIntent.CHECK_OUT,
        label = "Check-out di sini"
    )

    assertEquals("Check-out di sini", state.ctaLabel)
    assertTrue(state.isCtaEnabled)
    assertFalse(state.legacyIsCheckInMode)
}

@Test
fun blocked_exposesDisabledBridgeAndReasonCopy() {
    val state = AttendanceActionState.Blocked(
        reason = AttendanceBlockReason.WFH_LOCATION_MISSING,
        title = "Lokasi WFH belum tersedia",
        message = "Lengkapi lokasi WFH sebelum absen dari rumah."
    )

    assertEquals("Lokasi WFH belum tersedia", state.ctaLabel)
    assertFalse(state.isCtaEnabled)
    assertTrue(state.legacyIsCheckInMode)
}

@Test
fun completed_exposesHonestDisabledCompletedCopy() {
    val state = AttendanceActionState.Completed

    assertEquals("Anda sudah absen hari ini", state.ctaLabel)
    assertFalse(state.isCtaEnabled)
    assertFalse(state.legacyIsCheckInMode)
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
./gradlew app:testDebugUnitTest --tests "com.example.infinite_track.presentation.screen.attendance.AttendanceActionStateTest"
```

Expected: FAIL because `AttendanceActionState` does not exist.

- [ ] **Step 3: Implement minimal action model and bridge helpers**

Create `AttendanceActionState.kt` with required enums/sealed interface and helper properties.

- [ ] **Step 4: Add `actionState` to `AttendanceScreenState`**

Add:

```kotlin
val actionState: AttendanceActionState = AttendanceActionState.Loading
```

Do not remove bridge fields yet.

- [ ] **Step 5: Run action-state tests to verify GREEN**

Run same targeted test command. Expected: PASS.

## Task 2: Resolve action state from TodayStatus and work-mode eligibility

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Create/Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModelActionStateTest.kt`

**Interfaces:**
- Produces private resolver: `resolveAttendanceActionState(todayStatus: TodayStatus): AttendanceActionState`
- Produces private update helper: `applyActionState(actionState: AttendanceActionState)`

- [ ] **Step 1: Write failing tests for resolver semantics if current test construction supports ViewModel dependencies**

Cover at minimum:
- `checkedInAt == null` -> `Ready(CHECK_IN)`
- `checkedInAt != null && canCheckOut` -> `Ready(CHECK_OUT)`
- otherwise -> `Completed`
- WFH mode without `wfhLocation` -> `Blocked(WFH_LOCATION_MISSING)`
- WFA mode without selected/approved booking path -> `Blocked(WFA_BOOKING_REQUIRED)` or `TARGET_LOCATION_UNAVAILABLE` depending current data availability

If direct ViewModel construction is too coupled, test public bridge behavior through a small internal resolver object extracted into `AttendanceActionResolver.kt` in the same package.

- [ ] **Step 2: Run resolver tests to verify RED**

Run targeted tests. Expected: FAIL because resolver/action state is not wired.

- [ ] **Step 3: Replace `calculateDynamicButtonState()` with explicit resolver**

In `fetchTodayStatus()`, compute:

```kotlin
val actionState = resolveAttendanceActionState(todayStatus)
```

Then write state with:

```kotlin
actionState = actionState,
buttonText = actionState.ctaLabel,
isButtonEnabled = actionState.isCtaEnabled,
isCheckInMode = actionState.legacyIsCheckInMode
```

- [ ] **Step 4: Ensure failure loading branches set honest action state**

For today-status load failures, set:

```kotlin
actionState = AttendanceActionState.RetryableFailure(
    intent = null,
    title = "Status absensi gagal dimuat",
    message = errorMessage
)
```

- [ ] **Step 5: Run targeted resolver tests to verify GREEN**

Expected: PASS.

## Task 3: Remove buttonText business-action inference and add VerifyingFace transition

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify/Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModelActionStateTest.kt`

**Interfaces:**
- Consumes: `AttendanceActionState.Ready(intent, label)`
- Produces: `AttendanceActionState.VerifyingFace(intent)` before navigation
- Produces: `NavigationTarget.FaceScanner(intent: AttendanceActionIntent)` or continues boolean bridge from explicit intent only

- [ ] **Step 1: Write failing test that button label cannot determine action**

Test shape:

```kotlin
@Test
fun attendanceButtonClick_usesReadyIntentNotButtonText() {
    // Given Ready(CHECK_OUT) but buttonText contains Check-in-like stale copy
    // When onAttendanceButtonClicked()
    // Then navigation target is checkout and actionState is VerifyingFace(CHECK_OUT)
}
```

- [ ] **Step 2: Run test to verify RED**

Expected: FAIL because current code uses `buttonText.contains("Check-in")`.

- [ ] **Step 3: Update `onAttendanceButtonClicked()`**

Use only:

```kotlin
val readyState = _uiState.value.actionState as? AttendanceActionState.Ready ?: return
val intent = readyState.intent
```

Set:

```kotlin
actionState = AttendanceActionState.VerifyingFace(intent)
navigationTarget = NavigationTarget.FaceScanner(intent)
```

If keeping boolean navigation bridge, derive `isCheckIn = intent == AttendanceActionIntent.CHECK_IN`.

- [ ] **Step 4: Run grep evidence**

Run:

```bash
rg 'buttonText\.contains\("Check-in"|contains\("Check-in"' app/src/main/java app/src/test/java
```

Expected: no production business logic match.

- [ ] **Step 5: Run targeted tests to verify GREEN**

Expected: PASS.

## Task 4: Add Submitting, Success, and RetryableFailure backend transitions

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify/Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModelActionStateTest.kt`

**Interfaces:**
- Consumes: `FaceVerificationResult`
- Produces: `Submitting(intent, message)` after face success and before backend use case
- Produces: `Success(intent, message)` only after backend success
- Produces: `RetryableFailure(intent, title, message)` for backend failure or face failure/timeout

- [ ] **Step 1: Write failing tests for face/backend transition semantics**

Cover:
- `FaceVerificationResult.FAILED` does not call backend and becomes `RetryableFailure` or returns to Ready with visible error.
- `FaceVerificationResult.TIMEOUT` does not call backend and becomes `RetryableFailure` or returns to Ready with visible error.
- `FaceVerificationResult.CANCELLED` does not call backend and returns to previous Ready action.
- `FaceVerificationResult.SUCCESS` first sets `Submitting(intent)` before backend result.
- backend success sets `Success(intent)`.
- backend failure sets `RetryableFailure(intent)`.

- [ ] **Step 2: Run tests to verify RED**

Expected: FAIL because current code jumps from face success directly to use case/dialog.

- [ ] **Step 3: Update `onFaceVerificationResult()`**

Use current action state to recover intent from `VerifyingFace(intent)`.

Rules:
- SUCCESS -> `Submitting(intent)` -> `proceedWithCheckIn(intent)` or `proceedWithCheckOut(intent)`.
- FAILED/TIMEOUT -> `RetryableFailure(intent, title, message)` and no backend call.
- CANCELLED -> return to `Ready(intent, label)` and no backend call.

- [ ] **Step 4: Update backend methods**

In check-in/check-out success callbacks:

```kotlin
actionState = AttendanceActionState.Success(intent, message)
activeDialog = DialogState.Success(message)
```

In failure callbacks:

```kotlin
actionState = AttendanceActionState.RetryableFailure(intent, "Absensi gagal", errorMessage)
activeDialog = DialogState.Error(errorMessage)
```

- [ ] **Step 5: Run targeted transition tests to verify GREEN**

Expected: PASS.

## Task 5: Render blocked/submitting states with existing components

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceActionButtons.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`

**Interfaces:**
- Consumes: `AttendanceActionState`
- Produces: inline alert for `Blocked`, `VerifyingFace`, `Submitting`, and optionally recoverable `RetryableFailure`
- Produces: status dialog for terminal `Success` and backend `RetryableFailure`

- [ ] **Step 1: Add actionState parameter to bottom sheet**

Pass `uiState.actionState` from `AttendanceScreen` to `AttendanceBottomSheetContent`.

- [ ] **Step 2: Render InlineAlert for Blocked**

Use:

```kotlin
InfiniteTrackInlineAlert(
    status = StatusStates.Warning,
    title = state.title,
    message = state.message
)
```

Use `StatusStates.Error` for hard server/unknown blockers.

- [ ] **Step 3: Render submitting/verifying guidance**

Use `InfiniteTrackInlineAlert(StatusStates.Info, ...)` plus disabled CTA. Keep `LoadingAnimation` overlay for global loading/submitting if appropriate.

- [ ] **Step 4: Keep StatefulButton for CTA**

Ready states use enabled `StatefulButton` label:
- `Check-in di sini`
- `Check-out di sini`

Completed uses disabled button:
- `Anda sudah absen hari ini`

- [ ] **Step 5: Run Compose compile via assembleDebug later**

Compile will validate UI signatures.

## Task 6: Final verification and evidence

**Files:**
- All modified files above

- [ ] **Step 1: Run code evidence grep**

```bash
rg 'buttonText\.contains\("Check-in"|contains\("Check-in"' app/src/main/java app/src/test/java
```

Expected: no production business-action inference remains.

- [ ] **Step 2: Run unit tests**

```bash
./gradlew app:testDebugUnitTest
```

Expected: PASS, or report failures exactly.

- [ ] **Step 3: Run required build**

```bash
./gradlew app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Run lint if time/environment permits**

```bash
./gradlew app:lint
```

Expected: PASS, or report failures exactly.

- [ ] **Step 5: Runtime evidence**

Attempt emulator/device smoke for:
- Ready Check-in state
- Ready Checkout state
- Blocked inline alert
- VerifyingFace transition/scanner navigation
- Submitting state after face success
- Check-in success dialog
- Check-out success dialog
- Backend failure/retryable failure

If emulator/backend cannot run, report each as `Needs Verification`.

## Self-Review

- Spec coverage: This plan covers explicit models, state field, resolver replacement, removal of button text inference, face/submitting/backend transitions, blocked inline alert rendering, and verification.
- Placeholder scan: No implementation task uses TBD/TODO as a deliverable.
- Type consistency: Model names match requested INF-239 names and current package paths.
