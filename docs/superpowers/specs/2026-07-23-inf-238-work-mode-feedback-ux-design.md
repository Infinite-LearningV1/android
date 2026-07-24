# INF-238 Work Mode, Target Location, WFA Recommendation, and Feedback UX

**Status:** Design approved

**Linear:** INF-238

**GitHub:** #96

**Branch:** `codex/inf-238-work-mode-feedback-ux`

**Delivery:** One worktree, one branch, one pull request

**Architecture prerequisite included:** Minimum authoritative-target subset from INF-242
**Supersedes for this delivery:** `docs/superpowers/plans/2026-07-09-inf-238-work-mode-target-location-hardening.md`

## Goal

Redesign the existing Attendance Work Mode experience around an explicit authoritative target, compact WFA discovery, and one clear action per state. In the same pull request, finish the shared state-feedback UX and replace login/logout result popups with non-modal feedback.

The result must remain truthful to backend authority:

```text
WFO target -> status-today.active_location
WFH target -> /api/auth/me admin-provisioned profile location
WFA target -> approved booking for the Attendance date
```

WFA search results, recommendations, preview markers, and user selection are discovery/request state. They never become an Attendance target automatically.

## Problem

The current Attendance state allows the same target concept to be stored in several fields, including `targetLocation`, `wfoLocation`, `wfhLocation`, `approvedWfaLocation`, `selectedTargetLocation`, marker copies, recommendation selection, and picked-location state. The current `SelectedTargetLocation` also combines a nullable location, a mutable availability flag, and user-facing copy in the domain model. Async mode changes can therefore leave contradictory target, marker, and CTA state.

The existing presentation also has inconsistent feedback behavior:

- inline titles do not have the required strong emphasis;
- warning and error inline alerts can remain indefinitely even when they are transient;
- login success and failure are presented as status dialogs;
- login loading uses another modal dialog;
- logout uses separate confirmation, loading, and result dialogs;
- a manual logout can arrive at Login while a stale `SessionManager.reauthReason` is still observable, causing session-expiry copy to appear for an intentional logout.

## Locked Scope

This pull request includes:

1. the minimum INF-242 contract required to implement INF-238 correctly;
2. a cohesive Attendance preparation state;
3. the Work Mode, authoritative Target Location, and WFA recommendation redesign;
4. provider-neutral map projection and latest-selection-wins behavior;
5. shared state-feedback typography, surface, timer, and dismissal rules;
6. non-modal login/logout result feedback and intentional-logout session cleanup;
7. tests and runtime evidence for the affected flows.

The pull request does not claim full INF-242 completion. INF-242 concerns outside the direct INF-238 path remain separate.

## Non-Goals

- rewriting the existing draggable bottom-sheet mechanics;
- changing backend Attendance or WFA endpoints;
- redesigning Attendance permissions, Face Verification, or geofence lifecycle;
- changing checkout to require Work Mode selection again;
- employee editing or map-picking an authoritative WFH target;
- allowing WFA recommendation selection to satisfy Attendance readiness;
- adding venue photos, a new image provider, or assumed backend fields;
- introducing Mapbox or Google Maps SDK models into domain or ViewModel contracts;
- performing another map-provider migration;
- completing unrelated auth/session backlog work.

## Architecture Decision

Use the **Cohesive Preparation State** approach. Keep the existing `AttendanceViewModel` lifecycle owner, but replace parallel target/preparation writes with one nested state:

```text
AttendanceScreenState
└── preparation: AttendancePreparationState
    ├── selectedMode
    ├── targetResolution
    ├── currentLocation
    ├── rangeStatus
    ├── wfaDiscovery
    └── eligibility
```

The existing ViewModel remains the screen owner to avoid introducing a second lifecycle and duplicate API collection. Pure domain use cases decide target identity, range, and eligibility. Presentation mappers produce render-only state, map markers, copy, and actions.

No consumer may maintain another mutable copy of the authoritative target. Temporary migration adapters are allowed only while converting an existing call site within this pull request; no new writes may be added to legacy fields.

