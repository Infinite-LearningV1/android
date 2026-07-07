# INF-226 — Account Hub Main Screen Refresh Spec

Date: 2026-07-07
Branch: `fix/android-account-hub-main-refresh`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-account-hub-main-refresh`
Base: `develop` at `5438044`

## Goal

Refresh the Android Profile / Account Hub **main screen only** so it presents account identity, account preferences, company access, help, and security actions in a clearer Material 3 friendly account hub.

Android remains a trusted data-capture client, not the source of truth for backend-owned state. This issue must not change backend contracts, auth/session behavior, logout business logic, or navigation route contracts.

## Worktree and branch mapping

- Main checkout: `E:\skrisi\android`, branch `develop`, head `5438044`.
- Main checkout had one unrelated untracked plan file and must not be edited for INF-226.
- Existing branch/worktree search for `profile|account|hub|inf-226|inf-165` found no safe/relevant candidate.
- INF-226 work happens only in `C:\Users\Febriyadi\.claude\worktrees\android-account-hub-main-refresh` on `fix/android-account-hub-main-refresh`.

## Current source mapping

### Profile main screen

- File: `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileScreen.kt`
- Current profile success content shows:
  - name
  - position
  - circular photo
  - account information rows: Edit Profile, Pay Slip, My Document, Employees shortcut
  - settings rows: Language, Contact Us, About Us, Log Out
- Current loading/error states still expose `EmployeesAccessShortcut`, which is pre-existing behavior and not part of this visual refresh unless layout requires safe containment.

### Profile state and user fields

- File: `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileViewModel.kt`
- Data source: `GetLoggedInUserUseCase()` into `profileState: UiState<UserModel>`.
- User model fields available from `app/src/main/java/com/example/infinite_track/domain/model/auth/UserModel.kt`:
  - `fullName`
  - `email`
  - `roleName`
  - `positionName`
  - `programName`
  - `divisionName`
  - `nipNim`
  - `phone`
  - `photoUrl`
- Active account status is not a current user-model field. The Account Hub may show a visual `Active` status pill as a client-side display label only, not as backend truth.

### Existing navigation actions

- File: `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- Existing callbacks must be preserved:
  - `navigateToEditProfile` -> `Screen.EditProfile.route`
  - `navigateToContactUs` -> `Screen.ContactUs.route`
  - `navigateToContacts` -> `Screen.Contact.route`
  - `navigateToMyDocument` -> `Screen.MyDocument.route`
  - `navigateToPaySlip` -> `Screen.PaySlip.route`
  - Logout entry -> existing `InfiniteTrackConfirmDialog`, `ProfileViewModel.onConfirmLogout()`, and root auth graph navigation after status dialog.

### Existing reusable UI foundation

INF-222 components are present and safe to use additively:

- `presentation/design/components/surface/InfiniteCard.kt`
- `presentation/design/components/surface/InfiniteSurface.kt`
- `presentation/design/components/status/InfiniteStatusPill.kt`
- `presentation/design/components/data/InfiniteSectionHeader.kt`
- `presentation/design/components/data/InfiniteInfoRow.kt`
- `presentation/design/tokens/InfiniteColors.kt`
- `presentation/design/tokens/InfiniteSpacing.kt`
- `presentation/design/tokens/InfiniteRadius.kt`

No new global icon token object should be added. Existing drawable icons and local Material Icons imports are acceptable if compile-safe.

## Target content grouping

Implement the Profile main screen sections as:

1. Identity
   - avatar/photo
   - full name
   - role badge
   - position
   - division
   - NIP/NIM or employee ID
   - active status pill
2. Account
   - Edit Profile
   - Language / account preference row (existing behavior)
3. Company Access
   - My Document
   - Pay Slip only when current role check safely identifies employee/admin/management and excludes internship
   - Employees / contacts access only if current existing behavior remains intentionally visible; if role contract is unclear, preserve current visibility rather than inventing backend role semantics
4. Help
   - Contact Us / Support
   - About if already available as an entry, but do not create a new About screen or route
   - FAQ remains out-of-scope because it is not currently wired in `ProfileScreen.kt`
5. Security
   - Logout visual entry only; existing logout confirmation and business logic preserved

## Role-based visibility policy

The current `ProfileScreen.kt` has no role-gated Pay Slip / My Document visibility. `UserModel.roleName` is available, and other screens distinguish user roles by strings such as home content role branching.

