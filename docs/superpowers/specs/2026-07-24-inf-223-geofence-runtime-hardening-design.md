# INF-223 — OS-Triggered Geofence Runtime Hardening

Date: 2026-07-24

Linear: `INF-223`

Branch: `codex/inf-223-geofence-runtime-spec-plan`

Worktree: `E:\skrisi\android\.worktrees\inf-223-geofence-runtime-spec-plan`

Base: `origin/develop` at `384de74`

Status: Design approved

Supersedes the runtime semantics in
`docs/superpowers/plans/2026-07-09-inf-223-geofence-reminder-active-monitoring.md`.
The older file remains historical evidence and must not be executed as the
current INF-223 plan.

## Summary

INF-223 hardens the existing Android geofence implementation into one
backend-truth-driven runtime. Android continues to own Google Play Services
geofence registration, local ENTER/EXIT/DWELL handling, local notifications,
and deferred location-evidence delivery. The backend remains authoritative for
attendance/session state and final attendance validation.

The design replaces independent imperative calls with one idempotent runtime
operation:

```kotlin
suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult
```

Normal foreground refresh, successful check-in, successful checkout, reboot
recovery, and logout all converge on that boundary. Persisted runtime state is
treated as a registration snapshot, not attendance truth.

The implementation also removes legacy Firebase Messaging configuration that
has no bounded runtime consumer. Firebase App Distribution remains unchanged
because it is a release channel driven by the Firebase CLI, not an FCM runtime
feature.

## Locked Product Decisions

### Local OS notification ownership

Geofence notifications are triggered locally:

```text
Google Play Services Geofencing
→ PendingIntent
→ GeofenceBroadcastReceiver
→ NotificationManager / NotificationCompat
```

No backend push, FCM message, token, topic, or service participates in reminder
or active-monitoring notification delivery.

### Source-of-truth ownership

```text
Android
= registration, transition processing, local notification, local inside/outside,
  persisted runtime snapshot, deferred evidence enqueue

Backend
= authenticated session truth, attendance state, booking approval, final
  check-in/check-out validation

WorkManager
= retryable reconciliation after boot and deferred active-session evidence sync
```

Android must not infer a successful attendance transition from local geofence
state.

### Delivery boundary

This is architecture and lifecycle hardening, not a new geofence feature from
zero. Existing notification channels, cooldown intent, unique evidence work,
and Android permission UX remain unless this specification changes them
explicitly.

## Goals

1. Restore the `presentation -> domain -> data` dependency direction.
2. Give domain code project-owned geofence modes, candidates, targets, results,
   failures, and a runtime repository contract.
3. Remove Android/runtime dependencies from `CheckInUseCase` and
   `CheckOutUseCase`.
4. Move candidate policy out of `AttendanceViewModel` into pure domain logic.
5. Resolve one expected runtime mode from refreshed authoritative inputs.
6. Reconcile expected and applied registrations through one idempotent entry
   point.
7. Persist expected mode, backend session identity, applied registrations,
   date, generation, and reconciliation status consistently.
8. Register a complete reminder set in one Play Services request.
9. Prevent stale state from reviving reminders or active monitoring after
   process death, checkout, reboot, logout, or date rollover.
10. Preserve local notification cooldown and unique evidence delivery.
11. Remove repository configuration proven unused, including the current FCM
    stub and dependency path.
12. Produce automated and supported-device runtime evidence.

## Non-Goals

- No FCM or backend push trigger for geofence notifications.
- No notification action that submits check-in or checkout.
- No backend attendance-contract change by Android assumption.
- No addition of WFH fields to `status-today`; `/me` remains the WFH owner.
- No broad Attendance UI redesign.
- No navigation, face-verification, auth-refresh, map-provider, or Work Mode
  redesign.
- No generic background-runtime framework beyond geofence ownership.
- No Firebase App Distribution removal.
- No unrelated dependency or toolchain upgrade.
- No rename of the existing `data/soucre` tree as part of this issue.

## Current Repository Facts

