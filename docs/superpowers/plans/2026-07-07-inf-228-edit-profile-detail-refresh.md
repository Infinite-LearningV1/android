# INF-228 — Android Edit Profile Detail Screen Refresh Plan

Date: 2026-07-07
Branch: `fix/android-profile-edit-detail-refresh`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-profile-edit-detail-refresh`
Spec: `docs/superpowers/specs/2026-07-07-inf-228-edit-profile-detail-refresh.md`

## Guardrails

- Work only in the INF-228 isolated worktree and branch.
- Do not edit the main `develop` checkout.
- Do not touch local main-checkout network changes.
- Scope is only `Profile > Edit Profile` detail screen.
- Do not redesign Profile main screen.
- Do not implement About/FAQ/MyDocument/PaySlip redesigns.
- Do not change backend profile update contract.
- Do not add profile photo upload/change logic.
- Do not change auth/session behavior.
- Do not change bottom navigation.
- Do not introduce a global `InfiniteIcons` object.
- Do not delete old profile field components unless usage search proves safe; prefer replacing screen usage first.
- Use existing reusable components where practical; create screen-local wrappers only when reusable components are too limited.

## Task 1 — Visual reference mapping gate

Completed from uploaded Edit Profile reference image on 2026-07-07.

Reference mapping:

- Glass top app bar -> local rounded glass top bar with back button and centered title.
- Profile hero -> avatar/photo from `UserModel.photoUrl` mapped from `/api/auth/me` `photo`, disabled/no-op camera badge, full name, position, role pill.
- Editable form card -> Full Name, NIP/NIM, Phone rows with pencil affordance.
- Phone active state -> focus-aware purple accent border/shadow using tokens.
- Read-only rows -> Division, Position, Email rows with lock/read-only badge.
- Inline feedback -> success and error banners with dismiss action.
- Sticky bottom action bar -> Save/Saving + Cancel buttons in glass surface.

Out-of-scope:

- Photo upload implementation.
- Backend update contract changes.
- Other Profile detail screen redesigns.

## Task 2 — Re-read source before editing

Read immediately before implementation:

- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/details/edit_profile/EditProfile.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/details/edit_profile/EditProfileViewModel.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/auth/UserModel.kt`
- `app/src/main/java/com/example/infinite_track/data/soucre/network/request/ProfileUpdateRequest.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/profile_textfield/ProfileTextField.kt`
- `app/src/main/java/com/example/infinite_track/presentation/components/profile_textfield/ProfilePhoneNumber.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/button/InfiniteButton.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/input/InfiniteTextField.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteBottomActionBar.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/navigation/InfiniteTopBar.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteFeedback.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteColors.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/string.xml`

## Task 3 — ViewModel behavior preservation / small state additions

Preserve backend update request exactly:

```kotlin
ProfileUpdateRequest(
    fullName = ...,
    nipNim = ...,
    phone = ...
)
```

Allowed ViewModel changes if needed:

1. Start screen in editing mode by default, or remove dependency on `isEditing` from redesigned UI.
2. Add public helpers/state for:
   - `hasChanges`
   - `canSave`
   - `isSaving`
3. Add/reset feedback state if screen needs dismissible inline alerts.
4. Keep `onCancelClick()` resetting local fields. Route can call `onBackClick()` after reset.
5. Do not add division/position/email/photo update fields.

## Task 4 — Replace EditProfile UI structure

In `EditProfile.kt`:

1. Remove Toast-based success/error flow from redesigned UI.
2. Remove centered loading overlay for save request.
3. Add top-level layout:
   - `Scaffold` with transparent/light background.
   - Glass top app bar with back button and title.
   - Scrollable content for hero/form/feedback.
   - Bottom action bar for Save/Cancel.
4. Add screen-local wrappers if needed:
   - `EditProfileTopBar`
   - `EditProfileHeroCard`
   - `EditProfileFormCard`
   - `EditProfileField`
   - `EditProfileFeedbackBanner`
   - `EditProfileBottomActionBar`
5. Prefer reusable design components:
   - `InfiniteButton`
   - `InfiniteBottomActionBar`
   - `InfiniteStatusPill`
   - `InfiniteInlineAlert`
   - `InfiniteCard` / `InfiniteSurface`
6. Render camera/photo edit visual button disabled/no-op.
7. Handle missing data gracefully with localized fallback copy.

## Task 5 — Form fields

Editable fields:

- Full Name
- NIP / NIM
- Phone Number (`KeyboardType.Phone`)

Read-only fields:

- Division
- Position
- Email

