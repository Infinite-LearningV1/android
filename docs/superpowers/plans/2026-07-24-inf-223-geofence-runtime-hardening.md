# INF-223 Geofence Runtime Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current imperative reminder/active geofence calls with one backend-truth-driven, idempotent Android runtime that survives process death, checkout, reboot, and logout without stale notifications.

**Architecture:** Pure domain use cases build candidates and resolve one expected `GeofenceRuntimeMode`. A data/platform repository applies that mode through a two-phase DataStore snapshot and a single Google Play Services batch, while receiver and boot worker entry points validate the applied generation before notifying or enqueueing evidence. Attendance submission remains separate from runtime reconciliation, and Compose receives only a small readiness projection.

**Tech Stack:** Kotlin 1.9.0, Android SDK 26–34, Jetpack Compose/StateFlow, Hilt, Preferences DataStore, Google Play Services Geofencing, WorkManager, NotificationCompat, JUnit 4, kotlinx-coroutines-test.

## Global Constraints

- Work only in `E:\skrisi\android\.worktrees\inf-223-geofence-runtime-spec-plan` on `codex/inf-223-geofence-runtime-spec-plan`.
- Base behavior is `origin/develop` at `384de74`; preserve the main checkout's local `NetworkModule.kt` and `network_security_config.xml` edits.
- Backend remains authoritative for authenticated session, attendance state, booking approval, and final check-in/check-out validation.
- Local geofence notifications flow only through Google Play Services → `PendingIntent` → receiver → `NotificationManager`/`NotificationCompat`.
- Do not add an FCM service, token, topic, message, or backend push trigger.
- Keep compile SDK 34, target SDK 34, min SDK 26, AGP 8.5.2, Gradle 8.7, and the current Kotlin version unchanged.
- Use existing `GeoCoordinate`, `DistanceMeters`, `WorkMode`, and authoritative-target contracts; do not introduce SDK coordinate types into domain or ViewModel code.
- Reminder eligibility is exactly: `activeAttendanceId == null`, state `not_started`, and `canCheckIn == true`.
- Active eligibility is exactly: positive active attendance ID, state `active`, and a valid authoritative target.
- Reminder transitions are ENTER + DWELL with a 120,000 ms dwell delay; active transitions are ENTER + EXIT.
- Preserve reminder cooldown at 45 minutes and active alert cooldown at seven minutes.
- Deduplicate reminders by stable location identity, then owner identity, then a 10.0-meter coordinate tolerance; source priority is primary, approved WFA, then WFH.
- Missing, zero, negative, or non-finite authoritative radius is typed failure; never default WFA radius to 100 meters.
- Physical request IDs use `gf2:<generation-base36>:<r-or-a>:<first-20-lowercase-hex-of-SHA256(logicalId)>`.
- A partial Play Services operation must never be persisted as `Applied`.
- Boot recovery requires current backend truth and must not restore stored geometry while offline.
- Notification permission denial does not invalidate registration; it suppresses notification only.
- Evidence WorkManager delivery stays unique/retry-safe and never drives notification delivery.
- Preserve Firebase App Distribution, Firebase CLI upload, service-account handling, `FIREBASE_APP_ID`, tester groups, Maps Secrets Gradle Plugin, and `MAPS_API_KEY` handling.
- Remove Firebase Messaging, Google Services build wiring, `google-services.json` workflow handling, and legacy geofence code only after repository-wide consumer searches confirm they are unused.
- Runtime-sensitive results without emulator/device evidence are `Needs Verification`, never Done.
- Do not print or commit secrets, tokens, personal data, Google keys, `local.properties`, `google-services.json`, or keystore material.

## Execution Environment

Run this once in every fresh PowerShell used for plan execution:

```powershell
$env:JAVA_HOME = 'C:\Users\Febriyadi\.jdks\jbr-17.0.14'
$env:ANDROID_HOME = 'C:\Users\Febriyadi\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
Set-Location 'E:\skrisi\android\.worktrees\inf-223-geofence-runtime-spec-plan'
java -version
.\gradlew.bat --version
```

Expected: Java 17.0.14 and Gradle 8.7. Do not copy the main checkout's
`local.properties`; the environment variables are sufficient and avoid copying
secret-bearing ignored configuration.

## File Map

### Domain contracts and policy

```text
app/src/main/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeMode.kt
app/src/main/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeContracts.kt
app/src/main/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeResult.kt
app/src/main/java/com/example/infinite_track/domain/repository/GeofenceRuntimeRepository.kt
app/src/main/java/com/example/infinite_track/domain/use_case/geofence/BuildReminderGeofenceCandidatesUseCase.kt
app/src/main/java/com/example/infinite_track/domain/use_case/geofence/ResolveGeofenceRuntimeModeUseCase.kt
app/src/main/java/com/example/infinite_track/domain/use_case/geofence/RefreshAndReconcileGeofenceRuntimeUseCase.kt
```

### Data/platform runtime

```text
app/src/main/java/com/example/infinite_track/data/platform/geofence/GeofenceRequestIdCodec.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/GeofencingPlatformClient.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/PlayServicesGeofencingPlatformClient.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/AndroidGeofenceRuntimeRepository.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/store/GeofenceRuntimeSnapshot.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/store/GeofenceRuntimeStore.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/store/PreferencesGeofenceRuntimeStore.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/event/GeofenceEventProcessor.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/event/GeofenceNotificationGateway.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/event/LocationEvidenceScheduler.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/event/AndroidGeofenceNotificationGateway.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/event/WorkManagerLocationEvidenceScheduler.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/receiver/GeofenceBroadcastReceiver.kt
app/src/main/java/com/example/infinite_track/data/platform/geofence/receiver/BootCompletedReceiver.kt
app/src/main/java/com/example/infinite_track/data/worker/GeofenceReconciliationWorker.kt
```

### Presentation projection

```text
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiState.kt
app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiMapper.kt
```

### Deleted after cutover

```text
app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceManager.kt
app/src/main/java/com/example/infinite_track/presentation/geofencing/ReminderGeofenceCandidate.kt
app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceBroadcastReceiver.kt
app/src/main/java/com/example/infinite_track/presentation/geofencing/BootCompletedReceiver.kt
app/src/main/java/com/example/infinite_track/presentation/fcm/InfiniteTrackFCMService.kt
```

---

### Task 1: Add Domain-Owned Runtime Contracts

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeMode.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeContracts.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeResult.kt`
- Create: `app/src/main/java/com/example/infinite_track/domain/repository/GeofenceRuntimeRepository.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/model/geofence/GeofenceRuntimeContractTest.kt`

**Interfaces:**

- Consumes: existing `GeoCoordinate`, `DistanceMeters`, `WorkMode`, `TodayStatus`, `UserModel`, and `WfaBookingForDate`.
- Produces: every public geofence runtime type consumed by Tasks 2–12.

- [ ] **Step 1: Write the failing domain contract test**

Create `GeofenceRuntimeContractTest.kt` with these exact assertions:

```kotlin
class GeofenceRuntimeContractTest {
    @Test
    fun `active mode requires a positive attendance id`() {
        assertFailsWith<IllegalArgumentException> {
            GeofenceRuntimeMode.ActiveMonitoring(
                effectiveDate = LocalDate.parse("2026-07-24"),
                attendanceId = 0,
                target = activeTarget()
            )
        }
    }

    @Test
    fun `candidate logical id cannot be blank`() {
        assertFailsWith<IllegalArgumentException> {
            reminderCandidate(logicalId = " ")
        }
    }

