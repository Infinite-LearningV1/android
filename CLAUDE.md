# CLAUDE.md

## Project Overview

Infinite Track Android is the Kotlin / Jetpack Compose mobile client for Infinite Track Palu. It owns the Android runtime experience for login, session bootstrap, attendance check-in/check-out capture, work-mode selection, geofence reminders, face verification, WFA booking, local persistence, notifications, and Firebase pre-release distribution.

Android is a trusted data-capture client, not the final source of truth. Backend remains authoritative for attendance outcomes, auth/session validity, booking approval semantics, scheduled-job effects, and final reporting state.

## Repository Identity

- Root Gradle project: `Infinite_Track`.
- Module: single Android app module, `:app`.
- Android namespace/applicationId: `com.example.infinite_track`.
- Current Android config: compile/target SDK 34, min SDK 26.
- Primary language/runtime: Kotlin on Android with Jetpack Compose UI.
- Primary branch roles:
  - `develop` = QA / human validation branch.
  - `master` = release/distribution branch.

## Agent Context Policy

Root `CLAUDE.md` is the operating contract for Android agents. The old root `memory-bank/` model is retired for this repository and must not be used as current source of truth.

Worktree-specific execution context should come from `docs/superpowers/specs/` and `docs/superpowers/plans/` in the active worktree, plus current repo/runtime evidence. If an old `memory-bank/` appears in historical worktrees, treat it as stale supporting context only, never as authoritative progress status.

Do not recreate `memory-bank/` as the default context system. When context needs to be added, prefer updating `CLAUDE.md`, `AGENTS.md`, a focused spec, a focused plan, an ADR, CI/deploy docs, or GitHub/Linear evidence docs depending on scope.

## Commands

Run commands from the Android repository root unless a worktree-specific plan says otherwise.

```bash
# Compile / build
./gradlew app:compileDebugKotlin
./gradlew app:assembleDebug
./gradlew app:assembleRelease

# Tests / lint
./gradlew app:test
./gradlew app:testDebugUnitTest
./gradlew app:lint

# Device / runtime checks when applicable
adb devices -l
./gradlew app:connectedDebugAndroidTest
```

CI branch verification should cover debug assemble, unit tests, lint, and release assemble smoke. Release distribution uses `./gradlew app:assembleRelease` and distributes `app/build/outputs/apk/release/app-release.apk` through Firebase App Distribution.

For UI, navigation, login/session, attendance, face verification, geofence, or device-specific behavior, compile/build evidence alone is not enough. Use emulator/device runtime feedback and Maestro flow verification when the changed flow requires it. If runtime verification cannot be executed, mark the item as `Needs Verification` rather than Done.

## Architecture

The app follows a Clean Architecture style with MVVM-oriented presentation flow:

```text
presentation -> domain -> data
```

Core source layout:

```text
app/src/main/java/com/example/infinite_track/
├── presentation/   # Compose UI, navigation, ViewModels, receivers, runtime policies
├── domain/         # Use cases, models, repository contracts, managers
├── data/           # Repository implementations, local/network sources, workers
└── di/             # Hilt modules and dependency wiring
```

Important path caveat: the current codebase uses `data/soucre/...` in several paths. Treat that spelling as repo reality unless a dedicated cleanup task explicitly renames it.

Primary stack:

- Kotlin, Android Gradle Plugin, Gradle wrapper, version catalog in `gradle/libs.versions.toml`
- Jetpack Compose, Material 3, Navigation Compose, Lottie
- Hilt for dependency injection
- Retrofit + OkHttp for networking
- Room + DataStore for persistence
- WorkManager for retryable/background work
- Mapbox + Google Play Services Location / Geofencing
- CameraX + ML Kit Face Detection + TensorFlow Lite
- Firebase Cloud Messaging + Firebase App Distribution workflow integration

### Main runtime areas

Authentication/session:

```text
app/src/main/java/com/example/infinite_track/presentation/screen/auth/
app/src/main/java/com/example/infinite_track/presentation/screen/splash/
app/src/main/java/com/example/infinite_track/domain/manager/SessionManager.kt
app/src/main/java/com/example/infinite_track/di/auth/
app/src/main/java/com/example/infinite_track/data/repository/auth/
app/src/main/java/com/example/infinite_track/data/soucre/local/preferences/UserPreference.kt
app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt
```

Attendance / face / geofence:

```text
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/face/
app/src/main/java/com/example/infinite_track/presentation/geofencing/
app/src/main/java/com/example/infinite_track/data/worker/LocationEventWorker.kt
app/src/main/java/com/example/infinite_track/data/repository/attendance/
app/src/main/java/com/example/infinite_track/data/face/
```

