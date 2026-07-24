package com.example.infinite_track.domain.model.geofence

import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import java.time.LocalDate

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
    init {
        require(ownerKey.isNotBlank())
    }
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
    init {
        require(logicalId.isNotBlank())
    }
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
        init {
            require(attendanceId > 0)
        }
    }

    data class Completed(val effectiveDate: LocalDate) : GeofenceRuntimeMode
}