    @Test
    fun `notification denial is independent from registration readiness`() {
        val readiness = GeofenceRuntimeReadiness(
            registration = RegistrationReadiness.Ready,
            notification = NotificationReadiness.PERMISSION_REQUIRED
        )
        assertEquals(RegistrationReadiness.Ready, readiness.registration)
        assertEquals(NotificationReadiness.PERMISSION_REQUIRED, readiness.notification)
    }
}
```

Use local helpers that construct a valid `GeoCoordinate(-0.9, 119.8)`,
`DistanceMeters(100.0)`, `GeofenceTargetIdentity(null, "user-home:7")`, and
`ActiveMonitoringTarget`.

- [ ] **Step 2: Run the test to verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceRuntimeContractTest'
```

Expected: compilation fails because `domain.model.geofence` and
`GeofenceRuntimeRepository` do not exist.

- [ ] **Step 3: Add the exact domain type surface**

Implement the following public signatures; split them across the three model
files named above:

```kotlin
enum class GeofenceDisabledReason {
    LOGGED_OUT,
    NO_ELIGIBLE_SESSION,
    INCONSISTENT_SESSION_TRUTH,
    ACTIVE_TARGET_UNAVAILABLE
}

data class GeofenceTargetIdentity(
    val stableLocationId: Int?,
    val ownerKey: String
) {
    init { require(ownerKey.isNotBlank()) }
}

enum class ReminderCandidateSource { STATUS_TODAY, USER_PROFILE, APPROVED_WFA_BOOKING }

data class ReminderGeofenceCandidate(
    val logicalId: String,
    val identity: GeofenceTargetIdentity,
    val mode: WorkMode,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val source: ReminderCandidateSource
) {
    init { require(logicalId.isNotBlank()) }
}

data class ActiveMonitoringTarget(
    val identity: GeofenceTargetIdentity,
    val mode: WorkMode,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters
)

sealed interface GeofenceRuntimeMode {
    data class Disabled(val reason: GeofenceDisabledReason) : GeofenceRuntimeMode
    data class Reminder(
        val effectiveDate: LocalDate,
        val candidates: List<ReminderGeofenceCandidate>
    ) : GeofenceRuntimeMode
    data class ActiveMonitoring(
        val effectiveDate: LocalDate,
        val attendanceId: Int,
        val target: ActiveMonitoringTarget
    ) : GeofenceRuntimeMode {
        init { require(attendanceId > 0) }
    }
    data class Completed(val effectiveDate: LocalDate) : GeofenceRuntimeMode
}

enum class GeofenceReconcileReason {
    FOREGROUND_REFRESH,
    CHECK_IN_SUCCEEDED,
    CHECK_OUT_SUCCEEDED,
    BOOT_RECOVERY
}

sealed interface RegistrationReadiness {
    data object Ready : RegistrationReadiness
    data class PermissionRequired(val missing: Set<GeofencePermissionRequirement>) : RegistrationReadiness
    data object DeviceLocationDisabled : RegistrationReadiness
    data object PlayServicesUnavailable : RegistrationReadiness
}

enum class GeofencePermissionRequirement { PRECISE_FOREGROUND, BACKGROUND_LOCATION }
enum class NotificationReadiness { READY, PERMISSION_REQUIRED }

data class GeofenceRuntimeReadiness(
    val registration: RegistrationReadiness,
    val notification: NotificationReadiness
)

data class GeofenceRuntimeInputs(
    val todayStatus: TodayStatus,
    val profile: UserModel?,
    val approvedWfaBooking: WfaBookingForDate,
    val readiness: GeofenceRuntimeReadiness,
    val reason: GeofenceReconcileReason
)
```

Add `GeofenceRuntimeFailure`, `GeofenceRuntimeResult`, and mode resolution:

```kotlin
sealed interface GeofenceRuntimeFailure {
    data class PermissionNotGranted(val missing: Set<GeofencePermissionRequirement>) : GeofenceRuntimeFailure
    data object DeviceLocationDisabled : GeofenceRuntimeFailure
    data object PlayServicesUnavailable : GeofenceRuntimeFailure
    data class InvalidCoordinate(val source: ReminderCandidateSource) : GeofenceRuntimeFailure
    data class InvalidAuthoritativeRadius(val source: ReminderCandidateSource) : GeofenceRuntimeFailure
    data object ActiveTargetUnavailable : GeofenceRuntimeFailure
    data class InconsistentSessionTruth(val attendanceId: Int?, val stateKey: String?) : GeofenceRuntimeFailure
    data class BackendTruthUnavailable(val source: BackendTruthSource) : GeofenceRuntimeFailure
    data object StaleRuntimeSnapshot : GeofenceRuntimeFailure
    data class RegistrationFailed(val category: String) : GeofenceRuntimeFailure
    data class RemovalFailed(val category: String) : GeofenceRuntimeFailure
}

enum class BackendTruthSource { STATUS_TODAY, PROFILE, WFA_BOOKING, SESSION_VALIDATION }

data class GeofenceRuntimeModeResolution(
    val mode: GeofenceRuntimeMode,
    val warnings: List<GeofenceRuntimeFailure> = emptyList(),
    val blockingFailure: GeofenceRuntimeFailure? = null
)

sealed interface GeofenceRuntimeResult {
    val mode: GeofenceRuntimeMode
    data class Applied(
        override val mode: GeofenceRuntimeMode,
        val generation: Long,
        val logicalIds: Set<String>
    ) : GeofenceRuntimeResult
    data class NoOp(override val mode: GeofenceRuntimeMode, val generation: Long) : GeofenceRuntimeResult
    data class Degraded(
        override val mode: GeofenceRuntimeMode,
        val failure: GeofenceRuntimeFailure
    ) : GeofenceRuntimeResult
}
```

Create the repository exactly as:

```kotlin
interface GeofenceRuntimeRepository {
    suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult
    suspend fun clearForLogout(): GeofenceRuntimeResult
    fun observeReadiness(): Flow<GeofenceRuntimeReadiness>
}
```

- [ ] **Step 4: Run domain contract tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceRuntimeContractTest'
```

Expected: all three contract tests pass.

- [ ] **Step 5: Commit Task 1**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/model/geofence app/src/main/java/com/example/infinite_track/domain/repository/GeofenceRuntimeRepository.kt app/src/test/java/com/example/infinite_track/domain/model/geofence
git commit -m "feat: add geofence runtime domain contracts"
```

---

### Task 2: Extract and Test Reminder Candidate Policy

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/geofence/BuildReminderGeofenceCandidatesUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/geofence/BuildReminderGeofenceCandidatesUseCaseTest.kt`
- Reuse: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/ResolveAuthoritativeTargetLocationUseCase.kt`

**Interfaces:**

- Consumes: Task 1 candidates/failures plus existing authoritative target resolver.
- Produces: `ReminderCandidateBuildResult` for Task 3.

- [ ] **Step 1: Write failing candidate tests**

Create test functions with these exact names and expectations:

```kotlin
@Test fun `builds WFO WFH and approved WFA candidates`()
@Test fun `deduplicates WFA against primary by stable location id`()
@Test fun `deduplicates targets within ten meters`()
@Test fun `keeps targets outside ten meters`()
@Test fun `uses primary then WFA then WFH source priority`()
@Test fun `reports invalid WFA radius without default`()
@Test fun `reports invalid WFH coordinate and keeps valid WFO`()
@Test fun `uses stable logical ids`()
```

The first test must assert these IDs in this order:

```kotlin
assertEquals(
    listOf(
        "reminder:primary:11",
        "reminder:wfa:31:22",
        "reminder:wfh:user_home:7"
    ),
    result.candidates.map { it.logicalId }
)
```

The invalid-radius test supplies `radiusMeters = null`, asserts no WFA
candidate, and asserts exactly
`InvalidAuthoritativeRadius(APPROVED_WFA_BOOKING)` in `failures`.

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*BuildReminderGeofenceCandidatesUseCaseTest'
```

Expected: compilation fails because the builder/result do not exist.

- [ ] **Step 3: Implement pure candidate composition**

Use this public surface:

```kotlin
data class ReminderCandidateBuildResult(
    val candidates: List<ReminderGeofenceCandidate>,
    val failures: List<GeofenceRuntimeFailure>
)