## Authoritative Target Contract

### Source and identity

Introduce project-owned types equivalent to:

```kotlin
@JvmInline
value class TargetLocationId(val value: String)

enum class TargetLocationSource {
    STATUS_TODAY,
    ADMIN_PROFILE,
    APPROVED_WFA_BOOKING,
}

data class ApprovedWfaTargetContext(
    val bookingId: Int,
    val scheduleDate: String,
)

data class AuthoritativeTargetLocation(
    val targetId: TargetLocationId,
    val mode: WorkMode,
    val source: TargetLocationSource,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val displayName: String,
    val approvedWfaContext: ApprovedWfaTargetContext? = null,
)
```

A resolved target always has a valid coordinate, a positive radius, a stable target ID, and an explicit source. It cannot express availability through a second Boolean.

Approved WFA metadata is preserved until check-in handoff. It must not be discarded by converting a booking to a generic `Location` too early.

### Resolution state

```kotlin
sealed interface TargetLocationResolution {
    data class Resolving(val mode: WorkMode) : TargetLocationResolution

    data class Resolved(
        val target: AuthoritativeTargetLocation,
    ) : TargetLocationResolution

    data class Unavailable(
        val mode: WorkMode,
        val reason: TargetUnavailableReason,
        val recovery: TargetRecoveryAction,
    ) : TargetLocationResolution

    data class Failed(
        val mode: WorkMode,
        val failure: TargetResolutionFailure,
    ) : TargetLocationResolution
}
```

Typed unavailable reasons distinguish normal product state from technical failure:

```text
WFO not assigned
WFH profile contract violation
WFA not requested
WFA pending
WFA rejected
WFA approval missing for Attendance date
```

Typed failures cover status/profile/booking refresh failure, invalid coordinate, and invalid radius. Domain emits types only; localized copy, icon, semantic color, and CTA label remain presentation-owned.

### Source rules

#### WFO

- Resolve only from `status-today.active_location`.
- Show the assigned office identity, radius, and current range evidence.
- Do not expose search, editing, or map picking.

#### WFH

- Resolve only from the authenticated `/api/auth/me` profile mapping.
- Treat the target as mandatory and admin-provisioned.
- Missing or invalid data is a profile contract/synchronization failure.
- Do not expose employee editing, search, or map picking.

#### WFA

- Resolve only from an approved booking for the Attendance date.
- Preserve booking ID and schedule date.
- `Not requested`, `Pending`, and `Rejected` are typed unavailable states.
- A recommendation or selected preview cannot be converted directly into `AuthoritativeTargetLocation`.

## Separation of Concerns

Target identity, range, and readiness are evaluated separately:

```text
ResolveAuthoritativeTargetLocationUseCase
-> which target is authoritative?

EvaluateTargetRangeUseCase
-> inside, outside, unknown, or stale current location?

EvaluateAttendancePreparationUseCase
-> which single action or recovery is available?
```

A current-location failure never removes or replaces a resolved target. Checkout continues to use the active backend Attendance session and does not re-resolve Work Mode.

## Latest-Selection-Wins

Mode-dependent resolution and WFA recommendation loading use cancellation (`flatMapLatest` or an owned mode-resolution job) plus request identity protection.

Required behavior:

```text
select WFA
-> WFA target and recommendation requests start

select WFO before WFA completes
-> WFA jobs are cancelled
-> any late WFA result fails the request-identity check
-> WFO remains selected and authoritative
```

Recommendation selection and marker preview may update only while WFA is the latest selected mode.

## Map Projection

Map UI is derived from preparation state rather than stored as another target copy:

```text
target resolution
+ current location
+ WFA discovery/preview
-> AttendanceMapUiMapper
-> MapUiState
```

Minimum marker roles:

```text
CURRENT_LOCATION
AUTHORITATIVE_TARGET
WFA_RECOMMENDATION
SEARCH_PREVIEW
```

`AuthoritativeTargetLocation` maps to one authoritative marker and one radius circle. Recommendation row selection and recommendation marker selection remain synchronized. Search and recommendation marker clicks update preview state only.

