# Android Family A Foreground / Resume Session Validation Design

## Status

Proposed Android-only continuation design for Family A / Access & Session.

This design extends the existing Android `INF-147` session continuity chain so the app validates session truth not only on cold start and protected-request `401`, but also when the app returns to the foreground.

## Problem Statement

Current Android auth/session behavior already covers two important paths:

1. **Cold start bootstrap** via `SplashViewModel` + `CheckSessionUseCase`.
2. **Reactive protected-request refresh** via `AuthRefreshInterceptor` + `RefreshSingleFlightCoordinator`.

However, there is no explicit owner for **app foreground / resume transition**.

That means Android can return to an already-authenticated UI state after backgrounding without immediately revalidating whether the backend still accepts the session. In practice, session truth is only re-established when either:

- the app is relaunched into the splash bootstrap flow, or
- a later protected API call fails with `401` and the interceptor reacts.

This leaves a contract gap against the backend truth boundary for Family A.

## Contract Context

Family taxonomy and shared-context rules require:

- Backend is the source of truth for session validity, token lifecycle, revocation, inactivity, and final auth decisions.
- Android is a consumer and must orchestrate local runtime truthfully against backend responses.
- Android must not treat cached local user/session data as proof that the backend still accepts the session.
- Temporary transport failures such as offline, timeout, or server-down must not trigger false logout or misleading auth cleanup.

This design does **not** redefine backend auth/session semantics. It only ensures Android has an explicit foreground/resume validation owner that follows the existing contract.

## Goals

- Add an explicit Android-owned foreground/resume validation path for Family A.
- Ensure Android revalidates session truth when the app transitions from background to foreground.
- Reuse the existing auth/session contract handling and failure classification logic.
- Preserve the current silent refresh / forced re-auth semantics already established in the existing `INF-147` chain.
- Preserve truthful temporary-failure behavior for offline/server-down conditions.
- Keep the solution narrow, bounded, and Android-only.

## Non-Goals

- Do not rewrite the auth stack.
- Do not change backend error codes, refresh semantics, or inactivity rules.
- Do not move auth/session decisions into screen-specific UI code.
- Do not fan auth validation logic out across multiple screens.
- Do not broaden this work into unrelated auth cleanup or attendance/navigation refactors.

## Existing Architecture to Preserve

The current boundaries remain valid and should be preserved:

- `AuthRepositoryImpl`
  - performs login/refresh primitives
  - classifies refresh and profile-sync failures
  - persists local tokens/session state
- `AuthRefreshInterceptor`
  - attaches authorization headers
  - reacts to protected `401` responses
  - refreshes and retries once where allowed
  - triggers forced re-auth for terminal auth outcomes
- `RefreshSingleFlightCoordinator`
  - deduplicates concurrent refresh attempts
- `CheckSessionUseCase`
  - owns cold-start bootstrap validation
  - validates refresh + profile sync during splash bootstrap
- `SessionManager`
  - owns forced re-auth state and one-shot session-expiry handling
- `UserPreference`
  - stores access token, refresh token, user id, and last refresh timestamp

The foreground/resume solution should integrate with these boundaries, not replace them.

## Recommended Approach

Use a **process-level foreground observer** that triggers a **resume session validator** when the app transitions from background to foreground.

### Why this approach

This is the best fit for the contract requirement because:

- it directly matches the verification scope of “app resume / foreground transition”;
- it provides a single explicit owner for resume validation;
- it avoids spreading auth behavior into screens;
- it keeps auth/session semantics centralized in the domain/data stack;
- it remains narrow enough to implement and verify without a broad rewrite.

## Proposed Components

### 1. Foreground lifecycle owner

Add an app-level or process-level lifecycle observer whose only responsibility is to detect a **background -> foreground** transition.

Responsibilities:

- detect the transition to foreground;
- trigger resume session validation once per meaningful foreground event;
- avoid triggering during ordinary recomposition, screen navigation, or local UI state changes.

Non-responsibilities:

- deciding whether a session is valid;
- handling logout directly;
- performing UI navigation decisions.

### 2. Resume session validator

Add a focused validator/coordinator responsible for deciding whether and how to validate session state when the app resumes.

Responsibilities:

- inspect local auth/session availability;
- determine whether validation should run or be skipped;
- invoke the existing auth/session machinery through the correct contract-aware path;
- surface one of the expected outcomes:
  - valid / unchanged
  - refreshed / still valid
  - reauth required
  - temporary failure

Non-responsibilities:

- acting as a second full splash bootstrap flow;
- duplicating interceptor logic;
- owning screen-specific UX.

## Validation Trigger Rules

The resume validator should run only on a real **background -> foreground** transition.

It should **not** run for:

- recomposition;
- navigation between Compose destinations;
- every request;
- every activity callback that does not represent a meaningful return to foreground.

## Skip Rules

The validator should skip execution if any of the following conditions is true:

1. **No local access token**
   - there is no logged-in session to validate.

2. **No local refresh token**
   - the session is not refresh-capable.
   - the validator should avoid launching an operation that cannot complete meaningfully.

3. **Bootstrap session is already in progress**
   - cold-start bootstrap owns the session validation lane during splash.
   - resume validation must not race or double-validate against bootstrap.

4. **Resume validation is already in flight**
   - only one validation attempt should run per foreground event cluster.

5. **Debounce window still active**
   - rapid lifecycle bouncing should not create refresh storms or repeated validation.

These guards exist to enforce truthfulness *and* runtime stability.

## Validation Behavior

When the validator runs, it should follow the existing contract-aware session semantics.

### If session is still valid

- no forced re-auth is triggered;
- app continues normally.

### If access token is expired but refresh is valid