Navigation and role-visible shell:

```text
app/src/main/java/com/example/infinite_track/presentation/navigation/
app/src/main/java/com/example/infinite_track/presentation/main/
app/src/main/java/com/example/infinite_track/presentation/components/navigation/
```

Network/environment/release wiring:

```text
app/src/main/java/com/example/infinite_track/di/NetworkModule.kt
app/build.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
gradle.properties
.github/workflows/
docs/ci/
RELEASE_BUILD_GUIDE.md
RELEASE_BUILD_SUMMARY.md
```

## Source of Truth Boundaries

- Backend is final truth for attendance state, auth/session validity, booking approval semantics, and reporting outcomes.
- Android captures intent, local runtime state, device signals, face/geofence signals, and user interaction state.
- Android must not invent final attendance/reporting truth when backend state is unavailable.
- Android auth/session behavior must follow the mobile contract from backend/shared context, including `X-Client-Type: mobile`, JSON refresh token, refresh endpoint semantics, and forced re-auth codes.
- If Android UI state, local persistence, backend response, docs, GitHub/Linear evidence, and runtime evidence disagree, mark it as `Mismatch` or `Needs Verification` instead of silently choosing one.

## Shared Context (Cross-Repo)

Before cross-contract work, read the cockpit `shared-context` files:

- `API_CONTRACT.md`
- `GLOBAL_STATUS.md`
- `ROUTING_POLICY.md`
- `QUALITY_GATE.md`
- `DECISIONS.md`
- `RISK_REGISTER.md`

Official operating model: `Cowork -> Claude Desktop Host -> Claude Code CLI -> GitHub + Linear`.
If repo/runtime/GitHub/Linear/docs differ, live repo/runtime is the highest factual source. For backend-owned semantics, backend contract/runtime wins; Android should request contract changes rather than invent downstream semantics.

Done = diff/PR + fresh verification + review verdict. Missing one means `Needs Verification`, not Done.

## High-Risk Areas

Treat these areas as high-risk. Always read current code first, state impact/risk, and plan verification before editing.

### Authentication and session

- Login/session bootstrap and refresh behavior.
- `SessionManager` forced re-auth state.
- `AuthRefreshInterceptor` single-flight refresh and replay logic.
- DataStore token persistence in `UserPreference`.
- Auth error code mapping: `AUTH_ACCESS_TOKEN_EXPIRED`, `AUTH_REFRESH_TOKEN_INVALID`, `AUTH_REFRESH_TOKEN_REVOKED`, `AUTH_SESSION_INACTIVE`.

Sensitive rule: token values, email, full name, identifiers, and raw auth-bearing logs must never be pasted into docs, commits, comments, or chat. Redact runtime evidence before saving it.

### Attendance, face verification, geofence, and location events

- Attendance check-in/check-out capture and work-mode semantics.
- Face verification and liveness challenge behavior.
- Geofence registration, receiver behavior, reminders, and location-event worker retry semantics.
- Runtime permission behavior for foreground/background location, camera, and notifications.

Android runtime/auth/attendance closure requires emulator/device and, where appropriate, Maestro or equivalent user-flow evidence. `develop` is the human validation branch for this verification.

### Network, environment, secrets, and release/distribution

High-risk files/areas:

```text
app/src/main/java/com/example/infinite_track/di/NetworkModule.kt
app/src/main/AndroidManifest.xml
app/src/main/res/xml/network_security_config.xml
local.properties
app/google-services.json
release-keystore.jks
firebase-service-account.json
.github/workflows/
docs/ci/
```

Do not expose or repeat secret values from `local.properties`, Firebase service account material, keystores, Mapbox tokens, Google/Firebase config, manifests, generated runtime artifacts, screenshots, logs, or XML dumps. If a tracked config file appears to contain sensitive values, flag the posture instead of quoting values.

Network/environment caveats to verify before changing:

- Backend base URL behavior differs between emulator and physical device.
- Physical-device runtime may require host/LAN/backend URL adjustment.
- Cleartext/network security policy can affect local runtime and release posture.
- CI reconstructs temporary secret/config files from GitHub inputs and should clean them up.

### Navigation and role-visible app shell

Navigation shell and role-visible action surfaces are user-facing and easy to regress. Verify active branch/worktree structure before applying older plans, because some historical plans may reference files not present on the current branch.

## Execution Model

- Agent always works in an isolated branch inside a worktree.
- The main branch held by the human/operator in the terminal remains `develop`.
- Agent output returns to `develop` through PR/merge; then the human pulls and tests on `develop`.
- `master` only receives fix/no-bug/release-ready results from `develop`.
- Do not edit the main working tree for agent work.
- One card = one worktree = one owner.