The `origin/develop` baseline already contains:

- multiple reminder candidates and request IDs;
- active registration after successful check-in;
- local reminder/session/evidence notification channels;
- notification cooldown persistence;
- unique WorkManager delivery for active ENTER/EXIT evidence;
- stale-event rejection and bounded worker retry;
- provider-neutral `GeoCoordinate` and `DistanceMeters` types;
- a permission contract that separates automatic monitoring readiness from the
  core attendance action.

The baseline unit suite completed with 465 tests, zero failures, zero errors,
and two skipped tests using JBR 17.0.14 and the configured Android SDK.

### Boundary leaks

`CheckInUseCase` and `CheckOutUseCase` import both
`presentation.geofencing.GeofenceManager` and the DataStore implementation
`AttendancePreference`. `UseCaseModule` therefore wires platform and data
implementations directly into domain attendance use cases.

`AuthRuntimeCleanerImpl` also depends on the presentation-owned manager even
though logout teardown belongs at a domain repository boundary implemented by
data/platform code.

### Duplicated runtime truth

Runtime-related state currently spans:

```text
status-today.activeAttendanceId
status-today.attendanceSessionState
AttendancePreference.active_attendance_id
AttendancePreference.attendance_session_state_key
last active request ID and geometry
serialized reminder set
receiver request-ID prefix interpretation
```

These values are updated in separate DataStore edits and asynchronous Play
Services callbacks. They can disagree without an explicit degraded state.

### Non-atomic reminders

`GeofenceManager.registerReminderGeofences` loops through candidates and calls
Play Services once per candidate. Every success independently appends to
DataStore. A later failure can therefore leave a partial physical and persisted
set that appears usable.

### Stale checkout semantics

`CheckOutUseCase` currently removes active monitoring, saves `completed`, and
then calls `restoreReminderGeofences`. This contradicts the current Linear
contract. A successful checkout must refresh status and reconcile. Completed
attendance must not restore check-in reminders.

### Stale boot semantics

`BootCompletedReceiver` directly reads DataStore and re-registers stored
geometry. It does not validate effective date, authenticated session, current
backend status, current profile, approved WFA booking, or snapshot generation.

### ViewModel policy ownership

`AttendanceViewModel` builds WFO, WFH, and WFA candidates. It also silently
replaces an absent WFA radius with `100f` and deduplicates only against the
primary coordinate using a raw degree epsilon.

### Receiver ambiguity

The receiver treats any non-active local state as potential reminder mode and
uses string prefixes to infer ownership. It does not compare events against a
current applied registration snapshot or effective date. Reminder DWELL is not
currently registered or handled.

### Unused Firebase runtime path

The live baseline contains:

```text
implementation(libs.firebase.messaging)
firebaseMessaging version and library aliases
Google Services Gradle plugin wiring
a fully commented InfiniteTrackFCMService.kt stub
workflow restoration of app/google-services.json
```

Repository-wide source search finds no active Firebase Messaging service,
token, topic, or message consumer. Firebase App Distribution uses the Firebase
CLI, service-account credentials, and `FIREBASE_APP_ID`; it does not require an
in-app FCM service.

## Target Architecture

```text
status-today + /me profile + approved WFA booking + runtime readiness
                              ↓
             ResolveGeofenceRuntimeModeUseCase
                              ↓
                   GeofenceRuntimeMode
                              ↓
          GeofenceRuntimeRepository.reconcile(mode)
                              ↓
 Google Play Services + runtime snapshot + receiver + notifications/workers
```

### Domain models

The domain layer owns the following concepts under
`domain/model/geofence/`:

```kotlin
sealed interface GeofenceRuntimeMode {
    data class Disabled(val reason: DisabledReason) : GeofenceRuntimeMode

    data class Reminder(
        val effectiveDate: LocalDate,
        val candidates: List<ReminderGeofenceCandidate>
    ) : GeofenceRuntimeMode

    data class ActiveMonitoring(
        val effectiveDate: LocalDate,
        val attendanceId: Int,
        val target: ActiveMonitoringTarget
    ) : GeofenceRuntimeMode

    data class Completed(val effectiveDate: LocalDate) : GeofenceRuntimeMode
}
```