class BuildReminderGeofenceCandidatesUseCase @Inject constructor(
    private val resolveTarget: ResolveAuthoritativeTargetLocationUseCase
) {
    operator fun invoke(
        todayStatus: TodayStatus,
        profile: UserModel?,
        wfaBooking: WfaBookingForDate
    ): ReminderCandidateBuildResult
}
```

Resolve WFO, WFA, and WFH independently through the existing authoritative
resolver; map successful targets with these identities:

```kotlin
WFO -> GeofenceTargetIdentity(todayStatus.activeLocation?.locationId, "primary:${target.targetId.value}")
WFA -> GeofenceTargetIdentity((wfaBooking as? WfaBookingForDate.Approved)?.booking?.locationId, "wfa:${booking.bookingId}")
WFH -> GeofenceTargetIdentity(null, "user-home:${profile?.id}")
```

Map `TargetResolutionFailure.INVALID_COORDINATE` and `INVALID_RADIUS` into the
source-specific runtime failures. Do not turn unavailable optional sources into
blocking failures.

Implement Haversine distance locally with earth radius `6_371_000.0` meters.
Canonicalize with this algorithm:

```kotlin
val ordered = raw.sortedBy { sourcePriority(it.source) }
val deduplicated = ordered.fold(mutableListOf<ReminderGeofenceCandidate>()) { kept, candidate ->
    val duplicate = kept.any { existing ->
        sameNonNullStableLocation(existing, candidate) ||
            existing.identity.ownerKey == candidate.identity.ownerKey ||
            distanceMeters(existing.coordinate, candidate.coordinate) <= 10.0
    }
    if (!duplicate) kept += candidate
    kept
}
```

Return candidates ordered primary, WFA, WFH. Never inspect Android APIs or
perform repository/network calls.

- [ ] **Step 4: Run candidate tests and target regression tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*BuildReminderGeofenceCandidatesUseCaseTest' --tests '*ResolveAuthoritativeTargetLocationUseCaseTest'
```

Expected: all candidate and authoritative-target tests pass.

- [ ] **Step 5: Commit Task 2**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/use_case/geofence/BuildReminderGeofenceCandidatesUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/geofence/BuildReminderGeofenceCandidatesUseCaseTest.kt
git commit -m "feat: extract reminder geofence candidate policy"
```

---

### Task 3: Resolve One Expected Runtime Mode

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/geofence/ResolveGeofenceRuntimeModeUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/geofence/ResolveGeofenceRuntimeModeUseCaseTest.kt`

**Interfaces:**

- Consumes: Task 1 `GeofenceRuntimeInputs` and Task 2 builder.
- Produces: `GeofenceRuntimeModeResolution` consumed by Task 7 coordinator.

- [ ] **Step 1: Write failing mode matrix tests**

Create these exact tests:

```kotlin
@Test fun `not started eligible status resolves reminder`()
@Test fun `active id and active state resolve one active target`()
@Test fun `active location without active session never resolves active`()
@Test fun `completed state resolves completed`()
@Test fun `checkout success and can check in false resolves completed`()
@Test fun `non checkout can check in false resolves disabled`()
@Test fun `active id with non active state resolves safe disabled with typed failure`()
@Test fun `active state without attendance id resolves safe disabled with typed failure`()
@Test fun `invalid active target resolves safe disabled with typed failure`()
```

For contradictory truth assert:

```kotlin
assertEquals(
    GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.INCONSISTENT_SESSION_TRUTH),
    result.mode
)
assertEquals(
    GeofenceRuntimeFailure.InconsistentSessionTruth(91, "completed"),
    result.blockingFailure
)
```

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveGeofenceRuntimeModeUseCaseTest'
```

Expected: compilation fails because the resolver does not exist.

- [ ] **Step 3: Implement precedence exactly**

```kotlin
class ResolveGeofenceRuntimeModeUseCase @Inject constructor(
    private val buildCandidates: BuildReminderGeofenceCandidatesUseCase,
    private val resolveTarget: ResolveAuthoritativeTargetLocationUseCase
) {
    operator fun invoke(inputs: GeofenceRuntimeInputs): GeofenceRuntimeModeResolution
}
```

Use this decision order:

```text
positive ID + active state + valid WorkMode target -> ActiveMonitoring
ID/state contradiction -> Disabled(INCONSISTENT_SESSION_TRUTH) + blocking failure
state completed -> Completed
reason CHECK_OUT_SUCCEEDED and canCheckIn false -> Completed
no ID + not_started + canCheckIn true -> Reminder
otherwise -> Disabled(NO_ELIGIBLE_SESSION)
```

Parse `todayStatus.todayDate` with `LocalDate.parse`; invalid dates return safe
Disabled with `BackendTruthUnavailable(STATUS_TODAY)`. Map the resolved
`AuthoritativeTargetLocation` to `ActiveMonitoringTarget` without using the
ViewModel-selected map target.

- [ ] **Step 4: Run resolver and candidate tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ResolveGeofenceRuntimeModeUseCaseTest' --tests '*BuildReminderGeofenceCandidatesUseCaseTest'
```

Expected: all tests pass.

- [ ] **Step 5: Commit Task 3**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/use_case/geofence/ResolveGeofenceRuntimeModeUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/geofence/ResolveGeofenceRuntimeModeUseCaseTest.kt
git commit -m "feat: resolve authoritative geofence runtime modes"
```

---

### Task 4: Add the Coherent v2 Runtime Snapshot Store

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/store/GeofenceRuntimeSnapshot.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/store/GeofenceRuntimeStore.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/store/PreferencesGeofenceRuntimeStore.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/platform/geofence/store/PreferencesGeofenceRuntimeStoreTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/AppModule.kt`

**Interfaces:**

- Consumes: flat persisted values derived from Task 1 modes; Gson already exists in the graph.
- Produces: atomic snapshot/cooldown/inside-state operations for Tasks 5 and 9.

- [ ] **Step 1: Write failing DataStore tests**

Use `PreferenceDataStoreFactory` and a temporary file, following
`AttendancePreferenceRuntimeStateTest`. Add exact tests:

```kotlin
@Test fun `writes and reads one v2 snapshot atomically`() = runTest
@Test fun `corrupt snapshot returns null instead of throwing`() = runTest
@Test fun `claim cooldown allows first suppresses second and allows expiry`() = runTest
@Test fun `set inside updates only current applied active snapshot`() = runTest
@Test fun `clear removes snapshot and cooldown state`() = runTest
```

The cooldown test uses `now = 1_000L`, cooldown `100L`, and asserts true,
false, true at `1_000L`, `1_050L`, and `1_100L`.

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*PreferencesGeofenceRuntimeStoreTest'
```

Expected: compilation fails because store classes do not exist.

- [ ] **Step 3: Implement flat persisted snapshot types**

```kotlin
enum class PersistedGeofenceMode { DISABLED, REMINDER, ACTIVE, COMPLETED }
enum class PersistedReconciliationState { APPLYING, APPLIED, DEGRADED }
enum class PersistedRegistrationKind { REMINDER, ACTIVE }

data class PersistedGeofenceRegistration(
    val requestId: String,
    val logicalId: String,
    val kind: PersistedRegistrationKind,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val attendanceId: Int?
)

