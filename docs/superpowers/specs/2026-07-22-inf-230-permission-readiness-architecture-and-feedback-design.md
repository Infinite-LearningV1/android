# INF-230 — Attendance Permission Readiness Architecture and Feedback Design

Date: 2026-07-22

Issue: `INF-230`

Branch: `codex/inf-230-permission-readiness-refinement`

Worktree: `E:\skrisi\android\.worktrees\inf-230-permission-readiness-refinement`

Base: `origin/develop` at `2dbe36c`

## Summary

INF-230 refines the existing Attendance Permission Readiness flow in four phases. It moves platform inspection and readiness policy out of Compose, introduces a lifecycle-safe ViewModel state/effect contract, maps the screen to reusable Infinite components, and removes duplicate normal permission ownership only after runtime verification.

The operator expanded the visual scope on 2026-07-22. The work also establishes a unified Android-safe glass treatment for the existing state/status component family and adds a designed Infinite snackbar. This is a reusable design-system foundation, but call-site migration remains bounded to Permission Readiness and directly affected Attendance feedback. Broad application migration remains coordinated with INF-219 and INF-222.

This spec supersedes the architecture and visual ownership described in `docs/superpowers/specs/2026-07-09-inf-230-attendance-permission-readiness.md`. The older document remains historical evidence for the first shipped gate.

## Goals

1. Make Android OS and device settings the source of truth for actual access.
2. Make domain policy the source of truth for Attendance eligibility.
3. Make immutable ViewModel state the source of truth for persistent UI state.
4. Keep Activity Result launchers, rationale inspection, Settings intents, and navigation in the presentation Route.
5. Make the Compose Screen stateless.
6. Preserve required-versus-optional permission semantics and navigation placement.
7. Provide a coherent feedback taxonomy instead of representing every outcome as an inline card or modal.
8. Restyle the reusable state/status family with portable glass effects that work from Android API 26.
9. Preserve downstream fallbacks until device/emulator evidence proves that normal permission ownership was migrated safely.

## Non-goals

- No backend, DTO, API, attendance-submission, face-match, or reporting contract changes.
- No Work Mode Selection or Face Scanner redesign.
- No navigation graph ownership refactor from INF-209.
- No required-versus-optional permission reclassification.
- No change to the required permission request order.
- No automatic request of all native permissions on screen entry.
- No DataStore or Room persistence for granted permission state.
- No Activity, Context, NavController, Android permission string, Compose type, icon, color, or copy in domain.
- No global permission manager or global permission ViewModel.
- No mass migration of every state/status call site in the application.
- No web-style backdrop blur dependency or GPU-heavy blur requirement.
- No new global icon registry.

## Product Contract

The existing placement remains:

```text
Attendance entry
→ Permission Readiness
→ Work Mode Selection: WFO / WFH / WFA
→ Attendance flow
```

Required for basic/manual Attendance:

```text
foreground precise location
AND camera access
AND device location setting enabled
```

Optional or recommended:

```text
notification permission
background location permission
```

The hard invariant is:

```text
canEnterAttendance =
    preciseLocationReady
    && cameraReady
    && deviceLocationEnabled
```

Notification and background location never participate in this invariant. Their denial may degrade reminders or active geofence monitoring, but manual attendance remains available when the three required conditions are ready.

Fine and coarse location are not equivalent:

```text
fine granted        → precise location Ready
coarse only granted → precise location ActionRequired
neither granted     → permission required or denied
```

## Approved Architecture

The selected approach is phased replacement behind the existing stable route and UI:

```text
Android OS / device settings
↓
AndroidAttendancePermissionDataSource
↓
AttendancePermissionRepositoryImpl
↓
AttendancePermissionRepository
↓
Observe / Refresh / Resolve use cases
↓
AttendancePermissionReadinessViewModel
├── StateFlow<AttendancePermissionReadinessUiState>
└── Flow<AttendancePermissionReadinessEffect>
↓
AttendancePermissionReadinessRoute
├── Activity Result launchers
├── rationale inspection
├── Settings intents
├── Infinite Snackbar host
└── navigation callbacks
↓
stateless AttendancePermissionReadinessScreen
```

