# INF-207 Android Auth Session / Logout / Status Today Runtime Design

## Status

Approved Android-only design for `INF-207 — Refactor Android Auth Session, Logout, and Status Today Runtime Flow`.

This design is a bounded continuation of the existing Family A auth/session chain already merged into `develop` through PR #29 (`78e474b`). It does not restart Family A from scratch and it does not reuse the stale `fix/android-family-a-access-session-continuation` worktree as an execution base.

## Product-First Context

Family taxonomy and repo policy still apply:

- Backend remains the source of truth for auth/session validity, refresh semantics, logout semantics, booking approval, attendance truth, and final reporting outcomes.
- Android is a trusted runtime data-capture and orchestration client.
- Android may cache and reuse recent runtime state, but it must not invent final auth or attendance truth when backend state is stale, terminal, or unknown.

`INF-207` exists because Android runtime ownership is currently spread across interceptor, splash bootstrap, foreground validation, repositories, preferences, and ViewModels. That spread causes two concrete problems:

1. forced reauth still reuses user logout behavior in terminal auth paths; and
2. Android over-fetches `/api/auth/me` and `/api/attendance/status-today` instead of reusing bounded local freshness.

## Existing Repository Truth

Current `develop` already contains:

- `SessionManager`, `AuthRefreshInterceptor`, `RefreshSingleFlightCoordinator`, `CheckSessionUseCase`, `ValidateForegroundSessionUseCase`, and `ForegroundSessionLifecycleObserver` from the existing Family A chain.
- `AuthRepositoryImpl` login / refresh / logout primitives aligned with the mobile contract from PR #29.
- attendance `checkIn`, `checkOut`, and `getTodayStatus` flows.

Current gaps confirmed during design exploration:

- `AuthRefreshInterceptor` still calls `LogoutUseCase` for terminal auth failure.
- `ForegroundSessionLifecycleObserver` still calls `LogoutUseCase` on `ReauthRequired`.
- `SplashViewModel` still calls `LogoutUseCase` on bootstrap `ReAuthRequired`.
- `TodayStatusResponse` still models only the old payload shape and does not support the INF-206 fields.
- `AttendanceRepositoryImpl.getTodayStatus()` still fetches from network every time.
- local `/me` validation freshness is not bounded by a shared 300-second policy.

## Goals

- Separate forced reauth from user-initiated logout.
- Ensure terminal auth/session failure clears local authenticated runtime without calling backend logout.
- Preserve user-initiated logout as backend logout followed by local cleanup.
- Add INF-206-compatible `status-today` response support:
  - `data.attendance_session_state.id`
  - `data.attendance_session_state.key`
  - `data.attendance_session_state.label`
  - `data.active_attendance_id`
  - `meta.cache_ttl_seconds`
- Add bounded local `status-today` caching with a final TTL of **300 seconds**.
- Add bounded local `/me` freshness with a final TTL of **300 seconds**.
- Invalidate local `status-today` cache after check-in, checkout, logout, forced reauth, and user/date/session changes.
- Preserve current login, refresh, retry-original-request, logout, check-in, checkout, and status render behavior.

## Non-Goals

- Do not change backend endpoint paths.
- Do not change Android base URL strategy.
- Do not redesign UI.
- Do not refactor face processing.
- Do not rewrite the entire auth/session architecture.
- Do not change backend login / refresh / logout / me contract semantics beyond Android consumption.
- Do not expand this work into unrelated Family B attendance/action cleanup.

## Recommended Approach

Use a **surgical boundary refactor** inside the existing architecture.

This design intentionally avoids a broad coordinator rewrite and avoids a tactical patch that would keep semantics scattered. The chosen approach introduces a small number of explicit use cases/preferences and rewires existing callers to those boundaries.

Why this approach:

- It matches the current repository shape.
- It minimizes blast radius against `develop`.
- It preserves Family A logic already proven by PR #29.
- It creates sharper invariants around logout vs forced reauth.
- It supports targeted tests and honest runtime verification.