data class GeofenceRuntimeSnapshot(
    val schemaVersion: Int = 2,
    val generation: Long,
    val effectiveDateIso: String?,
    val expectedMode: PersistedGeofenceMode,
    val attendanceId: Int?,
    val sessionStateKey: String?,
    val expectedRegistrations: List<PersistedGeofenceRegistration>,
    val appliedRequestIds: Set<String>,
    val reconciliationState: PersistedReconciliationState,
    val insideActiveGeofence: Boolean,
    val failureCategory: String?,
    val updatedAtEpochMillis: Long
)
```

Store interface:

```kotlin
interface GeofenceRuntimeStore {
    suspend fun readSnapshot(): GeofenceRuntimeSnapshot?
    suspend fun writeSnapshot(snapshot: GeofenceRuntimeSnapshot)
    suspend fun claimNotification(key: String, nowMillis: Long, cooldownMillis: Long): Boolean
    suspend fun setInsideActiveGeofence(isInside: Boolean)
    suspend fun clear()
}
```

`PreferencesGeofenceRuntimeStore` uses one string key
`geofence_runtime_snapshot_v2` and one string-set key
`geofence_runtime_notification_cooldowns_v2` in a dedicated DataStore named
`geofence_runtime_v2`. Every snapshot change is one `dataStore.edit`.
`setInsideActiveGeofence` changes the flag only when the snapshot is schema 2,
`APPLIED`, and `ACTIVE`; every other state is a no-op.

Provide `Clock.systemDefaultZone()` in `AppModule`:

```kotlin
@Provides
@Singleton
fun provideClock(): Clock = Clock.systemDefaultZone()
```

- [ ] **Step 4: Run store tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*PreferencesGeofenceRuntimeStoreTest'
```

Expected: all five tests pass.

- [ ] **Step 5: Commit Task 4**

```powershell
git add app/src/main/java/com/example/infinite_track/data/platform/geofence/store app/src/test/java/com/example/infinite_track/data/platform/geofence/store app/src/main/java/com/example/infinite_track/di/AppModule.kt
git commit -m "feat: persist coherent geofence runtime snapshots"
```

---

### Task 5: Implement Request IDs, Play Services Batch Adapter, and Reconciler

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/GeofenceRequestIdCodec.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/GeofencingPlatformClient.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/PlayServicesGeofencingPlatformClient.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/AndroidGeofenceRuntimeRepository.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/platform/geofence/GeofenceRequestIdCodecTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/platform/geofence/AndroidGeofenceRuntimeRepositoryTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt`

**Interfaces:**

- Consumes: Task 1 repository contract and Task 4 store.
- Produces: the only normal runtime writer used by Tasks 6–10.

- [ ] **Step 1: Write failing codec and reconciliation tests**

Codec tests assert exact deterministic output length/prefix, generation decode,
reminder/active kind decode, and different logical IDs producing different
hashes.

Repository tests use `FakeGeofencingPlatformClient` and
`InMemoryGeofenceRuntimeStore` and include:

```kotlin
@Test fun `unchanged applied mode is no op`() = runTest
@Test fun `reminder set is added in one batch`() = runTest
@Test fun `reminder replacement removes then adds complete set`() = runTest
@Test fun `reminder to active produces exactly one active registration`() = runTest
@Test fun `active to completed removes and applies empty set`() = runTest
@Test fun `registration failure rolls back and persists degraded empty applied set`() = runTest
@Test fun `permission denial returns degraded without calling add`() = runTest
@Test fun `logout removes owned geofences and clears store`() = runTest
```

For failure assert `reconciliationState == DEGRADED` and
`appliedRequestIds == emptySet<String>()`.

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceRequestIdCodecTest' --tests '*AndroidGeofenceRuntimeRepositoryTest'
```

Expected: compilation fails because codec/client/repository do not exist.

- [ ] **Step 3: Implement the platform seam**

```kotlin
data class PlatformGeofenceRegistration(
    val requestId: String,
    val logicalId: String,
    val kind: PersistedRegistrationKind,
    val label: String,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val attendanceId: Int?
)

interface GeofencingPlatformClient {
    fun observeReadiness(): Flow<GeofenceRuntimeReadiness>
    suspend fun removeOwnedGeofences()
    suspend fun addAll(registrations: List<PlatformGeofenceRegistration>)
}
```

`PlayServicesGeofencingPlatformClient.addAll` must build every `Geofence`
before invoking `geofencingClient.addGeofences` once. Reminder geofences use:

```kotlin
.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
.setLoiteringDelay(120_000)
```

Active geofences use ENTER + EXIT. Both use `Geofence.NEVER_EXPIRE`. Bridge
Play Services `Task` with a cancellation-safe suspend function.

`GeofenceRequestIdCodec` hashes UTF-8 logical ID bytes with SHA-256, uses the
first 20 lowercase hex characters, and exposes:

```kotlin
fun encode(generation: Long, kind: PersistedRegistrationKind, logicalId: String): String
fun decode(requestId: String): DecodedGeofenceRequestId?
```

- [ ] **Step 4: Implement two-phase reconciliation**

`AndroidGeofenceRuntimeRepository` must:

1. canonicalize expected logical registrations;
2. return `NoOp` only for an equal `APPLIED` snapshot;
3. persist next generation as `APPLYING` with empty applied IDs;
4. call `removeOwnedGeofences`;
5. call one `addAll` when expected is non-empty;
6. persist `APPLIED` with exact IDs;
7. on removal/add failure, best-effort remove again and persist `DEGRADED` with
   empty IDs;
8. rethrow `CancellationException`;
9. map registration readiness to typed failures without calling Play Services.

Canonical equality compares mode kind, effective date, attendance ID, logical
identity, coordinate, radius, and label; it ignores generation and physical
request ID. Active logical IDs are exactly
`active:<attendanceId>:<target.identity.ownerKey>`. Detect duplicate encoded
request IDs before Play Services and return
`RegistrationFailed("request_id_collision")`.

`clearForLogout` calls `removeOwnedGeofences` and then `store.clear`. Success
returns `Applied(Disabled(LOGGED_OUT), generation = 0, logicalIds = emptySet())`;
removal failure returns `Degraded(Disabled(LOGGED_OUT), RemovalFailed(category))`
after still clearing the owned snapshot/cooldown store.

Bind it in `RepositoryModule`:

```kotlin
@Provides
@Singleton
fun provideGeofencingPlatformClient(
    client: PlayServicesGeofencingPlatformClient
): GeofencingPlatformClient = client

@Provides
@Singleton
fun provideGeofenceRuntimeRepository(
    repository: AndroidGeofenceRuntimeRepository
): GeofenceRuntimeRepository = repository
```

- [ ] **Step 5: Run repository tests and compile**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceRequestIdCodecTest' --tests '*AndroidGeofenceRuntimeRepositoryTest'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected: tests pass and compile succeeds.

- [ ] **Step 6: Commit Task 5**

```powershell
git add app/src/main/java/com/example/infinite_track/data/platform/geofence app/src/test/java/com/example/infinite_track/data/platform/geofence app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt
git commit -m "feat: reconcile geofence registrations atomically"
```

---

### Task 6: Move Logout Teardown Behind the Runtime Repository

**Files:**

- Modify: `app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRuntimeCleanerImpl.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt`
- Modify: `app/src/test/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCaseTest.kt`
- Modify: `app/src/test/java/com/example/infinite_track/presentation/screen/profile/ManualLogoutIntegrationContractTest.kt`

**Interfaces:**

- Consumes: Task 5 `GeofenceRuntimeRepository.clearForLogout`.
- Produces: logout cleanup with no presentation-layer dependency.

- [ ] **Step 1: Update tests first**

Replace fake `GeofenceManager` cleanup with a fake `GeofenceRuntimeRepository`.
Assert this exact call order:

```kotlin
assertEquals(
    listOf("geofence.clearForLogout", "auth.clear", "profile.clear", "today.clear", "attendance.clear"),
    calls
)
```

Keep the existing best-effort failure aggregation test and make
`clearForLogout` failure appear as a suppressed cleanup error while remaining
steps still execute.

