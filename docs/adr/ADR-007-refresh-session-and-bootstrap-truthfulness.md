# ADR-007: Refresh session and bootstrap truthfulness for Android

- Status: Accepted
- Date: 2026-04-22

## Context

Android previously treated `401` as an immediate logout signal and allowed bootstrap session continuity to rely on local state more than backend confirmation. That behavior was no longer acceptable once backend-authored refresh-session semantics were introduced.

This change is governance-significant because it touches:
- session/token contract interpretation
- source-of-truth handling between Android and backend
- retry/idempotency-sensitive networking behavior
- bootstrap behavior at cold start and app resume

## Decision

### 1. Backend remains the source of truth
Android does not treat cached local user/profile state as proof that a backend session is still valid. Bootstrap only enters the authenticated flow after backend validity is re-established.

### 2. Android uses an explicit mobile refresh contract
Android consumes the backend refresh endpoint through:
- `POST /api/auth/refresh`
- header: `X-Client-Type: android`
- request body containing the stored `refresh_token`

Android does not rely on an implicit cookie-only assumption for the mobile refresh path.

### 3. Refresh-token presence is a session invariant
A login response must contain a usable refresh token for Android to persist a logged-in session. Android does not store a session that cannot satisfy the refresh contract.

### 4. Refresh outcomes are classified explicitly
Android classifies refresh into four outcomes:
- `Success`
- `ReAuthRequired.InvalidOrRevoked`
- `ReAuthRequired.InactivityExceeded`
- `TemporaryFailure`

This classification is used to keep auth/session behavior truthful:
- `InvalidOrRevoked` and `InactivityExceeded` trigger cleanup and full re-auth.
- `TemporaryFailure` preserves local session state and must not be treated as auth invalidity.

### 5. Protected request refresh uses single-flight orchestration
For protected Android API requests:
1. attach the current bearer token
2. execute the request
3. if the response is `401` on a protected non-auth endpoint, refresh once through a shared single-flight coordinator
4. if refresh succeeds, retry the original request exactly once with the latest access token
5. if refresh requires re-auth, clear session and trigger the forced re-auth path
6. if refresh fails temporarily, do not clear session and do not trigger forced re-auth

Android excludes login, logout, and refresh endpoints from bearer injection and refresh recursion.

### 6. Forced re-auth side effects are de-duplicated per wave
Concurrent requests that share the same `ReAuthRequired` refresh result must not fan out repeated logout/session-expired side effects. Android guards forced re-auth handling so one wave of concurrent 401s triggers logout/session-expired once, and the guard resets after the UI acknowledges the session-expired state.

### 7. Bootstrap failure handling is bounded and truthful
At bootstrap:
- unauthorized sync failure may attempt refresh
- non-auth sync failure must not consume refresh-session state
- if refresh succeeds but the follow-up backend sync is still unauthorized, Android treats that as re-auth required
- post-sync local side-effect failures (for example face-embedding generation) are treated as temporary bootstrap failures, not as proof that backend session is invalid

Android uses a bounded temporary bootstrap failure state with user actions to retry or go to login. It does not remain in indefinite splash loading and does not force logout for temporary bootstrap failure.

## Consequences

### Positive
- Android session continuity now follows backend truth more closely.
- Temporary transport/server failures no longer masquerade as invalid session.
- Concurrent 401 handling avoids refresh storms.
- Bootstrap no longer silently trusts cached local state as authenticated truth.

### Trade-offs
- Android now depends on a stricter auth/session contract from backend.
- Login is stricter because refresh-token availability is mandatory for a persisted session.
- Temporary bootstrap failures surface a bounded retry/login UX instead of silently entering the app.

## Verification expectations

The following scenarios must remain verifiable:
- login -> access token expired -> refresh succeeds
- active protected request -> refresh succeeds -> retry once
- invalid/revoked refresh -> cleanup + login
- inactivity `> 48 jam` -> full re-auth required
- temporary refresh failure -> preserve local session, no forced logout
- concurrent 401s -> one refresh wave, no refresh storm
- bootstrap temporary failure -> bounded retry/login state, not cached-user continuity and not forced logout
