# Android Family A Access & Session Continuation Design

## Status

Approved continuation design for Android-only Family A / Access & Session work.

This is a continuation of the existing Android `INF-147` chain, not a fresh reimplementation. The goal is to harden Android as a backend-auth/session contract consumer after `INF-145` was completed on the backend.

## Product-First Context

Family taxonomy:

- Family A = Access & Session.
- Truth boundary = backend auth/session contract.
- Backend is the source of truth for session validity, token lifecycle, refresh semantics, revocation, inactivity, and final auth decisions.
- Android is a consumer and must orchestrate local runtime state truthfully against backend responses.

Android must not treat cached local user/session data as proof that the backend still accepts the session. Local state can only be used as runtime input for calls to the backend contract.

## Existing Chain Evidence

Relevant Linear / PR chain:

- `INF-145` — backend auth/session refresh contract; status Done.
- `INF-147` — Android silent token refresh orchestration; Linear still shows Backlog, but Android repo has merged implementation evidence.
- `INF-148` and `INF-149` — backend follow-up context only; not Android implementation scope.
- Android PR #10 — initial Android session handling alignment.
- Android PR #21 — Android auth continuity contract alignment.
- Android PR #22 — Android auth bootstrap/session contract stabilization.

Relevant Android docs:

- `docs/superpowers/specs/2026-05-10-android-auth-continuity-refactor-design.md`
- `docs/adr/ADR-XXX-android-refresh-session-compat.md`
- `docs/auth-runtime-evidence/RUN_2026-05-30.md`
- `docs/linear-sync/INF-147-followup-2026-05-30.md`
- `docs/linear-sync/INF-147-followup-2026-05-31-bootstrap-contract.md`

Current evidence supports continuation because the Android orchestration already exists and has targeted tests. The remaining work is contract hardening and verification closure against the final backend `INF-145` shape.

## Mismatch / Gap Being Addressed

Backend `INF-145` final comments state that native/mobile clients should consume the primary token response shape:

```json
{
  "data": {
    "auth": {
      "access_token": "...",
      "refresh_token": "..."
    }
  }
}
```

Legacy direct fields remain temporarily for compatibility:

```json
{
  "data": {
    "token": "...",
    "refresh_token": "..."
  }
}
```

Android currently still models and tests the legacy direct fields as the main shape. Android should prefer `data.auth.access_token` / `data.auth.refresh_token` and keep legacy fields only as fallback.

A second mismatch is client-type naming:

- Contract and Android ADR say `X-Client-Type: mobile`.
- `AuthRefreshInterceptor` already sends `mobile`.
- `AuthSessionApiService` currently hardcodes `X-Client-Type: android`.

The canonical Android consumer value should be `mobile` unless backend contract evidence changes.

## Goals

- Continue the existing Android `INF-147` implementation path instead of rewriting it.
- Make Android prefer backend primary `data.auth.*` token fields for login and refresh.
- Preserve compatibility with legacy `data.token` / `data.refresh_token` while backend cutover is still in progress.
- Normalize Android auth/session client type to `X-Client-Type: mobile`.
- Preserve current single-flight refresh orchestration and protected-request replay behavior.
- Preserve current forced re-auth handling for invalid refresh, revoked refresh, and 48-hour inactivity.
- Preserve temporary-failure semantics for offline/server unreachable so Android does not falsely clear auth state.
- Update tests and ADR/evidence notes so the repo documents the final Android consumer contract accurately.

## Non-Goals

- Do not redefine backend token lifetime, rotation, revocation, inactivity, cleanup, or legacy-token cutover policy.
- Do not work on Web FE.
- Do not rework attendance, WFA, face verification, maps, notifications, or other families.
- Do not replace the existing `AuthRefreshInterceptor`, `RefreshSingleFlightCoordinator`, `CheckSessionUseCase`, or `SessionManager` architecture unless tests expose a direct contract gap.
- Do not claim runtime Done without emulator/device evidence against a reachable `INF-145` backend environment.

## Architecture

Keep the existing boundaries:

- `AuthRepositoryImpl`
  - Performs primitive auth operations.
  - Parses backend token payloads.
  - Classifies refresh/login/profile-sync failures.
  - Persists tokens only after validating usable payloads.
- `AuthRefreshInterceptor`
  - Attaches `Authorization` and `X-Client-Type` to protected requests.
  - Handles protected 401 responses.
  - Refreshes and retries once for refreshable `AUTH_ACCESS_TOKEN_EXPIRED` outcomes.
  - Triggers forced re-auth only for terminal auth outcomes.
- `RefreshSingleFlightCoordinator`
  - Deduplicates concurrent refresh attempts.
- `CheckSessionUseCase`
  - Owns bootstrap/resume validation.
  - Syncs profile against backend truth.
  - Refreshes and retries sync once when appropriate.
- `SessionManager`
  - Holds forced re-auth reason and one-shot session-expiry handling guard.
- `UserPreference`
  - Persists access token, refresh token, user id, and last refresh timestamp.

This design intentionally avoids moving orchestration into UI screens. UI should react to domain/session outcomes; it should not decide backend truth.

## Token Resolution Rules

Android token resolution should use this priority:

1. Access token from `data.auth.access_token`.
2. Fallback access token from `data.token`.
3. Refresh token from `data.auth.refresh_token`.
4. Fallback refresh token from `data.refresh_token`.

Login rules:

- Login succeeds only when a usable access token is present after applying the priority above.
- A missing refresh token remains compatibility-tolerated as an existing Android behavior, but the session is treated as non-refreshable later.
- Persist access token, user id, optional refresh token, and refresh timestamp only after payload validation succeeds.
- Do not persist partial session state when access token is blank or missing.

Refresh rules:

- Refresh request body remains `{ "refresh_token": "..." }`.
- Refresh success requires a usable access token and valid user id after applying the priority above.
- If a rotated refresh token is returned in `data.auth.refresh_token`, store it.
- If only legacy `data.refresh_token` is returned, store it.
- If no new refresh token is returned on refresh success, retain the existing refresh token.
- Invalid payload remains temporary/transient failure and must not mutate local session data.

## Client Type Rules

Every authenticated/mobile auth request should use:

```text
X-Client-Type: mobile
```

This includes the auth-session refresh lane. Do not introduce `android` as a competing client type unless backend contract evidence changes.

## Refresh / Re-auth Orchestration Rules

Preserve existing Family A behavior:

- Protected endpoint returns `401 AUTH_ACCESS_TOKEN_EXPIRED`:
  - call single-flight refresh;
  - retry original request once with the new access token;
  - do not retry repeatedly.
- Refresh returns `AUTH_REFRESH_TOKEN_INVALID`:
  - force full re-auth with invalid reason.
- Refresh returns `AUTH_REFRESH_TOKEN_REVOKED`:
  - force full re-auth with revoked reason.
- Refresh returns `AUTH_SESSION_INACTIVE` or supported inactivity alias:
  - force full re-auth with inactivity reason.
- Refresh transport failure / offline / server down / malformed temporary response:
  - return temporary failure;
  - do not call logout;
  - do not clear local session.
- Concurrent protected 401 responses:
  - join one in-flight refresh attempt;
  - avoid refresh storms.
- Auth endpoints themselves:
  - do not trigger interceptor refresh loops.

## Affected Android Areas

Expected touched areas:

- `app/src/main/java/com/example/infinite_track/data/soucre/network/response/LoginResponse.kt`
- `app/src/main/java/com/example/infinite_track/data/soucre/network/response/RefreshSessionResponse.kt`
- `app/src/main/java/com/example/infinite_track/data/soucre/network/retrofit/AuthSessionApiService.kt`
- `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImpl.kt`
- `app/src/test/java/com/example/infinite_track/data/soucre/network/AuthApiContractTest.kt`
- `app/src/test/java/com/example/infinite_track/data/repository/auth/AuthRepositoryImplRefreshSessionTest.kt`
- `docs/adr/ADR-XXX-android-refresh-session-compat.md`
- `docs/auth-runtime-evidence/RUN_2026-05-30.md` if runtime status wording needs correction.

Only touch `AuthRefreshInterceptor`, `RefreshSingleFlightCoordinator`, `CheckSessionUseCase`, or `SessionManager` if targeted tests expose a direct gap.

## Testing Strategy

Repository/unit tests:

- Login parses primary `data.auth.access_token` and `data.auth.refresh_token`.
- Login falls back to legacy `data.token` and `data.refresh_token`.
- Login fails and persists nothing when neither primary nor fallback access token is usable.
- Refresh parses primary `data.auth.access_token` and `data.auth.refresh_token`.
- Refresh falls back to legacy `data.token` and `data.refresh_token`.
- Refresh success with no returned refresh token keeps the existing refresh token.
- Refresh invalid/revoked/inactivity still maps to forced re-auth outcomes.
- Refresh offline/server-down/malformed temporary response still preserves local session.
- Auth-session refresh API uses `X-Client-Type: mobile`.
- Existing concurrent refresh/single-flight tests remain passing.

Baseline and verification commands:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:assembleDebug
./gradlew app:lint
```

Runtime verification remains required before claiming Done for Family A:

- access token expired -> silent refresh succeeds and original request is replayed;
- refresh invalid/revoked -> full re-auth;
- inactivity > 48 hours -> full re-auth;
- offline/server down during refresh -> no false invalid-auth cleanup;
- concurrent protected 401s -> one refresh attempt.

If runtime verification cannot run because backend/emulator/device environment is unavailable, mark the result `Needs Verification` and update evidence docs truthfully.

## Documentation / ADR Requirements

This work changes auth/session contract consumption, so docs/ADR must be updated.

Minimum docs update:

- `docs/adr/ADR-XXX-android-refresh-session-compat.md`
  - Android prioritizes `data.auth.access_token` / `data.auth.refresh_token`.
  - Legacy direct fields are compatibility fallback only.
  - Canonical `X-Client-Type` for Android mobile consumer is `mobile`.

Runtime evidence docs should distinguish:

- repository/unit verification;
- emulator/device runtime verification;
- backend-environment blockers.

## Acceptance Criteria

- Android continues existing `INF-147` orchestration instead of duplicating it.
- Android login and refresh consume primary backend `data.auth.*` token payloads.
- Android remains compatible with legacy direct token fields during backend cutover.
- Android auth-session refresh lane sends `X-Client-Type: mobile`.
- Silent refresh, terminal re-auth, offline temporary failure, logout/clear-session semantics, and single-flight behavior remain covered by tests.
- Docs/ADR reflect the final backend-authored token payload priority and client type.
- Verification status is honest: runtime-sensitive Family A closure is `Done` only with fresh emulator/device/backend evidence; otherwise `Needs Verification`.