- [ ] **Step 2: Verify tests fail**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ClearAuthenticatedRuntimeUseCaseTest' --tests '*ManualLogoutIntegrationContractTest'
```

Expected: tests fail because `AuthRuntimeCleanerImpl` still requires
`GeofenceManager`.

- [ ] **Step 3: Replace the dependency**

Use this constructor:

```kotlin
class AuthRuntimeCleanerImpl @Inject constructor(
    private val userPreference: UserPreference,
    private val userDao: UserDao,
    private val attendancePreference: AttendancePreference,
    private val todayStatusPreference: TodayStatusPreference,
    private val geofenceRuntimeRepository: GeofenceRuntimeRepository
) : AuthRuntimeCleaner
```

Call `geofenceRuntimeRepository.clearForLogout()` before clearing local auth
and attendance caches. A `Degraded` result becomes an
`IllegalStateException("geofenceRuntimeRepository.clearForLogout degraded")`
inside the existing failure accumulator.

Update `RepositoryModule.provideAuthRuntimeCleaner` accordingly and remove its
presentation import.

- [ ] **Step 4: Run logout tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*ClearAuthenticatedRuntimeUseCaseTest' --tests '*ManualLogoutIntegrationContractTest' --tests '*ProfileViewModelLogoutTest'
```

Expected: all logout tests pass.

- [ ] **Step 5: Commit Task 6**

```powershell
git add app/src/main/java/com/example/infinite_track/data/repository/auth/AuthRuntimeCleanerImpl.kt app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt app/src/test/java/com/example/infinite_track/domain/use_case/auth/ClearAuthenticatedRuntimeUseCaseTest.kt app/src/test/java/com/example/infinite_track/presentation/screen/profile/ManualLogoutIntegrationContractTest.kt
git commit -m "refactor: route logout through geofence runtime contract"
```

---

### Task 7: Add Shared Truth Refresh and Decouple Attendance Submission

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/domain/use_case/geofence/RefreshAndReconcileGeofenceRuntimeUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/geofence/RefreshAndReconcileGeofenceRuntimeUseCaseTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/CheckInUseCase.kt`
- Modify: `app/src/main/java/com/example/infinite_track/domain/use_case/attendance/CheckOutUseCase.kt`
- Create: `app/src/test/java/com/example/infinite_track/domain/use_case/attendance/AttendanceSubmissionBoundaryTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`

**Interfaces:**

- Consumes: Tasks 1–5 plus existing status/profile/booking/session use cases.
- Produces: one coordinator consumed by ViewModel and boot worker.

- [ ] **Step 1: Write coordinator and boundary tests**

Coordinator tests:

```kotlin
@Test fun `foreground refresh obtains status profile booking then reconciles`() = runTest
@Test fun `check in success reason reaches mode resolver`() = runTest
@Test fun `check out success reason resolves completed without reminders`() = runTest
@Test fun `status failure returns backend unavailable and does not reconcile`() = runTest
@Test fun `boot temporary session failure returns retryable failure`() = runTest
@Test fun `boot reauth clears runtime and returns auth unavailable`() = runTest
@Test fun `profile temporary failure uses cached profile and records warning`() = runTest
```

Boundary reflection test:

```kotlin
@Test
fun `attendance submission use cases have no geofence or preference dependency`() {
    listOf(CheckInUseCase::class.java, CheckOutUseCase::class.java).forEach { type ->
        val dependencies = type.declaredConstructors.single().parameterTypes.map { it.name }
        assertFalse(dependencies.any { it.contains("GeofenceManager") })
        assertFalse(dependencies.any { it.contains("AttendancePreference") })
        assertFalse(dependencies.any { it.startsWith("android.") })
    }
}
```

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*RefreshAndReconcileGeofenceRuntimeUseCaseTest' --tests '*AttendanceSubmissionBoundaryTest'
```

Expected: coordinator missing and boundary reflection test fails.

- [ ] **Step 3: Implement coordinator result and algorithm**

```kotlin
sealed interface RefreshAndReconcileGeofenceRuntimeResult {
    data class Reconciled(
        val todayStatus: TodayStatus,
        val profile: UserModel?,
        val wfaBooking: WfaBookingForDate,
        val resolution: GeofenceRuntimeModeResolution,
        val runtime: GeofenceRuntimeResult
    ) : RefreshAndReconcileGeofenceRuntimeResult
    data class BackendUnavailable(val failure: GeofenceRuntimeFailure.BackendTruthUnavailable) : RefreshAndReconcileGeofenceRuntimeResult
    data class AuthUnavailable(val runtime: GeofenceRuntimeResult) : RefreshAndReconcileGeofenceRuntimeResult
}

class RefreshAndReconcileGeofenceRuntimeUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val refreshProfile: RefreshAttendanceProfileUseCase,
    private val getLoggedInUser: GetLoggedInUserUseCase,
    private val resolveBooking: ResolveTodayWfaBookingStateUseCase,
    private val validateSession: ValidateForegroundSessionUseCase,
    private val resolveMode: ResolveGeofenceRuntimeModeUseCase,
    private val runtimeRepository: GeofenceRuntimeRepository
) {
    suspend operator fun invoke(reason: GeofenceReconcileReason): RefreshAndReconcileGeofenceRuntimeResult
}
```

For `BOOT_RECOVERY`, validate session first. `ReauthRequired`/`Skipped` calls
`clearForLogout`; `TemporaryFailure` returns
`BackendUnavailable(SESSION_VALIDATION)`. Always fetch status with
`forceRefresh = true`. Refresh profile; on temporary failure use
`getLoggedInUser().first()` and append a PROFILE warning. Resolve booking for
`todayStatus.todayDate`. Read readiness with `observeReadiness().first()`,
resolve mode, then reconcile exactly once.

`ProfileSyncResult.Unauthorized` must call `clearForLogout` and return
`AuthUnavailable`; it must never fall back to a cached profile. A temporary
profile failure with no cached profile remains a PROFILE warning, allowing WFO
resolution while WFH candidate/active-target resolution fails explicitly.

- [ ] **Step 4: Simplify attendance use cases**

`CheckInUseCase` constructor becomes:

```kotlin
class CheckInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
)
```

Its operator accepts only `AttendanceRequestModel`; remove `targetLocation`,
preference writes, geofence calls, and `android.util.Log` from domain.

`CheckOutUseCase` constructor uses the same two dependencies. Keep attendance
ID resolution and GPS capture, but remove active removal, `completed` write,
and `restoreReminderGeofences`.

Update `UseCaseModule` providers and add providers for builder, resolver, and
coordinator without presentation/data implementation parameters.

- [ ] **Step 5: Run coordinator, boundary, and attendance tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*RefreshAndReconcileGeofenceRuntimeUseCaseTest' --tests '*AttendanceSubmissionBoundaryTest' --tests '*AttendanceActionResolverTest' --tests '*AttendanceCheckInRequestFactoryTest'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected: tests and compile pass.

- [ ] **Step 6: Commit Task 7**

```powershell
git add app/src/main/java/com/example/infinite_track/domain/use_case/geofence app/src/test/java/com/example/infinite_track/domain/use_case/geofence app/src/main/java/com/example/infinite_track/domain/use_case/attendance/CheckInUseCase.kt app/src/main/java/com/example/infinite_track/domain/use_case/attendance/CheckOutUseCase.kt app/src/test/java/com/example/infinite_track/domain/use_case/attendance/AttendanceSubmissionBoundaryTest.kt app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt
git commit -m "refactor: reconcile runtime after attendance truth refresh"
```

---

### Task 8: Cut AttendanceViewModel Over to the Coordinator

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiState.kt`
- Create: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiMapper.kt`
- Create: `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/GeofenceRuntimeUiMapperTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreenState.kt`
- Modify: `app/src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceViewModel.kt`
- Modify: relevant constructor setup in `app/src/test/java/com/example/infinite_track/presentation/screen/attendance/`

**Interfaces:**

- Consumes: Task 7 coordinator and Task 1 readiness/result types.
- Produces: presentation projection only; no platform/runtime state in Compose.

- [ ] **Step 1: Write failing mapper and ownership tests**

