# INF-241 Transparent Animated Splash Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement transparent branded Splash Screen with `infinite_track_splash` Lottie, native-to-Compose handoff, and one-shot navigation gated by animation completion + session destination.

**Architecture:** Keep session truth in `SplashViewModel`. Move only presentation timing into `SplashScreen` via Lottie completion. Release native splash on Compose readiness, not full bootstrap Loading. Global `BaseLayout` remains owned by `InfiniteTrackApp`.

**Tech Stack:** Kotlin, Jetpack Compose, Lottie Compose, AndroidX SplashScreen API, Material 3.

## Global Constraints

- Work only in isolated branch/worktree `fix/inf-241-transparent-animated-splash`.
- Do not redesign Login, CheckSessionUseCase, or ReAuth vs TemporaryFailure classification.
- Do not duplicate BaseLayout inside Splash.
- Splash container + Lottie background remain transparent.
- Navigation fires once only when `isAnimationFinished && destinationResolved`.
- Duration = max(Lottie, session), not sum; no arbitrary primary delay.
- Do not expose tokens/email/NIP/NIM.

---

### Task 1: Add branded Lottie asset + theme alignment resources

**Files:**
- Create/Track: `app/src/main/res/raw/infinite_track_splash.json`
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/drawable/ic_splash_transparent.xml`
- Modify: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Ensure asset is present in worktree raw/**

Copy/track `infinite_track_splash.json` if missing.

- [ ] **Step 2: Add soft splash background tone**

In `colors.xml`:

```xml
<color name="splash_background">#FFF7F7FB</color>
```

Tone approximates first Compose frame (white overlay over soft BaseLayout), reducing hard pure-white flash without claiming literal transparency.

- [ ] **Step 3: Add transparent/minimal splash icon**

Create `ic_splash_transparent.xml` as a fully transparent vector (or near-invisible) so Android 12+ system splash does not pop the launcher icon.

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#00000000"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

- [ ] **Step 4: Align Theme.App.Starting**

```xml
<style name="Theme.App.Starting" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_transparent</item>
    <item name="postSplashScreenTheme">@style/Theme.Infinite_Track</item>
</style>
```

---

### Task 2: Fix native splash handoff in MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/main/MainActivity.kt`

**Interfaces:**
- Consumes: AndroidX SplashScreen `setKeepOnScreenCondition`
- Produces: native splash released when Compose content is ready, independent of session Loading

- [ ] **Step 1: Replace Loading-based keep condition**

Replace:

```kotlin
splashScreen.setKeepOnScreenCondition {
    viewModel.navigationState.value is SplashNavigationState.Loading
}
```

With a Compose-readiness flag:

```kotlin
private var isComposeReady = false

// in onCreate after installSplashScreen():
splashScreen.setKeepOnScreenCondition { !isComposeReady }

setContent {
    // first composition marks readiness
    SideEffect { isComposeReady = true }
    Infinite_TrackTheme {
        InfiniteTrackApp(...)
    }
}
```

Notes:
- Do not wait for NavigateToHome/Login.
- Remove unused SplashNavigationState import if no longer referenced.

---

### Task 3: Rebuild SplashScreen presentation gate

**Files:**
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashScreen.kt`

**Interfaces:**
- Consumes: `splashViewModel.navigationState`
- Consumes: `R.raw.infinite_track_splash`
- Produces: one-shot navigation to Home/Login after animation finished
- Produces: TemporaryFailure recovery UI after animation finished

- [ ] **Step 1: Load branded asset once and animate once**

```kotlin
val composition by rememberLottieComposition(
    LottieCompositionSpec.RawRes(R.raw.infinite_track_splash)
)
val progress by animateLottieCompositionAsState(
    composition = composition,
    iterations = 1,
    isPlaying = true,
    restartOnPlay = false
)
val isAnimationFinished = composition != null && progress >= 1f
```

- [ ] **Step 2: Responsive transparent Lottie**

- full-screen transparent Box
- no BaseLayout
- no opaque Surface/Scaffold background
- keep aspect ratio (canvas 184x93)
- width fraction ~0.72f with max width ~320.dp, height auto via aspectRatio
- contentDescription for accessibility
- hold final frame after completion (progress stays at 1f)

- [ ] **Step 3: One-shot navigation gate**

```kotlin
var hasNavigated by remember { mutableStateOf(false) }

LaunchedEffect(navigationState, isAnimationFinished, hasNavigated) {
    if (hasNavigated || !isAnimationFinished) return@LaunchedEffect
    when (navigationState) {
        is SplashNavigationState.NavigateToHome -> {
            hasNavigated = true
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
        is SplashNavigationState.NavigateToLogin -> {
            hasNavigated = true
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
        else -> Unit
    }
}
```

- [ ] **Step 4: TemporaryFailure recovery after animation**

Show only when:

```text
navigationState is TemporaryFailure && isAnimationFinished
```

UI:
- message: "Unable to verify session"
- Retry → `splashViewModel.retrySessionCheck()`
- Login → navigate to Login once
- transparent compact layout; no opaque page background
- remove normal-state "Sync Profile" text entirely

- [ ] **Step 5: Retry behavior rule**

On Retry, ViewModel returns to Loading. Do **not** restart Lottie if already finished; keep final frame and show recovery again only after next TemporaryFailure. If destination resolves after retry, navigate immediately because `isAnimationFinished` is already true.

---

### Task 4: Preserve SplashViewModel semantics (verify only)

**Files:**
- Verify only: `SplashViewModel.kt`, `SplashBootstrapGate.kt`

- [ ] Confirm no business-state changes.
- [ ] Confirm Retry still sets Loading then re-runs bootstrap.
- [ ] Confirm ReAuthRequired → NavigateToLogin and TemporaryFailure remain distinct.

---

### Task 5: Build + verification evidence

- [ ] `./gradlew app:assembleDebug`
- [ ] Manual/runtime checklist:
  - cold start valid session
  - cold start invalid session
  - temporary failure recovery
  - no double navigation
  - transparent global background continuity
- [ ] Mark any missing runtime path as `Needs Verification`

---

### Task 6: Commit + PR notes

- [ ] Commit asset + presentation/native handoff changes with clear message
- [ ] Draft PR notes covering risk, verification, non-goals

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| global background ownership unchanged | Task 3 (no BaseLayout) |
| transparent splash container | Task 3 |
| Lottie completion contract | Task 3 |
| native splash handoff | Task 2 + Task 1 |
| TemporaryFailure recovery UI | Task 3 |
| navigation one-shot gate | Task 3 |
| verification matrix | Task 5 |