## Auth / Logout / Forced Reauth Boundary

### `ClearAuthenticatedRuntimeUseCase`

Create a dedicated use case whose responsibility is local authenticated runtime cleanup only.

It should clear:

- DataStore auth/session state through `UserPreference.clearAuthData()`.
- persisted Room user profile through `UserDao.clearUserProfile()`.
- local `status-today` cache.
- local `active_attendance_id`.
- stored geofence runtime parameters.
- stored reminder geofences.

It must not:

- call backend logout;
- publish reauth reason;
- own navigation decisions.

### `ForceReauthUseCase`

Create a dedicated use case for terminal auth/session failure.

Behavior:

1. acquire the one-shot guard with `SessionManager.beginSessionExpiryHandling()`;
2. if already handled, stop;
3. run `ClearAuthenticatedRuntimeUseCase`;
4. publish `SessionManager.triggerForcedReauth(reason)`.

It must never:

- call `/api/auth/logout`;
- delegate to `LogoutUseCase`;
- treat terminal auth invalidation as user intent.

### `LogoutUseCase`

Narrow `LogoutUseCase` to user-initiated logout only.

Behavior:

1. call backend logout through repository remote logout behavior;
2. always run `ClearAuthenticatedRuntimeUseCase` afterward;
3. tolerate backend logout failure if local cleanup succeeds, while still preserving truthful logging/result handling.

### Caller rewiring

The following callers must use `ForceReauthUseCase` instead of `LogoutUseCase` for terminal auth paths:

- `AuthRefreshInterceptor`
- `ForegroundSessionLifecycleObserver`
- `SplashViewModel`

This enforces the core invariant:

- forced reauth = local cleanup + reason publication
- user logout = remote logout + local cleanup

## Auth Repository Boundary

`AuthRepositoryImpl` currently bundles remote logout and local cleanup into one `logout()` path.

The design requires repository behavior to be split so `LogoutUseCase` can call remote logout without forcing terminal reauth flows to inherit that behavior.

Recommended contract direction:

- keep a user-facing logout operation for user intent;
- add explicit remote logout behavior and explicit local cleanup orchestration through the new use case.

The exact repository method naming can follow current project idiom, but the semantics must remain explicit:

- forced reauth must not call backend logout;
- user logout must still attempt backend logout.

## `status-today` Model Changes

### DTO compatibility

Extend `TodayStatusResponse` to support both existing and INF-206 payloads.

New nullable DTOs/fields:

- `AttendanceSessionStateDto`
- `TodayStatusMeta`
- `data.attendance_session_state.id`
- `data.attendance_session_state.key`
- `data.attendance_session_state.label`
- `data.active_attendance_id`
- `meta.cache_ttl_seconds`

All new fields remain nullable/backward-compatible because backend INF-206 may not yet be fully deployed in every environment.

### Domain model compatibility

Extend the domain `TodayStatus` model with:

- `attendanceSessionState: AttendanceSessionState?`
- `activeAttendanceId: Int?`
- `cacheTtlSeconds: Int`

Default behavior:

- if `meta.cache_ttl_seconds` is missing, null, or invalid, Android must default to `300`.

### Mapping rule

Preferred mapping ownership should move to a mapper that can see both `data` and `meta`, so the domain model can be assembled with TTL and new state fields in one place.

## `status-today` Cache Design

### Cache storage

Create `TodayStatusPreference` to persist cache state.

Store at minimum:

- serialized `TodayStatus`
- `userId`
- `todayDate`
- `attendanceSessionStateId`
- `attendanceSessionStateKey`
- `activeAttendanceId`
- `fetchedAtMillis`
- `ttlSeconds`

### Final TTL policy

The final cache freshness policy for `status-today` is **300 seconds**.

If backend omits `meta.cache_ttl_seconds`, Android still uses `300`.

### Valid-cache rules

Cache is valid only when:

- the same user is active;
- the same date is active;
- TTL has not expired;
- the cache payload parses successfully;
- no force refresh was requested;
- no invalidating mutation/auth boundary event has already occurred.

If any condition fails, repository must fetch from backend.

### Repository API direction

Repository behavior must support a normal cached read and an explicit bypass path.

Examples of acceptable semantics:

- `getTodayStatus(forceRefresh: Boolean = false)`
- `clearTodayStatusCache()`

If default parameters do not fit existing style, separate methods are acceptable as long as the semantics are explicit.

## `active_attendance_id` Synchronization Rules

When backend returns `active_attendance_id`:

- if non-null and positive, store it via `AttendancePreference.saveActiveAttendanceId()`;
- if null and `attendance_session_state.key` is `not_started` or `completed`, clear local active attendance id;
- if backend omits the field entirely, do not clear local active attendance id solely because the field is absent.

This preserves compatibility during backend rollout and avoids false local clearing.

## Cache Invalidation Rules

`status-today` cache must be invalidated when any of the following occurs:

- check-in succeeds;
- checkout succeeds;
- user logout occurs;
- forced reauth occurs;
- user changes;
- date changes;
- session/runtime cleanup occurs.

After check-in success:

- invalidate cache first;
- then force-refresh `status-today` once.

After checkout success:

- invalidate cache first;
- then force-refresh `status-today` once.

This ensures post-mutation UI is backend-confirmed rather than cache-stale.

## `/me` Freshness Policy

### Final freshness policy

The final `/me` freshness window is also **300 seconds**.

Add a local timestamp such as:

- `last_profile_sync_at`

Update it only after successful `/me` sync and local profile persistence.

### Bootstrap ownership

`CheckSessionUseCase` remains the cold-start/bootstrap owner.

Within bootstrap:

- if profile freshness is still valid and local auth/profile state is intact, `/me` may be skipped;
- if freshness is stale or local state is ambiguous, `/me` must be called;
- terminal auth outcomes still route to forced reauth.

### Foreground ownership

`ValidateForegroundSessionUseCase` remains the foreground/resume owner.

Within foreground validation:

- if freshness is still valid, Android may skip `/me`;
- if freshness has expired, Android should validate against backend;
- temporary transport failure must not trigger false logout;
- terminal auth outcome still wins over local freshness.

### Safety invariant

Fresh local profile does not become a permanent proof of backend validity.

It is only a bounded optimization window. If the session becomes terminal through refresh/interceptor/backend outcome, local freshness is immediately irrelevant.

## Testing Strategy

### Auth boundary tests

Add or update tests that prove:

- terminal auth failure uses forced reauth, not backend logout;
- user logout still uses backend logout;
- both paths clear local runtime;
- forced reauth reason is published once;
- duplicate terminal failures do not duplicate cleanup.

Targeted areas include:

- `ForceReauthUseCaseTest`
- `LogoutUseCaseTest`
- `AuthRefreshInterceptorTest`
- `ForegroundSessionLifecycleObserverTest`
- `SplashViewModelTest`

### `status-today` mapping/cache tests

Add or update tests that prove:

- old response shape still maps correctly;
- INF-206 response shape maps correctly;
- missing TTL defaults to `300`;
- first fetch hits network;
- second fetch inside TTL uses cache;
- expired TTL refreshes network;
- force refresh bypasses cache;
- user mismatch/date mismatch/corrupt cache bypass cache;
- check-in and checkout invalidate cache;
- logout and forced reauth clear cache.

### Regression preservation

Existing tests for login, refresh, retry-original-request, logout, foreground validation, check-in, checkout, and status rendering must continue to pass.

## Verification Gate

Minimum local verification remains:

```bash
./gradlew app:compileDebugKotlin
./gradlew app:test
./gradlew app:lint
```

Because INF-207 touches auth/session and attendance runtime, compile/test/lint alone are not sufficient for full closure.