Mapper tests cover ready, notification-denied, registration permission required,
device setting required, Play Services unavailable, and degraded registration.

Add a reflection/source ownership test asserting `AttendanceViewModel` has no
constructor dependency or import containing:

```text
GeofenceManager
AttendancePreference
ReminderGeofenceCandidate
GeofencingClient
```

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceRuntimeUiMapperTest' --tests '*AttendancePreparationStateOwnershipTest'
```

Expected: mapper missing and ownership test fails.

- [ ] **Step 3: Add the stable presentation projection**

```kotlin
enum class GeofenceRuntimeUiReason {
    PRECISE_LOCATION_REQUIRED,
    BACKGROUND_LOCATION_REQUIRED,
    DEVICE_LOCATION_DISABLED,
    PLAY_SERVICES_UNAVAILABLE,
    NOTIFICATION_PERMISSION_REQUIRED,
    REGISTRATION_DEGRADED
}

data class GeofenceRuntimeUiState(
    val monitoringAvailable: Boolean = false,
    val notificationAvailable: Boolean = false,
    val reason: GeofenceRuntimeUiReason? = null
)
```

Add `geofenceRuntime: GeofenceRuntimeUiState = GeofenceRuntimeUiState()` to
`AttendanceScreenState`. Do not add applied request IDs, generations, target
geometry, platform types, or snapshot data.

- [ ] **Step 4: Replace ViewModel runtime orchestration**

Inject only `RefreshAndReconcileGeofenceRuntimeUseCase` and mapper for geofence
runtime. Delete `refreshReminderGeofencesIfNeeded`, `buildReminderCandidates`,
`isDuplicateWithPrimary`, constants used only by them, and all direct preference
or manager calls.

After successful check-in call:

```kotlin
refreshAttendanceAndRuntime(GeofenceReconcileReason.CHECK_IN_SUCCEEDED)
```

After successful checkout call:

```kotlin
refreshAttendanceAndRuntime(GeofenceReconcileReason.CHECK_OUT_SUCCEEDED)
```

Initialization and explicit refresh use `FOREGROUND_REFRESH`. The helper maps
`Reconciled.todayStatus` into existing UI/preparation state and maps runtime
readiness/result into `geofenceRuntime`. `BackendUnavailable` keeps the existing
retryable status error. Do not turn `GeofenceRuntimeResult.Degraded` into a
failed attendance submission.

- [ ] **Step 5: Run ViewModel and preparation tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceRuntimeUiMapperTest' --tests '*AttendancePreparationStateOwnershipTest' --tests '*AttendancePreparationRefreshCoordinatorTest' --tests '*AttendanceActionStateTest' --tests '*AttendanceSelectionTransitionTest'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected: tests and compile pass.

- [ ] **Step 6: Commit Task 8**

```powershell
git add app/src/main/java/com/example/infinite_track/presentation/screen/attendance app/src/test/java/com/example/infinite_track/presentation/screen/attendance
git commit -m "refactor: project geofence runtime through attendance state"
```

---

### Task 9: Harden Receiver Processing Behind a Pure Event Processor

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/event/GeofenceEventProcessor.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/event/GeofenceNotificationGateway.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/event/LocationEvidenceScheduler.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/event/AndroidGeofenceNotificationGateway.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/event/WorkManagerLocationEvidenceScheduler.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/receiver/GeofenceBroadcastReceiver.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/platform/geofence/event/GeofenceEventProcessorTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**

- Consumes: Task 4 applied snapshot, Task 5 codec, existing `NotificationHelper`, and `LocationEventWorker` input contract.
- Produces: validated local notification/inside-state/evidence handling.

- [ ] **Step 1: Write failing event matrix tests**

Use fake store, notification gateway, evidence scheduler, and fixed Clock. Add:

```kotlin
@Test fun `reminder enter notifies once and does not enqueue evidence`() = runTest
@Test fun `reminder dwell notifies and respects 45 minute cooldown`() = runTest
@Test fun `active enter sets inside notifies and enqueues unique evidence`() = runTest
@Test fun `active exit clears inside and enqueues evidence`() = runTest
@Test fun `notification denial keeps active state and evidence but skips notify`() = runTest
@Test fun `stale date is ignored`() = runTest
@Test fun `stale generation is ignored`() = runTest
@Test fun `unknown request id is ignored`() = runTest
@Test fun `applying and degraded snapshots are ignored`() = runTest
@Test fun `active session mismatch is ignored`() = runTest
```

Assert no notification/evidence/store mutation for every ignored outcome.

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceEventProcessorTest'
```

Expected: compilation fails because event processor/gateways do not exist.

- [ ] **Step 3: Implement event contracts and validation**

```kotlin
enum class GeofenceTransition { ENTER, EXIT, DWELL }

data class GeofenceTransitionEvent(
    val transition: GeofenceTransition,
    val requestIds: Set<String>,
    val occurredAt: Instant
)

sealed interface GeofenceEventOutcome {
    data class Processed(val logicalIds: Set<String>) : GeofenceEventOutcome
    data class Ignored(val reason: String) : GeofenceEventOutcome
}

interface GeofenceNotificationGateway {
    fun canPostNotifications(): Boolean
    fun showReminder(label: String)
    fun showActive(transition: GeofenceTransition, label: String)
}

interface LocationEvidenceScheduler {
    fun enqueue(attendanceId: Int, logicalId: String, transition: GeofenceTransition, occurredAt: Instant)
}
```

Processor gate order is: snapshot exists → schema 2 → `APPLIED` → effective
date equals `LocalDate.now(clock)` → request ID is applied → codec generation
matches → transition matches registration kind → active attendance/session
match. Only then claim cooldown atomically.

Cooldown keys are:

```text
reminder:<logicalId>
active:<attendanceId>:<transition>:<logicalId>
```

Use 2,700,000 ms and 420,000 ms respectively.

- [ ] **Step 4: Implement Android gateways and thin receiver**

`WorkManagerLocationEvidenceScheduler` must preserve:

```kotlin
val uniqueName = "location_event_${attendanceId}_${logicalId}_${transition.name}"
workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
```

`GeofenceBroadcastReceiver` parses `GeofencingEvent`, maps ENTER/EXIT/DWELL,
uses `goAsync`, obtains `GeofenceEventProcessor` through one Hilt entry point,
and always finishes in `finally`. It contains no session/cooldown/business
branching.

Change the manifest receiver class to:

```xml
<receiver
    android:name=".data.platform.geofence.receiver.GeofenceBroadcastReceiver"
    android:enabled="true"
    android:exported="false" />
```

- [ ] **Step 5: Run receiver tests and compile Android tests**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceEventProcessorTest' --tests '*LocationEventWorker*'
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
```

Expected: processor tests and Android-test compilation pass.

- [ ] **Step 6: Commit Task 9**

```powershell
git add app/src/main/java/com/example/infinite_track/data/platform/geofence/event app/src/main/java/com/example/infinite_track/data/platform/geofence/receiver/GeofenceBroadcastReceiver.kt app/src/test/java/com/example/infinite_track/data/platform/geofence/event app/src/main/AndroidManifest.xml
git commit -m "feat: validate geofence events against applied runtime"
```

---

### Task 10: Replace Blind Boot Restore with Unique Reconciliation Work

**Files:**

- Create: `app/src/main/java/com/example/infinite_track/data/worker/GeofenceReconciliationWorker.kt`
- Create: `app/src/main/java/com/example/infinite_track/data/platform/geofence/receiver/BootCompletedReceiver.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/worker/GeofenceReconciliationWorkerTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/data/platform/geofence/receiver/BootCompletedReceiverContractTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**

- Consumes: Task 7 coordinator and existing Hilt WorkManager configuration.
- Produces: reboot recovery that never reads persisted geometry directly.

- [ ] **Step 1: Write failing worker and boot contract tests**

