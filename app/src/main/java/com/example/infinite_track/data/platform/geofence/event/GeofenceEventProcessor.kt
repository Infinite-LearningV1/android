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
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class GeofenceEventProcessor @Inject constructor(
    private val store: GeofenceRuntimeStore,
    private val requestIdCodec: GeofenceRequestIdCodec,
    private val notificationGateway: GeofenceNotificationGateway,
    private val evidenceScheduler: LocationEvidenceScheduler,
    private val clock: Clock
) {

    suspend fun process(event: GeofenceTransitionEvent): GeofenceEventOutcome {
        val snapshot = store.readSnapshot() ?: return GeofenceEventOutcome.Ignored("missing_snapshot")
        validateSnapshot(snapshot)?.let { reason -> return GeofenceEventOutcome.Ignored(reason) }

        val processedLogicalIds = linkedSetOf<String>()
        var ignoredReason = "no_request_ids"
        event.requestIds.forEach { requestId ->
            val registration = snapshot.expectedRegistrations
                .singleOrNull { it.requestId == requestId }
            val validationError = validateRegistration(snapshot, registration, requestId, event.transition)
            if (validationError != null) {
                ignoredReason = validationError
                return@forEach
            }

            processValidatedRegistration(snapshot, registration!!, event)
            processedLogicalIds += registration.logicalId
        }
        return if (processedLogicalIds.isEmpty()) {
            GeofenceEventOutcome.Ignored(ignoredReason)
        } else {
            GeofenceEventOutcome.Processed(processedLogicalIds)
        }
    }

    private fun validateSnapshot(snapshot: GeofenceRuntimeSnapshot): String? = when {
        snapshot.schemaVersion != SNAPSHOT_SCHEMA_VERSION -> "unsupported_snapshot_schema"
        snapshot.reconciliationState != PersistedReconciliationState.APPLIED ->
            "snapshot_not_applied"
        snapshot.effectiveDateIso != LocalDate.now(clock).toString() -> "stale_date"
        else -> null
    }

    private fun validateRegistration(
        snapshot: GeofenceRuntimeSnapshot,
        registration: PersistedGeofenceRegistration?,
        requestId: String,
        transition: GeofenceTransition
    ): String? {
        if (requestId !in snapshot.appliedRequestIds) return "request_id_not_applied"
        if (registration == null) return "unknown_request_id"

        val decoded = requestIdCodec.decode(requestId) ?: return "invalid_request_id"
        if (decoded.generation != snapshot.generation) return "stale_generation"
        if (decoded.kind != registration.kind) return "request_kind_mismatch"
        if (!matchesSnapshotMode(snapshot.expectedMode, registration.kind)) return "mode_kind_mismatch"
        if (!matchesTransition(registration.kind, transition)) return "transition_kind_mismatch"
        if (registration.kind == PersistedRegistrationKind.ACTIVE && !hasActiveSessionMatch(snapshot, registration)) {
            return "active_session_mismatch"
        }
        return null
    }

    private suspend fun processValidatedRegistration(
        snapshot: GeofenceRuntimeSnapshot,
        registration: PersistedGeofenceRegistration,
        event: GeofenceTransitionEvent
    ) {
        when (registration.kind) {
            PersistedRegistrationKind.REMINDER -> notifyReminderIfAllowed(registration)
            PersistedRegistrationKind.ACTIVE -> processActiveEvent(snapshot, registration, event)
        }
    }

    private suspend fun notifyReminderIfAllowed(registration: PersistedGeofenceRegistration) {
        if (!notificationGateway.canPostNotifications()) return
        if (
            store.claimNotification(
                key = "reminder:${registration.logicalId}",
                nowMillis = clock.millis(),
                cooldownMillis = REMINDER_COOLDOWN_MILLIS
            )
        ) {
            notificationGateway.showReminder(registration.label)
        }
    }

    private suspend fun processActiveEvent(
        snapshot: GeofenceRuntimeSnapshot,
        registration: PersistedGeofenceRegistration,
        event: GeofenceTransitionEvent
    ) {
        val attendanceId = snapshot.attendanceId!!
        store.setInsideActiveGeofence(event.transition == GeofenceTransition.ENTER)
        evidenceScheduler.enqueue(
            attendanceId = attendanceId,
            logicalId = registration.logicalId,
            transition = event.transition,
            occurredAt = event.occurredAt
        )

        if (!notificationGateway.canPostNotifications()) return
        if (
            store.claimNotification(
                key = "active:$attendanceId:${event.transition.name}:${registration.logicalId}",
                nowMillis = clock.millis(),
                cooldownMillis = ACTIVE_COOLDOWN_MILLIS
            )
        ) {
            notificationGateway.showActive(event.transition, registration.label)
        }
    }

    private fun matchesSnapshotMode(
        mode: PersistedGeofenceMode,
        kind: PersistedRegistrationKind
    ): Boolean = when (kind) {
        PersistedRegistrationKind.REMINDER -> mode == PersistedGeofenceMode.REMINDER
        PersistedRegistrationKind.ACTIVE -> mode == PersistedGeofenceMode.ACTIVE
    }

    private fun matchesTransition(
        kind: PersistedRegistrationKind,
        transition: GeofenceTransition
    ): Boolean = when (kind) {
        PersistedRegistrationKind.REMINDER ->
            transition == GeofenceTransition.ENTER || transition == GeofenceTransition.DWELL
        PersistedRegistrationKind.ACTIVE ->
            transition == GeofenceTransition.ENTER || transition == GeofenceTransition.EXIT
    }

    private fun hasActiveSessionMatch(
        snapshot: GeofenceRuntimeSnapshot,
        registration: PersistedGeofenceRegistration
    ): Boolean = snapshot.expectedMode == PersistedGeofenceMode.ACTIVE &&
        snapshot.attendanceId != null &&
        snapshot.attendanceId > 0 &&
        snapshot.sessionStateKey == ACTIVE_SESSION_STATE &&
        registration.attendanceId == snapshot.attendanceId

    private companion object {
        const val SNAPSHOT_SCHEMA_VERSION = 2
        const val ACTIVE_SESSION_STATE = "active"
        const val REMINDER_COOLDOWN_MILLIS = 2_700_000L
        const val ACTIVE_COOLDOWN_MILLIS = 420_000L
    }
}