```kotlin
data class ReminderGeofenceCandidate(
    val logicalId: String,
    val identity: GeofenceTargetIdentity,
    val mode: WorkMode,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val source: ReminderCandidateSource
)
```

```kotlin
data class ActiveMonitoringTarget(
    val identity: GeofenceTargetIdentity,
    val mode: WorkMode,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters
)
```

String literals from transport responses are mapped into typed mode, source,
identity, result, and failure types before runtime reconciliation.

### Domain repository contract

```kotlin
interface GeofenceRuntimeRepository {
    suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult
    suspend fun clearForLogout(): GeofenceRuntimeResult
    fun observeReadiness(): Flow<GeofenceRuntimeReadiness>
}
```

The contract exposes no `Context`, `PendingIntent`, `GeofencingClient`, Android
permission type, DataStore type, receiver, notification manager, or WorkManager
type.

### Domain use cases

`BuildReminderGeofenceCandidatesUseCase` is pure and owns:

- WFO candidate mapping from `status-today.active_location`;
- WFH candidate mapping from `/me` user-home data;
- approved-today WFA candidate mapping;
- coordinate and radius validation;
- stable-identity deduplication;
- coordinate fallback deduplication.

`ResolveGeofenceRuntimeModeUseCase` is pure and owns mode precedence.

Its input is explicit about why reconciliation is running:

```kotlin
data class GeofenceRuntimeInputs(
    val todayStatus: TodayStatus,
    val profile: UserModel,
    val approvedWfaBooking: WfaBookingForDate?,
    val readiness: GeofenceRuntimeReadiness,
    val reason: GeofenceReconcileReason
)

enum class GeofenceReconcileReason {
    ForegroundRefresh,
    CheckInSucceeded,
    CheckOutSucceeded,
    BootRecovery
}
```

Active-target resolution follows refreshed Work Mode truth: WFO uses the
status-today active location, WFH uses `/me` user-home data, and WFA uses the
approved booking for `todayStatus.todayDate`. Missing authoritative target data
for an otherwise active session produces `ActiveTargetUnavailable` rather than
reusing a stale selected-map target.

`RefreshAndReconcileGeofenceRuntimeUseCase` coordinates existing repository
contracts, resolves the expected mode, and calls the runtime repository. It is
shared by foreground refresh, successful attendance transitions, and recovery
work. It does not import Android APIs.

### Data/platform ownership

Platform implementation lives under `data/platform/geofence/` and owns:

- `GeofencingClient` and location-settings checks;
- `PendingIntent` and Android component wiring;
- conversion from domain candidates to Play Services geofences;
- persisted runtime snapshot storage;
- applied-set replacement and rollback/degraded semantics;
- BroadcastReceiver event validation;
- local notification dispatch;
- WorkManager enqueue.

The existing presentation geofence classes are moved or replaced; compatibility
wrappers must be deleted once all consumers use the domain contract.

### Presentation and Compose ownership

Presentation sends attendance/refresh events and observes a small readiness
projection. It must not store the applied request set, registration generation,
platform client, or persisted runtime snapshot in `AttendanceScreenState`.

If monitoring readiness is surfaced to Compose, the ViewModel exposes it using
the repository/use-case Flow and the screen collects it lifecycle-aware. UI-only
state remains local. No new UI is required by this issue beyond existing
permission/readiness feedback.

## Runtime Snapshot Contract

The data layer persists one coherent snapshot in one DataStore edit:

```kotlin
data class GeofenceRuntimeSnapshot(
    val schemaVersion: Int,
    val generation: Long,
    val effectiveDate: LocalDate?,
    val expectedMode: PersistedGeofenceMode,
    val attendanceId: Int?,
    val sessionStateKey: String?,
    val expectedRegistrations: List<PersistedGeofenceRegistration>,
    val appliedRequestIds: Set<String>,
    val reconciliationState: PersistedReconciliationState,
    val updatedAt: Instant
)
```