Safe INF-226 policy:

- Do not introduce or change backend role contracts.
- Use only existing `roleName` strings if the current app already exposes enough evidence.
- Pay Slip may be hidden for role names containing `intern` / `magang` / `internship`, because acceptance explicitly says Internship must not see employee-only payroll/service access when supported.
- If runtime role variants cannot be verified, mark role visibility as `Needs Verification`.
- Do not remove existing contact/company shortcuts unless the user approves broader behavior change.

## Visual reference mapping

User uploaded a Profile / Account Hub reference image on 2026-07-07. The image shows a light, soft-glass profile hub with a large identity hero, quick summary cards, grouped menu surfaces, and a red-tinted logout row.

| Reference visual element | Android implementation |
| --- | --- |
| Large `My Profile` title | Header text at the top of the Profile main screen |
| Large soft-glass identity hero card | Screen-local identity hero composable in `ProfileScreen.kt` using rounded Material 3 surfaces, soft border, subtle shadow, and light purple/cyan visual tone |
| Circular avatar with glow ring | Existing `AsyncImage` clipped as circle with soft purple/cyan ring treatment |
| Floating pencil edit button | Small rounded edit action in identity hero, wired to existing `navigateToEditProfile` callback |
| Name + position hierarchy | `fullName` as primary text and `positionName` as purple-accent subtitle |
| Role pill | Existing `roleName` rendered as a soft pill |
| Division and NIP rows | Existing `divisionName`/`programName` and `nipNim` rendered as compact identity rows |
| Active Employee pill | Visual active status pill only; not treated as backend truth |
| Three quick summary cards | Role, division/program, and contact/phone summary cards below hero |
| Account & Identity menu card | Account section with Edit Profile and Language rows |
| Company Access card | My Document and Pay Slip rows, with conservative internship payroll hiding if role text supports it |
| Help & Information card | Contact Support plus existing About entry; FAQ remains out-of-scope because current Profile route/callback is not wired |
| Security card | Red-tinted Logout row that opens the existing logout confirmation dialog |
| Liquid/glass background blobs | Subtle gradients and soft cards only; no heavy blur dependency, no dark mode, no neon |

Out-of-scope from the reference for INF-226:

- FAQ navigation/feature activation because the current Profile callback is commented/not wired.
- About detail screen creation; existing About row may remain no-op, but no new route/screen is added.
- Dashboard, WFA, attendance, report, bottom navigation, or detail screen redesign.
- Backend-driven account status beyond fields currently available in `UserModel`.

## Non-goals

- No redesign of `EditProfile.kt`, `PaySlipScreen.kt`, `MyDocumentScreen.kt`, `ContactUsScreen.kt`, FAQ detail, About detail, Login, Attendance, Home, or WFA screens.
- No backend/API contract change.
- No auth/session/logout business-logic change.
- No navigation graph or route contract change.
- No bottom navigation change.
- No global icon token object.
- No full INF-165 implementation.
- No new Pay Slip / My Document feature logic.

## Risks

- Logout is a session boundary surface. Only the visual row may change; confirm dialog and `ProfileViewModel.onConfirmLogout()` behavior must remain unchanged.
- Role names may vary by backend data. Any role-based visibility beyond existing model evidence must be treated as `Needs Verification`.
- UI reference image may contain out-of-scope screens/elements; those must be excluded unless separately approved.
- UI/navigation runtime verification requires emulator/device; build alone is not enough to mark Account Hub as Done.

## Verification plan

Run from the isolated worktree:

```bash
./gradlew app:assembleDebug
```

If time/environment permits:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime smoke, if emulator/device is available:

1. Open Profile tab.
2. Verify Account Hub main screen renders and sections are visible.
3. Tap Edit Profile, then back.
4. Tap My Document, then back.
5. Tap Pay Slip if visible, then back.
6. Tap Contact/Support, then back.
7. Tap Logout entry, verify existing confirmation appears, cancel logout.
8. Capture screenshot/screen recording as visual evidence.

If runtime cannot be executed, mark runtime and screenshot evidence as `Needs Verification`.

## Docs / ADR note

No ADR expected if implementation remains a main-screen visual refresh only. PR note must explicitly say:

- INF-226 only refreshes Profile / Account Hub main screen.
- No backend contract changed.
- No auth/session logic changed.
- No detail screens redesigned.
- No global icon token object created.
- Uploaded UI reference followed within INF-226 scope.
