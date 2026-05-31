# Android Auth Continuity Refactor Design

## Context

Linear `INF-147` asks Android to consume the backend refresh-token contract so mobile sessions stay alive while silent refresh succeeds, and require full re-auth when refresh is invalid, revoked, or blocked by the 48-hour inactivity policy. Linear `INF-145` defines the cross-client backend-authored contract: access tokens may expire normally, refresh is allowed while the refresh token remains valid, and clients must distinguish retryable transport failures from terminal auth failures.

The Android code already has most of the right building blocks:

- `AuthRepositoryImpl` handles primitive auth operations: login, refresh, profile sync, and logout.
- `RefreshSingleFlightCoordinator` prevents duplicated refresh attempts.
- `AuthRefreshInterceptor` handles 401 responses from protected API calls.
- `CheckSessionUseCase` handles app bootstrap / session resume flow.

The refactor should not collapse those boundaries into one large class. The main problem is inconsistent failure vocabulary across the auth path: refresh already returns `RefreshSessionResult`, while sync and bootstrap still rely on generic `Result` failures and exception types for auth decisions.

## Goals

- Keep login compatible with backend responses that omit `refresh_token` while still requiring a valid access token.
- Treat a missing local refresh token as a non-refreshable session, not as an immediate login failure.
- Use consistent auth-continuity semantics across login, refresh, profile sync, protected API retry, and logout.
- Preserve local auth state on temporary refresh failures such as offline, timeout, or server-down conditions.
- Trigger full re-auth only for terminal auth failures: invalid/revoked refresh token or the 48-hour inactivity policy.
- Keep single-flight refresh behavior so concurrent 401 responses do not create refresh storms.
- Align logout / clear-session behavior with the backend refresh contract.

## Non-Goals

- Redefining backend token lifetime, rotation, revocation, or inactivity policy.
- Changing Web frontend auth orchestration.
- Adding new UI flows beyond surfacing the existing session-expired behavior more consistently.
- Moving all auth orchestration into `AuthRepositoryImpl`.

## Recommended Approach

Use centralized auth-continuity semantics while preserving the current architectural boundaries.

`AuthRepositoryImpl` remains responsible for primitive operations and backend failure classification. Higher layers remain responsible for orchestration:

- `AuthRefreshInterceptor` handles protected API retry after 401.
- `RefreshSingleFlightCoordinator` ensures only one refresh call is active at a time.
- `CheckSessionUseCase` handles app bootstrap / resume by syncing the profile, refreshing when appropriate, and retrying sync once after refresh success.

The refactor should expand use of the existing `RefreshSessionResult` vocabulary instead of introducing unrelated exception-based auth signals. Auth decisions should be explicit:

- `Success`
- `TemporaryFailure`
- `ReAuthRequired.InvalidOrRevoked`
- `ReAuthRequired.InactivityExceeded`

This keeps the Android contract close to Linear `INF-147` / `INF-145` and avoids interpreting transport failures as session-invalid states.

## Auth Flow Design

### Login

Login succeeds when the backend returns a usable access token. A missing or blank refresh token does not fail login. Instead, the session is stored with an empty refresh token and is treated as non-refreshable later.

Rules:

- Valid access token + present refresh token: store both tokens and user id.
- Valid access token + missing or blank refresh token: store access token and user id; store no refresh token.
- Blank access token: fail login and do not persist session data.
- Successful login still updates the local user profile while preserving any existing face embedding.

### Refresh Session

Refresh session remains the source of truth for deciding whether a session can be silently recovered.

Rules:

- Missing local refresh token: return `ReAuthRequired.InvalidOrRevoked` without calling the backend.
- Successful refresh with valid access token and user id: persist the new access token and user id.
- Successful refresh with a new refresh token: persist the new refresh token.
- Successful refresh with missing refresh token: retain the previous refresh token.
- Blank access token or invalid user id in refresh response: return `TemporaryFailure` and do not mutate local session data.
- Invalid/revoked refresh token: return `ReAuthRequired.InvalidOrRevoked`.
- 48-hour inactivity response: return `ReAuthRequired.InactivityExceeded`.
- Offline, timeout, malformed error body, and 5xx failures: return `TemporaryFailure`.

### Protected API Access

`AuthRefreshInterceptor` owns protected API retry after 401.

Rules:

