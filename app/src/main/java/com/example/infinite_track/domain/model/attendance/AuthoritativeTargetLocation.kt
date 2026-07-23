package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate

@JvmInline
value class TargetLocationId(val value: String)

enum class TargetLocationSource {
    STATUS_TODAY,
    ADMIN_PROFILE,
    APPROVED_WFA_BOOKING
}

data class ApprovedWfaTargetContext(
    val bookingId: Int,
    val scheduleDate: String
)

data class AuthoritativeTargetLocation(
    val targetId: TargetLocationId,
    val mode: WorkMode,
    val source: TargetLocationSource,
    val coordinate: GeoCoordinate,
    val radius: DistanceMeters,
    val displayName: String,
    val approvedWfaContext: ApprovedWfaTargetContext? = null
)

sealed interface TargetLocationResolution {
    data class Resolving(val mode: WorkMode) : TargetLocationResolution

    data class Resolved(val target: AuthoritativeTargetLocation) : TargetLocationResolution

    data class Unavailable(
        val mode: WorkMode,
        val reason: TargetUnavailableReason,
        val recovery: TargetRecoveryAction
    ) : TargetLocationResolution

    data class Failed(
        val mode: WorkMode,
        val failure: TargetResolutionFailure
    ) : TargetLocationResolution
}

enum class TargetUnavailableReason {
    WFO_NOT_ASSIGNED,
    WFH_PROFILE_CONTRACT_VIOLATION,
    WFA_NOT_REQUESTED,
    WFA_PENDING,
    WFA_REJECTED,
    WFA_APPROVAL_MISSING_FOR_DATE
}

enum class TargetRecoveryAction {
    REFRESH_STATUS,
    REFRESH_PROFILE,
    CONTACT_ADMIN,
    OPEN_WFA_BOOKING,
    OPEN_WFA_REQUESTS
}

enum class TargetResolutionFailure {
    STATUS_REFRESH_FAILED,
    PROFILE_REFRESH_FAILED,
    BOOKING_REFRESH_FAILED,
    INVALID_COORDINATE,
    INVALID_RADIUS
}
