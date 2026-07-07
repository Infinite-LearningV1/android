# INF-226 — Account Hub Main Screen Refresh Plan

Date: 2026-07-07
Branch: `fix/android-account-hub-main-refresh`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-account-hub-main-refresh`
Spec: `docs/superpowers/specs/2026-07-07-inf-226-account-hub-main-screen-refresh.md`

## Guardrails

- Work only in the INF-226 isolated worktree and branch.
- Do not edit the main `develop` checkout.
- Keep scope limited to Profile / Account Hub main screen.
- Do not redesign detail screens.
- Do not change backend/API contracts.
- Do not change auth/session/logout business logic.
- Do not change navigation graph or route contracts.
- Do not change bottom navigation.
- Do not add a global icon token object.
- Do not implement full INF-165.
- Use existing state and callbacks from `ProfileScreen.kt` / `ProfileViewModel.kt`.
- Follow the uploaded UI reference once available; exclude out-of-scope elements.

## Baseline evidence

- Main checkout: `E:\skrisi\android`, `develop`, `5438044`, dirty with unrelated untracked plan file.
- INF-226 worktree: `C:\Users\Febriyadi\.claude\worktrees\android-account-hub-main-refresh`, branch `fix/android-account-hub-main-refresh`, `5438044`.
- No existing relevant branch/worktree found for `profile|account|hub|inf-226|inf-165`.
- `.claude/rules/*.md` files requested by user are absent in current repo; root `CLAUDE.md` is the available repo contract.

## Task 1 — Finish visual-reference mapping before implementation

Completed from the uploaded reference image on 2026-07-07:

| Reference visual element | Android implementation |
| --- | --- |
| `My Profile` large title | Top account hub header |
| Large soft-glass identity card | Screen-local identity hero in `ProfileScreen.kt` |
| Avatar glow ring | Circular `AsyncImage` with soft purple/cyan border |
| Pencil edit button | Rounded icon action wired to `navigateToEditProfile` |
| Role/status pills | Soft pills using current `roleName` and visual active display |
| Division/NIP rows | Existing `divisionName`/`programName` and `nipNim` |
| Three mini cards | Role, division, contact summary cards |
| Rounded grouped menu cards | Account, Company Access, Help, Security sections |
| Red logout row | Security section row wired to existing logout confirm dialog |

Out-of-scope from the reference:

- FAQ navigation/feature activation because current Profile route/callback is not wired.
- About detail screen creation; preserve existing no-op behavior only if displayed.
- Dashboard/WFA/attendance/report/bottom-nav/detail-screen redesign.
- Backend-driven active status semantics not available in `UserModel`.

## Task 2 — Re-read affected files immediately before editing

Read current versions in the INF-226 worktree:

- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileScreen.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileViewModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/navigation/MainContentNavGraph.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/auth/UserModel.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/surface/InfiniteCard.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/status/InfiniteStatusPill.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/components/data/InfiniteSectionHeader.kt`
- `app/src/main/java/com/example/infinite_track/presentation/design/tokens/InfiniteColors.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-in/string.xml`

## Task 3 — Refactor ProfileScreen main success content only

1. Keep existing top-level state collection, language popup, logout confirm/loading/status dialogs, and navigation callbacks intact.
2. Replace the success-state content layout with a scrollable Account Hub layout:
   - page title/header if visually aligned with reference
   - Identity hero section
   - Account section
   - Company Access section
   - Help section
   - Security section
3. Use existing `UserModel` fields:
   - full name
   - role name
   - position
   - division or program as secondary org detail
   - NIP/NIM
   - photo URL
4. Use local helper composables inside `ProfileScreen.kt` if they are screen-specific:
   - identity hero card
   - section card
   - menu action row
   - info chips/rows
5. Prefer existing reusable Material 3 design components where they fit:
   - `InfiniteCard`
   - `InfiniteStatusPill`
   - `InfiniteSectionHeader`
   - `InfiniteColors`
6. Do not create a global icon token object.
7. Keep Account Hub colors out of `ProfileScreen.kt`; add/reuse theme/design tokens in `InfiniteColors.kt` backed by existing `presentation/theme/Color.kt` palette.
8. Do not modify `ProfileViewModel.kt` unless compile requires a no-behavior-change cleanup.

## Task 4 — Preserve existing actions and role behavior safely

1. Wire row clicks to the existing callbacks:
   - Edit Profile -> `navigateToEditProfile`
   - Language -> `profileViewModel.onLanguageSettingsClicked()`
   - My Document -> `navigateToMyDocument`
   - Pay Slip -> `navigateToPaySlip`
   - Employees / contacts -> `navigateToContacts` if retained
   - Contact Us -> `navigateToContactUs`
   - About -> no-op as current behavior, unless removed from visible UI because it is unwired; do not create route/screen
   - Logout -> `showLogoutConfirmDialog = true`
2. Role-gate Pay Slip only with a conservative local helper if supported by current `roleName` evidence:
   - hide for role strings containing `intern`, `internship`, or `magang`
   - show for other roles to preserve current behavior
3. Mark role visibility as `Needs Verification` unless runtime data confirms role variants.
4. Do not change `MainContentNavGraph.kt`.

## Task 5 — Strings and labels

1. Reuse existing string resources where possible.
2. Add only small section label strings if needed:
   - Identity
   - Account
   - Company Access
   - Help
   - Security
   - Active
   - NIP/NIM
3. Add matching Indonesian translations in `values-in/string.xml` if new strings are introduced.
4. Avoid changing existing labels used by detail screens unless required for the main screen only.

## Task 6 — Build and local verification

Run from `C:\Users\Febriyadi\.claude\worktrees\android-account-hub-main-refresh`:

```bash
./gradlew app:assembleDebug
```

If feasible:

```bash
./gradlew app:test
./gradlew app:lint
```

If Gradle fails with a known local loopback/daemon issue, preserve the exact output and mark build as `Needs Verification` rather than claiming success.

### Current local verification status

Attempts in this worktree failed before Kotlin compile with the same environment-level Gradle bootstrap error:

```text
java.io.IOException: Unable to establish loopback connection
```

Commands attempted:

```bash
./gradlew app:assembleDebug
./gradlew --no-daemon app:assembleDebug
./gradlew --no-daemon -Djava.net.preferIPv4Stack=true app:assembleDebug
```

Static checks completed:

- `git diff --check` returned no whitespace errors, only Windows line-ending warning for `ProfileScreen.kt`.
- `ProfileScreen.kt` grep found no Account Hub hardcoded hex/theme colors after moving colors to `InfiniteColors.kt`.
- Account Hub string parity check found 20 `account_hub_*` strings in both `values/strings.xml` and `values-in/string.xml`, with no missing counterpart.

## Task 7 — Runtime / visual verification

If emulator/device is available:

1. Open Profile tab.
2. Capture refreshed Account Hub screenshot.
3. Confirm visual grouping:
   - Identity
   - Account
   - Company Access
   - Help
   - Security
4. Tap and return:
   - Edit Profile
   - My Document
   - Pay Slip if visible
   - Contact/Support
5. Tap Logout and verify existing confirmation dialog appears.
6. Cancel logout. Do not perform destructive logout unless explicitly safe.

If emulator/runtime cannot be run, mark these as `Needs Verification`.

## Expected affected files

Likely:

- `app/src/main/java/com/example/infinite_track/presentation/screen/profile/ProfileScreen.kt`
- `app/src/main/res/values/strings.xml` if section labels need resources
- `app/src/main/res/values-in/string.xml` if section labels need translations

Possibly not touched:

- `ProfileViewModel.kt`
- `MainContentNavGraph.kt`
- detail screens

## PR / review note draft

- Work done in isolated branch/worktree: `fix/android-account-hub-main-refresh` / `C:\Users\Febriyadi\.claude\worktrees\android-account-hub-main-refresh`.
- Main Account Hub/Profile screen refreshed.
- Visual direction follows uploaded reference image within INF-226 scope.
- Content grouped into identity/account/company access/help/security.
- Existing navigation behavior preserved.
- Role-based visibility respected or marked `Needs Verification`.
- No detail screens redesigned.
- No backend changes.
- No auth/session changes.
- No global icon token object.
- Build evidence included.
- Runtime screenshot/navigation evidence included or marked `Needs Verification`.