`schemaVersion` starts at `2`. Existing scalar and serialized reminder keys are
migrated only as cleanup inputs; they are not trusted as current registrations.
The first v2 reconciliation removes owned legacy registrations and writes a v2
snapshot from refreshed truth.

`reconciliationState` distinguishes:

```text
Applying
Applied
Degraded
```

The snapshot may record expected active attendance truth while registration is
degraded, but `appliedRequestIds` must not claim registrations that did not
complete successfully. Receivers process events only when state is `Applied`.

Google Play Services and DataStore cannot form one database transaction. The
repository therefore uses an explicit two-phase protocol:

1. Persist the next generation as `Applying` with an empty applied set.
2. Remove obsolete owned registrations.
3. Add the complete expected set.
4. On success, persist the same generation as `Applied` with exact request IDs.
5. On failure, perform best-effort removal of the attempted set and persist
   `Degraded` with an empty applied set and a typed failure category.

This protocol prioritizes suppressing misleading events over pretending a
partial registration is healthy.

## Candidate Policy

### Eligibility

Reminder candidates are built only for Reminder mode:

```text
activeAttendanceId == null
attendanceSessionState.key == not_started
canCheckIn == true
```

Candidate sources are:

```text
Primary → status-today.active_location
WFH     → /me user-home location
WFA     → approved booking for status-today.todayDate
```

### Logical identity

Logical IDs remain human-auditable:

```text
reminder:primary:<locationId>
reminder:wfh:user_home:<userId>
reminder:wfa:<bookingId>:<locationId-or-0>
active:<attendanceId>:<targetIdentity>
```

Physical request IDs are produced by one bounded encoder and include the
snapshot generation. Receiver parsing is delegated to that encoder; business
logic does not branch on raw prefixes.

The physical format is:

```text
gf2:<generation-base36>:<r-or-a>:<first-20-lowercase-hex-of-SHA256(logicalId)>
```

The snapshot stores the physical-to-logical mapping. The receiver decodes the
schema, generation, and kind, then resolves the logical ID only through the
current snapshot. Hash collisions inside one expected set are a typed
`RegistrationFailed` result and block registration.

### Deduplication

Candidates are deduplicated in this order:

1. same authoritative location identity;
2. same booking/user-home identity;
3. coordinates within `10.0` meters.

When candidates collide, source priority is primary, approved WFA, then WFH.
Tests cover both sides of the 10-meter boundary.

### Validation

Coordinates must be finite and accepted by `GeoCoordinate`. Radius must be
finite and greater than zero. Missing or invalid authoritative WFA radius
produces `InvalidAuthoritativeRadius`; it never becomes 100 meters implicitly.

An invalid optional candidate is reported as a typed candidate failure while
valid candidates remain visible to resolution. An invalid active target blocks
active registration and produces a degraded result.

## Mode Resolution

Mode resolution uses this precedence:

1. unauthenticated, logged out, or explicit full teardown → `Disabled`;
2. active attendance ID plus session state `active` plus valid target →
   `ActiveMonitoring`;
3. session state `completed` → `Completed`;
4. successful-checkout reconciliation with `canCheckIn == false` → `Completed`;
5. no active attendance, state `not_started`, and `canCheckIn == true` →
   `Reminder`;
6. every other consistent state → `Disabled`;
7. contradictory active ID/session combinations → typed
   `InconsistentSessionTruth` and safe `Disabled` reconciliation.

`status-today.active_location` alone never creates ActiveMonitoring mode.

## Reconciliation Semantics

### Idempotency

The repository canonicalizes expected registrations. If expected mode,
effective date, active attendance ID, geometry, radius, and applied request set
equal the current `Applied` snapshot, reconciliation is a no-op.

### Reminder mode

- Remove any active registration.
- Build every reminder geofence first.
- Submit all reminder geofences in one `GeofencingRequest`.
- Use ENTER and DWELL transitions.
- Set reminder dwell delay to 120 seconds.
- Persist the exact complete set only after success.
- Never accumulate IDs from a previous date or candidate set.

