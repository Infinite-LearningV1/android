# Interactive Attendance Action Hub Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the static Layer 3 attendance alert/button presentation with a tested Interactive Attendance Action Hub that exposes one honest action, progressive evidence, typed recovery, and backend-confirmed outcomes.

**Architecture:** Keep `AttendanceActionState` as business truth, remove display copy from it, and introduce a mapper-owned `AttendanceActionHubUiModel`. The existing Attendance route executes semantic effects, while focused Compose components render compact, expanded, progress, success, and completed states. Backend success remains the only final Attendance success authority.

**Tech Stack:** Kotlin, Android, Jetpack Compose Material 3, StateFlow/SharedFlow, coroutines, Hilt, JUnit, kotlinx-coroutines-test, Compose UI tests, existing Infinite design tokens and primitives.

## Global Constraints

- Repository: `Infinite-LearningV1/android`.
- Integration branch: `develop`.
- Primary issue: INF-239.
- Design authority: `docs/superpowers/specs/2026-07-23-inf-239-interactive-attendance-action-hub-design.md`.
- Preserve `Screen → ViewModel → UseCase → Repository Interface → RepositoryImpl → API/Room/platform`.
- Backend remains the final Attendance success authority.
- Tap Ready Check-in/Checkout opens Face Recognition directly without confirmation dialog.
- Face verification success only transitions to Submitting.
- WFA requires an approved booking; recommendation alone never enables Attendance.
- WFH target is mandatory and admin-provisioned; unexpected absence is a contract/sync failure.
- Checkout does not require work-mode or target reselection.
- At most one primary action and one secondary text action are rendered.
- ViewModels do not depend on `NavController`, Activity, Compose, DTO, Entity, Retrofit, or Room.
- Reuse Infinite design tokens and existing button/status primitives.
- Use TDD and bounded commits.

---

## File Structure

### Domain/presentation contracts

- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionHubUiModel.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionHubMapper.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionEffect.kt`

### Compose

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionHub.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionEvidenceSummary.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionProgress.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionResultSummary.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionHubPreview.kt`

### Integration

- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionResolver.kt`

### Tests

- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionHubMapperTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionInteractionTest.kt`
- Create: `app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/actionhub/AttendanceActionHubTest.kt`
- Modify existing Attendance action/resolver tests as required.

---

### Task 1: Make AttendanceActionState copy-free and typed

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionState.kt`
- Test: existing Attendance action-state tests or create `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionStateTest.kt`

**Interfaces:**
- Produces: `AttendanceActionState`, `AttendanceRecoveryAction`, `AttendanceFailureReason`, `AttendanceActionResult`, `AttendanceDaySummary`.
- Consumers: mapper and `AttendanceViewModel` in later tasks.

- [ ] **Step 1: Write failing tests proving state contains typed values, not copy**

Assert that Ready only carries intent, Blocked carries typed reason/recovery, Submitting carries intent, and Completed carries summary.

- [ ] **Step 2: Run focused tests and verify failure**

Run:

```bash
./gradlew app:testDebugUnitTest --tests '*AttendanceActionStateTest*'
```

Expected: FAIL because the current model still requires `label`, `title`, or `message`.

- [ ] **Step 3: Implement the copy-free state model**

Remove display-copy fields and introduce typed recovery/failure/result/summary contracts exactly as defined by the design spec.

- [ ] **Step 4: Update compilation bridges only where necessary**

Temporary presentation extensions may remain in a clearly named mapper bridge, but business decisions must no longer depend on text.

- [ ] **Step 5: Run focused tests**

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionState.kt app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionStateTest.kt
git commit -m "refactor(inf-239): make attendance action state typed"
```

### Task 2: Build the Action Hub presentation mapper

**Files:**
- Create: `.../actionhub/AttendanceActionHubUiModel.kt`
- Create: `.../actionhub/AttendanceActionHubMapper.kt`
- Test: `.../actionhub/AttendanceActionHubMapperTest.kt`

**Interfaces:**
- Consumes: `AttendanceActionState` and preparation/session evidence.
- Produces: `AttendanceActionHubUiModel`.

- [ ] **Step 1: Write mapper matrix tests**

