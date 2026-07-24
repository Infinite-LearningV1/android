package com.example.infinite_track.data.platform.geofence

import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeSnapshot
import com.example.infinite_track.data.platform.geofence.store.GeofenceRuntimeStore
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceMode
import com.example.infinite_track.data.platform.geofence.store.PersistedGeofenceRegistration
import com.example.infinite_track.data.platform.geofence.store.PersistedReconciliationState
import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import com.example.infinite_track.domain.model.geofence.GeofenceDisabledReason
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeMode
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.repository.GeofenceRuntimeRepository
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class AndroidGeofenceRuntimeRepository @Inject constructor(
    private val platformClient: GeofencingPlatformClient,
    private val store: GeofenceRuntimeStore,
    private val requestIdCodec: GeofenceRequestIdCodec,
    private val clock: Clock
) : GeofenceRuntimeRepository {

    private val reconciliationMutex = Mutex()

    override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> =
        platformClient.observeReadiness()

    override suspend fun reconcile(mode: GeofenceRuntimeMode): GeofenceRuntimeResult =
        reconciliationMutex.withLock {
            reconcileLocked(mode)
        }

    override suspend fun clearForLogout(): GeofenceRuntimeResult = reconciliationMutex.withLock {
        val loggedOut = GeofenceRuntimeMode.Disabled(GeofenceDisabledReason.LOGGED_OUT)
        var removalFailure: Throwable? = null
        try {
            platformClient.removeOwnedGeofences()
        } catch (exception: CancellationException) {
            withContext(NonCancellable) { store.clear() }
            throw exception
        } catch (exception: Throwable) {
            removalFailure = exception
        }
        store.clear()
        removalFailure?.let { exception ->
            return@withLock GeofenceRuntimeResult.Degraded(
                loggedOut,
                GeofenceRuntimeFailure.RemovalFailed(exceptionCategory(exception))
            )
        }
        GeofenceRuntimeResult.Applied(loggedOut, generation = 0, logicalIds = emptySet())
    }

    private suspend fun reconcileLocked(mode: GeofenceRuntimeMode): GeofenceRuntimeResult {
        val currentSnapshot = store.readSnapshot()
        val logicalRegistrations = canonicalRegistrations(mode)
        if (currentSnapshot.isEqualApplied(mode, logicalRegistrations)) {
            return GeofenceRuntimeResult.NoOp(mode, currentSnapshot!!.generation)
        }

        val generation = (currentSnapshot?.generation ?: 0L) + 1L
        val registrations = logicalRegistrations.map { registration ->
            registration.copy(
                requestId = requestIdCodec.encode(
                    generation,
                    registration.kind,
                    registration.logicalId
                )
            )
        }
        val baseSnapshot = snapshotFor(mode, generation, registrations)
        val requestIds = registrations.map(PersistedGeofenceRegistration::requestId)
        if (requestIds.size != requestIds.toSet().size) {
            val failure = GeofenceRuntimeFailure.RegistrationFailed("request_id_collision")
            val rejectedSnapshot = baseSnapshot.copy(expectedRegistrations = emptyList())
            store.writeSnapshot(rejectedSnapshot)
            store.writeSnapshot(rejectedSnapshot.asDegraded("request_id_collision"))
            return GeofenceRuntimeResult.Degraded(mode, failure)
        }

        store.writeSnapshot(baseSnapshot)
        val readinessFailure = platformClient.observeReadiness().first().registration.toFailure()
        if (readinessFailure != null) {
            store.writeSnapshot(baseSnapshot.asDegraded(readinessFailure.failureCategory()))
            return GeofenceRuntimeResult.Degraded(mode, readinessFailure)
        }

        try {
            platformClient.removeOwnedGeofences()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            val failure = GeofenceRuntimeFailure.RemovalFailed(exceptionCategory(exception))
            bestEffortRemove()
            store.writeSnapshot(baseSnapshot.asDegraded(failure.failureCategory()))
            return GeofenceRuntimeResult.Degraded(mode, failure)
        }

        if (registrations.isNotEmpty()) {
            try {
                platformClient.addAll(registrations.map { it.toPlatform() })
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                val failure = GeofenceRuntimeFailure.RegistrationFailed(
                    exceptionCategory(exception)
                )
                bestEffortRemove()
                store.writeSnapshot(baseSnapshot.asDegraded(failure.failureCategory()))
                return GeofenceRuntimeResult.Degraded(mode, failure)
            }
        }

        store.writeSnapshot(
            baseSnapshot.copy(
                appliedRequestIds = requestIds.toSet(),
                reconciliationState = PersistedReconciliationState.APPLIED
            )
        )
        return GeofenceRuntimeResult.Applied(
            mode,
            generation,
            registrations.map(PersistedGeofenceRegistration::logicalId).toSet()
        )
    }

    private suspend fun bestEffortRemove() {
        try {
            platformClient.removeOwnedGeofences()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            // The degraded snapshot remains authoritative even if cleanup also fails.
        }
    }

    private fun canonicalRegistrations(mode: GeofenceRuntimeMode): List<PersistedGeofenceRegistration> =
        when (mode) {
            is GeofenceRuntimeMode.Reminder -> mode.candidates.map { candidate ->
                PersistedGeofenceRegistration(
                    requestId = "",
                    logicalId = candidate.logicalId,
                    kind = PersistedRegistrationKind.REMINDER,
                    label = candidate.label,
                    latitude = candidate.coordinate.latitude,
                    longitude = candidate.coordinate.longitude,
                    radiusMeters = candidate.radius.value,
                    attendanceId = null
                )
            }
            is GeofenceRuntimeMode.ActiveMonitoring -> listOf(
                PersistedGeofenceRegistration(
                    requestId = "",
                    logicalId = "active:${mode.attendanceId}:${mode.target.identity.ownerKey}",
                    kind = PersistedRegistrationKind.ACTIVE,
                    label = mode.target.label,
                    latitude = mode.target.coordinate.latitude,
                    longitude = mode.target.coordinate.longitude,
                    radiusMeters = mode.target.radius.value,
                    attendanceId = mode.attendanceId
                )
            )
            is GeofenceRuntimeMode.Completed,
            is GeofenceRuntimeMode.Disabled -> emptyList()
        }.sortedWith(compareBy(PersistedGeofenceRegistration::kind, PersistedGeofenceRegistration::logicalId))

    private fun snapshotFor(
        mode: GeofenceRuntimeMode,
        generation: Long,
        registrations: List<PersistedGeofenceRegistration>
    ) = GeofenceRuntimeSnapshot(
        generation = generation,
        effectiveDateIso = mode.effectiveDateIso(),
        expectedMode = mode.persistedMode(),
        attendanceId = (mode as? GeofenceRuntimeMode.ActiveMonitoring)?.attendanceId,
        sessionStateKey = null,
        expectedRegistrations = registrations,
        appliedRequestIds = emptySet(),
        reconciliationState = PersistedReconciliationState.APPLYING,
        insideActiveGeofence = false,
        failureCategory = null,
        updatedAtEpochMillis = clock.millis()
    )

    private fun GeofenceRuntimeSnapshot?.isEqualApplied(
        mode: GeofenceRuntimeMode,
        registrations: List<PersistedGeofenceRegistration>
    ): Boolean {
        if (this == null || reconciliationState != PersistedReconciliationState.APPLIED) return false
        if (expectedMode != mode.persistedMode()) return false
        if (effectiveDateIso != mode.effectiveDateIso()) return false
        if (attendanceId != (mode as? GeofenceRuntimeMode.ActiveMonitoring)?.attendanceId) return false
        return expectedRegistrations
            .map { it.withoutRequestId() }
            .sortedWith(compareBy(CanonicalRegistration::kind, CanonicalRegistration::logicalId)) ==
            registrations.map { it.withoutRequestId() }
    }

    private fun PersistedGeofenceRegistration.withoutRequestId() = CanonicalRegistration(
        logicalId,
        kind,
        label,
        latitude,
        longitude,
        radiusMeters,
        attendanceId
    )

    private fun PersistedGeofenceRegistration.toPlatform() = PlatformGeofenceRegistration(
        requestId = requestId,
        logicalId = logicalId,
        kind = kind,
        label = label,
        coordinate = GeoCoordinate(latitude, longitude),
        radius = DistanceMeters(radiusMeters),
        attendanceId = attendanceId
    )

    private fun GeofenceRuntimeSnapshot.asDegraded(category: String) = copy(
        appliedRequestIds = emptySet(),
        reconciliationState = PersistedReconciliationState.DEGRADED,
        failureCategory = category,
        updatedAtEpochMillis = clock.millis()
    )

    private fun RegistrationReadiness.toFailure(): GeofenceRuntimeFailure? = when (this) {
        RegistrationReadiness.Ready -> null
        is RegistrationReadiness.PermissionRequired ->
            GeofenceRuntimeFailure.PermissionNotGranted(missing)
        RegistrationReadiness.DeviceLocationDisabled ->
            GeofenceRuntimeFailure.DeviceLocationDisabled
        RegistrationReadiness.PlayServicesUnavailable ->
            GeofenceRuntimeFailure.PlayServicesUnavailable
    }

    private fun GeofenceRuntimeFailure.failureCategory(): String = when (this) {
        is GeofenceRuntimeFailure.PermissionNotGranted -> "permission_not_granted"
        GeofenceRuntimeFailure.DeviceLocationDisabled -> "device_location_disabled"
        GeofenceRuntimeFailure.PlayServicesUnavailable -> "play_services_unavailable"
        is GeofenceRuntimeFailure.RegistrationFailed -> category
        is GeofenceRuntimeFailure.RemovalFailed -> category
        else -> javaClass.simpleName.toSnakeCase()
    }

    private fun exceptionCategory(exception: Throwable): String =
        exception.javaClass.simpleName.ifBlank { "unknown" }.toSnakeCase()

    private fun String.toSnakeCase(): String =
        replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

    private fun GeofenceRuntimeMode.persistedMode(): PersistedGeofenceMode = when (this) {
        is GeofenceRuntimeMode.Disabled -> PersistedGeofenceMode.DISABLED
        is GeofenceRuntimeMode.Reminder -> PersistedGeofenceMode.REMINDER
        is GeofenceRuntimeMode.ActiveMonitoring -> PersistedGeofenceMode.ACTIVE
        is GeofenceRuntimeMode.Completed -> PersistedGeofenceMode.COMPLETED
    }

    private fun GeofenceRuntimeMode.effectiveDateIso(): String? = when (this) {
        is GeofenceRuntimeMode.Reminder -> effectiveDate.toString()
        is GeofenceRuntimeMode.ActiveMonitoring -> effectiveDate.toString()
        is GeofenceRuntimeMode.Completed -> effectiveDate.toString()
        is GeofenceRuntimeMode.Disabled -> null
    }

    private data class CanonicalRegistration(
        val logicalId: String,
        val kind: PersistedRegistrationKind,
        val label: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Double,
        val attendanceId: Int?
    )
}
