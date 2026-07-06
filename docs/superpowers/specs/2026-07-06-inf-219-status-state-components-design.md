# INF-219 — Android Status State Components and SweetAlert Migration Design

Date: 2026-07-06
Branch: `fix/inf-219-status-state-components`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-inf-219-status-state-components`

## 1. Scope

INF-219 adds reusable Compose + Material3 status-state UI components and removes SweetAlert usage from the Android app.

In scope:

- Add package `app/src/main/java/com/example/infinite_track/presentation/components/status/`.
- Add the required files:
  - `StatusStateSpec.kt`
  - `StatusStateTokens.kt`
  - `InfiniteTrackInlineAlert.kt`
  - `InfiniteTrackStatusDialog.kt`
  - `InfiniteTrackConfirmDialog.kt`
  - `StatusStatePreview.kt`
- Use the required status model:

```kotlin
data class StatusStateSpec(
    val id: String,
    val value: String
)
```

- Support status ids `success`, `error`, `warning`, and `info` without introducing an enum.
- Reuse existing Infinite Track color tokens from `presentation/theme/Color.kt`.
- Reuse existing `LoadingAnimation()` from `presentation/components/loading/LoadingTrack.kt`; do not create a new loading/progress component.
- Migrate SweetAlert usage from the explicit target files:
  - `LoginScreen.kt`
  - `ProfileScreen.kt`
  - `AttendanceConfirmPopup.kt`
- Also migrate remaining `DialogHelper`/SweetAlert usage that blocks dependency removal, with minimal UI-only changes:
  - `InfiniteTrackApp.kt`
  - `AttendanceScreen.kt`
  - `WfaBookingScreen.kt`
- Remove SweetAlert Gradle dependency and version-catalog entries after search evidence shows no remaining usage.
- Delete `DialogHelper.kt` after all usages are gone.

Out of scope:

- Backend changes.
- Navigation route redesign.
- WFA Requests form redesign.
- My Attendance Report redesign.
- Auth/session business logic changes.
- Attendance check-in/check-out business logic changes.
- Permission/date-picker migration.
- New loading/progress component.
- New enum-based status model.

## 2. Current Mapping

`DialogHelper.kt` wraps `cn.pedant.SweetAlert.SweetAlertDialog` and is used by several Compose screens through imperative Android-context dialogs.

Mapped SweetAlert/DialogHelper surfaces:

- `LoginScreen.kt`: loading, success, and error dialogs for login state.
- `ProfileScreen.kt`: logout confirm and loading dialog.
- `InfiniteTrackApp.kt`: session-expired dialog.
- `AttendanceScreen.kt`: attendance success/error/location-error dialog from existing `DialogState`.
- `WfaBookingScreen.kt`: WFA booking success/error dialog after submit.
- `DialogHelper.kt`: direct SweetAlert import and helper functions.

`AttendanceConfirmPopup.kt` is already a Compose dialog. It will be aligned with the new status-state component model where practical while preserving its current status values and copy.

SweetAlert dependency entries:

- `app/build.gradle.kts`: `implementation(libs.library)`.
- `gradle/libs.versions.toml`: version alias `library = "1.6.2"` and library alias for `com.github.f0ris.sweetalert:library`.

Existing reusable tokens:

- `Green_Success`
- `Blue_Info`
- `Yellow_Warning`
- `Red_Error`
- `Blue_500`, `Blue_700`, `Violet_*`, `Purple_*`, `White`, `Text`

Existing loading component:

- `LoadingAnimation()` in `presentation/components/loading/LoadingTrack.kt`.

## 3. Design Goals

1. Replace SweetAlert with Compose-native components.
2. Keep status identity data-driven via `StatusStateSpec(id, value)`.
3. Keep reusable components small and focused:
   - inline alert
   - status dialog
   - confirmation dialog
4. Preserve existing screen behavior and callbacks.
5. Remove SweetAlert only after all imports/usages are gone.
6. Keep changes reviewable and minimal in high-risk screens.

## 4. Component Design

### 4.1 `StatusStateSpec.kt`

Defines the required data class only:

```kotlin
data class StatusStateSpec(
    val id: String,
    val value: String
)
```

The `id` is the stable status key used by tokens and behavior. The `value` is human-facing display text.

### 4.2 `StatusStateTokens.kt`

Provides Compose token resolution from `StatusStateSpec.id`.

Responsibilities:

- Normalize `id.lowercase()`.
- Map known ids:
  - `success`
  - `error`
  - `warning`
  - `info`
- Return colors, icon, and supporting background/border colors.
- Fallback unknown ids to `info` tokens.

This remains a plain function/data mapping and does not introduce an enum.

### 4.3 `InfiniteTrackInlineAlert.kt`

A reusable Material3 inline alert for status messages.

Inputs:

- `status: StatusStateSpec`
- `title: String?`
- `message: String`
- `modifier: Modifier`
- optional dismiss action

Behavior:

- Renders a compact card/row with status icon, title/message, and status colors.
- Supports success/error/warning/info via token mapping.
- Used by preview and future screen adoption.

### 4.4 `InfiniteTrackStatusDialog.kt`

A reusable Material3 dialog for informational status outcomes.

Inputs:

- `status: StatusStateSpec`
- `showDialog: Boolean`
- `title: String`
- `message: String`
- `confirmText: String = "OK"`
- optional image resource for legacy illustration cases such as login success
- `onConfirm: () -> Unit`
- `onDismiss: () -> Unit`

Behavior:

- If `showDialog` is false, renders nothing.
- If true, renders Compose `Dialog`/`Card` with status icon or supplied image, title, message, and one confirm button.
- Confirm invokes `onConfirm` and leaves dismissal control to the caller where needed.
- Dismiss invokes `onDismiss`.

### 4.5 `InfiniteTrackConfirmDialog.kt`

A reusable Material3 confirm/cancel dialog.

Inputs:

- `status: StatusStateSpec`
- `showDialog: Boolean`
- `title: String`
- `message: String`
- `confirmText: String`
- `cancelText: String`
- `isDestructive: Boolean = false`
- `onConfirm: () -> Unit`
- `onDismiss: () -> Unit`

Behavior:

- Supports logout, check-in, and checkout confirmation copy.
- Uses warning/error styling for destructive flows such as logout if requested.
- Does not encode flow-specific enum or business logic.

### 4.6 `StatusStatePreview.kt`

Compose previews for verification and screenshots:

- Inline success alert.
- Inline error alert.
- Inline warning alert.
- Inline info alert.
- Status success dialog.
- Status error dialog.
- Confirm logout dialog.
- Confirm check-in and checkout examples to prove the reusable confirm-dialog API supports those flows without wiring new business logic in this issue.

## 5. Migration Design

### 5.1 LoginScreen

Replace `SweetAlertDialog?` state and `DialogHelper` calls with Compose dialog state.

Planned state shape:

- `showLoadingDialog: Boolean`
- `statusDialogData` or equivalent local nullable data for success/error.

Behavior preservation:

- `UiState.Loading`: show dialog/card containing existing `LoadingAnimation()` and "Please wait".
- `UiState.Success`: show `InfiniteTrackStatusDialog` with success status, current title/message, and `img_login`; confirm navigates home and resets login state.
- `UiState.Error`: show `InfiniteTrackStatusDialog` with error status and current error message.
- Existing login validation/snackbar behavior remains unchanged.
- Auth/session ViewModel logic is not changed.

### 5.2 ProfileScreen

Replace logout SweetAlert warning/loading with Compose state.

Behavior preservation:

- Tapping logout shows `InfiniteTrackConfirmDialog`.
- Confirm dismisses confirm dialog, shows loading dialog with existing `LoadingAnimation()`, waits existing delay, calls `profileViewModel.onConfirmLogout()`, navigates to auth graph, and shows the existing Toast.
- Navigation command and ViewModel calls remain unchanged.

### 5.3 AttendanceConfirmPopup

Keep the current public API unless implementation requires a small addition.

Behavior preservation:

- `status` string values remain accepted: `confirmed`, `overtime`, `late`, fallback.
- Existing copy and imagery remain stable where possible.
- Align status identity through `StatusStateSpec` internally rather than introducing enum.

### 5.4 InfiniteTrackApp

Replace session-expired `DialogHelper.showDialogError` with `InfiniteTrackStatusDialog`.

Behavior preservation:

- Existing `sessionExpired` collection remains unchanged.
- Confirm still calls `sessionManager?.resetSessionExpired()` and navigates to login with the same navigation options.
- No auth/session manager business logic changes.

### 5.5 AttendanceScreen

Replace imperative `DialogHelper` calls inside `LaunchedEffect(uiState.activeDialog)` with Compose rendering of the already-existing dialog state.

Behavior preservation:

- `DialogState.Success` still navigates to Home on confirm and calls `viewModel.onDialogDismissed()`.
- `DialogState.Error` stays on AttendanceScreen and calls `viewModel.onDialogDismissed()`.
- `DialogState.LocationError` calls `viewModel.onDialogDismissed()`.
- No check-in/check-out state machine or API call behavior changes.

### 5.6 WfaBookingScreen

Replace WFA booking success/error SweetAlert with Compose status dialogs.

Behavior preservation:

- `uiState.isBookingSuccessful` still triggers success dialog.
- Confirm still navigates to `Screen.Home.route` using current options.
- `uiState.error` still triggers error dialog and confirm calls `viewModel.clearError()`.
- `WfaBookingDialog` form layout remains unchanged.

## 6. Dependency Cleanup

Cleanup sequence:

1. Remove all `DialogHelper` imports and usage from source files.
2. Search for:
   - `cn.pedant.SweetAlert`
   - `SweetAlertDialog`
   - `DialogHelper`
3. Delete `app/src/main/java/com/example/infinite_track/utils/DialogHelper.kt` only after search is clean except the file itself.
4. Remove SweetAlert dependency from `app/build.gradle.kts`.
5. Remove SweetAlert version/library aliases from `gradle/libs.versions.toml`.
6. Re-run search evidence.

## 7. Verification Plan

Required local checks:

- `./gradlew app:assembleDebug`

Required search evidence:

- No `cn.pedant.SweetAlert` in `app/src/main/java`.
- No `SweetAlertDialog` in `app/src/main/java`.
- No `DialogHelper` usage in target screens or remaining app source.
- No SweetAlert dependency in `app/build.gradle.kts` or `gradle/libs.versions.toml`.

Required visual/runtime evidence:

- Screenshot inline alert success/error/warning/info.
- Screenshot confirm logout dialog.
- Screenshot login success/error dialog after migration.
- Screenshot or preview evidence for confirm check-in and checkout component states. Runtime screenshots are required only if an existing migrated flow already displays those confirm dialogs; INF-219 must not add new check-in/checkout business logic just to create screenshots.

If emulator/device screenshot capture cannot be executed in the current environment, mark visual evidence as `Needs Verification` and provide Compose preview coverage as implementation evidence.

Known baseline issue:

- Before implementation, `./gradlew app:assembleDebug` failed with `java.io.IOException: Unable to establish loopback connection`.
- If this persists after implementation, report it as environment/baseline blocked rather than claiming build pass.

## 8. Risk Controls

- Keep changes in isolated worktree and branch.
- Keep screen callback behavior unchanged.
- Do not alter ViewModel business logic.
- Do not alter navigation graph definitions.
- Do not redesign WFA request form.
- Do not migrate permission/date picker components.
- Do not create enum or new loading/progress component.

## 9. Docs/ADR Note

No ADR is required because the work is a UI component migration and dependency removal. It does not change auth/session contract, attendance semantics, backend contract, navigation route behavior, or release workflow.

The spec and implementation plan are sufficient documentation for INF-219.
