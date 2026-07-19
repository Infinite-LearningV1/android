# INF-241 — Transparent Animated Splash Screen Design

**Issue:** INF-241  
**Branch:** `fix/inf-241-transparent-animated-splash`  
**Date:** 2026-07-19  
**Status:** Product decision LOCK (executor prompt)

## Goal

Implement branded Android Splash Screen using `R.raw.infinite_track_splash` as a transparent presentation gate above the global app background owned by `InfiniteTrackApp`. Session bootstrap remains authoritative in `SplashViewModel`; Splash only gates navigation until animation completion AND destination resolution.

## Product Decision (LOCK)

```text
InfiniteTrackApp
→ owns BaseLayout + semi-transparent white overlay
→ owns transparent root Surface
→ NavHost destinations render transparent content

Splash container = transparent
Lottie background = transparent
No local white/black/full-color page background
Global BaseLayout remains visible behind splash animation
```

Do **not** duplicate `BaseLayout` inside Splash.

## Observed Repo Facts

### Global background ownership

`presentation/main/InfiniteTrackApp.kt`:

```text
zIndex -2 → BaseLayout animated circles
zIndex -1 → Color.White.copy(alpha = 0.5f)
root Surface → Color.Transparent
NavHost startDestination → Screen.Splash
```

### Current SplashScreen

`presentation/screen/splash/SplashScreen.kt`:

- loads `R.raw.profile_sync`
- Lottie size hardcoded `200.dp`
- shows "Sync Profile" on normal state
- TemporaryFailure shows Retry/Login
- container already transparent
- navigates immediately when destination resolves
- does **not** wait for animation completion

### SplashViewModel contract (preserve)

States:

```text
Loading
TemporaryFailure
NavigateToHome
NavigateToLogin
```

Business meaning of these states is out of scope. Presentation only gates navigation on animation completion.

### Native splash hold bug

`MainActivity` currently keeps native splash while:

```kotlin
viewModel.navigationState.value is SplashNavigationState.Loading
```

This holds native splash through full session bootstrap. When Loading ends, Compose Splash often navigates immediately and branded animation is skipped or nearly invisible.

### Lottie asset (worktree-local, previously untracked)

```text
name: Infinite Liquid Motion
canvas: 184 x 93
frame rate: 60
ip/op: 0 / 226
duration: ~3.77s
solid_layers: 0
shape_layers: 18
assets: 0
```

No solid background layer in JSON metadata. Runtime transparency still needs visual verification.

## Architecture

### Ownership split

| Layer | Owner | Responsibility |
|---|---|---|
| Global background | `InfiniteTrackApp` | BaseLayout + white 50% overlay + transparent Surface |
| Native splash | `MainActivity` + `Theme.App.Starting` | Protect process startup / Compose readiness only |
| Branded animation | `SplashScreen` | Play Lottie once, transparent container |
| Session truth | `SplashViewModel` + `CheckSessionUseCase` | Resolve Home / Login / TemporaryFailure |
| Navigation gate | `SplashScreen` | Fire navigation once when animation finished AND destination resolved |

### Target cold-start flow

```text
App cold start
→ native Android splash appears briefly
→ Compose root becomes ready
→ native splash exits
→ global InfiniteTrackApp background visible
→ infinite_track_splash Lottie plays once above transparent Splash
→ session bootstrap runs in parallel
→ navigate only when animation finished AND destination resolved
→ Home or Login
```

### Duration rule

```text
normal splash duration = max(Lottie duration, session resolution duration)
```

Not additive. No arbitrary primary `delay(...)` if Lottie progress/completion is observable.

### Animation completion contract

```text
animateLottieCompositionAsState(iterations = 1)
isAnimationFinished = composition loaded AND (progress >= 1f OR isAtEnd)
navigate when: resolvedDestination != null AND isAnimationFinished
```

Rules:

- navigation fires only once
- fast session resolution must not cut animation
- slow session resolution must not restart animation
- TemporaryFailure must not trap user in infinite animation loop
- after animation ends on TemporaryFailure, show compact recovery UI

### Native handoff contract

```text
native splash = Compose readiness only
Compose Splash = owns branded animation duration
```

`setKeepOnScreenCondition` must stop depending on `SplashNavigationState.Loading`.

Native splash cannot be literally transparent. Align:

- `windowSplashScreenBackground` with the light white/overlay tone of first Compose frame
- avoid obvious icon jump where possible (minimal/transparent animated icon preferred over launcher icon pop)

### TemporaryFailure recovery

Preserve truthful TemporaryFailure semantics:

```text
Unable to verify session
Retry
Login
```

UI:

- compact
- transparent over global background
- no opaque full-screen background
- shown only after initial animation completion (or when animation already finished)

## Approaches considered

### A. Presentation-only completion gate (recommended)

Keep `SplashViewModel` unchanged. Track `isAnimationFinished` in Compose and AND it with destination states before navigating.

Pros: no auth/session semantic change; lowest risk; matches product lock.  
Cons: navigation logic lives in UI layer (acceptable for presentation gate).

### B. Move animation completion into ViewModel

Pros: single state machine.  
Cons: couples ViewModel to animation lifecycle; out of scope / higher risk.

### C. Fixed delay gate

Pros: simple.  
Cons: fights real Lottie duration / reduced-motion settings; rejected by product rule.

**Chosen:** Approach A.

## Non-goals

- redesign Login Screen
- change CheckSessionUseCase semantics
- change ReAuthRequired vs TemporaryFailure classification
- duplicate/move global BaseLayout into Splash
- add MP4/VideoView/Media3
- introduce another page-level background
- change Home visual composition
- claim session success before SplashViewModel resolves destination

## File impact

Primary:

```text
app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashScreen.kt
app/src/main/java/com/example/infinite_track/presentation/main/MainActivity.kt
app/src/main/res/values/themes.xml
app/src/main/res/raw/infinite_track_splash.json
app/src/main/res/drawable/ic_splash_transparent.xml   # optional native icon alignment
app/src/main/res/values/colors.xml                    # optional splash background tone
```

Reference only (no ownership change):

```text
presentation/main/InfiniteTrackApp.kt
presentation/main/MainScreen.kt
presentation/screen/home/HomeScreen.kt
presentation/screen/splash/SplashViewModel.kt
```

## Verification matrix

| Scenario | Expected |
|---|---|
| cold start valid session | full animation → Home |
| cold start invalid session | full animation → Login |
| temporary bootstrap failure | animation completes → recovery UI (Retry/Login) |
| fast session resolution | animation still plays fully before navigate |
| slow session resolution | animation ends once, waits on last frame, then navigates |
| warm start / recompose | navigation fires once |
| reduced animator scale | no infinite loop; gate still resolves |
| visual continuity | global BaseLayout visible behind Lottie; no opaque splash page |

Build:

```bash
./gradlew app:assembleDebug
```

Runtime cold-start evidence required before Done. Compile-only is not enough.

## Risk / high-risk impact

- Splash sits on auth/session startup path (high-risk area).
- Changing native keep-on-screen condition can cause white/black flash if theme alignment is wrong.
- Navigation one-shot bugs can double-navigate or trap user.
- Must not alter session truth classification.

## Docs / ADR note

No session/auth contract change. No ADR required if only presentation gate + native handoff change. Spec/plan docs in `docs/superpowers/` are sufficient.

## Definition of Done

1. Isolation worktree/branch clear
2. Spec + plan present
3. Diff/PR evidence present
4. Fresh verification evidence present (or explicit Needs Verification)
5. High-risk impact stated
6. PR/review notes available
