package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetResolutionFailure
import com.example.infinite_track.domain.model.attendance.TargetUnavailableReason
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import javax.inject.Inject

class EvaluateAttendancePreparationUseCase @Inject constructor() {

    operator fun invoke(
        resolution: TargetLocationResolution,
        rangeStatus: TargetRangeStatus
    ): AttendancePreparationEligibility {
        return when (resolution) {
            is TargetLocationResolution.Resolving -> AttendancePreparationEligibility.Resolving
            is TargetLocationResolution.Unavailable -> resolution.unavailableEligibility()
            is TargetLocationResolution.Failed -> resolution.failedEligibility()
            is TargetLocationResolution.Resolved -> resolution.resolvedEligibility(rangeStatus)
        }
    }

    private fun TargetLocationResolution.Resolved.resolvedEligibility(
        rangeStatus: TargetRangeStatus
    ): AttendancePreparationEligibility = when (rangeStatus) {
        is TargetRangeStatus.Inside -> AttendancePreparationEligibility.Ready(target, rangeStatus)
        is TargetRangeStatus.Outside -> blocked(
            AttendancePreparationBlockReason.OUTSIDE_TARGET_RANGE,
            AttendancePreparationRecovery.FOCUS_TARGET
        )

        is TargetRangeStatus.Unknown -> when (rangeStatus.reason) {
            TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE -> blocked(
                AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE,
                AttendancePreparationRecovery.REFRESH_LOCATION
            )

            TargetRangeUnknownReason.CURRENT_LOCATION_STALE -> blocked(
                AttendancePreparationBlockReason.CURRENT_LOCATION_STALE,
                AttendancePreparationRecovery.REFRESH_LOCATION
            )
        }
    }

    private fun TargetLocationResolution.Unavailable.unavailableEligibility() = when (reason) {
        TargetUnavailableReason.WFO_NOT_ASSIGNED -> blocked(
            AttendancePreparationBlockReason.WFO_NOT_ASSIGNED,
            AttendancePreparationRecovery.REFRESH_STATUS
        )

        TargetUnavailableReason.WFH_PROFILE_CONTRACT_VIOLATION -> blocked(
            AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT,
            AttendancePreparationRecovery.REFRESH_PROFILE
        )

        TargetUnavailableReason.WFA_NOT_REQUESTED -> blocked(
            AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
            AttendancePreparationRecovery.OPEN_WFA_BOOKING
        )

        TargetUnavailableReason.WFA_PENDING -> blocked(
            AttendancePreparationBlockReason.WFA_PENDING,
            AttendancePreparationRecovery.OPEN_WFA_REQUESTS
        )

        TargetUnavailableReason.WFA_REJECTED -> blocked(
            AttendancePreparationBlockReason.WFA_REJECTED,
            AttendancePreparationRecovery.OPEN_WFA_REQUESTS
        )

        TargetUnavailableReason.WFA_APPROVAL_MISSING_FOR_DATE -> blocked(
            AttendancePreparationBlockReason.WFA_APPROVAL_MISSING_FOR_DATE,
            AttendancePreparationRecovery.OPEN_WFA_REQUESTS
        )
    }

    private fun TargetLocationResolution.Failed.failedEligibility() = when (failure) {
        TargetResolutionFailure.STATUS_REFRESH_FAILED -> blocked(
            AttendancePreparationBlockReason.STATUS_REFRESH_FAILED,
            AttendancePreparationRecovery.REFRESH_STATUS
        )

        TargetResolutionFailure.PROFILE_REFRESH_FAILED -> blocked(
            AttendancePreparationBlockReason.PROFILE_REFRESH_FAILED,
            AttendancePreparationRecovery.REFRESH_PROFILE
        )

        TargetResolutionFailure.BOOKING_REFRESH_FAILED -> blocked(
            AttendancePreparationBlockReason.BOOKING_REFRESH_FAILED,
            AttendancePreparationRecovery.OPEN_WFA_REQUESTS
        )

        TargetResolutionFailure.INVALID_COORDINATE,
        TargetResolutionFailure.INVALID_RADIUS -> blocked(
            AttendancePreparationBlockReason.TARGET_CONTRACT_INVALID,
            AttendancePreparationRecovery.CONTACT_ADMIN
        )
    }

    private fun blocked(
        reason: AttendancePreparationBlockReason,
        recovery: AttendancePreparationRecovery
    ) = AttendancePreparationEligibility.Blocked(reason, recovery)
}
