# Infinite Track Android

Android mobile client for Infinite Track Palu, focused on attendance capture, geofencing, face verification, WFA booking, and internal pre-release distribution through Firebase App Distribution.

## Repository Role

This repository owns the Android client experience and runtime behavior for:
- login and session bootstrap
- attendance check-in / check-out flow
- work mode capture (`WFO`, `WFH`, `WFA`)
- geofence reminders and background location-event handling
- face verification and liveness flow
- WFA booking and related client-side state
- local persistence for session, profile, and attendance-related state

Android is treated as a trusted data-capture client. Backend state remains the final source of truth for attendance outcomes.

## Core Features

- Smart attendance flow with geofence-aware validation
- Face verification and liveness challenge for attendance actions
- Support for `WFO`, `WFH`, and `WFA` work modes
- WFA booking flow with recommendation and map interaction support
- Attendance and booking history screens
- Internal master-only Firebase App Distribution lane for pre-release delivery

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture style with MVVM-oriented presentation flow
- **Dependency Injection:** Hilt
- **Networking:** Retrofit + OkHttp
- **Persistence:** Room + DataStore
- **Background Work:** WorkManager
- **Maps & Location:** Mapbox + Google Play Services Location / Geofencing
- **Camera & ML:** CameraX + ML Kit Face Detection + TensorFlow Lite
- **Firebase:** Firebase Cloud Messaging, Firebase App Distribution workflow integration

## Getting Started

### Prerequisites
- Android Studio
- JDK 17-compatible Android toolchain
- Reachable backend environment for device/runtime testing
- Firebase app config for Android
- Mapbox access token

### Local Configuration

#### 1. Mapbox token
Create `local.properties` in the repository root:

```properties
MAPBOX_ACCESS_TOKEN=YOUR_MAPBOX_ACCESS_TOKEN
```

#### 2. Firebase config
Place the correct Android Firebase config file at:

```text
app/google-services.json
```

#### 3. Backend URL
The app chooses its backend base URL in:

```text
app/src/main/java/com/example/infinite_track/di/NetworkModule.kt
```

Current behavior:
- emulator → `http://10.0.2.2:3005/`
- physical device → hardcoded local-network IP

If you test on a physical device, update that IP to a reachable backend host for your environment.

## Verification Commands

Use these commands from the repository root:

```bash
./gradlew app:assembleDebug
./gradlew app:test
./gradlew app:lint
./gradlew app:assembleRelease
```

These are the primary local verification commands used for CI/CD and release-prep validation.

## Project Structure

```text
app/src/main/java/com/example/infinite_track/
├── presentation/   # Compose UI, navigation, ViewModels, receivers
├── domain/         # Use cases, models, repository contracts, managers
├── data/           # Repository implementations, local/network data sources, workers
└── di/             # Dependency injection wiring
```

Keep detailed architecture reasoning in `CLAUDE.md` and dedicated docs; this README is the operational entry point.

## Branch and Release Flow

Branch model in practice:
- `feature/*` → isolated development branches
- `develop` → integration branch
- `master` → release-ready branch

Release-prep work is validated before final merge to `master`, and Firebase App Distribution runs from `master`.

## Firebase Distribution Status

The repository now supports a master-only Firebase App Distribution lane.

What is currently true:
- release APK builds are validated locally and in CI
- branch verification checks lint, unit tests, debug build, and release compile smoke
- signed release distribution is triggered from `master`

Important distinction:
- **Distribution-ready** means the build/sign/upload path is working
- **End-to-end-ready** still depends on backend/runtime validation with the target environment

## Packages Used

### UI & App
- **Jetpack Compose** — UI rendering and screen composition
- **Material 3** — design system and core UI components
- **Navigation Compose** — in-app navigation and graph ownership
- **Lottie** — loading and motion animations

### Dependency Injection & Background Work
- **Hilt** — dependency injection for app, ViewModel, and worker wiring
- **WorkManager** — background work execution and retryable tasks
- **Hilt Work** — worker injection support

### Networking & Persistence
- **Retrofit** — backend API client
- **OkHttp + logging-interceptor** — HTTP stack and debug logging
- **Room** — structured local persistence
- **DataStore** — lightweight preference and session storage

### Location & Maps
- **Mapbox Maps SDK** — map rendering and camera control
- **Mapbox Compose Extension** — Compose integration for map UI
- **Google Play Services Location** — current location and geofencing support

### Camera & ML
- **CameraX** — camera preview and image analysis
- **ML Kit Face Detection** — face detection and liveness-related detection support
- **TensorFlow Lite** — face embedding and on-device inference runtime
- **TensorFlow Lite Support / Metadata** — preprocessing and model support utilities

### Firebase
- **Firebase Cloud Messaging** — push notification delivery
- **Firebase App Distribution** — internal release distribution from `master` via GitHub Actions

### Build & Tooling
- **Android Gradle Plugin**
- **Kotlin**
- **Google Services Gradle Plugin**

For exact dependency versions, use:
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

## Related Docs

- `CLAUDE.md` — repo operating contract and Android risk policy
- `docs/ci/android-master-distribution.md` — master-only distribution workflow notes
- `.github/workflows/README.md` — workflow inventory
- `RELEASE_BUILD_GUIDE.md` — release-build details