The route and screen replace the current implementation incrementally. The existing route name and navigation placement remain stable. Attendance Screen, Attendance Map, Face Scanner, and Geofence Manager keep defensive behavior through Phase 4.

### Ownership boundaries

| Unit | Owns | Must not own |
| --- | --- | --- |
| Android data source | Reading permission grants, Android version capability, and device-location state | Display copy, request order, navigation, or attendance eligibility |
| Repository implementation | Mapping platform snapshots and publishing refreshed readiness input | Launching permission dialogs or Settings |
| Domain policy/use cases | Required/optional classification, readiness invariant, and next required action | Android APIs, UI semantics, or display models |
| ViewModel | Persistent state, event handling, refresh coordination, and one-time effects | Context, Activity, NavController, launchers, or Compose values |
| Route | Lifecycle collection, Activity Result launchers, rationale inspection, Settings intents, snackbar display, and navigation callbacks | Long-lived permission truth or business eligibility |
| Screen | Rendering immutable UI state and emitting events | Platform reads, launchers, local permission truth, or navigation |

## Domain Model

Minimum access types:

```kotlin
enum class AttendanceAccess {
    PRECISE_LOCATION,
    CAMERA,
    DEVICE_LOCATION,
    NOTIFICATION,
    BACKGROUND_LOCATION
}
```

Minimum requirement and status vocabulary:

```text
AttendanceAccessRequirement: REQUIRED | OPTIONAL

AttendanceAccessStatus:
Ready
ActionRequired
Denied
PermanentlyDenied
Degraded
NotRequiredOnDevice
DeviceLocationDisabled
```

The domain readiness aggregate contains typed entries, required-ready count, required-total count, `canEnterAttendance`, optional capability summary, and a typed inspection failure when applicable.

The repository contract is:

```kotlin
interface AttendancePermissionRepository {
    fun observeReadiness(): Flow<AttendancePermissionReadiness>
    suspend fun refreshReadiness()
}
```

Minimum use cases:

```text
ObserveAttendancePermissionReadinessUseCase
RefreshAttendancePermissionReadinessUseCase
ResolveNextAttendancePermissionActionUseCase
```

Required action resolution remains:

```text
Precise location
→ Camera
→ Device location setting
→ Continue to Work Mode
```

Optional setup is available through optional rows after required access is ready. It is not inserted into the required primary-CTA sequence.

## Platform Inspection and Permission Classification

`AndroidAttendancePermissionDataSource` may inspect:

- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` independently;
- camera access;
- notification access and whether the Android version requires it;
- background-location access and whether the Android version requires it;
- device location setting and relevant platform capability.

Permission callback booleans are transient signals, not long-lived truth. Every permission callback or return from Settings triggers a repository refresh and a fresh Android state read.

Permanently denied cannot be inferred from `checkSelfPermission` alone. It requires:

```text
not granted
AND requested in the current known flow
AND shouldShowRequestPermissionRationale == false
```

Because rationale inspection requires Activity context, the Route performs that inspection and reports a typed outcome to the ViewModel. Activity references do not enter repository, data, domain, or ViewModel layers.

## Presentation Contract

The target screen contract is:

```kotlin
@Composable
fun AttendancePermissionReadinessScreen(
    uiState: AttendancePermissionReadinessUiState,
    onEvent: (AttendancePermissionReadinessEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

The screen accepts typed `PermissionItemUiModel` entries instead of primitive boolean parameter lists. Presentation mapping owns copy, icons, `InfiniteSemantic`, actions, and accessibility descriptions.

Persistent state represents at least:

```text
loading
required items
optional items
required-ready count
required-total count
can continue
primary action
contextual guidance
recoverable inspection failure
refreshing state without destructive content reset
```

Minimum events:

```text
ScreenResumed
PrimaryActionClicked
PermissionItemClicked(access)
PermissionResultReceived(access, outcome)
ReturnedFromSettings
RetryRefresh
```

Minimum one-time effects:

```text
RequestPreciseLocation
RequestCamera
RequestNotification
RequestBackgroundLocation
OpenApplicationSettings
OpenDeviceLocationSettings
NavigateToWorkMode
ShowSnackbar(feedback)
```

Effects are not stored as persistent state and `savedStateHandle` is not used as a free-form event bus. Navigation and native requests must be idempotent against rapid/repeated events.

Refresh occurs on initial observation, screen `ON_RESUME`, native permission callback, return from application Settings, return from device-location Settings, and manual Retry.

## Screen Composition

The hierarchy is:

```text
InfiniteTopBar
Hero and purpose
Single required-readiness progress summary
Required access section
Optional access section
Contextual guidance or recovery
Primary next-action CTA
```

Progress is shown once and counts required access only, for example `2/3 akses wajib siap`. Optional access never changes that denominator.

Reusable mapping:

| Screen role | Component |
| --- | --- |
| Page navigation | `InfiniteTopBar` |
| Hero/progress surface | `InfiniteCard` / `InfiniteSurface` |
| Section heading | `InfiniteSectionHeader` |
| Permission item | feature-local `PermissionMissionRow` composed from `InfiniteInfoRow`, `InfiniteStatusPill`, and compact action |
| Persistent guidance/recovery | `InfiniteInlineAlert` |
| Primary next action | `InfiniteButton` |
| Transient outcome | `InfiniteSnackbarHost` + `InfiniteSnackbar` |

Rows communicate state with copy, iconography, and semantics rather than color alone. Compact trailing actions are preferred over multiple full-size row buttons. The main CTA remains the single recommended next required action.

## Feedback Taxonomy

The component is chosen by message lifetime and user consequence, not by semantic color alone.

| Pattern | Use | Lifetime | Must not be used for |
| --- | --- | --- | --- |
| Snackbar | Completed or failed transient operation while content remains usable | Material `Short` or `Long`, accessibility-adjusted | Persistent blockers, detailed explanations, or multiple actions |
| Inline alert | Contextual condition tied to the current section/screen | Remains until condition resolves or user explicitly dismisses a dismissible notice | Routine success confirmation |
| Banner | Cross-screen degraded capability or broad service condition | Remains while condition applies | Frequent local outcomes |
| Confirmation dialog | Risky, destructive, or irreversible user decision | Until explicit choice | Passive success, passive error, or informational acknowledgement |
| Embedded/full state | Content is loading, empty, blocked, or unavailable | While the content state applies | Small operation feedback |
| Status pill | Compact state label | While the represented item state applies | Sentences, causes, or recovery instructions |
| Toast | External OS/application fallback only | OS-controlled | Primary in-app feedback |

Examples:

- “Lokasi berhasil diperbarui” → success snackbar.
- “GPS perangkat belum aktif” → persistent inline alert with Settings recovery.
- “Koneksi melemah; data belum tersinkron” while content remains valid → warning snackbar with one Retry action.
- “Selesaikan absensi?” → confirmation dialog.
- Required platform inspection failed and content cannot be trusted → embedded error state or inline recovery, based on how much content remains usable.

## Infinite Snackbar Contract

The snackbar is a designed Infinite component, not the default dark Material surface.

Recommended API shape:

```kotlin
data class InfiniteSnackbarVisuals(
    override val message: String,
    val semantic: InfiniteSemantic,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration
) : SnackbarVisuals

@Composable
fun InfiniteSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
)
```

Behavior:

- success and information use `SnackbarDuration.Short`;
- non-blocking warning and error use `SnackbarDuration.Long`;
- an essential persistent action may use `Indefinite`, but a continuing screen condition should normally become an inline alert instead;
- at most two text lines;
- at most one action;
- messages are queued through `SnackbarHostState`, not stacked;
- duration is owned by `SnackbarHost`, not by a `LaunchedEffect(delay(...))` inside the visual component;
- accessibility services may extend the effective timeout;
- duplicate events are prevented at the ViewModel/event boundary.

The Route translates a typed feedback effect into `SnackbarHostState.showSnackbar(InfiniteSnackbarVisuals(...))` and sends the resulting action/dismiss outcome back as a typed event when needed.

## Unified State/Status Component Family

The reusable family includes:

```text
InfiniteSnackbar
InfiniteInlineAlert
InfiniteStatusPill
InfiniteEmptyState
InfiniteLoadingState
InfiniteErrorState
InfiniteConfirmDialog
InfiniteStatusDialog compatibility path
```

All members share semantic visual resolution for container, content, border, accent, icon, and glow. They do not share the same density or behavior.

`InfiniteStatusDialog` remains source-compatible during migration, but passive success and recoverable non-blocking error should move to snackbar or inline feedback at the affected Attendance call sites. It is not silently converted to snackbar because modal versus transient behavior is a product semantic change.

The implementation may update reusable component visuals globally. Semantic call-site migration is bounded to Permission Readiness and directly affected Attendance feedback. Other application call sites continue under INF-219/INF-222 follow-up.

## Android-safe Glass Visual Contract

The project minSdk is API 26. Glass styling must render correctly without backdrop blur:

```text
translucent white/lavender/blue gradient surface
1 dp high-alpha light border
top-edge highlight
small semantic accent rail or icon halo
soft purple or semantic-tinted shadow
opaque, high-contrast text and actions
```

The effect is built with Compose-supported `Brush`, alpha colors, border, shape, and shadow. `Modifier.blur`, web `backdrop-filter`, or API-31-only rendering is not required. This keeps text sharp and avoids making performance or modern OS support a correctness dependency.

The glass effect is decorative. State remains understandable when shadows, gradients, or animation are absent.

## Typography and Motion

State/status components continue using the existing SF Compact family and design tokens. They must use only registered weights—Medium and Bold for hierarchy instead of requesting an undeclared SemiBold weight.

Component typography corrects the current disproportionate `12sp / 24sp` body treatment without changing the entire application theme:

```text
inline title / snackbar message: 14–16sp with 18–20sp line height
supporting body: 14sp with 20sp line height
compact pill/action label: 11–12sp with 14–16sp line height
dialog title: 20sp with 24sp line height
dialog body: 14sp with 20sp line height
```

Exact styles are centralized as feedback/component typography tokens backed by the existing font family. Components do not hardcode unrelated font families.

Entry/exit motion uses the existing 160–240 ms motion range. Snackbar may slide/fade; inline and embedded states may crossfade. No perpetual glass shimmer is required. The UI remains understandable when system animation scale is reduced or disabled.

## Accessibility and Responsive Behavior

- Color is never the only state signal; copy and icon semantics are required.
- Decorative icons use null semantics; the component exposes one coherent state description.
- A permission row announces title, required/optional classification, current status, and available action.
- Actions meet a 48 dp interactive target even when their visual icon is smaller.
- Snackbar action labels are verbs such as Retry, Open Settings, or Undo.
- Dismiss actions have explicit descriptions.
- Large font scaling may increase component height; text must not be clipped.
- Permission rows stack or reflow trailing actions on narrow layouts.
- Snackbar avoids covering the primary CTA and respects scaffold/navigation insets.
- Dialog focus remains trapped within the modal until the user chooses or dismisses according to its contract.

## Error Handling

Typed failures include:

```text
PlatformStateUnavailable
DeviceLocationStatusUnavailable
SettingsIntentUnavailable
Unknown
```

Rules:

- Never display a raw exception message.
- A refresh failure preserves the last trustworthy content when available and shows inline Retry guidance.
- If there is no trustworthy content, show an embedded recoverable error state.
- Failure to launch Settings produces non-crashing feedback and keeps the screen usable.
- Unknown platform behavior fails safely and does not mark required access as ready.
- Optional inspection failure cannot silently become a required blocker; it is explicit degraded/unknown capability.

## Downstream Defensive Contracts

### Attendance Screen

- Does not start the normal setup sequence.
- If required access disappears unexpectedly, shows a defensive blocking state and returns the user to Permission Readiness.
- Direct-route hardening remains until Phase 4 runtime evidence passes.

### Attendance Map

- Missing foreground location may show a defensive fallback.
- Missing background location does not prevent basic map rendering or manual attendance.
- Does not request background permission automatically.

### Face Scanner

- Missing camera may show a defensive fallback with explicit recovery.
- Does not automatically request camera when the scanner opens.
- Face match remains evidence for backend submission, not Attendance success.

### Geofence Manager

- Registers only capabilities currently available.
- Missing background location produces degraded geofence capability.
- Does not decide basic Attendance eligibility.

`LocationPermissionHelper` is audited and migrated rather than extended as the new architecture owner. Obsolete paths are removed only after verified migration.

## Four-phase Migration

### Phase 1 — Architecture extraction

- Add domain models, invariant tests, repository interface, Android data source, repository implementation, mappers, and DI.
- Preserve current route and user-visible behavior.
- Establish OS/device settings as the inspected source of truth.

### Phase 2 — ViewModel state and effects

- Add ViewModel, immutable `StateFlow`, typed events, and typed effects.
- Move persistent state, refresh coordination, and next-action resolution out of Compose.
- Keep Activity-bound interactions in the Route.

### Phase 3 — Stateless UI and unified feedback mapping

- Split Route from stateless Screen.
- Map typed presentation models to Infinite components.
- Add Android-safe glass tokens and snackbar implementation.
- Restyle the reusable state/status family and keep compatibility adapters.
- Migrate Permission Readiness and directly affected Attendance feedback semantics.
- Remove duplicate progress and verify accessibility/responsive states.

### Phase 4 — Defensive fallback cleanup

- Audit `LocationPermissionHelper`, Attendance Screen, Attendance Map, Face Scanner, and Geofence Manager.
- Remove duplicate normal request ownership only after emulator/device verification.
- Retain explicit defensive recovery for permissions revoked after readiness.

Each phase must compile and remain reviewable. Phase 4 must not be bundled before Phases 1–3 behavior is proven.

## Testing Strategy

### Domain tests

- fine + camera + GPS → can continue;
- coarse only + camera + GPS → blocked;
- fine + no camera + GPS → blocked;
- fine + camera + GPS off → blocked;
- optional denied → can continue;
- notification/background unsupported → not degraded and not blocking;
- required count excludes optional entries;
- next required action follows the approved sequence.

### Repository/data tests

- Android-version capability mapping;
- fine versus coarse mapping;
- notification mapping;
- background-location mapping;
- device-location mapping;
- refresh propagation;
- typed failure mapping.

### ViewModel tests

- initial loading and observation;
- primary action resolves the next required effect;
- permission item click emits the correct effect;
- permanently denied emits application Settings;
- device location disabled emits device Settings;
- all required ready emits one navigation effect;
- optional degradation does not block navigation;
- permission/Settings return refreshes the repository;
- repeated events do not duplicate request or navigation effects;
- transient feedback is emitted once and duplicate feedback is suppressed.

### Component and Compose tests

- stateless screen renders default, partial, all-required-ready, all-ready, denied, permanently denied, optional degraded, unsupported, GPS disabled, and recoverable-error states;
- required progress appears once;
- snackbar semantic variants, action, dismiss, queue, and duration mapping;
- inline alert remains persistent;
- confirmation dialog requires explicit choice;
- status pill remains a compact label;
- small screen and large font scale remain usable;
- TalkBack semantics identify requirement, state, and action coherently;
- glass treatment preserves readable content without blur.

### Runtime matrix

```text
fresh install
location denied
coarse-only location
camera denied
permanently denied
GPS off
notification skipped
background location skipped
Android below 10
Android 10
Android 11+ background Settings behavior
Android 13+ notification permission
permission revoked after entering downstream Attendance
```

Required runtime evidence includes screenshots of core readiness states, snackbar and inline recovery, confirmation dialog styling, and recordings proving that normal requests originate from Permission Readiness rather than downstream consumers.

Compile and unit tests are necessary but not sufficient. If emulator/device verification cannot run, the issue remains `Needs Verification`, not Done.

## Baseline and Verification Commands

Baseline on the isolated worktree before this document:

```text
app:testDebugUnitTest → exit 0 on JBR 17.0.14
app:assembleDebug     → exit 0 on Corretto 18.0.2
```

Implementation verification should include:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:assembleDebug
.\gradlew.bat --no-daemon app:lint
```

Lint failures must be separated into introduced versus pre-existing findings with file/line evidence.

## Acceptance Criteria

### Architecture and behavior

- [ ] Implementation follows `Screen → ViewModel → UseCase → Repository → RepositoryImpl → Android data source`.
- [ ] Android OS/device settings remain the source of truth for granted access.
- [ ] Domain owns required/optional policy and `canEnterAttendance`.
- [ ] ViewModel exposes immutable persistent state and a separate one-time effect stream.
- [ ] Route owns launchers, rationale inspection, Settings intents, snackbar, and navigation callbacks.
- [ ] Screen is stateless.
- [ ] Fine and coarse location are explicitly distinguished.
- [ ] Callback booleans never become long-lived permission truth.
- [ ] Notification/background denial never blocks manual attendance.
- [ ] Downstream paths remain defensive consumers.
- [ ] Duplicate normal ownership is removed only after runtime verification.

### Screen and feedback UX

- [ ] Required and optional access are visually separated.
- [ ] Required progress is shown once and uses a denominator of three.
- [ ] Main CTA represents the next required action or Continue.
- [ ] Snackbar is a designed Infinite component and auto-dismisses through accessibility-aware `SnackbarHost` duration.
- [ ] Persistent conditions use inline/embedded state rather than expiring feedback.
- [ ] Dialogs are reserved for explicit decisions, not routine passive outcomes.
- [ ] Toast is not used as primary in-app feedback.
- [ ] State/status components share Android-safe glass styling from API 26 without requiring blur.
- [ ] Typography uses registered SF Compact weights and corrected component line heights.
- [ ] Copy and iconography communicate state without depending on color alone.
- [ ] Layout remains usable on small screens, with large font scale, and with screen-reader semantics.

### Scope and safety

- [ ] Existing state/status primitives receive the approved visual foundation.
- [ ] Semantic call-site migration is limited to Permission Readiness and directly affected Attendance feedback.
- [ ] INF-209 navigation ownership remains out of scope.
- [ ] Backend and attendance submission contracts remain unchanged.
- [ ] No sensitive value is logged or committed.

### Evidence

- [ ] Domain, repository, and ViewModel tests pass.
- [ ] Relevant Compose/component tests pass.
- [ ] `app:assembleDebug` passes.
- [ ] Lint passes or blockers are documented with evidence.
- [ ] Runtime matrix evidence is attached, or the issue remains `Needs Verification`.

## Rollback Strategy

The four phases remain separable. If architecture wiring fails, restore the current presentation source temporarily while keeping domain/data code isolated. If Route integration fails, retain the stable route and current Screen while correcting effect handling. If a reusable visual change causes a regression, preserve the new semantic tokens but route affected legacy wrappers back to their prior surface until the component is fixed. Do not restore downstream normal request ownership unless runtime evidence proves the new owner unsafe.

## Related Contracts

- INF-222 — reusable component contract and tokens.
- INF-219 — broader state/status component migration.
- INF-209 — future navigation ownership; not implemented here.
- INF-223 — reminder versus active-monitoring geofence semantics.
- INF-238 — Work Mode and target-location behavior.
- INF-239 — attendance action state machine.
- GitHub #71 — accepted Permission Readiness visual baseline.