## Attendance UX

### Screen hierarchy

Preserve the current Attendance app bar and bottom-sheet mechanics:

```text
Existing Attendance App Bar
Map with authoritative marker/radius
Existing draggable bottom sheet
├── Work Mode heading
├── WFO / WFH / WFA option cards
├── authoritative Target Location summary
├── WFA discovery when relevant
└── one primary action
```

The approved visual reference is used for hierarchy and density, not for authority semantics.

### Work Mode option cards

Cards reuse the existing Infinite design system. They use no gradients. A selected card is communicated through border, light semantic tint, check indicator, text, and accessibility `selected` semantics.

```text
WFO
title: Work From Office
supporting: assigned office location
evidence: office name and geofence/radius

WFH
title: Work From Home
supporting: location assigned by admin
evidence: home geofence/radius

WFA
title: Work From Anywhere
supporting: approved booking required
evidence: current booking lifecycle
```

### Authoritative Target summary

The summary displays:

- target display name;
- explicit source;
- radius;
- current distance when available;
- inside, outside, or unknown range;
- WFA booking status/date when relevant.

Authoritative evidence and recommendation preview are visually and structurally separate.

### WFA discovery

Search Location / Pick on map appears only when WFA is selected. It opens recommendation discovery and preview selection. The selected recommendation becomes booking draft input, not an Attendance target.

The compact recommendation row renders only typed backend-supported fields:

```text
category icon
name
category + formatted distance
WFA suitability score + label
selection indicator
```

There is no venue photo and no public-rating interpretation. The row and its `WFA_RECOMMENDATION` marker share one stable selection key.

### Primary action hierarchy

Exactly one primary action is visible per state:

```text
WFO/WFH ready             -> Lanjut ke Verifikasi Wajah
WFA not requested         -> Ajukan WFA
WFA pending               -> Lihat status permintaan
WFA rejected              -> Lihat status permintaan
WFA approved and ready    -> Lanjut ke Verifikasi Wajah
Outside range             -> Fokus ke lokasi target or refresh location
WFH profile failure       -> Muat ulang
Recommendation failure   -> Coba lagi
```

Search/Pick location is a WFA-only secondary discovery action. A disabled Attendance CTA must not compete with a recovery CTA.

## Error and Recovery Matrix

```text
WFO unavailable          -> refresh status
WFH invalid/missing      -> refresh profile + contact admin
WFA not requested        -> open WFA booking
WFA pending              -> open WFA requests
WFA rejected             -> open WFA requests; any reapply action stays in that flow
Recommendation failed    -> retry discovery
Current location failed  -> refresh current location; keep target identity
Outside range            -> focus target / refresh current location
```

State-bearing recovery surfaces remain visible until the underlying state changes or the user successfully performs recovery.

## Shared Feedback Design System

### Visual contract

`InfiniteInlineAlert`, `InfiniteSnackbar`, and persistent recovery surfaces remain one component family:

- use the same established surface token as `InfiniteTopBar`;
- use no gradient;
- use semantic color only for border, progress timer, icon container, and action;
- use existing spacing, shape, elevation, motion, and touch-target tokens;
- keep supporting copy on the existing Infinite Track type scale;
- define the inline title centrally from the existing `body1` metrics with Bold weight;
- prohibit per-screen font size, line height, or weight overrides.

Feature compositions such as `WorkModeOptionCard`, `TargetLocationSummary`, and `WfaRecommendationOption` may compose existing primitives. They do not introduce a second global design system.

### Transient inline feedback

```text
Success / Info    -> 4 seconds
Warning / Error   -> 8 seconds
```

Every transient inline alert has both a dismiss X and a visible progress timer. The timer runs even when X is present. Dismissal is emitted exactly once whether initiated by X, timeout, or state replacement.

Accessibility-recommended timeout adjustment is applied for users who require additional reading/action time. Entry, exit, timer, and selected-state transitions use Material 3 motion through the existing `InfiniteMotion` tokens.

### Persistent recovery feedback