Normal promotion path:

```text
feature/* or isolated worktree branch -> review branch / PR -> develop -> human verification -> master -> Firebase distribution
```

`develop` is the integration and final human-verification branch. `master` is release-ready. Normal Firebase distribution is from `master`; manual workflow dispatch, if present in GitHub Actions, is a release-gate/manual-first path and must not be treated as feature/develop distribution.

Do not push normal feature work directly to `master`. Direct push to `develop` should be exceptional; prefer review branches and PRs.

## Local Runtime Feedback Gate

Agent coding without runtime feedback is unsafe for tasks that require runtime validation.

Required gate before Done:

- Kotlin / compile-sensitive work: run compile and relevant unit tests.
- UI / navigation / auth / attendance flow work: run build/tests and verify on emulator/device when applicable.
- Android flow work: run Maestro on a local emulator when the changed user flow needs device confirmation.
- Release/distribution work: verify release build path and CI/deploy contract evidence.

If a check is not applicable, state `Not Applicable` with reason. If a check cannot be run, state `Needs Verification` with the missing command/environment.

## Quality Gate

Minimum local checks before an Android issue can be treated as verified:

```bash
./gradlew app:lint
./gradlew app:test
```

For auth, attendance, geofence, face verification, notification, or role/navigation flows, add emulator/device runtime evidence. Compile-only is not enough for those areas.

For release/distribution work, also validate release build and workflow/CI contract evidence:

```bash
./gradlew app:assembleRelease
```

## Docs / ADR Trigger Rule

Write `DOCS/ADR UPDATE REQUIRED` or update an existing doc/ADR when work changes:

- auth/session contract or token refresh behavior
- attendance capture semantics or backend source-of-truth expectations
- face verification or liveness behavior
- geofence/background location-event behavior
- navigation shell or role-visible route behavior
- network base URL or environment contract
- Firebase distribution, branch promotion, signing, or release workflow
- major architecture, dependency, or module organization

## Related References

Use these as active references before changing related areas:

- `README.md` — repo role, setup, commands, Firebase distribution overview, related docs.
- `docs/adr/ADR-XXX-android-refresh-session-compat.md` — accepted Android mobile refresh/session contract.
- `docs/auth-runtime-evidence/` — runtime auth evidence; redact sensitive values before committing new evidence.
- `docs/ci/android-master-distribution.md` — Firebase App Distribution CI contract and current readiness caveats.
- `.github/workflows/README.md` — workflow inventory.
- `.github/workflows/android-branch-verification.yml` — branch verification workflow.
- `.github/workflows/android-master-firebase-distribution.yml` — master distribution workflow; verify actual triggers against docs before changing release policy.
- `docs/superpowers/specs/` — focused design specs.
- `docs/superpowers/plans/` — task execution plans; verify paths against current branch before applying older plans.
- `docs/linear-sync/` — Linear follow-up context.
- `RELEASE_BUILD_GUIDE.md` — release build details.

Historical `memory-bank/` content can be useful for archaeology, but it is not authoritative for current progress or active branch truth.

## Required Task Response Shape

For Android repo tasks, separate these categories clearly:

```text
Fact
Assumption
Mismatch
Risk
Needs Verification
Recommendation
```

When implementing, also name affected files/areas, verification plan, docs/ADR note, and PR/release notes.

## Definition of Done

A task is Done only when:

- scope is clear and bounded
- affected files/areas are named
- high-risk impact is stated when relevant
- diff/PR/commit evidence exists
- fresh verification evidence exists, or missing verification is explicitly marked `Needs Verification`
- runtime feedback exists when runtime validation applies
- docs/ADR update need is handled or explicitly noted
- review verdict exists
- PR/review/release notes are available when the task produces code or deployment changes
- GitHub/Linear/reporting status is synchronized only after repo evidence supports it

Do not claim completion based only on agent summary, code appearance, compile success for runtime-sensitive flows, or a single happy-path manual observation.

## Current Context Notes

- `README.md` is the operational entry point for repository role, setup, verification commands, Firebase distribution, and related docs.
- Worktree specs and plans are the preferred default context for focused implementation work.
- Historical `memory-bank/` progress files are not authoritative and should not be used to decide current feature status.
- Android auth/session and attendance closure work needs real runtime evidence on emulator/device; if unavailable, leave status as `Needs Verification`.
- If a local/untracked file in the main checkout contains useful context, do not assume it exists in a worktree from `develop`; verify tracked state first.