### Active monitoring

- Remove all reminder registrations.
- Expect exactly one active geofence.
- Register only after backend check-in success is confirmed by active session
  truth.
- Use ENTER and EXIT transitions.
- Persist attendance ID, state, target metadata, and applied request ID in the
  coherent snapshot.

### Completed

- Remove active and reminder registrations.
- Clear inside/outside state and active registration metadata.
- Preserve only the minimal completed snapshot needed to reject stale events.
- Do not restore persisted reminder candidates.

### Disabled

- Remove all owned registrations.
- Clear applied IDs and inside/outside state.
- Preserve a typed disabled reason for diagnostics.

### Logout

`clearForLogout` removes all geofences registered through the owned
`PendingIntent`, clears the runtime snapshot and cooldown state, and participates
in existing authenticated-runtime cleanup. It remains separate from normal mode
switching.

## Lifecycle Flows

### Foreground/status refresh

```text
refresh status-today
→ refresh/read /me and approved-today WFA inputs as required
→ resolve expected mode
→ reconcile
→ project readiness/result to presentation
```

Repeated refreshes with unchanged truth produce a no-op.

### Check-in

`CheckInUseCase` remains responsible for location capture and attendance
submission. After success, the caller refreshes authoritative status and invokes
the shared reconciliation use case. A geofence failure must not change the
successful backend check-in result into a failed attendance submission; it is
reported separately as monitoring degraded.

### Checkout

`CheckOutUseCase` remains responsible for location capture and attendance
submission. After success, the caller force-refreshes status and invokes shared
reconciliation. Completed/disabled resolution removes active and reminder
registrations. No `restoreReminderGeofences` call remains.

### Process recreation

The receiver reads the coherent snapshot for every event. A recreated
ViewModel refreshes and reconciles through the same coordinator. Runtime state
does not depend on an in-memory ViewModel candidate list.

### Boot

`BootCompletedReceiver` performs no direct network or registration work. It
enqueues unique `GeofenceReconciliationWorker` work using
`ExistingWorkPolicy.REPLACE` and a connected-network constraint.

The worker:

1. validates authenticated session availability;
2. refreshes backend truth;
3. reads profile and approved booking inputs;
4. resolves expected mode;
5. reconciles.

If current backend truth is unavailable, the worker retries. It does not restore
stored geometry while offline. This intentionally prefers temporarily disabled
monitoring over reviving a stale attendance/date registration.

## Receiver Contract

For every event, the receiver must verify:

- geofencing event has no Play Services error;
- transition is valid for the current applied mode;
- snapshot schema and generation are supported;
- snapshot state is `Applied`;
- snapshot effective date came from the most recently refreshed
  `status-today.todayDate` and equals `LocalDate.now(clock)` in the device's
  configured zone;
- request ID belongs to `appliedRequestIds` and decodes to the current
  generation;
- active mode has matching attendance ID and session state;
- reminder mode still satisfies reminder truth;
- notification permission and cooldown permit notification delivery.

Reminder mode accepts ENTER and DWELL. Active mode accepts ENTER and EXIT.
Unknown, stale, applying, degraded, or mismatched events are ignored and never
enqueue evidence.

Active ENTER/EXIT updates local inside/outside state. Active evidence work keeps
the unique key:

```text
location_event_<attendanceId>_<logicalTargetId>_<eventType>
```

Evidence delivery remains independent from local notification delivery.

## Permission and Device Readiness

Automatic registration requires precise foreground location, background
location when required by the OS version, usable Google Play Services, and
enabled device location settings.

Notification permission is independent:

- denied notification permission does not remove a valid registration;
- the readiness projection reports notification delivery as unavailable;
- receiver notification calls are skipped;
- active inside/outside updates and evidence enqueue remain allowed when their
  other gates pass.