A recovery surface that represents a currently active blocker has no X and no timer. It remains until state recovery. This prevents a real blocker from disappearing while the condition still exists.

## Authentication Feedback UX

### Login

```text
Idle       -> normal Login form
Loading    -> progress in Login button; inputs/actions protected from duplicate submit
Failure    -> transient inline error for 8 seconds
Success    -> navigate directly to Home; root snackbar for 4 seconds
```

Remove the login loading dialog and `LoginStatusDialog`. Remove the unconditional `Complete your Profile` success popup. Profile completeness belongs to its own profile/status flow.

### Logout

The destructive confirmation dialog remains. It is the only logout modal.

```text
Confirming                    -> confirmation dialog
Submitting                    -> progress in confirm action; no second loading dialog
Remote success + local clear  -> Login + success snackbar for 4 seconds
Remote failure + local clear  -> Login + warning snackbar for 8 seconds
```

Remote logout is best-effort; local authenticated runtime cleanup remains authoritative for ending the device session.

The logout use case exposes a typed outcome so presentation does not infer it from exception text:

```kotlin
sealed interface LogoutOutcome {
    data object Success : LogoutOutcome
    data object SuccessWithRemoteWarning : LogoutOutcome
    data class LocalCleanupFailed(val cause: Throwable) : LogoutOutcome
}
```

`SuccessWithRemoteWarning` navigates to Login because local cleanup succeeded. `LocalCleanupFailed` does not claim logout success or navigate into an unauthenticated flow; Profile renders a persistent recovery state and allows retry.

### Cross-navigation feedback

Login/logout result feedback is represented as a semantic one-shot effect. A root-level `InfiniteSnackbarHost` renders it after navigation. ViewModels do not own `SnackbarHostState`, navigation controllers, or localized snackbar strings.

The effect is replay-safe and consumed once. Recomposition and back-stack restoration must not show the same success message again.

### Intentional logout versus forced re-auth

The current ownership gap is that Login observes any non-null `SessionManager.reauthReason`, while intentional logout does not guarantee that stale or interceptor-triggered re-auth state is cleared before Login renders.

The user-initiated logout path must explicitly establish an intentional-logout boundary and clear `sessionExpired`, `reauthReason`, and single-flight expiry state after local cleanup and before navigation feedback is rendered. A manual logout must never display `Sesi perlu login ulang`.

Forced re-auth remains separate. A real inactive, invalid, or revoked session may still navigate to Login and show the corresponding re-auth feedback.

## Responsive and Accessibility Rules

- All interactive targets remain at least 48 dp.
- Work Mode selection is announced through semantics, not color alone.
- Recommendation selection and suitability score have stable content descriptions.
- Large fonts may wrap supporting text but must not hide the target name, status, or primary action.
- Narrow screens keep one primary action visible and avoid horizontally clipped labels.
- Timed feedback respects accessibility-recommended timeout extension.
- Motion must preserve usability when system animation scale is reduced or disabled.

## Testing Strategy

### Domain tests

- WFO resolves from `status-today.active_location`.
- WFH resolves from `/api/auth/me` admin profile mapping.
- missing or invalid WFH is a typed profile contract failure.
- WFA not requested, pending, rejected, approved, wrong date, invalid coordinate, and invalid radius are covered.
- approved WFA preserves booking ID and schedule date.
- recommendation selection cannot become an authoritative target.
- range failure does not mutate target identity.
- checkout does not re-resolve Work Mode.

### ViewModel tests

- rapid WFA-to-WFO and WFH-to-WFO switching ignores stale results;
- WFA recommendation loading is cancelled when leaving WFA;
- row and marker selection update one preview state;
- duplicate primary taps emit one navigation effect;
- each preparation state exposes one primary action;
- target, range, and WFA lifecycle changes produce consistent state.

### Mapper tests

- authoritative target maps to `AUTHORITATIVE_TARGET` plus radius;
- current location maps independently;
- WFA recommendation and search preview remain separate from the target;
- backend WFA fields map without image or public-rating assumptions;
- presentation copy maps from typed reasons/failures.

### Compose and feedback tests

