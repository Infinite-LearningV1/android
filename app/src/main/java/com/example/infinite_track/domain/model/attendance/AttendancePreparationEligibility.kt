package com.example.infinite_track.domain.model.attendance

sealed interface AttendancePreparationEligibility {
    data object Resolving : AttendancePreparationEligibility

    data class Ready(
        val target: AuthoritativeTargetLocation,
        val range: TargetRangeStatus.Inside
    ) : AttendancePreparationEligibility

    data class Blocked(
        val reason: AttendancePreparationBlockReason,
        val recovery: AttendancePreparationRecovery
    ) : AttendancePreparationEligibility
}

enum class AttendancePreparationBlockReason {
    WFO_NOT_ASSIGNED,
    WFH_PROFILE_CONTRACT,
    WFA_NOT_REQUESTED,
    WFA_PENDING,
    WFA_REJECTED,
    WFA_APPROVAL_MISSING_FOR_DATE,
    CURRENT_LOCATION_UNAVAILABLE,
    CURRENT_LOCATION_STALE,
    OUTSIDE_TARGET_RANGE,
    STATUS_REFRESH_FAILED,
    PROFILE_REFRESH_FAILED,
    BOOKING_REFRESH_FAILED,
    TARGET_CONTRACT_INVALID
}

enum class AttendancePreparationRecovery {
    REFRESH_STATUS,
    REFRESH_PROFILE,
    REFRESH_LOCATION,
    FOCUS_TARGET,
    OPEN_WFA_BOOKING,
    OPEN_WFA_REQUESTS,
    CONTACT_ADMIN
}
