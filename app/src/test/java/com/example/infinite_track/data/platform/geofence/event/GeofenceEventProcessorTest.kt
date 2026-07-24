package com.example.infinite_track.data.platform.geofence.event

import com.example.infinite_track.data.platform.geofence.GeofenceRequestIdCodec
import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeSnapshot
import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeStore
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceMode
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceRegistration
import com.example.infinite_track.data.platform.geofence.store.PersistedReconciliationState
import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceEventProcessorTest {

    @Test
    fun `reminder enter notifies once and does not enqueue evidence`() = runTest {
        val fixture = fixture(reminderSnapshot())
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        val outcome = fixture.processor.process(event(GeofenceTransition.ENTER, registration.requestId))

        assertEquals(
            GeofenceEventOutcome.Processed(setOf(registration.logicalId)),
            outcome
        )
        assertEquals(listOf(registration.label), fixture.notifications.reminders)
        assertTrue(fixture.evidence.requests.isEmpty())
        assertTrue(fixture.store.insideWrites.isEmpty())
        assertEquals(
            listOf(Claim("reminder:${registration.logicalId}", CLOCK.millis(), 2_700_000L)),
            fixture.store.claims
        )
    }

    @Test
    fun `reminder dwell notifies and respects 45 minute cooldown`() = runTest {
        val fixture = fixture(reminderSnapshot())
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.processor.process(event(GeofenceTransition.DWELL, registration.requestId))
        fixture.processor.process(event(GeofenceTransition.DWELL, registration.requestId))

        assertEquals(listOf(registration.label), fixture.notifications.reminders)
        assertTrue(fixture.evidence.requests.isEmpty())
        assertEquals(
            listOf(
                Claim("reminder:${registration.logicalId}", CLOCK.millis(), 2_700_000L),
                Claim("reminder:${registration.logicalId}", CLOCK.millis(), 2_700_000L)
            ),
            fixture.store.claims
        )
    }

    @Test
    fun `active enter sets inside notifies and enqueues unique evidence`() = runTest {
        val fixture = fixture(activeSnapshot())
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        val outcome = fixture.processor.process(event(GeofenceTransition.ENTER, registration.requestId))

        assertEquals(
            GeofenceEventOutcome.Processed(setOf(registration.logicalId)),
            outcome
        )
        assertEquals(listOf(GeofenceTransition.ENTER to registration.label), fixture.notifications.active)
        assertEquals(listOf(true), fixture.store.insideWrites)
        assertTrue(fixture.store.snapshot!!.insideActiveGeofence)
        assertEquals(
            listOf(
                Evidence(
                    attendanceId = 91,
                    logicalId = registration.logicalId,
                    transition = GeofenceTransition.ENTER,
                    occurredAt = EVENT_AT
                )
            ),
            fixture.evidence.requests
        )
        assertEquals(
            listOf(
                Claim(
                    "active:91:ENTER:${registration.logicalId}",
                    CLOCK.millis(),
                    420_000L
                )
            ),
            fixture.store.claims
        )
    }

    @Test
    fun `active exit clears inside and enqueues evidence`() = runTest {
        val fixture = fixture(activeSnapshot(inside = true))
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        val outcome = fixture.processor.process(event(GeofenceTransition.EXIT, registration.requestId))

        assertEquals(
            GeofenceEventOutcome.Processed(setOf(registration.logicalId)),
            outcome
        )
        assertEquals(listOf(false), fixture.store.insideWrites)
        assertFalse(fixture.store.snapshot!!.insideActiveGeofence)
        assertEquals(
            listOf(Evidence(91, registration.logicalId, GeofenceTransition.EXIT, EVENT_AT)),
            fixture.evidence.requests
        )
        assertEquals(listOf(GeofenceTransition.EXIT to registration.label), fixture.notifications.active)
    }

    @Test
    fun `notification denial keeps active state and evidence but skips notify`() = runTest {
        val fixture = fixture(activeSnapshot(), canPostNotifications = false)
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        val outcome = fixture.processor.process(event(GeofenceTransition.ENTER, registration.requestId))

        assertEquals(
            GeofenceEventOutcome.Processed(setOf(registration.logicalId)),
            outcome
        )
        assertEquals(listOf(true), fixture.store.insideWrites)
        assertEquals(
            listOf(Evidence(91, registration.logicalId, GeofenceTransition.ENTER, EVENT_AT)),
            fixture.evidence.requests
        )
        assertTrue(fixture.notifications.active.isEmpty())
        assertTrue(fixture.store.claims.isEmpty())
    }

    @Test
    fun `active notification respects 7 minute cooldown without suppressing evidence`() = runTest {
        val fixture = fixture(activeSnapshot())
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.processor.process(event(GeofenceTransition.ENTER, registration.requestId))
        fixture.processor.process(event(GeofenceTransition.ENTER, registration.requestId))

        assertEquals(listOf(GeofenceTransition.ENTER to registration.label), fixture.notifications.active)
        assertEquals(2, fixture.evidence.requests.size)
        assertEquals(
            listOf(
                Claim("active:91:ENTER:${registration.logicalId}", CLOCK.millis(), 420_000L),
                Claim("active:91:ENTER:${registration.logicalId}", CLOCK.millis(), 420_000L)
            ),
            fixture.store.claims
        )
    }

    @Test
    fun `stale date is ignored`() = runTest {
        val fixture = fixture(reminderSnapshot(effectiveDate = TODAY.minusDays(1)))
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, registration.requestId))
    }

    @Test
    fun `stale generation is ignored`() = runTest {
        val fixture = fixture(reminderSnapshot(generation = 7, requestGeneration = 6))
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, registration.requestId))
    }

    @Test
    fun `unknown request id is ignored`() = runTest {
        val fixture = fixture(reminderSnapshot())
        val unknownRequestId = codec.encode(7, PersistedRegistrationKind.REMINDER, "reminder:unknown")

        fixture.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, unknownRequestId))
    }

    @Test
    fun `applying and degraded snapshots are ignored`() = runTest {
        val applying = fixture(
            reminderSnapshot().copy(reconciliationState = PersistedReconciliationState.APPLYING)
        )
        val applyingRequestId = applying.store.snapshot!!.expectedRegistrations.single().requestId
        applying.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, applyingRequestId))

        val degraded = fixture(
            reminderSnapshot().copy(reconciliationState = PersistedReconciliationState.DEGRADED)
        )
        val degradedRequestId = degraded.store.snapshot!!.expectedRegistrations.single().requestId
        degraded.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, degradedRequestId))
    }

    @Test
    fun `active session mismatch is ignored`() = runTest {
        val fixture = fixture(activeSnapshot(sessionStateKey = "completed"))
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, registration.requestId))
    }

    @Test
    fun `transition not registered for the geofence kind is ignored`() = runTest {
        val fixture = fixture(activeSnapshot())
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.assertIgnoredAndUnchanged(event(GeofenceTransition.DWELL, registration.requestId))
    }

    @Test
    fun `registration kind inconsistent with snapshot mode is ignored`() = runTest {
        val fixture = fixture(activeSnapshot().copy(expectedMode = PersistedGeofenceMode.REMINDER))
        val registration = fixture.store.snapshot!!.expectedRegistrations.single()

        fixture.assertIgnoredAndUnchanged(event(GeofenceTransition.ENTER, registration.requestId))
    }

    private fun fixture(
        snapshot: GeofenceRuntimeSnapshot,
        canPostNotifications: Boolean = true
    ): Fixture {
        val store = FakeStore(snapshot)
        val notifications = FakeNotificationGateway(canPostNotifications)
        val evidence = FakeLocationEvidenceScheduler()
        return Fixture(
            processor = GeofenceEventProcessor(store, codec, notifications, evidence, CLOCK),
            store = store,
            notifications = notifications,
            evidence = evidence
        )
    }

    private fun reminderSnapshot(
        effectiveDate: LocalDate = TODAY,
        generation: Long = 7,
        requestGeneration: Long = generation
    ): GeofenceRuntimeSnapshot {
        val registration = registration(
            generation = requestGeneration,
            kind = PersistedRegistrationKind.REMINDER,
            logicalId = "reminder:primary:11",
            label = "Main Office",
            attendanceId = null
        )
        return snapshot(
            generation = generation,
            effectiveDate = effectiveDate,
            expectedMode = PersistedGeofenceMode.REMINDER,
            attendanceId = null,
            sessionStateKey = null,
            registrations = listOf(registration)
        )
    }

    private fun activeSnapshot(
        inside: Boolean = false,
        sessionStateKey: String? = "active"
    ): GeofenceRuntimeSnapshot {
        val registration = registration(
            generation = 7,
            kind = PersistedRegistrationKind.ACTIVE,
            logicalId = "active:91:office:11",
            label = "Main Office",
            attendanceId = 91
        )
        return snapshot(
            expectedMode = PersistedGeofenceMode.ACTIVE,
            attendanceId = 91,
            sessionStateKey = sessionStateKey,
            registrations = listOf(registration),
            inside = inside
        )
    }

    private fun registration(
        generation: Long,
        kind: PersistedRegistrationKind,
        logicalId: String,
        label: String,
        attendanceId: Int?
    ) = PersistedGeofenceRegistration(
        requestId = codec.encode(generation, kind, logicalId),
        logicalId = logicalId,
        kind = kind,
        label = label,
        latitude = -0.87,
        longitude = 119.86,
        radiusMeters = 100.0,
        attendanceId = attendanceId
    )

    private fun snapshot(
        generation: Long = 7,
        effectiveDate: LocalDate = TODAY,
        expectedMode: PersistedGeofenceMode,
        attendanceId: Int?,
        sessionStateKey: String?,
        registrations: List<PersistedGeofenceRegistration>,
        inside: Boolean = false
    ) = GeofenceRuntimeSnapshot(
        generation = generation,
        effectiveDateIso = effectiveDate.toString(),
        expectedMode = expectedMode,
        attendanceId = attendanceId,
        sessionStateKey = sessionStateKey,
        expectedRegistrations = registrations,
        appliedRequestIds = registrations.map(PersistedGeofenceRegistration::requestId).toSet(),
        reconciliationState = PersistedReconciliationState.APPLIED,
        insideActiveGeofence = inside,
        failureCategory = null,
        updatedAtEpochMillis = CLOCK.millis()
    )

    private fun event(
        transition: GeofenceTransition,
        requestId: String
    ) = GeofenceTransitionEvent(transition, setOf(requestId), EVENT_AT)

    private suspend fun Fixture.assertIgnoredAndUnchanged(event: GeofenceTransitionEvent) {
        val snapshotBefore = store.snapshot

        val outcome = processor.process(event)

        assertTrue("Expected ignored outcome, was $outcome", outcome is GeofenceEventOutcome.Ignored)
        assertEquals(snapshotBefore, store.snapshot)
        assertTrue(store.claims.isEmpty())
        assertTrue(store.insideWrites.isEmpty())
        assertTrue(notifications.reminders.isEmpty())
        assertTrue(notifications.active.isEmpty())
        assertTrue(evidence.requests.isEmpty())
    }

    private data class Fixture(
        val processor: GeofenceEventProcessor,
        val store: FakeStore,
        val notifications: FakeNotificationGateway,
        val evidence: FakeLocationEvidenceScheduler
    )

    private data class Claim(val key: String, val nowMillis: Long, val cooldownMillis: Long)

    private data class Evidence(
        val attendanceId: Int,
        val logicalId: String,
        val transition: GeofenceTransition,
        val occurredAt: Instant
    )

    private class FakeStore(initialSnapshot: GeofenceRuntimeSnapshot) : GeofenceRuntimeStore {
        var snapshot: GeofenceRuntimeSnapshot? = initialSnapshot
        val claims = mutableListOf<Claim>()
        val insideWrites = mutableListOf<Boolean>()
        private val claimedAtByKey = mutableMapOf<String, Long>()

        override suspend fun readSnapshot(): GeofenceRuntimeSnapshot? = snapshot

        override suspend fun writeSnapshot(snapshot: GeofenceRuntimeSnapshot) {
            this.snapshot = snapshot
        }

        override suspend fun claimNotification(
            key: String,
            nowMillis: Long,
            cooldownMillis: Long
        ): Boolean {
            claims += Claim(key, nowMillis, cooldownMillis)
            val previous = claimedAtByKey[key]
            val allowed = previous == null || nowMillis - previous >= cooldownMillis
            if (allowed) claimedAtByKey[key] = nowMillis
            return allowed
        }

        override suspend fun setInsideActiveGeofence(isInside: Boolean) {
            insideWrites += isInside
            snapshot = snapshot?.copy(insideActiveGeofence = isInside)
        }

        override suspend fun clear() {
            snapshot = null
        }
    }

    private class FakeNotificationGateway(
        private val notificationsAllowed: Boolean
    ) : GeofenceNotificationGateway {
        val reminders = mutableListOf<String>()
        val active = mutableListOf<Pair<GeofenceTransition, String>>()

        override fun canPostNotifications(): Boolean = notificationsAllowed

        override fun showReminder(label: String) {
            reminders += label
        }

        override fun showActive(transition: GeofenceTransition, label: String) {
            active += transition to label
        }
    }

    private class FakeLocationEvidenceScheduler : LocationEvidenceScheduler {
        val requests = mutableListOf<Evidence>()

        override fun enqueue(
            attendanceId: Int,
            logicalId: String,
            transition: GeofenceTransition,
            occurredAt: Instant
        ) {
            requests += Evidence(attendanceId, logicalId, transition, occurredAt)
        }
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.parse("2026-07-25")
        val EVENT_AT: Instant = Instant.parse("2026-07-25T01:02:03Z")
        val CLOCK: Clock = Clock.fixed(EVENT_AT, ZoneOffset.UTC)
        val codec = GeofenceRequestIdCodec()
    }
}
