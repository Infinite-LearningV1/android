# INF-219 — Status State Components and SweetAlert Migration Plan

Date: 2026-07-06
Branch: `fix/inf-219-status-state-components`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-inf-219-status-state-components`
Spec: `docs/superpowers/specs/2026-07-06-inf-219-status-state-components-design.md`

## Guardrails

- Work only in the INF-219 worktree and branch.
- Do not edit the main `develop` checkout.
- Do not change backend code.
- Do not change navigation graph definitions or route contracts.
- Do not redesign WFA Requests.
- Do not redesign My Attendance Report.
- Do not change auth/session business logic.
- Do not change attendance check-in/check-out business logic.
- Do not migrate permission/date picker components.
- Do not introduce an enum.
- Do not create a new loading/progress component.

## Baseline

- Spec is already committed in `9deeca1 docs: add INF-219 status components design`.
- Baseline `./gradlew app:assembleDebug` failed before implementation with `java.io.IOException: Unable to establish loopback connection`.
- Treat that as a baseline environment issue unless a later run proves otherwise.

## Task 1 — Re-map current source before editing

1. Confirm branch/worktree:
   - `git -C /c/Users/Febriyadi/.claude/worktrees/android-inf-219-status-state-components status --short --branch`
2. Re-run search evidence:
   - SweetAlert/DialogHelper source usage.
   - SweetAlert Gradle dependency usage.
3. Re-read the affected source files immediately before editing:
   - `LoginScreen.kt`
   - `ProfileScreen.kt`
   - `AttendanceConfirmPopup.kt`
   - `InfiniteTrackApp.kt`
   - `AttendanceScreen.kt`
   - `WfaBookingScreen.kt`
   - `DialogHelper.kt`
   - `Color.kt`
   - `LoadingTrack.kt`

## Task 2 — Add reusable status component package

Create package:

`app/src/main/java/com/example/infinite_track/presentation/components/status/`

Add files:

1. `StatusStateSpec.kt`
   - Define only the required `data class StatusStateSpec(val id: String, val value: String)`.

2. `StatusStateTokens.kt`
   - Add token data class internal to the package.
   - Resolve status ids `success`, `error`, `warning`, `info` by string.
   - Use existing theme tokens from `Color.kt`.
   - Fallback unknown ids to info tokens.
   - Do not add enum.

3. `InfiniteTrackInlineAlert.kt`
   - Material3 inline status alert.
   - Support title/message/status and optional dismiss action.
   - Use `StatusStateTokens`.

4. `InfiniteTrackStatusDialog.kt`
   - Material3 status outcome dialog.
   - Support optional image resource for legacy login success artwork.
   - Support title, message, confirm text, confirm callback, dismiss callback.
   - No SweetAlert/View API usage.

5. `InfiniteTrackConfirmDialog.kt`
   - Material3 confirm/cancel dialog.
   - Support logout/check-in/checkout copy through caller-provided text.
   - Use `StatusStateSpec`; no enum.

6. `StatusStatePreview.kt`
   - Add previews for inline success/error/warning/info.
   - Add previews for status dialog, logout confirm, check-in confirm, checkout confirm.

## Task 3 — Migrate LoginScreen

1. Remove `SweetAlertDialog`, `DialogHelper`, `LocalContext` if unused after migration.
2. Replace imperative dialog variable with Compose state for:
   - loading dialog visibility
   - status dialog data
3. Map `UiState.Loading` to a Compose dialog/card that reuses `LoadingAnimation()`.
4. Map `UiState.Success` to `InfiniteTrackStatusDialog` success state with existing login success title/message/image.
5. Map `UiState.Error` to `InfiniteTrackStatusDialog` error state with existing error message.
6. Preserve login validation, ViewModel calls, `navigateToHome()`, and `loginViewModel.resetState()` behavior.

## Task 4 — Migrate ProfileScreen logout dialogs

1. Remove SweetAlert/DialogHelper imports and dialog state.
2. Add state for logout confirm and logout loading.
3. Render `InfiniteTrackConfirmDialog` for logout confirmation.
4. On confirm, preserve existing sequence:
   - hide confirm
   - show loading
   - delay 2000
   - hide loading
   - `profileViewModel.onConfirmLogout()`
   - navigate to `auth_graph` using current options
   - show Toast
5. Use existing `LoadingAnimation()` for loading UI.
6. Keep LanguagePopUp/profile UI unchanged.

## Task 5 — Align AttendanceConfirmPopup

1. Keep public API stable unless compile requires a tiny change.
2. Internally use `StatusStateSpec` to represent the existing string statuses.
3. Preserve existing images, text, colors, OK button behavior, and dimensions as much as possible.
4. Do not change attendance semantics.

## Task 6 — Migrate remaining DialogHelper usages for dependency removal

### InfiniteTrackApp

1. Replace `DialogHelper.showDialogError` session-expired call with Compose state + `InfiniteTrackStatusDialog`.
2. Preserve `sessionManager?.resetSessionExpired()` and navigation to `Screen.Login.route`.
3. Do not change session manager behavior.

### AttendanceScreen

1. Remove imperative `DialogHelper` calls from `LaunchedEffect(uiState.activeDialog)`.
2. Render `InfiniteTrackStatusDialog` based on `uiState.activeDialog`.
3. Preserve all confirm callbacks and navigation behavior.
4. Do not change check-in/check-out ViewModel logic.

### WfaBookingScreen

1. Replace booking success/error `DialogHelper` calls with Compose dialog state/rendering.
2. Preserve success navigation to `Screen.Home.route`.
3. Preserve error acknowledgment `viewModel.clearError()`.
4. Do not change `WfaBookingDialog` layout.

## Task 7 — Remove SweetAlert helper and Gradle dependency

1. Search for source usages:
   - `cn.pedant.SweetAlert`
   - `SweetAlertDialog`
   - `DialogHelper`
2. If only `DialogHelper.kt` remains, delete `DialogHelper.kt`.
3. Remove `implementation(libs.library)` from `app/build.gradle.kts`.
4. Remove SweetAlert version alias and library alias from `gradle/libs.versions.toml`.
5. Re-run search evidence and save output in final response.

## Task 8 — Verification

1. Run:
   - `./gradlew app:assembleDebug`
2. If loopback failure persists, report exact output as `Needs Verification` baseline/environment blocked.
3. Run source/dependency searches:
   - no `cn.pedant.SweetAlert`
   - no `SweetAlertDialog`
   - no `DialogHelper`
   - no SweetAlert Gradle dependency
4. Attempt visual evidence if emulator/preview screenshot path is available.
5. If screenshots cannot be captured in this environment, report required screenshots as `Needs Verification` and point to preview coverage.

## Task 9 — Review and final report

1. Inspect diff.
2. Run a code review pass focused on:
   - no scope creep
   - no business logic changes
   - no enum/new loading component
   - dependency cleanup correctness
3. Final response must include:
   - Fact
   - Assumption
   - Mismatch
   - Risk
   - Needs Verification
   - Recommendation
   - Files/areas affected
   - Verification evidence
   - Docs/ADR update note
   - PR/review note