Cover Resolving, Ready check-in, Ready checkout, permission blocker, GPS blocker, WFA no request, WFA pending, WFA rejected, unexpected WFH absence, VerifyingFace, Submitting, retry submission, refresh status, success, and completed.

Required assertions:

```text
one primary action maximum
one secondary action maximum
no disabled Attendance CTA beside recovery CTA
WFA booking status shown for WFA
Submitting shows Face verified + backend pending
Completed has summary and no dead Attendance CTA
```

- [ ] **Step 2: Run focused mapper tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*AttendanceActionHubMapperTest*'
```

- [ ] **Step 3: Implement immutable UI models and mapper**

Keep copy, icons/semantic identifiers, and component choices in presentation.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub app/src/test/java/com/example/infinite_track/presentation/screen/attendance/actionhub
git commit -m "feat(inf-239): add attendance action hub mapper"
```

### Task 3: Harden direct Face Recognition handoff and duplicate-tap protection

**Files:**
- Modify: `AttendanceViewModel.kt`
- Create: `actionhub/AttendanceActionEffect.kt`
- Test: `AttendanceActionInteractionTest.kt`

**Interfaces:**
- Produces semantic effect `OpenFaceRecognition(intent)`.
- Route executes the effect.

- [ ] **Step 1: Write failing interaction tests**

Test:

```text
Ready tap → VerifyingFace + one OpenFaceRecognition effect
second tap while VerifyingFace → no duplicate effect
blocked tap → recovery effect only
checkout ready tap → CHECK_OUT effect without preparation reselection
```

- [ ] **Step 2: Run focused tests and verify failure**

```bash
./gradlew app:testDebugUnitTest --tests '*AttendanceActionInteractionTest*'
```

- [ ] **Step 3: Implement semantic effects and guards**

Do not store raw routes or `NavController` in ViewModel.

- [ ] **Step 4: Run focused tests**

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test/java/com/example/infinite_track/presentation/screen/attendance/AttendanceActionInteractionTest.kt
git commit -m "feat(inf-239): harden direct face verification handoff"
```

### Task 4: Implement focused Compose Action Hub components

**Files:**
- Create Compose files under `.../actionhub/`.
- Test: `AttendanceActionHubTest.kt`.

**Interfaces:**
- Consumes: `AttendanceActionHubUiModel` and callbacks.
- Produces: visual hub only; no business logic.

- [ ] **Step 1: Write failing Compose tests**

Test tags and assertions for:

```text
compact headline and evidence
one primary action
expand/collapse details
WFA pending booking status
Submitting two-step progress
Completed summary without disabled Attendance button
```

- [ ] **Step 2: Run instrumentation test and verify failure**

```bash
./gradlew app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.infinite_track.presentation.screen.attendance.actionhub.AttendanceActionHubTest
```

Expected: FAIL because components do not exist.

- [ ] **Step 3: Implement components with existing tokens/primitives**

Use focused composition components and avoid a parallel component system.

- [ ] **Step 4: Add previews for Ready, WFA blocked/pending, Submitting, Success, and Completed**

- [ ] **Step 5: Run instrumentation tests**

Expected: PASS on configured emulator/device.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance/actionhub app/src/androidTest/java/com/example/infinite_track/presentation/screen/attendance/actionhub
git commit -m "feat(inf-239): build interactive attendance action hub"
```

### Task 5: Replace the legacy bottom-sheet alert and button stack

**Files:**
- Modify: `AttendanceBottomSheetContent.kt`
- Modify: `AttendanceScreenState.kt`
- Modify relevant previews/tests.

**Interfaces:**
- Consumes: `AttendanceActionHubUiModel`.
- Removes visual dependence on `isCheckInEnabled`, `checkInButtonText`, and duplicate action buttons.

- [ ] **Step 1: Write or update tests showing the legacy stack is not rendered**

Assert that the hub owns the primary action and recovery presentation.

- [ ] **Step 2: Replace `AttendanceActionInlineAlert + AttendanceActionButtons` with `AttendanceActionHub`**

Keep Preparation information above the hub, but remove repeated action copy and competing buttons.

- [ ] **Step 3: Preserve WFA request/search entry only through typed hub actions**

