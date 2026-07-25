package com.example.infinite_track.domain.model.geofence

sealed interface GeofenceRuntimeFailure {
    data class PermissionNotGranted(
        val missing: Set<GeofencePermissionRequirement>
    ) : GeofenceRuntimeFailure

    data object DeviceLocationDisabled : GeofenceRuntimeFailure
    data object PlayServicesUnavailable : GeofenceRuntimeFailure
    data class InvalidCoordinate(val source: ReminderCandidateSource) : GeofenceRuntimeFailure
    data class InvalidAuthoritativeRadius(
        val source: ReminderCandidateSource
    ) : GeofenceRuntimeFailure

    data object ActiveTargetUnavailable : GeofenceRuntimeFailure
    data class InconsistentSessionTruth(
        val attendanceId: Int?,
        val stateKey: String?
    ) : GeofenceRuntimeFailure

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

    data class NoOp(
        override val mode: GeofenceRuntimeMode,
        val generation: Long
    ) : GeofenceRuntimeResult

    data class Degraded(
        override val mode: GeofenceRuntimeMode,
        val failure: GeofenceRuntimeFailure
    ) : GeofenceRuntimeResult
}
