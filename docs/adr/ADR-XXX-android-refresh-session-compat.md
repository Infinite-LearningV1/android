# ADR-XXX: Android Refresh Session Compatibility

## Status

Accepted for INF-147 implementation; runtime smoke evidence is tracked separately in `docs/auth-runtime-evidence/RUN_2026-05-30.md`.

## Context

INF-145 changes the backend auth model for non-web consumers. Android must use a JSON refresh token instead of cookie-backed web session behavior. Backend also limits mobile sessions to one active mobile session per user and emits explicit auth error codes.

## Decision

Android will align to the INF-145 mobile contract:

1. Every authenticated/mobile auth request sends `X-Client-Type: mobile`.
2. Login reads `refresh_token` from the JSON response body.
3. Refresh uses `POST /api/auth/refresh` with body `{ "refresh_token": "..." }`.
4. Refresh attempts are single-flight so concurrent 401 responses share one refresh call.
5. `AUTH_ACCESS_TOKEN_EXPIRED` triggers refresh and protected request replay.
6. `AUTH_REFRESH_TOKEN_INVALID`, `AUTH_REFRESH_TOKEN_REVOKED`, and `AUTH_SESSION_INACTIVE` trigger forced re-auth.
7. Transport failure while refreshing does not cause a false logout; cached session can remain usable until connectivity returns.
8. Logout sends `refresh_token` body when available so backend can revoke the session even if access token is expired.

## Reauth UX Reasons

Android maps backend/session outcomes to `SessionManager.ReauthReason`:

- `AUTH_SESSION_INACTIVE` → `INACTIVITY_EXPIRED`
- `AUTH_REFRESH_TOKEN_INVALID` → `REFRESH_INVALID`
- `AUTH_REFRESH_TOKEN_REVOKED` → `REFRESH_REVOKED`
- refresh transport/offline path → no forced logout; offline banner can use `NETWORK_OFFLINE_AT_REFRESH`
- unknown legacy session expiry → `UNKNOWN`

## Consequences

- Android stores refresh tokens in DataStore. Token values must never be logged.
- Interceptor logic must parse 401 response `code` before deciding refresh vs forced re-auth.
- Tests cover model parsing, persistence, repository refresh classification, single-flight refresh, interceptor retry, session manager reason state, bootstrap behavior, login banner mapping, and logout fallback.
- Runtime evidence must redact token, email, full name, and identifiers before being committed.

## References

- INF-145
- INF-147
- INF-148
- INF-149
- INF-152