- Attach the current access token to non-auth endpoints.
- On 401 from a protected endpoint, call `RefreshSingleFlightCoordinator.refreshOrJoin()`.
- If refresh succeeds, retry the original request once with the new access token.
- If refresh requires re-auth, trigger session-expiry handling once and clear local auth state through the logout use case.
- If refresh temporarily fails, return the original 401 response and do not clear local state.
- Do not refresh login, refresh, or logout requests.
- Do not retry requests already marked as refresh retries.

### App Bootstrap / Resume

`CheckSessionUseCase` owns app bootstrap and foreground/resume session validation.

Rules:

- First attempt to sync the profile with the current access token.
- If sync succeeds, proceed with existing face-embedding bootstrap behavior.
- If sync fails for a non-auth reason, return a temporary bootstrap failure.
- If sync fails because the session is unauthorized, attempt refresh.
- If refresh succeeds, retry profile sync once.
- If retry sync is still unauthorized after refresh success, return `ReAuthRequired.InvalidOrRevoked`.
- If refresh requires re-auth, propagate the exact re-auth reason.
- If refresh temporarily fails, keep local auth state intact and return a temporary failure.

### Logout and Clear Session

Logout remains best-effort against the server and deterministic locally.

Rules:

- User-initiated logout calls the backend logout endpoint when possible.
- Local auth data and local profile are always cleared after user-initiated logout, even if backend logout fails.
- Session-expired cleanup reuses the same local cleanup semantics.
- Temporary refresh failures must not call logout or clear local auth data.

## Data and Error Semantics

The refactor should reduce auth control flow that depends on generic exceptions.

Expected semantics:

- Auth-terminal conditions are represented as typed `ReAuthRequired` outcomes.
- Retryable transport/server conditions are represented as typed temporary failures.
- Exceptions remain valid for unexpected internal failures and technical failures, but should not be the main way to encode auth state.
- UI-facing orchestration can distinguish “try again later” from “login again”.

## Testing Strategy

### Repository Tests

- Login succeeds when refresh token is `null` or blank and access token is valid.
- Login fails when access token is blank and does not persist partial session state.
- Refresh without a local refresh token does not call the backend and returns re-auth required.
- Refresh with invalid/revoked token returns re-auth required.
- Refresh with 48-hour inactivity response returns `InactivityExceeded`.
- Refresh with offline/server-down/malformed response returns temporary failure.
- Refresh success stores rotated refresh tokens.
- Refresh success with missing refresh token keeps the existing refresh token.
- Refresh invalid payload does not mutate existing session data.
- Logout clears local auth/profile even when server logout fails.

### Bootstrap Tests

- Profile sync success returns the synced user and preserves existing embedding behavior.
- Unauthorized sync followed by successful refresh retries sync once and succeeds.
- Unauthorized sync followed by temporary refresh failure preserves local auth state and returns temporary failure.
- Unauthorized sync followed by invalid/revoked refresh returns re-auth required.
- Unauthorized sync followed by inactivity response returns `InactivityExceeded`.
- Unauthorized sync after refresh success returns re-auth required.

### Interceptor / Coordinator Tests

- Protected endpoint 401 triggers refresh and retries once on success.
- Login, logout, and refresh endpoints are excluded from refresh attempts.
- Temporary refresh failure does not call logout or clear local auth state.
- Re-auth-required refresh result triggers session-expired handling once.
- Concurrent protected 401 responses join a single refresh attempt.

## Implementation Boundaries

Implementation should stay focused on auth continuity. It may touch:

- `AuthRepositoryImpl`
- `RefreshSessionResult` or a closely related domain outcome type
- `CheckSessionUseCase`
- `AuthRefreshInterceptor`
- `RefreshSingleFlightCoordinator` only if tests expose a contract gap
- Auth repository / use case / interceptor tests

Implementation should avoid unrelated cleanup in attendance, booking, maps, profile editing, or UI screens that do not consume auth-continuity outcomes.

## Acceptance Criteria

- Android login remains compatible with backend responses that omit `refresh_token`.
- Silent refresh recovers protected API access when refresh succeeds.
- App bootstrap/resume can recover through refresh and retry profile sync once.
- Invalid/revoked refresh token and 48-hour inactivity force full re-auth.
- Offline/server-down refresh failures do not clear local session state.
- Concurrent refresh attempts are deduplicated.
- Logout is deterministic locally even if the server call fails.
- Targeted auth tests pass.
- Debug build still succeeds.
