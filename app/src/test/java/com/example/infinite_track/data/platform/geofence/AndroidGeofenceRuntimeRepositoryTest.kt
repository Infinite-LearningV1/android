package com.example.infinite_track.data.platform.geofence

import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeSnapshot
import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeStore
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceMode
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceRegistration
import com.example.infinite_track.data.platform.geofence.store.PersistedReconciliationState
import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.geofence.ActiveMonitoringTarget
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofencePermissionRequirement
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.GeofenceTargetIdentity
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.domain.model.geofence.ReminderCandidateSource
import com.example.infinite_track.domain.model.geofence.ReminderGeofenceCandidate
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidGeofenceRuntimeRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-07-25T01:02:03Z"), ZoneOffset.UTC)

    @Test
    fun `unchanged applied mode is no op`() = runTest {
        val mode = reminderMode(reminder("reminder:primary:11", 11, -0.87, 119.86))
        val store = InMemoryGeofenceRuntimeStore(appliedSnapshot(mode, generation = 7))
        val platform = FakeGeofencingPlatformClient()
        val repository = repository(platform, store)

        val result = repository.reconcile(mode)

        assertEquals(GeofenceRuntimeResult.NoOp(mode, 7), result)
        assertEquals(emptyList<String>(), platform.calls)
        assertEquals(0, store.writeCount)
    }

    @Test
    fun `reminder set is added in one batch`() = runTest {
        val mode = reminderMode(
            reminder("reminder:primary:11", 11, -0.87, 119.86),
            reminder("reminder:wfa:31:22", 22, -0.92, 119.89)
        )
        val store = InMemoryGeofenceRuntimeStore()
        val platform = FakeGeofencingPlatformClient()

        val result = repository(platform, store).reconcile(mode)

        assertTrue(result is GeofenceRuntimeResult.Applied)
        assertEquals(listOf("remove", "add"), platform.calls)
        assertEquals(1, platform.addBatches.size)
        assertEquals(setOf("reminder:primary:11", "reminder:wfa:31:22"), platform.addBatches.single().map { it.logicalId }.toSet())
        assertEquals(PersistedReconciliationState.APPLIED, store.snapshot?.reconciliationState)
        assertEquals(store.snapshot?.expectedRegistrations?.map { it.requestId }?.toSet(), store.snapshot?.appliedRequestIds)
    }

    @Test
    fun `reminder replacement removes then adds complete set`() = runTest {
        val oldMode = reminderMode(reminder("reminder:primary:11", 11, -0.87, 119.86))
        val nextMode = reminderMode(
            reminder("reminder:primary:12", 12, -0.88, 119.87),
            reminder("reminder:wfa:31:22", 22, -0.92, 119.89)
        )
        val store = InMemoryGeofenceRuntimeStore(appliedSnapshot(oldMode, generation = 2))
        val platform = FakeGeofencingPlatformClient()

        val result = repository(platform, store).reconcile(nextMode)

        assertTrue(result is GeofenceRuntimeResult.Applied)
        assertEquals(listOf("remove", "add"), platform.calls)
        assertEquals(3L, store.snapshot?.generation)
        assertEquals(nextMode.candidates.map { it.logicalId }.toSet(), platform.addBatches.single().map { it.logicalId }.toSet())
    }

    @Test
    fun `reminder to active produces exactly one active registration`() = runTest {
        val store = InMemoryGeofenceRuntimeStore(
            appliedSnapshot(reminderMode(reminder("reminder:primary:11", 11, -0.87, 119.86)), 4)
        )
        val platform = FakeGeofencingPlatformClient()
        val mode = activeMode()

        repository(platform, store).reconcile(mode)

        val registration = platform.addBatches.single().single()
        assertEquals(listOf("remove", "add"), platform.calls)
        assertEquals("active:91:office:11", registration.logicalId)
        assertEquals(PersistedRegistrationKind.ACTIVE, registration.kind)
        assertEquals(91, registration.attendanceId)
        assertEquals(PersistedGeofenceMode.ACTIVE, store.snapshot?.expectedMode)
    }

    @Test
    fun `legacy active snapshot without session state is reconciled`() = runTest {
        val store = InMemoryGeofenceRuntimeStore(appliedSnapshot(activeMode(), generation = 7))
        val platform = FakeGeofencingPlatformClient()

        val result = repository(platform, store).reconcile(activeMode())

        assertEquals(listOf("remove", "add"), platform.calls)
        assertEquals(GeofenceRuntimeResult.Applied(activeMode(), 8, setOf("active:91:office:11")), result)
        assertEquals("active", store.snapshot?.sessionStateKey)
    }

    @Test
    fun `active monitoring snapshot records the active session state`() = runTest {
        val store = InMemoryGeofenceRuntimeStore()
        val platform = FakeGeofencingPlatformClient()

        repository(platform, store).reconcile(activeMode())

        assertEquals("active", store.snapshot?.sessionStateKey)
    }

    @Test
    fun `active to completed removes and applies empty set`() = runTest {
        val store = InMemoryGeofenceRuntimeStore(appliedSnapshot(activeMode(), 8))
        val platform = FakeGeofencingPlatformClient()
        val mode = GeofenceRuntimeMode.Completed(LocalDate.parse("2026-07-25"))

        val result = repository(platform, store).reconcile(mode)

        assertEquals(listOf("remove"), platform.calls)
        assertEquals(emptyList<List<PlatformGeofenceRegistration>>(), platform.addBatches)
        assertEquals(PersistedReconciliationState.APPLIED, store.snapshot?.reconciliationState)
        assertEquals(emptySet<String>(), store.snapshot?.appliedRequestIds)
        assertEquals(GeofenceRuntimeResult.Applied(mode, 9, emptySet()), result)
    }

    @Test
    fun `registration failure rolls back and persists degraded empty applied set`() = runTest {
        val store = InMemoryGeofenceRuntimeStore()
        val platform = FakeGeofencingPlatformClient(addFailure = IllegalStateException("batch failed"))
        val mode = reminderMode(reminder("reminder:primary:11", 11, -0.87, 119.86))

        val result = repository(platform, store).reconcile(mode)

        assertEquals(listOf("remove", "add", "remove"), platform.calls)
        assertTrue((result as GeofenceRuntimeResult.Degraded).failure is GeofenceRuntimeFailure.RegistrationFailed)
        assertEquals(PersistedReconciliationState.DEGRADED, store.snapshot?.reconciliationState)
        assertEquals(emptySet<String>(), store.snapshot?.appliedRequestIds)
    }

    @Test
    fun `permission denial returns degraded without calling add`() = runTest {
        val missing = setOf(GeofencePermissionRequirement.BACKGROUND_LOCATION)
        val readiness = ready().copy(registration = RegistrationReadiness.PermissionRequired(missing))
        val store = InMemoryGeofenceRuntimeStore()
        val platform = FakeGeofencingPlatformClient(readiness)
        val mode = reminderMode(reminder("reminder:primary:11", 11, -0.87, 119.86))

        val result = repository(platform, store).reconcile(mode)

        assertEquals(emptyList<String>(), platform.calls)
        assertEquals(GeofenceRuntimeFailure.PermissionNotGranted(missing), (result as GeofenceRuntimeResult.Degraded).failure)
        assertEquals(2, store.writeCount)
        assertEquals(
            listOf(
                PersistedReconciliationState.APPLYING,
                PersistedReconciliationState.DEGRADED
            ),
            store.writtenSnapshots.map(GeofenceRuntimeSnapshot::reconciliationState)
        )
        assertTrue(store.writtenSnapshots.all { it.appliedRequestIds.isEmpty() })
        assertEquals(PersistedReconciliationState.DEGRADED, store.snapshot?.reconciliationState)
        assertEquals(emptySet<String>(), store.snapshot?.appliedRequestIds)
    }

    @Test
    fun `request id collision degrades without persisting an ambiguous mapping`() = runTest {
        val duplicate = reminder("reminder:primary:11", 11, -0.87, 119.86)
        val store = InMemoryGeofenceRuntimeStore()
        val platform = FakeGeofencingPlatformClient()
        val mode = reminderMode(duplicate, duplicate.copy(label = "Duplicate label"))

        val result = repository(platform, store).reconcile(mode)

        assertEquals(
            GeofenceRuntimeFailure.RegistrationFailed("request_id_collision"),
            (result as GeofenceRuntimeResult.Degraded).failure
        )
        assertEquals(emptyList<String>(), platform.calls)
        assertEquals(
            listOf(
                PersistedReconciliationState.APPLYING,
                PersistedReconciliationState.DEGRADED
            ),
            store.writtenSnapshots.map(GeofenceRuntimeSnapshot::reconciliationState)
        )
        assertTrue(store.writtenSnapshots.all { it.expectedRegistrations.isEmpty() })
        assertEquals(emptyList<PersistedGeofenceRegistration>(), store.snapshot?.expectedRegistrations)
        assertEquals(emptySet<String>(), store.snapshot?.appliedRequestIds)
        assertEquals(PersistedReconciliationState.DEGRADED, store.snapshot?.reconciliationState)
    }

    @Test
    fun `logout removes owned geofences and clears store`() = runTest {
        val store = InMemoryGeofenceRuntimeStore(appliedSnapshot(activeMode(), 3)).apply {
            claimNotification("old", 1, 10)
        }
        val platform = FakeGeofencingPlatformClient()

        val result = repository(platform, store).clearForLogout()

        assertEquals(listOf("remove"), platform.calls)
        assertTrue(store.cleared)
        assertEquals(null, store.snapshot)
        assertEquals(
            GeofenceRuntimeResult.Applied(
                GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT),
                0,
                emptySet()
            ),
            result
        )
    }

    private fun repository(
        platform: FakeGeofencingPlatformClient,
        store: InMemoryGeofenceRuntimeStore
    ) = AndroidGeofenceRuntimeRepository(
        platform,
        store,
        GeofenceRequestIdCodec(),
        clock,
        GeofenceRuntimeOperationLock()
    )

    private fun ready() = GeofenceRuntimeReadiness(
        registration = RegistrationReadiness.Ready,
        notification = NotificationReadiness.READY
    )

    private fun reminderMode(vararg candidates: ReminderGeofenceCandidate) =
        GeofenceRuntimeMode.Reminder(LocalDate.parse("2026-07-25"), candidates.toList())

    private fun reminder(
        logicalId: String,
        stableLocationId: Int,
        latitude: Double,
        longitude: Double
    ) = ReminderGeofenceCandidate(
        logicalId = logicalId,
        identity = GeofenceTargetIdentity(stableLocationId, "office:$stableLocationId"),
        mode = WorkMode.WFO,
        label = "Location $stableLocationId",
        coordinate = GeoCoordinate(latitude, longitude),
        radius = DistanceMeters(100.0),
        source = ReminderCandidateSource.STATUS_TODAY
    )

    private fun activeMode() = GeofenceRuntimeMode.ActiveMonitoring(
        effectiveDate = LocalDate.parse("2026-07-25"),
        attendanceId = 91,
        target = ActiveMonitoringTarget(
            identity = GeofenceTargetIdentity(11, "office:11"),
            mode = WorkMode.WFO,
            label = "Main Office",
            coordinate = GeoCoordinate(-0.87, 119.86),
            radius = DistanceMeters(100.0)
        )
    )

    private fun appliedSnapshot(mode: GeofenceRuntimeMode, generation: Long): GeofenceRuntimeSnapshot {
        val registrations = when (mode) {
            is GeofenceRuntimeMode.Reminder -> mode.candidates.map { candidate ->
                persistedRegistration(
                    generation,
                    PersistedRegistrationKind.REMINDER,
                    candidate.logicalId,
                    candidate.label,
                    candidate.coordinate,
                    candidate.radius,
                    null
                )
            }
            is GeofenceRuntimeMode.ActiveMonitoring -> listOf(
                persistedRegistration(
                    generation,
                    PersistedRegistrationKind.ACTIVE,
                    "active:${mode.attendanceId}:${mode.target.identity.ownerKey}",
                    mode.target.label,
                    mode.target.coordinate,
                    mode.target.radius,
                    mode.attendanceId
                )
            )
            else -> emptyList()
        }
        return GeofenceRuntimeSnapshot(
            generation = generation,
            effectiveDateIso = when (mode) {
                is GeofenceRuntimeMode.Reminder -> mode.effectiveDate.toString()
                is GeofenceRuntimeMode.ActiveMonitoring -> mode.effectiveDate.toString()
                is GeofenceRuntimeMode.Completed -> mode.effectiveDate.toString()
                is GeofenceRuntimeMode.Disabled -> null
            },
            expectedMode = when (mode) {
                is GeofenceRuntimeMode.Disabled -> PersistedGeofenceMode.DISABLED
                is GeofenceRuntimeMode.Reminder -> PersistedGeofenceMode.REMINDER
                is GeofenceRuntimeMode.ActiveMonitoring -> PersistedGeofenceMode.ACTIVE
                is GeofenceRuntimeMode.Completed -> PersistedGeofenceMode.COMPLETED
            },
            attendanceId = (mode as? GeofenceRuntimeMode.ActiveMonitoring)?.attendanceId,
            sessionStateKey = null,
            expectedRegistrations = registrations,
            appliedRequestIds = registrations.map { it.requestId }.toSet(),
            reconciliationState = PersistedReconciliationState.APPLIED,
            insideActiveGeofence = false,
            failureCategory = null,
            updatedAtEpochMillis = clock.millis()
        )
    }

    private fun persistedRegistration(
        generation: Long,
        kind: PersistedRegistrationKind,
        logicalId: String,
        label: String,
        coordinate: GeoCoordinate,
        radius: DistanceMeters,
        attendanceId: Int?
    ): PersistedGeofenceRegistration = PersistedGeofenceRegistration(
        requestId = GeofenceRequestIdCodec().encode(generation, kind, logicalId),
        logicalId = logicalId,
        kind = kind,
        label = label,
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
        radiusMeters = radius.value,
        attendanceId = attendanceId
    )

    private class FakeGeofencingPlatformClient(
        readiness: GeofenceRuntimeReadiness = GeofenceRuntimeReadiness(
            registration = RegistrationReadiness.Ready,
            notification = NotificationReadiness.READY
        ),
        private val removeFailure: Throwable? = null,
        private val addFailure: Throwable? = null
    ) : GeofencingPlatformClient {
        private val readinessFlow = MutableStateFlow(readiness)
        val calls = mutableListOf<String>()
        val addBatches = mutableListOf<List<PlatformGeofenceRegistration>>()

        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = readinessFlow

        override suspend fun removeOwnedGeofences() {
            calls += "remove"
            removeFailure?.let { throw it }
        }

        override suspend fun addAll(registrations: List<PlatformGeofenceRegistration>) {
            calls += "add"
            addBatches += registrations.toList()
            addFailure?.let { throw it }
        }
    }

    private class InMemoryGeofenceRuntimeStore(
        initialSnapshot: GeofenceRuntimeSnapshot? = null
    ) : GeofenceRuntimeStore {
        var snapshot: GeofenceRuntimeSnapshot? = initialSnapshot
        var writeCount = 0
        var cleared = false
        val writtenSnapshots = mutableListOf<GeofenceRuntimeSnapshot>()
        private val cooldowns = mutableMapOf<String, Long>()

        override suspend fun readSnapshot(): GeofenceRuntimeSnapshot? = snapshot

        override suspend fun writeSnapshot(snapshot: GeofenceRuntimeSnapshot) {
            this.snapshot = snapshot
            writeCount++
            writtenSnapshots += snapshot
        }

        override suspend fun claimNotification(key: String, nowMillis: Long, cooldownMillis: Long): Boolean {
            val previous = cooldowns[key]
            val allowed = previous == null || nowMillis - previous >= cooldownMillis
            if (allowed) cooldowns[key] = nowMillis
            return allowed
        }

        override suspend fun setInsideActiveGeofence(isInside: Boolean) {
            snapshot = snapshot?.copy(insideActiveGeofence = isInside)
        }

        override suspend fun clear() {
            snapshot = null
            cooldowns.clear()
            cleared = true
        }
    }
}
