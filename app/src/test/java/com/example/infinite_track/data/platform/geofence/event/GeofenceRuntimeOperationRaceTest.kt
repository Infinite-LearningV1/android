package com.example.infinite_track.data.platform.geofence.event

import com.example.infinite_track.data.platform.geofence.AndroidGeofenceRuntimeRepository
import com.example.infinite_track.data.platform.geofence.GeofenceRequestIdCodec
import com.example.infinite_track.data.platform.geofence.GeofenceRuntimeOperationLock
import com.example.infinite_track.data.platform.geofence.GeofencingPlatformClient
import com.example.infinite_track.data.platform.geofence.PlatformGeofenceRegistration
import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeSnapshot
import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeStore
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceMode
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceRegistration
import com.example.infinite_track.data.platform.geofence.store.PersistedReconciliationState
import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeofenceRuntimeOperationRaceTest {

    @Test
    fun `logout cannot clear runtime before a validated active event finishes its side effects`() = runTest {
        val store = BlockingActiveStore(activeSnapshot())
        val evidence = RecordingEvidenceScheduler(store.operationOrder)
        val operationLock = GeofenceRuntimeOperationLock()
        val processor = GeofenceEventProcessor(
            store = store,
            requestIdCodec = CODEC,
            notificationGateway = NotificationsDenied,
            evidenceScheduler = evidence,
            clock = CLOCK,
            operationLock = operationLock
        )
        val repository = AndroidGeofenceRuntimeRepository(
            platformClient = ReadyPlatformClient,
            store = store,
            requestIdCodec = CODEC,
            clock = CLOCK,
            operationLock = operationLock
        )
        val requestId = store.snapshot!!.expectedRegistrations.single().requestId

        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            processor.process(GeofenceTransitionEvent(GeofenceTransition.ENTER, setOf(requestId), EVENT_AT))
        }
        store.insideWriteStarted.await()

        val logout = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.clearForLogout()
        }
        assertFalse("logout cleared the snapshot while the event was still active", store.clearCompleted)

        store.allowInsideWrite.complete(Unit)
        event.await()
        logout.await()

        assertEquals(listOf("inside", "evidence", "clear"), store.operationOrder)
    }

    @Test
    fun `reconcile cannot replace runtime before a validated active event finishes its side effects`() = runTest {
        val store = BlockingActiveStore(activeSnapshot())
        val evidence = RecordingEvidenceScheduler(store.operationOrder)
        val operationLock = GeofenceRuntimeOperationLock()
        val processor = GeofenceEventProcessor(
            store = store,
            requestIdCodec = CODEC,
            notificationGateway = NotificationsDenied,
            evidenceScheduler = evidence,
            clock = CLOCK,
            operationLock = operationLock
        )
        val repository = AndroidGeofenceRuntimeRepository(
            platformClient = RecordingPlatformClient(store.operationOrder),
            store = store,
            requestIdCodec = CODEC,
            clock = CLOCK,
            operationLock = operationLock
        )
        val requestId = store.snapshot!!.expectedRegistrations.single().requestId

        val event = async(UnconfinedTestDispatcher(testScheduler)) {
            processor.process(GeofenceTransitionEvent(GeofenceTransition.ENTER, setOf(requestId), EVENT_AT))
        }
        store.insideWriteStarted.await()

        val reconcile = async(UnconfinedTestDispatcher(testScheduler)) {
            repository.reconcile(GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.NO_ELIGIBLE_SESSION))
        }
        assertFalse("reconcile removed registrations while the event was still active", store.operationOrder.contains("reconcile"))

        store.allowInsideWrite.complete(Unit)
        event.await()
        reconcile.await()

        assertEquals(listOf("inside", "evidence", "reconcile"), store.operationOrder)
    }

    private fun activeSnapshot(): GeofenceRuntimeSnapshot {
        val logicalId = "active:91:office:11"
        val requestId = CODEC.encode(7, PersistedRegistrationKind.ACTIVE, logicalId)
        val registration = PersistedGeofenceRegistration(
            requestId = requestId,
            logicalId = logicalId,
            kind = PersistedRegistrationKind.ACTIVE,
            label = "Main Office",
            latitude = -0.87,
            longitude = 119.86,
            radiusMeters = 100.0,
            attendanceId = 91
        )
        return GeofenceRuntimeSnapshot(
            generation = 7,
            effectiveDateIso = LocalDate.now(CLOCK).toString(),
            expectedMode = PersistedGeofenceMode.ACTIVE,
            attendanceId = 91,
            sessionStateKey = "active",
            expectedRegistrations = listOf(registration),
            appliedRequestIds = setOf(requestId),
            reconciliationState = PersistedReconciliationState.APPLIED,
            insideActiveGeofence = false,
            failureCategory = null,
            updatedAtEpochMillis = CLOCK.millis()
        )
    }

    private class BlockingActiveStore(
        initialSnapshot: GeofenceRuntimeSnapshot
    ) : GeofenceRuntimeStore {
        var snapshot: GeofenceRuntimeSnapshot? = initialSnapshot
        val insideWriteStarted = CompletableDeferred<Unit>()
        val allowInsideWrite = CompletableDeferred<Unit>()
        val operationOrder = mutableListOf<String>()
        var clearCompleted = false

        override suspend fun readSnapshot(): GeofenceRuntimeSnapshot? = snapshot

        override suspend fun writeSnapshot(snapshot: GeofenceRuntimeSnapshot) {
            this.snapshot = snapshot
        }

        override suspend fun claimNotification(
            key: String,
            nowMillis: Long,
            cooldownMillis: Long
        ): Boolean = true

        override suspend fun setInsideActiveGeofence(isInside: Boolean) {
            insideWriteStarted.complete(Unit)
            allowInsideWrite.await()
            operationOrder += "inside"
            snapshot = snapshot?.copy(insideActiveGeofence = isInside)
        }

        override suspend fun clear() {
            operationOrder += "clear"
            snapshot = null
            clearCompleted = true
        }
    }

    private class RecordingEvidenceScheduler(
        private val operationOrder: MutableList<String>
    ) : LocationEvidenceScheduler {
        override fun enqueue(
            attendanceId: Int,
            logicalId: String,
            transition: GeofenceTransition,
            occurredAt: Instant
        ) {
            operationOrder += "evidence"
        }
    }

    private object NotificationsDenied : GeofenceNotificationGateway {
        override fun canPostNotifications(): Boolean = false
        override fun showReminder(label: String) = Unit
        override fun showActive(transition: GeofenceTransition, label: String) = Unit
    }

    private object ReadyPlatformClient : GeofencingPlatformClient {
        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = flowOf(
            GeofenceRuntimeReadiness(
                registration = RegistrationReadiness.Ready,
                notification = NotificationReadiness.READY
            )
        )

        override suspend fun removeOwnedGeofences() = Unit

        override suspend fun addAll(registrations: List<PlatformGeofenceRegistration>) = Unit
    }

    private class RecordingPlatformClient(
        private val operationOrder: MutableList<String>
    ) : GeofencingPlatformClient {
        override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = ReadyPlatformClient.observeReadiness()

        override suspend fun removeOwnedGeofences() {
            operationOrder += "reconcile"
        }

        override suspend fun addAll(registrations: List<PlatformGeofenceRegistration>) = Unit
    }

    private companion object {
        val EVENT_AT: Instant = Instant.parse("2026-07-25T01:02:03Z")
        val CLOCK: Clock = Clock.fixed(EVENT_AT, ZoneOffset.UTC)
        val CODEC = GeofenceRequestIdCodec()
    }
}