Worker tests assert:

```kotlin
Reconciled -> Result.success()
AuthUnavailable -> Result.success()
BackendUnavailable -> Result.retry()
CancellationException -> rethrown
```

Boot contract test reads receiver source/bytecode contract and asserts it
contains unique work name `geofence_runtime_reconcile`, connected-network
constraint, `ExistingWorkPolicy.REPLACE`, and no reference to
`AttendancePreference`, `GeofenceManager`, `getLastGeofenceParams`, or
`getReminderGeofences`.

- [ ] **Step 2: Verify red state**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceReconciliationWorkerTest' --tests '*BootCompletedReceiverContractTest'
```

Expected: classes missing.

- [ ] **Step 3: Implement worker and receiver**

```kotlin
@HiltWorker
class GeofenceReconciliationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshAndReconcile: RefreshAndReconcileGeofenceRuntimeUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (
        refreshAndReconcile(GeofenceReconcileReason.BOOT_RECOVERY)
    ) {
        is RefreshAndReconcileGeofenceRuntimeResult.Reconciled,
        is RefreshAndReconcileGeofenceRuntimeResult.AuthUnavailable -> Result.success()
        is RefreshAndReconcileGeofenceRuntimeResult.BackendUnavailable -> Result.retry()
    }
}
```

The receiver enqueues:

```kotlin
OneTimeWorkRequestBuilder<GeofenceReconciliationWorker>()
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .addTag("geofence_runtime_reconcile")
    .build()
```

with `enqueueUniqueWork("geofence_runtime_reconcile", REPLACE, request)`.

Update manifest boot receiver to
`.data.platform.geofence.receiver.BootCompletedReceiver`; keep
`RECEIVE_BOOT_COMPLETED`.

- [ ] **Step 4: Run boot tests and compile**

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*GeofenceReconciliationWorkerTest' --tests '*BootCompletedReceiverContractTest'
.\gradlew.bat --no-daemon app:compileDebugKotlin
```

Expected: tests and compile pass.

- [ ] **Step 5: Commit Task 10**

```powershell
git add app/src/main/java/com/example/infinite_track/data/worker/GeofenceReconciliationWorker.kt app/src/main/java/com/example/infinite_track/data/platform/geofence/receiver/BootCompletedReceiver.kt app/src/test/java/com/example/infinite_track/data/worker/GeofenceReconciliationWorkerTest.kt app/src/test/java/com/example/infinite_track/data/platform/geofence/receiver/BootCompletedReceiverContractTest.kt app/src/main/AndroidManifest.xml
git commit -m "feat: reconcile geofence runtime after reboot"
```

---

### Task 11: Delete Legacy Geofence and Firebase Messaging Configuration

**Files:**

- Delete: `app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceManager.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/geofencing/ReminderGeofenceCandidate.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/geofencing/GeofenceBroadcastReceiver.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/geofencing/BootCompletedReceiver.kt`
- Delete: `app/src/main/java/com/example/infinite_track/presentation/fcm/InfiniteTrackFCMService.kt`
- Modify: `app/src/main/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreference.kt`
- Modify: `app/src/test/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreferenceRuntimeStateTest.kt`
- Create: `app/src/test/java/com/example/infinite_track/LegacyGeofenceConfigurationContractTest.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/UseCaseModule.kt`
- Modify: `app/src/main/java/com/example/infinite_track/di/RepositoryModule.kt`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `.github/workflows/android-branch-verification.yml`
- Modify: `.github/workflows/android-master-firebase-distribution.yml`

**Interfaces:**

- Consumes: completed cutover from Tasks 5–10.
- Produces: one runtime implementation and no FCM build/runtime path.

- [ ] **Step 1: Record the consumer baseline before deletion**

```powershell
rg -n 'GeofenceManager|ReminderGeofenceCandidate|presentation\.geofencing|saveLastGeofence|getLastGeofence|ReminderGeofence|canNotifyWithCooldown|setUserInsideGeofence' app/src/main app/src/test app/src/androidTest
rg -n -i 'firebase.messaging|FirebaseMessagingService|RemoteMessage|onNewToken|google-services' app build.gradle.kts gradle .github
```

Expected: legacy geofence hits are limited to the files scheduled here; active
Firebase runtime hits are the Messaging dependency/plugin/configuration and the
commented stub. Stop if a different bounded feature has appeared.

- [ ] **Step 2: Make cleanup contract tests fail first**

Add source contract assertions to
`AttendancePreferenceRuntimeStateTest`/a new
`LegacyGeofenceConfigurationContractTest` that fail while these names remain:

```text
LAST_GEOFENCE_
REMINDER_GEOFENCES_KEY
NOTIFICATION_COOLDOWNS_KEY
StoredGeofence
ReminderGeofence
firebase.messaging
FirebaseMessagingService
google.gms.google.services
```

Implement the source contract with repository-relative reads:

```kotlin
class LegacyGeofenceConfigurationContractTest {
    @Test
    fun `legacy geofence and FCM symbols are absent`() {
        val appRoot = File(".")
        val repositoryRoot = appRoot.parentFile
        val text = sequenceOf(
            File(appRoot, "src/main"),
            File(appRoot, "build.gradle.kts"),
            File(repositoryRoot, "build.gradle.kts"),
            File(repositoryRoot, "gradle/libs.versions.toml"),
            File(repositoryRoot, ".github/workflows")
        ).flatMap { root ->
            if (root.isFile) sequenceOf(root) else root.walkTopDown().filter(File::isFile)
        }.joinToString("\n") { it.readText() }

        listOf(
            "GeofenceManager",
            "LAST_GEOFENCE_",
            "REMINDER_GEOFENCES_KEY",
            "FirebaseMessagingService",
            "firebase.messaging",
            "google.gms.google.services"
        ).forEach { forbidden ->
            assertFalse("Forbidden symbol remains: $forbidden", text.contains(forbidden))
        }
    }
}
```

Run:

```powershell
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*LegacyGeofenceConfigurationContractTest'
```

Expected: test fails and lists the current legacy paths.

- [ ] **Step 3: Remove old geofence source and preference APIs**

Delete the four old presentation geofence files. In `AttendancePreference`,
retain only attendance session cache ownership:

```kotlin
suspend fun saveActiveAttendanceId(id: Int)
fun getActiveAttendanceId(): Flow<Int?>
suspend fun clearActiveAttendanceId()
suspend fun saveAttendanceSessionStateKey(key: String?)
fun getAttendanceSessionStateKey(): Flow<String?>
suspend fun clearAttendanceSessionState()
```

`clearAttendanceSessionState` removes only active attendance ID and session
state. Delete old geofence parameter/reminder/cooldown/inside keys, methods,
serializers, and data classes. Update auth/logout tests to assert attendance
session cleanup through this method and runtime cleanup through Task 5 store.

- [ ] **Step 4: Remove FCM and Google Services build wiring**

Delete:

```kotlin
implementation(libs.firebase.messaging)
alias(libs.plugins.google.gms.google.services)
```

from app build configuration; delete the apply-false Google Services alias from
root build configuration; delete `firebaseMessaging`, `firebase-messaging`,
`googleGmsGoogleServices`, and `google-gms-google-services` catalog entries.
Delete the commented FCM service file.

Remove `Restore google-services.json` and its secret validation from both
workflows. Remove only `app/google-services.json` from cleanup commands. Keep
all Firebase App Distribution CLI/service-account/app-ID/tester-group steps.

- [ ] **Step 5: Verify zero consumers and dependency removal**