Implementation requirements:

1. Editable fields show edit/pencil affordance.
2. Read-only fields show lock icon and/or `Read only` badge.
3. Read-only fields remain readable, not visually broken.
4. Required editable fields (`fullName`, `nipNim`) must not save blank values.
5. Phone field can be optional if current backend/model treats it optional, but should use phone keyboard.

## Task 6 — Feedback and actions

1. Success feedback:
   - inline alert/banner
   - copy: `Profile updated successfully.`
2. Error feedback:
   - inline alert/banner
   - default copy: `Unable to update profile. Please try again.`
   - can include API error message when available and safe.
3. Save button:
   - disabled when no changes or invalid input
   - enabled when changed and valid
   - loading state with `Saving...` while saving
4. Cancel button:
   - secondary/outline action
   - resets unsaved changes through ViewModel
   - returns back as MVP behavior

## Task 7 — Strings / localization

Add only needed Edit Profile strings to:

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/string.xml`

Likely strings:

- `edit_profile_title`
- `edit_profile_save_changes`
- `edit_profile_saving`
- `edit_profile_cancel`
- `edit_profile_read_only`
- `edit_profile_success_title/message`
- `edit_profile_error_title/message`
- field labels and helper text if not already present

Do not paste real user email/NIP/phone into docs or logs.

## Task 8 — Verification

Run from INF-228 worktree:

```bash
./gradlew app:assembleDebug
```

If feasible:

```bash
./gradlew app:test
./gradlew app:lint
```

Static checks:

- grep no `Toast` usage remains in redesigned `EditProfile.kt` success/error flow.
- grep `ProfileUpdateRequest` still only includes `fullName`, `nipNim`, `phone` in ViewModel.
- grep old `ProfileTextFieldComponent` / `PhoneNumberTextFieldComponent` usage removed from `EditProfile.kt` if replaced.

### Current local verification status

Build attempt:

```bash
./gradlew --no-daemon -Djava.net.preferIPv4Stack=true app:assembleDebug
```

Result: blocked before Kotlin compile by local Gradle environment error:

```text
java.io.IOException: Unable to establish loopback connection
```

Static checks completed:

- `git diff --check` returned no whitespace errors, only Windows line-ending warning for `EditProfile.kt`.
- `EditProfile.kt` grep found no `Toast`, `CircularProgressIndicator`, old `ProfileTextFieldComponent`, old `PhoneNumberTextFieldComponent`, `InfiniteTrackButton`, `CancelButton`, `onToggleEditMode`, or `isEditing` usage.
- `EditProfile.kt` grep found no hardcoded visible field labels/copy or hardcoded `Color(0x...)` palette.
- `EditProfileViewModel.kt` grep confirmed `ProfileUpdateRequest` still only uses `fullName`, `nipNim`, and `phone`.
- Resource parity check found 19 `edit_profile_*` strings in both `values/strings.xml` and `values-in/string.xml`, with no missing counterpart.

Runtime smoke if available:

1. Open Profile.
2. Open Edit Profile.
3. Screenshot default state.
4. Focus Full Name/NIP/Phone.
5. Verify read-only Division/Position/Email indicators.
6. Verify Save disabled when unchanged.
7. Change editable field and verify Save enabled.
8. Tap Save and verify Saving state.
9. Verify success/error inline alert.
10. Tap Cancel and verify reset/back behavior.

If runtime cannot be executed, mark screenshots/runtime as `Needs Verification`.

## Expected affected files

Likely:

- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/details/edit_profile/EditProfile.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/details/edit_profile/EditProfileViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/string.xml`
- this spec/plan docs

Possibly:

- `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteColors.kt` only if required tokens are missing.
- `presentation/components/profile_textfield/*` only if refactoring generic reusable wrapper is safe and usage search supports it.

## PR / review note draft

- Work done in isolated branch/worktree: `fix/android-profile-edit-detail-refresh` / `C:\Users\Febriyadi\.claude\worktrees\android-profile-edit-detail-refresh`.
- Redesigns Edit Profile detail screen only.
- Editable fields remain Full Name, NIP/NIM, and Phone.
- Read-only fields remain Division, Position, and Email with read-only indicators.
- Replaces Toast success/error with inline feedback.
- Moves save loading into Save button state.
- Cancel resets unsaved changes and returns back.
- No backend contract changes.
- No auth/session changes.
- No Profile main screen redesign.
- No About/MyDocument/PaySlip/FAQ redesign.
- No profile photo upload implemented.
- Build evidence and Needs Verification items included.
