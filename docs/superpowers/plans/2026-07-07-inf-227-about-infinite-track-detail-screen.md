# INF-227 — About Infinite Track Detail Screen Implementation Plan

Date: 2026-07-07
Branch: `fix/android-about-infinite-track-screen`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-about-infinite-track-screen`
Spec: `docs/superpowers/specs/2026-07-07-inf-227-about-infinite-track-detail-screen.md`

## Status

Implementation started in isolated worktree. Initial routing, data model, dummy content provider, use case, ViewModel, UI state, and About screen components have been added. Visual refinement remains to align more closely with the uploaded semi-liquid glass reference.

## Worktree mapping

- Main checkout `E:\skrisi\android` is dirty and must not be edited.
- Existing branch `fix/android-account-hub-main-refresh` is INF-226 scope and should not be reused for INF-227.
- Existing branch `fix/profile-detail-back-navigation` has dirty navigation changes and should not be reused.
- INF-227 worktree is isolated and safe for this task.

## Implementation steps

### 1. Preserve architecture separation

- Keep About content in `AboutDummyContentProvider`.
- Keep page model in `domain/model/about/AboutContent.kt`.
- Keep ViewModel state in `AboutViewModel` + `AboutUiState`.
- Do not move static long-form copy into `AboutScreen.kt`.

### 2. Finalize navigation wiring

- Keep `Screen.About : Screen("profile/about")`.
- Keep `navigateToAbout` callback on `ProfileScreen`.
- Keep About row wired to `onAbout`.
- Keep About screen inside `ProfileFlow`.
- Back action remains `navController.popBackStack()`.

### 3. Refine visual implementation to match uploaded reference

Implementation constraints:

- About color literals must live in `InfiniteColors` tokens, not inside About composables.
- Existing INF-222 components should be reused where applicable; section titles use `InfiniteSectionHeader`.
- About-specific components are allowed only for local layout/rendering surfaces and must not introduce new global icon token objects.
- Shared About glass sections should wrap existing `InfiniteCard` instead of replacing the design-system surface primitive.

Target refinements:

- Wrap page in lavender/white background with decorative soft blobs.
- Use a floating glass top app bar with rounded rectangle and rounded back button.
- Update hero card layout to match reference:
  - left app icon glass tile/ring
  - right title/tagline/description/badge
  - cyan liquid blob on lower right
- Update overview card:
  - icon + title row
  - long paragraph
  - three/four feature glass chips
- Replace generic `InfiniteTimelineRow` usage with About-specific timeline rows if needed:
  - vertical line
  - purple glowing dots
  - title and description rows
- Update creator card:
  - avatar-style glass circle
  - creator info column
  - contribution text column
  - skill chips row
- Update achievement card:
  - gold-tinted glass card
  - trophy/award style visual
  - visible `Preview only` status
  - no final verified claim unless evidence exists
- Update impact section:
  - three compact cards with icon, title, description
- Footer note centered below content.

### 4. Keep scope boundaries

Do not change:

- Profile main visual design
- bottom navigation
- auth/session
- logout
- backend/API
- unrelated detail screens

### 5. Verification plan

Run from isolated worktree:

```bash
./gradlew app:assembleDebug
```

If environment permits:

```bash
./gradlew app:test
./gradlew app:lint
```

Runtime smoke will be performed by the human/operator on `develop` after this isolated branch is integrated:

```text
Open Profile tab on develop
Tap About Infinite Track row
Verify About Infinite Track screen opens
Scroll all sections
Tap back
Verify Profile screen returns
Capture screenshot/screen recording
```

Until that `develop` runtime pass exists, navigation/screenshot evidence remains `Needs Verification` for this branch.

### 6. Known blocker handling

Current Gradle attempts fail before compilation:

```text
java.io.IOException: Unable to establish loopback connection
```

If this remains, final report must mark build/runtime as `Needs Verification` and include the exact failure.

### 7. PR note requirements

Mention:

- Isolated branch/worktree.
- Added About Infinite Track detail screen.
- Added route and Profile -> About navigation.
- Content is model/provider/ViewModel driven.
- UI follows uploaded semi-liquid glass reference.
- Achievement/appreciation is PreviewOnly / Needs Verification.
- No backend/auth/session/bottom-nav/Profile redesign changes.
- Build evidence or Gradle environment blocker.
- Runtime evidence or `Needs Verification`.