```powershell
rg -n 'GeofenceManager|ReminderGeofenceCandidate|presentation\.geofencing|LAST_GEOFENCE_|REMINDER_GEOFENCES_KEY|StoredGeofence|ReminderGeofence' app/src/main app/src/test app/src/androidTest
rg -n -i 'firebase.messaging|FirebaseMessagingService|RemoteMessage|onNewToken|google-services' app build.gradle.kts gradle .github
.\gradlew.bat --no-daemon app:dependencies --configuration debugRuntimeClasspath | Select-String 'firebase-messaging'
.\gradlew.bat --no-daemon app:testDebugUnitTest --tests '*LegacyGeofenceConfigurationContractTest' --tests '*AttendancePreferenceRuntimeStateTest' --tests '*ClearAuthenticatedRuntimeUseCaseTest'
.\gradlew.bat --no-daemon app:assembleDebug
```

Expected: first search has no output; second search has no runtime/build/workflow
Google Services or Messaging output; dependency search has no output; tests and
assemble pass. Firebase Distribution strings remain only in distribution
workflow/docs and are not part of the second pattern.

- [ ] **Step 6: Commit Task 11**

```powershell
git add -A app/src/main/java/com/example/infinite_track/presentation/geofencing app/src/main/java/com/example/infinite_track/presentation/fcm app/src/main/java/com/example/infinite_track/data/soucre/local/preferences/AttendancePreference.kt app/src/test/java/com/example/infinite_track/data/soucre/local/preferences app/src/main/java/com/example/infinite_track/di app/build.gradle.kts build.gradle.kts gradle/libs.versions.toml .github/workflows
git commit -m "chore: remove legacy geofence and FCM configuration"
```

---

### Task 12: Add ADR, Run Full Gates, and Capture Runtime Evidence

**Files:**

- Create: `docs/adr/ADR-INF-223-geofence-runtime-reconciliation.md`
- Create: `docs/linear-sync/INF-223-runtime-verification.md`
- Modify: `docs/superpowers/plans/2026-07-24-inf-223-geofence-runtime-hardening.md` only to check completed boxes during execution.

**Interfaces:**

- Consumes: all implementation tasks.
- Produces: governance, verification, and PR handoff evidence.

- [ ] **Step 1: Write the ADR**

The ADR must contain these decisions in complete prose:

```text
Status: Accepted
Context: duplicated attendance/runtime state and blind boot restore
Decision: backend-truth resolver + one two-phase Android reconciler
Snapshot: schema v2, generation, effective date, Applying/Applied/Degraded
Notifications: local OS/Google Play Services only, no FCM
Boot: unique connected WorkManager refresh, never blind restore
Consequences: temporary monitoring unavailability is preferred to stale alerts
Rollback: revert by PR commit, never restore the old checkout reminder behavior
```

- [ ] **Step 2: Run the complete automated gate from a clean worktree**

```powershell
git status --short
git diff --check
.\gradlew.bat --no-daemon app:testDebugUnitTest
.\gradlew.bat --no-daemon app:compileDebugAndroidTestKotlin
.\gradlew.bat --no-daemon app:lintDebug
.\gradlew.bat --no-daemon app:assembleDebug
```

Expected: clean status before generated outputs, no diff errors, all unit tests
pass, Android tests compile, lint completes without unreported blockers, and
debug APK assembles. Record command timestamps and exact failure counts; do not
summarize a failed gate as passed.

- [ ] **Step 3: Run architecture and cleanup searches**

```powershell
rg -n 'presentation\.geofencing|GeofenceManager|ReminderGeofenceCandidate' app/src/main app/src/test app/src/androidTest
rg -n 'AttendancePreference' app/src/main/java/com/example/infinite_track/domain app/src/main/java/com/example/infinite_track/presentation/screen/attendance
rg -n -i 'firebase.messaging|FirebaseMessagingService|RemoteMessage|onNewToken|google-services' app build.gradle.kts gradle .github
rg -n 'NotificationManager|NotificationCompat' app/src/main/java/com/example/infinite_track
```

Expected: first three searches have no prohibited runtime hits; the notification
search points to local Android notification code.

- [ ] **Step 4: Execute supported-device verification**

```powershell
adb devices -l
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat -c
adb logcat -s GeofenceRuntime GeofenceReceiver WM-WorkerWrapper LocationEventWorker
```

Using a Google Play Services-capable emulator Extended Controls route or a
physical device, record each row in `docs/linear-sync/INF-223-runtime-verification.md`:

```text
WFO reminder applied IDs
WFH reminder ENTER/DWELL notification
approved WFA reminder notification
45-minute duplicate suppression
active registration after backend-confirmed check-in only
active identity after process recreation
active ENTER/EXIT inside-state update
seven-minute active duplicate suppression
unique LocationEventWorker enqueue
checkout removes active and does not restore reminders when completed
reboot refreshes and rejects stale snapshot
notification denial keeps runtime/evidence and skips notification
no FCM message/token path
```

Redact user identity, coordinates, auth data, and keys. If a row cannot run,
write `Needs Verification` with the missing device/backend prerequisite.

- [ ] **Step 5: Run review preparation**

Invoke `superpowers:requesting-code-review`, then prepare PR notes containing:

```text
Fact
Assumption
Mismatch
Risk
Needs Verification
Recommendation
```

Include affected files, automated gate results, device rows, cleanup evidence,
ADR path, and explicit confirmation that Firebase App Distribution remains
master-only.

- [ ] **Step 6: Commit verification documentation**

```powershell
git add docs/adr/ADR-INF-223-geofence-runtime-reconciliation.md docs/linear-sync/INF-223-runtime-verification.md docs/superpowers/plans/2026-07-24-inf-223-geofence-runtime-hardening.md
git commit -m "docs: record INF-223 runtime verification"
```

- [ ] **Step 7: Final verification after the documentation commit**

```powershell
git status --short --branch
git log --oneline --decorate -12
git diff origin/develop...HEAD --check
```

Expected: clean worktree, intentional task commits, and no whitespace errors.

## Spec Coverage Matrix

| Spec requirement | Implementing task(s) | Verification gate |
|---|---:|---|
| Domain-owned modes, targets, results, failures, repository | 1 | `GeofenceRuntimeContractTest` |
| Pure WFO/WFH/WFA candidates, identity and 10 m dedupe, invalid radius | 2 | `BuildReminderGeofenceCandidatesUseCaseTest` |
| Disabled/reminder/active/completed precedence and contradiction safety | 3 | `ResolveGeofenceRuntimeModeUseCaseTest` |
| Schema v2 atomic snapshot, generation, cooldown, inside state | 4 | `PreferencesGeofenceRuntimeStoreTest` |
| Idempotent no-op, batch reminders, two-phase apply/degraded rollback | 5 | `AndroidGeofenceRuntimeRepositoryTest` |
| Logout/full teardown through domain contract | 6 | logout integration tests |
| Backend refresh, check-in/out decoupling, completed semantics | 7 | coordinator and boundary tests |
| Candidate removal from ViewModel and small Compose projection | 8 | mapper and ownership tests |
| ENTER/DWELL reminder, ENTER/EXIT active, stale rejection, evidence | 9 | `GeofenceEventProcessorTest` |
| Reboot current-truth refresh without blind restore | 10 | worker and boot contract tests |
| Legacy geofence keys/classes and unused FCM/Google Services removal | 11 | cleanup searches, dependency graph, assemble |
| ADR, complete Gradle gates, device evidence, PR/review notes | 12 | documented automated and device matrix |

Coverage review result: every architecture, reminder, active, checkout, reboot,
logout, cleanup, receiver, and verification acceptance criterion from the
approved spec maps to at least one implementation task and one named gate.

## Execution Handoff

After this plan is committed, implementation has two supported paths:

1. **Subagent-Driven (recommended):** invoke
   `superpowers:subagent-driven-development`; use a fresh implementer for each
   task and run specification then quality review between task commits.
2. **Inline Execution:** invoke `superpowers:executing-plans`; execute tasks in
   ordered batches with review checkpoints.

Do not start implementation without selecting one path. Do not mark INF-223
Done until Task 12 device/runtime evidence and review verdict exist.