- three Work Mode cards render with one selected semantic;
- WFO/WFH contain no search or edit action;
- WFA discovery states cover loading, content, empty, failure, and selected;
- target summary covers ready, unavailable, range, and booking evidence;
- one primary action renders on narrow screen and large font;
- inline title uses the central Bold token;
- transient alerts show timer and X together;
- success/info dismiss at 4 seconds and warning/error at 8 seconds;
- persistent recovery has no timer or X;
- timeout and manual dismissal call `onDismiss` once.

### Auth tests

- Login no longer renders loading or result status dialogs;
- login failure renders timed inline feedback;
- login success navigates once and emits one Home snackbar;
- logout keeps one confirmation modal and no secondary modal;
- logout success and remote-warning feedback render after navigation;
- local-cleanup failure remains on Profile with persistent recovery;
- manual logout clears stale re-auth state;
- forced re-auth still shows the correct reason;
- snackbar effects are not replayed after recomposition.

### Build and runtime gates

Run:

```text
app:testDebugUnitTest
app:compileDebugAndroidTestKotlin
app:lintDebug
app:assembleDebug
```

Runtime evidence on emulator/device must cover:

- WFO ready, unavailable, inside range, and outside range;
- WFH ready from `/me` and invalid profile recovery;
- WFA recommendation loading/content/empty/failure and synchronized preview;
- WFA not requested, pending, rejected, and approved target;
- rapid mode switching;
- login failure and success navigation feedback;
- logout success, remote-warning behavior, and absence of stale re-auth copy;
- timed inline alert auto-dismiss and manual X dismissal;
- large-font and narrow-screen presentation.

## Acceptance Criteria

### Product and contract

- [ ] WFO, WFH, and WFA use only their locked authoritative sources.
- [ ] A resolved target has explicit source, stable identity, valid coordinate, and positive radius.
- [ ] Approved WFA target preserves booking ID and date.
- [ ] Recommendation/search selection cannot satisfy Attendance target readiness.
- [ ] Checkout uses the active backend session.

### State ownership

- [ ] One cohesive preparation state owns target resolution, current location, range, WFA discovery, and eligibility.
- [ ] Mutable duplicate target and marker fields are removed from the completed flow.
- [ ] Map UI is a projection.
- [ ] Latest-selection-wins prevents stale async overwrite.
- [ ] Domain contains no localized recovery copy.

### UX

- [ ] Existing Attendance app bar and bottom-sheet mechanics remain unchanged.
- [ ] Work Mode cards and Target summary follow the approved visual hierarchy and existing design system.
- [ ] WFA Search/Pick is visible only in WFA and remains discovery/request state.
- [ ] Recommendation rows use typed backend fields, category icons, and no venue photos.
- [ ] Exactly one primary action is visible for every preparation state.
- [ ] Error, loading, empty, content, selected, large-font, and narrow-screen states are covered.

### Feedback and auth

- [ ] Inline titles are Bold through the shared typography token.
- [ ] Transient inline alerts show X and progress timer together and dismiss after 4/8 seconds.
- [ ] Active recovery surfaces remain persistent without X or timer.
- [ ] Login and logout results no longer use status dialogs.
- [ ] Logout confirmation remains modal; its loading/result dialogs are removed.
- [ ] Login/logout feedback survives navigation and is consumed once.
- [ ] Manual logout never displays session-expiry copy.

### Verification

- [ ] Required unit, mapper, ViewModel, and Compose tests pass.
- [ ] Android test sources compile.
- [ ] Lint and debug assembly pass or unrelated baseline findings are documented.
- [ ] Runtime evidence covers the locked Work Mode, WFA, feedback, login, and logout matrix.

## Baseline Evidence

The isolated worktree was created from `origin/develop` at `5d9278b` (`Align state status typography and surfaces (#98)`). Before writing this specification:

```text
./gradlew.bat --no-daemon app:testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 2m 13s
```

Existing compiler and deprecation warnings remain baseline findings and are not expanded into this pull request unless a touched file requires a narrowly-scoped correction.
