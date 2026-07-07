# INF-228 — Android Edit Profile Detail Screen Refresh Spec

Date: 2026-07-07
Branch: `fix/android-profile-edit-detail-refresh`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-profile-edit-detail-refresh`
Base: `develop` at `dec6435`

## Goal

Redesign only the Android `Profile > Edit Profile` detail screen with reusable Material 3 friendly components, premium semi-liquid visual direction, inline feedback, and a sticky bottom action bar while preserving existing profile update business logic.

Android remains a trusted data-capture client. Backend remains authoritative for profile update behavior. INF-228 must not change backend contracts, auth/session behavior, Profile main screen scope, About screen scope, bottom navigation, or route graph semantics.

## Worktree and branch mapping

Main checkout:

- Path: `E:\skrisi\android`
- Branch: `develop`
- Head during mapping: `dec6435`
- Dirty local files in main checkout are unrelated network/environment changes and must not be edited for INF-228:
  - `app/src/main/java/com/example/infinite_track/di/NetworkModule.kt`
  - `app/src/main/res/xml/network_security_config.xml`
  - untracked `docs/superpowers/plans/2026-06-21-inf-164-inf-161-inf-162-layer3-truthful-consumption.md`

Existing Profile worktrees found:

- `android-account-hub-main-refresh` / `fix/android-account-hub-main-refresh`: stale INF-226 branch already merged.
- `android-profile-detail-back-navigation` / `fix/profile-detail-back-navigation`: stale navigation fix branch already merged.
- `android-about-infinite-track-screen` / `fix/android-about-infinite-track-screen`: relevant Profile/INF-227 workspace, but dirty with uncommitted About changes; unsafe to mix with INF-228.

Decision: create fresh isolated INF-228 worktree from `develop`.

## Current repo facts verified

### Edit Profile screen

File: `app/src/main/java/com/example/infinite_track/presentation/screen/profile/details/edit_profile/EditProfile.kt`

Current screen:

- Collects `userProfile`, `fullName`, `phone`, `nipNim`, `isEditing`, and `updateProfileState` from `EditProfileViewModel`.
- Uses `Toast` for success and error.
- Calls `onBackClick()` immediately after success.
- Shows centered `CircularProgressIndicator` overlay while saving.
- Uses `ProfileCard`, `ProfileTextFieldComponent`, `PhoneNumberTextFieldComponent`, `InfiniteTrackButton`, and `CancelButton`.
- Has internal edit/view toggle via `isEditing`, but INF-228 should treat Edit Profile as editing mode by default.

### EditProfileViewModel

File: `app/src/main/java/com/example/infinite_track/presentation/screen/profile/details/edit_profile/EditProfileViewModel.kt`

Current ViewModel:

- Loads current user from `GetLoggedInUserUseCase`.
- Editable field state:
  - `fullName`
  - `nipNim`
  - `phone`
- Read-only field source from `UserModel`:
  - `divisionName`
  - `positionName`
  - `email`
  - also `roleName`, `photoUrl` available for hero.
- Update request uses only:
  - `ProfileUpdateRequest(fullName = ..., nipNim = ..., phone = ...)`
- Does not update division, position, email, role, or photo.

### Current field components

Files:

- `presentation/components/profile_textfield/ProfileTextField.kt`
- `presentation/components/profile_textfield/ProfilePhoneNumber.kt`

Current limitations:

- Hardcoded radius/color/elevation.
- Duplicate phone/text field implementation.
- No explicit read-only badge/lock indicator.
- No support/error text.
- No focus/active state beyond default TextField behavior.

### Reusable component availability

INF-222 components are present:

- `InfiniteCard` / `InfiniteSurface`
- `InfiniteButton`
- `InfiniteIconButton`
- `InfiniteTextField`
- `InfiniteStatusPill`
- `InfiniteBottomActionBar`
- `InfiniteTopBar`

INF-219/status components are present:

- `InfiniteTrackInlineAlert`
- `InfiniteInlineAlert`
- `InfiniteTrackStatusDialog`
- `InfiniteTrackConfirmDialog`

## Visual reference mapping

User uploaded the Edit Profile detail reference image on 2026-07-07. The image shows a light semi-liquid glass profile edit screen with a large rounded top bar, profile hero, grouped form card, inline success/error banners, and a sticky bottom action bar.

| Reference visual element | Android implementation plan |
| --- | --- |
| Glass top app bar with large rounded surface | Screen-local `EditProfileTopBar` with back button and centered title |
| Back button in soft circular glass | Rounded Material surface with existing back icon behavior via `onBackClick` |
| Profile hero card | `EditProfileHeroCard` using soft surface, avatar/photo, name, position, role pill |
| Camera/edit photo button on avatar | Render visual camera button as disabled/no-op; no photo upload implementation |
| Editable form card | `EditProfileFormCard` with local `EditProfileField` wrappers |
| Editable fields with pencil icon | Full Name, NIP/NIM, and Phone use editable field rows with `ic_pencil` |
| Phone focused purple glow | Focus-aware field border/elevation using tokenized purple accent; phone keyboard uses `KeyboardType.Phone` |
| Read-only fields | Division, Position, Email render readable text plus lock/read-only badge |
| Inline success banner | Local feedback banner / `InfiniteInlineAlert` style with success semantic |
| Inline error banner | Local feedback banner / `InfiniteInlineAlert` style with error semantic |
| Sticky bottom action bar | Bottom glass surface with Save/Saving and Cancel buttons |
| Purple/cyan liquid accents | Use existing `InfiniteColors` AccountHub/theme tokens and no screen-local hardcoded color palette |

Out-of-scope from the reference:

- Profile photo upload/change logic.
- Backend profile contract changes.
- Profile main screen redesign.
- About/FAQ/MyDocument/PaySlip redesign.
- Auth/session changes.
- Bottom navigation changes.
- New global icon token object.

## Behavior requirements

- Screen starts in editing mode by default.
- Save disabled when no changes or required editable field is blank.
- Save enabled when editable fields are valid and changed.
- Save shows loading state while `updateProfileState is UiState.Loading`.
- Success shows inline success banner, not Toast.
- Error shows inline error banner, not Toast.
- Cancel resets unsaved changes through ViewModel and returns back.
- Existing update logic remains limited to `fullName`, `nipNim`, `phone`.

## Risks

- Changing success behavior from immediate back+Toast to inline success can alter user flow. This is requested by INF-228 but must be called out in PR notes.
- ViewModel currently toggles `isEditing`; setting edit mode by default may require small ViewModel adjustment or route-level mapper.
- Build/runtime verification is required because this is UI/form behavior.
- Runtime screenshots must redact sensitive email/NIP/phone if shared.

## Verification plan

Run from INF-228 worktree:

```bash
./gradlew app:assembleDebug
```

If feasible:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime smoke if emulator/device available:

1. Open Profile.
2. Open Edit Profile.
3. Verify glass top app bar, hero, form card, read-only indicators, and bottom action bar.
4. Verify Save disabled initially.
5. Focus Full Name/NIP/Phone fields.
6. Edit one field and verify Save enabled.
7. Tap Save and verify Saving state.
8. Verify inline success or error feedback.
9. Tap Cancel and verify reset/back behavior.

If runtime cannot be executed, mark screenshot/runtime evidence as `Needs Verification`.

## Docs / ADR note

No ADR expected if implementation remains a visual/detail-screen refresh with no backend/auth/session/navigation contract change.