Presentation observes only a stable readiness projection such as ready,
permission-required, device-setting-required, Play-Services-unavailable, or
degraded. Raw Android permission objects and runtime snapshots do not enter
Compose screen state.

## Failure Model

The domain result/failure surface distinguishes exactly these public
categories; platform-specific causes remain internal diagnostics:

```text
PermissionNotGranted
DeviceLocationDisabled
PlayServicesUnavailable
InvalidCoordinate
InvalidAuthoritativeRadius
ActiveTargetUnavailable
InconsistentSessionTruth
BackendTruthUnavailable
StaleRuntimeSnapshot
RegistrationFailed
RemovalFailed
```

Cancellation is rethrown. Logs may include typed categories and request IDs but
must not include auth tokens, user personal data, secret configuration, or raw
backend bodies.

## Legacy Configuration Cleanup

Cleanup is evidence-driven and part of INF-223, not optional follow-up work.

The implementation must repeat repository-wide consumer searches before
deletion. On the approved baseline, it removes:

- `implementation(libs.firebase.messaging)`;
- `firebaseMessaging` and `firebase-messaging` version-catalog entries;
- the commented `presentation/fcm/InfiniteTrackFCMService.kt` stub;
- Google Services Gradle plugin application and catalog entries after confirming
  no remaining runtime Firebase SDK consumes generated resources;
- CI restoration/validation/cleanup of `app/google-services.json` after the
  plugin/runtime consumer is removed.

It explicitly retains:

- Firebase App Distribution workflow behavior;
- Firebase CLI installation and upload;
- service-account handling for distribution;
- `FIREBASE_APP_ID` and tester-group configuration;
- Maps Secrets Gradle Plugin and `MAPS_API_KEY` handling.

Any additional legacy geofence wrapper, DI provider, DataStore key, manifest
component, serializer, or compatibility alias must be deleted once the new
runtime owns all consumers. Removal requires source, manifest, Gradle, DI,
workflow, and test search evidence plus a successful post-cleanup build.

## File Ownership Direction

Expected final areas are:

```text
domain/model/geofence/
domain/repository/GeofenceRuntimeRepository.kt
domain/use_case/geofence/
data/platform/geofence/
data/worker/GeofenceReconciliationWorker.kt
data/worker/LocationEventWorker.kt
presentation/screen/attendance/AttendanceViewModel.kt
di/
app/src/main/AndroidManifest.xml
app/build.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
.github/workflows/
```

The detailed implementation plan will name every created, modified, moved, and
deleted file after this specification is approved in written form.

## Verification Strategy

### Unit tests

Mode-resolution tests cover:

- disabled/unauthenticated;
- reminder eligibility;
- active truth;
- completed status;
- successful-checkout `canCheckIn == false`;
- contradictory attendance ID/session state.

Candidate tests cover:

- WFO, WFH, and approved-today WFA;
- no approved booking;
- stable-identity duplicate;
- coordinate duplicate at and outside 10 meters;
- invalid/non-finite coordinate;
- missing, zero, negative, or non-finite authoritative radius;
- source precedence.

Snapshot/repository tests cover:

- no-op reconciliation;
- first migration from legacy keys;
- complete reminder batch;
- replacement reminder batch;
- partial registration failure to degraded state;
- reminder to active;
- active to completed;
- active target change;
- date rollover;
- logout teardown.

Receiver tests cover:

- reminder ENTER and DWELL;
- active ENTER and EXIT;
- reminder and active cooldown;
- notification permission denied;
- unknown request ID;
- stale generation/date;
- applying/degraded snapshot;
- mismatched active session;
- unique evidence enqueue.

Recovery tests cover:

- process recreation;
- boot enqueue uniqueness;
- boot refresh success;
- boot offline retry without blind restore;
- logout followed by a stale event.

