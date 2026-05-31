# INF-147 Follow-up — Android bootstrap/logout/permission-session contract

## Scope

This follow-up stays strictly inside Android startup auth continuity:
- one bootstrap cycle per launch
- one terminal logout side effect per failure cycle
- notification permission prompt delayed until session truth is known

## Explicit non-scope

This follow-up does not absorb:
- INF-97 face bootstrap semantics
- INF-96 location/background permission decoupling
- INF-66 attendance orchestration hardening

## Runtime expectation after implementation

- clean launch with invalid session shows one splash-owned terminal path to login without the global session-expired dialog
- `/api/auth/logout` occurs at most once during startup failure
- POST_NOTIFICATIONS prompt does not overlap the invalid-session flow
- offline startup follows the temporary/bootstrap-unavailable path without crash while preserving local auth state