Runtime evidence is required for:

1. app open with valid profile/status cache does not redundantly call `/api/auth/me` or `/api/attendance/status-today`;
2. app open after TTL expiry refreshes `status-today` once;
3. foreground within TTL avoids redundant `/me`;
4. check-in success invalidates cache, refreshes status, and stores active attendance id;
5. checkout success invalidates cache, refreshes status, and clears active attendance id;
6. user logout calls backend logout, clears runtime, and returns to login;
7. forced reauth clears runtime without backend logout and returns to login with the correct reason;
8. existing login, refresh, retry, logout, check-in, checkout, and status render flows remain working.

If emulator/device/backend runtime evidence cannot be executed, the result must remain:

- `Needs Verification: Runtime/emulator/backend evidence not executed.`

## Docs / ADR Impact

`DOCS/ADR UPDATE REQUIRED`

This work changes Android-side auth/session semantics and local runtime cache behavior. At minimum update:

- `docs/adr/ADR-XXX-android-refresh-session-compat.md`

The ADR update should explicitly record:

- forced reauth is not backend logout;
- user logout still attempts backend logout;
- authenticated runtime cleanup is centralized;
- `/me` freshness uses a 300-second bounded optimization window;
- `status-today` uses a 300-second bounded cache with explicit invalidation rules.

Also add or update one focused evidence/follow-up note under:

- `docs/auth-runtime-evidence/`
  or
- `docs/linear-sync/`

Redaction rules remain mandatory. Never commit tokens, refresh tokens, emails, full names, identifiers, or auth-bearing raw logs.

## Affected Areas

Expected code areas:

- `app/src/main/java/com/example/infinite_track/di/auth/AuthRefreshInterceptor.kt`
- `app/src/main/java/com/example/infinite_track/presentation/main/ForegroundSessionLifecycleObserver.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/splash/SplashViewModel.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/auth/LogoutUseCase.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/auth/CheckSessionUseCase.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/auth/ValidateForegroundSessionUseCase.kt`
- `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`
- `app/src/main/java/com/example/infinite_track/data/soucre/local/preferences/UserPreference.kt`
- `app/src/main/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreference.kt`
- `app/src/main/java/com/example/infinite_track/data/soucre/network/response/TodayStatusResponse.kt`
- `app/src/main/java/com/example/infinite_track/domain/model/attendance/AttendanceModel.kt`
- `app/src/main/java/com/example/infinite_track/data/mapper/attendance/AttendanceMapper.kt`
- `app/src/main/java/com/example/infinite_track/data/repository/attendance/AttendanceRepositoryImpl.kt`
- `app/src/main/java/com/example/infinite_track/domain/repository/AttendanceRepository.kt`
- `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/GetTodayStatusUseCase.kt`
- `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- DI wiring for the new use cases/preferences.

## Risks

Primary implementation risks:

- false logout if forced reauth still leaks into remote logout path;
- stale `status-today` if cache invalidation is incomplete;
- lingering over-fetch if `/me` freshness is only partially wired;
- incorrect active attendance synchronization during backward-compatible backend rollout;
- geofence runtime not fully cleared during authenticated runtime cleanup.

These risks are acceptable only if they are directly covered by tests or explicitly left as runtime `Needs Verification`.

## Acceptance Criteria

Implementation is successful only if:

- terminal auth failure no longer uses `LogoutUseCase` semantics;
- forced reauth clears local runtime without backend logout;
- user logout still calls backend logout before local cleanup;
- `TodayStatus` supports INF-206 fields with backward compatibility;
- `status-today` caching exists and follows the final 300-second TTL policy;
- `/me` freshness exists and follows the final 300-second TTL policy;
- cache invalidation happens after check-in, checkout, logout, forced reauth, and session/date/user changes;
- existing login/refresh/logout/check-in/checkout/status flows still work;
- runtime-sensitive closure is only claimed when runtime evidence exists; otherwise status remains `Needs Verification`.