- session refresh should be performed through the existing auth path;
- app continues normally after successful refresh.

### If refresh is invalid, revoked, or inactive

- validator should route into the existing forced re-auth lane;
- Android should surface the same re-auth state handling already used elsewhere.

### If refresh fails due to offline / server-down / transport failure

- validator must not clear local session state;
- validator must not trigger false logout;
- result should be treated as **temporary failure / temporarily unverified session state**.

This preserves the contract rule that backend truth must be respected without inventing terminal logout from temporary network unavailability.

## Outcome Model

The validator should conceptually produce one of these outcomes:

- **Valid**
  - session remains usable, no new action needed.
- **Refreshed**
  - access token was renewed successfully.
- **ReauthRequired**
  - backend-auth/session state is terminal and user must log in again.
- **TemporaryFailure**
  - backend truth could not be re-established right now due to transport or temporary failure.

The exact type names can follow existing project naming conventions, but the outcome categories above should remain explicit.

## Interaction with Existing Components

### `CheckSessionUseCase`

`CheckSessionUseCase` remains the cold-start bootstrap owner.

The resume validator should not blindly reuse it as-is if doing so would re-run splash-specific behavior too heavily. Instead, the implementation should reuse the same contract semantics with a scope appropriate to foreground/resume validation.

This means:

- preserve bootstrap behavior for splash;
- create a narrower resume-specific entry point if needed;
- do not make foreground/resume validation a second full splash bootstrap.

### `AuthRefreshInterceptor`

Interceptor behavior remains unchanged:

- protected-request `401` still triggers reactive refresh;
- refresh still deduplicates via `RefreshSingleFlightCoordinator`;
- forced re-auth behavior for terminal auth codes remains intact.

Foreground validation complements this behavior. It does not replace it.

### `SessionManager`

`SessionManager` remains the source of forced re-auth state for UI handling.

Foreground/resume validation should route terminal outcomes into the same `SessionManager`-based re-auth state already used for Family A.

## UI / UX Principles

The solution should preserve a clear separation:

- lifecycle owner detects foreground transitions;
- validator decides contract outcomes;
- app shell reacts to forced re-auth or temporary failure state;
- screens do not become auth decision makers.

This avoids inconsistent per-screen behavior and keeps Family A session semantics centralized.

## Error Handling Rules

### Terminal auth outcomes

The following remain terminal and must force re-auth through the existing lane:

- `AUTH_REFRESH_TOKEN_INVALID`
- `AUTH_REFRESH_TOKEN_REVOKED`
- `AUTH_SESSION_INACTIVE`

### Temporary outcomes

The following remain non-terminal and must **not** force logout:

- offline / no network
- server unavailable
- timeout / transient transport failure
- malformed temporary payload classified as transient
- unknown temporary 5xx-style failures where the existing stack already classifies them as non-terminal

## Verification Strategy

This change is only closure-worthy if it is proven at three levels.

### 1. Unit / logic verification

Prove that:

- foreground validation runs on foreground transition;
- skip rules are honored;
- duplicate/in-flight triggers are suppressed;
- outcomes are mapped correctly to valid / refreshed / reauth required / temporary failure.

### 2. Integration / orchestration verification

Prove that:

- foreground validation does not conflict with splash bootstrap;
- one meaningful foreground event produces one validation attempt;
- terminal auth outcomes still flow into the existing re-auth lane;
- transport failures still preserve local session state.

### 3. Runtime verification

Prove the following scenarios on emulator/device against a reachable backend environment:

1. app resumes with expired access token and valid refresh token;
2. app resumes with invalid or revoked refresh token;
3. app resumes with inactive session / inactivity denial;
4. app resumes while offline or while backend is unavailable.

## Required Baseline Commands

The existing Android verification baseline remains mandatory:

```bash
./gradlew app:testDebugUnitTest
./gradlew app:lint
./gradlew app:assembleDebug
```

If the local environment cannot execute these successfully, the work must remain `Needs Verification` rather than being overstated as complete.

## Risks

### 1. Duplicate-trigger risk

A poorly scoped lifecycle hook could spam refresh attempts or create repeated re-auth handling.

Mitigation:

- explicit in-flight guard;
- debounce window;
- respect bootstrap ownership.

### 2. Bootstrap collision risk

Foreground validation could race against splash bootstrap if not guarded.

Mitigation:

- skip while bootstrap is in progress;
- keep cold-start ownership with `CheckSessionUseCase` / splash flow.

### 3. False logout risk

A naive resume validator could treat transport failure as session invalidation.

Mitigation:

- preserve the existing distinction between terminal auth failure and temporary transport failure.

### 4. Scope-creep risk

This work could accidentally become a broad auth refactor.

Mitigation:

- keep implementation bounded to foreground owner + resume validation path + tests + evidence updates.

## Definition of Done for This Design

This foreground/resume contract-alignment work is done only when:

- Android has an explicit foreground/resume validation owner;
- the validator follows existing contract-aware auth/session semantics;
- terminal outcomes trigger existing forced re-auth handling;
- temporary failures do not clear local session;
- unit/integration proof exists;
- runtime evidence exists for the resume scenarios;
- docs and closure notes are updated truthfully.

Without runtime proof, the honest status remains:

- implementation aligned
- runtime closure still `Needs Verification`

## Files Likely Affected

The eventual implementation should remain tightly scoped. Likely touched areas:

- app-level or activity-level lifecycle owner wiring
- a new or updated resume validator / coordinator in auth/domain space
- tests for foreground/resume validation and guard behavior
- runtime evidence / closure notes for `INF-147`

It should avoid broad changes to unrelated screens or unrelated product families.