Recommendation discovery remains part of WFA Request flow, not a second competing Attendance CTA.

- [ ] **Step 4: Run unit and Compose tests**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/components/button/attendance/AttendanceBottomSheetContent.kt app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt app/src/test app/src/androidTest
git commit -m "refactor(inf-239): replace legacy attendance action stack"
```

### Task 6: Wire Route-owned effects and success outcome presentation

**Files:**
- Modify: `AttendanceScreen.kt`
- Modify: `AttendanceViewModel.kt`
- Modify success/status presentation integration.

**Interfaces:**
- Route consumes `AttendanceActionEffect`.
- Backend-confirmed result maps to Success and Completed summary.

- [ ] **Step 1: Add tests for effect consumption and backend-result transitions**

Test Face success → Submitting before API; backend success → Success; backend failure → typed Failure; unknown outcome → refresh-status recovery.

- [ ] **Step 2: Wire Route actions**

Handle Face Recognition, permissions/settings, WFA request/status, contact admin, and attendance detail through semantic effects.

- [ ] **Step 3: Ensure Face failure/timeout/cancel never submits backend Attendance**

- [ ] **Step 4: Ensure success details come from backend-confirmed/session data**

- [ ] **Step 5: Run focused tests**

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test app/src/androidTest
git commit -m "feat(inf-239): wire action hub effects and outcomes"
```

### Task 7: Remove legacy copy-driven bridges and duplicate rules

**Files:**
- Modify: `AttendanceActionResolver.kt`
- Modify: `AttendanceScreenState.kt`
- Modify: `AttendanceViewModel.kt`
- Search all Attendance presentation files.

- [ ] **Step 1: Add regression tests proving action does not depend on button text**

- [ ] **Step 2: Remove or isolate legacy fields**

Remove business usage of:

```text
buttonText
isButtonEnabled
isCheckInMode
buttonText.contains("Check-in")
```

- [ ] **Step 3: Remove duplicate preparation blocker evaluation**

Layer 3 consumes typed Attendance Preparation eligibility.

- [ ] **Step 4: Run repository search evidence**

```bash
rg 'buttonText\.contains|contains\("Check-in"|legacyIsCheckInMode' app/src/main/java/com/example/infinite_track/presentation/screen/attendance
```

Expected: no business-logic matches.

- [ ] **Step 5: Run unit tests**

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test
git commit -m "refactor(inf-239): remove legacy attendance action bridges"
```

### Task 8: Verification and evidence

**Files:**
- Update documentation/evidence under `docs/superpowers/` if repository convention requires it.

- [ ] **Step 1: Run unit tests**

```bash
./gradlew app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 2: Run relevant instrumentation tests**

```bash
./gradlew app:connectedDebugAndroidTest
```

Expected: PASS on configured emulator/device.

- [ ] **Step 3: Run build and lint**

```bash
./gradlew app:assembleDebug app:lintDebug
```

Expected: PASS, or blockers documented with exact output.

- [ ] **Step 4: Capture runtime evidence**

Required states:

```text
Ready check-in
Ready checkout
Permission/GPS recovery
WFA no request
WFA pending
WFA rejected
WFA approved and ready
Unexpected WFH contract failure
Verifying Face duplicate-tap guard
Submitting two-step progress
Backend failure recovery
Success details
Completed summary
```

- [ ] **Step 5: Perform final architecture scan**

Verify no ViewModel holds navigation/platform/UI dependencies and no WFA recommendation is treated as approved target.

- [ ] **Step 6: Commit evidence updates**

```bash
git add docs app/src/test app/src/androidTest
git commit -m "test(inf-239): verify interactive attendance action hub"
```

---

## Plan self-review

- Design states are covered by mapper, interaction, Compose, integration, and runtime tasks.
- Direct Face Recognition handoff is explicit and tested.
- WFH admin invariant and WFA approved-booking rule are explicit.
- Legacy copy-driven behavior has a dedicated removal task.
- No implementation task changes geofencing or Face Recognition internals.
- Build, lint, test, and runtime evidence are required before completion.

## Execution

Use `superpowers:subagent-driven-development` from an isolated worktree based on `develop`. Execute all tasks continuously with a spec-compliance and code-quality review after each task, then a whole-branch review before merge.