### Static and build gates

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:lintDebug
.\gradlew.bat --no-daemon app:assembleDebug
```

Also verify:

```powershell
rg -n -i "firebase.messaging|FirebaseMessagingService|RemoteMessage|onNewToken|google-services" app build.gradle.kts gradle .github
.\gradlew.bat --no-daemon app:dependencies
git diff --check
```

Expected cleanup search result contains no FCM runtime path. Remaining Firebase
matches must belong only to App Distribution documentation/workflow/CLI paths.

### Device/runtime evidence

A Google Play Services-capable emulator or physical device must demonstrate:

- expected WFO/WFH/WFA reminder candidate set and applied IDs;
- local reminder ENTER and DWELL notification;
- 45-minute reminder cooldown suppression;
- active registration only after backend-confirmed check-in;
- active attendance identity available after process recreation;
- active ENTER/EXIT inside/outside update;
- seven-minute active alert cooldown suppression;
- unique WorkManager evidence enqueue;
- checkout removal without reminder restoration when completed;
- reboot refresh/reconciliation and stale snapshot rejection;
- graceful denied-notification-permission behavior;
- no FCM message/token path.

Compile/build evidence alone cannot close this issue. Missing device evidence is
reported as `Needs Verification`.

## Documentation and Delivery

Geofence/background runtime behavior changes trigger a docs/ADR update under
the repository governance. The implementation must either update an existing
geofence runtime document/ADR or add a focused ADR describing source-of-truth,
snapshot, reconciliation, and boot policy.

Delivery remains:

```text
isolated codex/* branch
→ review/PR
→ develop
→ human device verification
→ master only after release-ready evidence
→ Firebase App Distribution from master
```

Linear status must not be moved to Done until diff/PR, fresh automated checks,
review verdict, and supported-device evidence exist.

## Acceptance Criteria

### Architecture

- Domain attendance use cases import neither presentation geofence classes,
  Android APIs, nor `AttendancePreference`.
- Domain owns runtime modes, candidates, targets, results, failures, and the
  repository contract.
- Candidate composition is pure, outside `AttendanceViewModel`, and tested.
- One reconciliation boundary owns all normal runtime transitions.
- Presentation exposes only readiness/result projections, not platform runtime
  details.

### Runtime correctness

- Reminder mode is possible only with no active attendance, `not_started`, and
  `canCheckIn == true`.
- Reminder candidates come from the locked WFO/WFH/WFA sources.
- Duplicate targets use stable identity then a 10-meter coordinate fallback.
- Invalid authoritative radius is typed and never silently defaulted.
- Complete reminder registration is one batch and partial failure cannot appear
  applied.
- Active mode requires backend-confirmed active attendance truth and exactly one
  target.
- Reminder registrations are removed during active monitoring.
- Completed/disabled modes remove all registrations and do not restore reminders.
- Boot never blindly restores persisted geometry.
- Logout clears all owned registrations, snapshot data, and cooldown state.

### Receiver and delivery

- Reminder ENTER/DWELL and active ENTER/EXIT are mode-appropriate.
- Receiver rejects stale date, generation, request ID, and session truth.
- Notification permission and cooldown gates are explicit.
- Active inside/outside state updates remain local.
- Evidence work remains unique, retry-safe, and independent of notification.

### Cleanup

- No FCM service, message, token, or topic flow remains.
- Unused Firebase Messaging and Google Services build/configuration paths are
  removed after consumer verification.
- Firebase App Distribution remains operational and master-only.
- Legacy geofence wrappers, keys, and DI wiring without consumers are removed.

### Verification

- Required unit, Android-test compile, lint, and debug assembly gates pass or a
  concrete blocker is documented.
- Device evidence confirms OS-triggered local notifications without FCM.
- Missing runtime evidence is labeled `Needs Verification`, never Done.

## Definition of Done

INF-223 is complete only when:

- Clean Architecture boundaries are restored;
- one idempotent reconciler owns runtime transitions;
- persisted state cannot claim a partial registration set as applied;
- process death, checkout, reboot, logout, and stale cache cannot confuse
  reminder and active monitoring;
- local Android notification ownership is demonstrated without FCM;
- legacy unused configuration is removed with build evidence;
- automated checks, device evidence, documentation/ADR, review verdict, and PR
  evidence are attached.
