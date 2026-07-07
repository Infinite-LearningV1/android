# INF-227 — Android About Infinite Track Detail Screen Spec

Date: 2026-07-07
Branch: `fix/android-about-infinite-track-screen`
Worktree: `C:\Users\Febriyadi\.claude\worktrees\android-about-infinite-track-screen`

## Problem

Profile already exposes an About Infinite Track row, but the row is not wired to a detail page. Android needs a dedicated About detail screen that explains the Infinite Track project story, purpose, timeline, creator/contribution, appreciation preview, and impact without adding a backend API or changing Profile main-screen visuals.

## Scope

Implement a local/static About Infinite Track detail page reachable from:

```text
Profile -> About Infinite Track
```

Final route:

```text
profile/about
```

## Non-goals

- Do not redesign Profile main screen.
- Do not redesign Edit Profile, FAQ, Contact tab, Pay Slip, or My Document.
- Do not add a backend About API.
- Do not change auth/session flow.
- Do not change bottom navigation policy.
- Do not change logout behavior.
- Do not create a new global icon token object.
- Do not claim final awards/appreciation without evidence.

## Architecture

Use a lightweight Clean Architecture path for static MVP content:

```text
AboutDummyContentProvider
        ↓
GetAboutContentUseCase
        ↓
AboutViewModel
        ↓
AboutUiState
        ↓
AboutScreen
        ↓
About section components
```

Packages:

```text
domain/model/about/
- AboutContent.kt

data/soucre/local/about/
- AboutDummyContentProvider.kt

domain/use_case/about/
- GetAboutContentUseCase.kt

presentation/screen/profile/details/about/
- AboutScreen.kt
- AboutViewModel.kt
- AboutUiState.kt
- components/*
```

## Data model

`AboutContent` owns all page content:

- `AboutHero`
- `AboutOverview`
- `AboutFeatureChip`
- `AboutTimelineItem`
- `AboutCreator`
- `AboutAchievement`
- `AboutImpactItem`
- `VerificationStatus`
- `AboutImpactSemantic`

Achievement/appreciation claims must use a verification marker. For INF-227 dummy content, the status is:

```kotlin
VerificationStatus.PreviewOnly
```

This prevents preview-only appreciation copy from being presented as a final verified award.

## Visual direction

Reference received on 2026-07-07. About UI colors must come from the existing design token layer (`InfiniteColors`). If a needed About color does not exist yet, add a named token there instead of hardcoding literals in About composables. Section headers should reuse existing INF-222 components such as `InfiniteSectionHeader`, and repeated glass section surfaces should wrap `InfiniteCard` instead of replacing the design-system surface primitive.

Required visual language:

- light theme only
- semi-liquid glass look
- transparent lavender/white surfaces
- rounded cards with soft borders
- purple and cyan accents
- clean long-form vertical content
- floating profile-detail top bar with back button
- hero app identity card with app icon treatment
- custom vertical timeline
- gold-tinted appreciation preview card
- three impact cards

## Required sections

1. Top bar: About Infinite Track
2. Hero app identity card
3. Project Overview
4. Development Timeline
5. Created By
6. Achievement & Appreciation
7. Project Impact
8. Footer note

## Navigation contract

- Add `Screen.About : Screen("profile/about")`.
- Add `navigateToAbout` callback to `ProfileScreen`.
- Wire About row `onClick` to `navigateToAbout`.
- Add About composable to `ProfileFlow` in `MainContentNavGraph`.
- Back button calls `navController.popBackStack()`.

## Source-of-truth boundary

Android only renders local About preview content. It does not define backend attendance outcomes, reporting truth, booking approval semantics, auth/session validity, or official award status.

## Acceptance criteria

- About route exists.
- Profile About row navigates to About screen.
- About content is model/provider/ViewModel-driven.
- About screen includes all required sections.
- Section components are split into small composables.
- Achievement/appreciation section is visibly preview-only or needs verification.
- No backend/auth/bottom-nav/Profile redesign changes.
- `./gradlew app:assembleDebug` passes when Gradle environment can start.
- Runtime smoke confirms Profile -> About -> Back.

## Verification evidence required

- Build: `./gradlew app:assembleDebug`
- Optional quality gate: `./gradlew app:test`, `./gradlew app:lint`
- Runtime screenshot of About screen on `develop` after branch integration
- Runtime screenshot/recording of Profile -> About navigation on `develop` after branch integration
- Code evidence for content separation and preview-only achievement status

## Current known verification caveat

In this session, Gradle fails before compilation with:

```text
java.io.IOException: Unable to establish loopback connection
```

This is an environment/Gradle bootstrap failure, not a source compile verdict. Build remains `Needs Verification` until rerun in a working Gradle environment.
