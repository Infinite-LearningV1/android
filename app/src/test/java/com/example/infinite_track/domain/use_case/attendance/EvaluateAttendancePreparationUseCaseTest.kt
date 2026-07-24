package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TargetRangeUnknownReason
import com.example.infinite_track.domain.model.attendance.TargetRecoveryAction
import com.example.infinite_track.domain.model.attendance.TargetUnavailableReason
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateAttendancePreparationUseCaseTest {

    private val preparationUseCase = EvaluateAttendancePreparationUseCase()

    @Test
    fun `resolving target remains resolving`() {
        assertEquals(
            AttendancePreparationEligibility.Resolving,
            preparationUseCase(
                resolution = TargetLocationResolution.Resolving(WorkMode.WFO),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
            )
        )
    }

    @Test
    fun `WFH profile violation maps to refresh profile recovery`() {
        val result = preparationUseCase(
            resolution = TargetLocationResolution.Unavailable(
                WorkMode.WFH,
                TargetUnavailableReason.WFH_PROFILE_CONTRACT_VIOLATION,
                TargetRecoveryAction.REFRESH_PROFILE
            ),
            rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
        )

        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.WFH_PROFILE_CONTRACT,
                AttendancePreparationRecovery.REFRESH_PROFILE
            ),
            result
        )
    }

    @Test
    fun `unassigned office maps to refresh status recovery`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.WFO_NOT_ASSIGNED,
                AttendancePreparationRecovery.REFRESH_STATUS
            ),
            preparationUseCase(
                resolution = TargetLocationResolution.Unavailable(
                    WorkMode.WFO,
                    TargetUnavailableReason.WFO_NOT_ASSIGNED,
                    TargetRecoveryAction.REFRESH_STATUS
                ),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
            )
        )
    }

    @Test
    fun `WFA not requested maps to booking recovery`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.WFA_NOT_REQUESTED,
                AttendancePreparationRecovery.OPEN_WFA_BOOKING
            ),
            preparationUseCase(
                resolution = unavailableWfa(
                    TargetUnavailableReason.WFA_NOT_REQUESTED,
                    TargetRecoveryAction.OPEN_WFA_BOOKING
                ),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
            )
        )
    }

    @Test
    fun `pending WFA maps to requests recovery`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.WFA_PENDING,
                AttendancePreparationRecovery.OPEN_WFA_REQUESTS
            ),
            preparationUseCase(
                resolution = unavailableWfa(
                    TargetUnavailableReason.WFA_PENDING,
                    TargetRecoveryAction.OPEN_WFA_REQUESTS
                ),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
            )
        )
    }

    @Test
    fun `rejected WFA maps to requests recovery`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.WFA_REJECTED,
                AttendancePreparationRecovery.OPEN_WFA_REQUESTS
            ),
            preparationUseCase(
                resolution = unavailableWfa(
                    TargetUnavailableReason.WFA_REJECTED,
                    TargetRecoveryAction.OPEN_WFA_REQUESTS
                ),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
            )
        )
    }

    @Test
    fun `resolved target with unavailable current location refreshes location`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.CURRENT_LOCATION_UNAVAILABLE,
                AttendancePreparationRecovery.REFRESH_LOCATION
            ),
            preparationUseCase(
                resolution = TargetLocationResolution.Resolved(target()),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_UNAVAILABLE)
            )
        )
    }

    @Test
    fun `resolved target with stale current location refreshes location`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.CURRENT_LOCATION_STALE,
                AttendancePreparationRecovery.REFRESH_LOCATION
            ),
            preparationUseCase(
                resolution = TargetLocationResolution.Resolved(target()),
                rangeStatus = TargetRangeStatus.Unknown(TargetRangeUnknownReason.CURRENT_LOCATION_STALE)
            )
        )
    }

    @Test
    fun `outside range focuses authoritative target`() {
        assertEquals(
            AttendancePreparationEligibility.Blocked(
                AttendancePreparationBlockReason.OUTSIDE_TARGET_RANGE,
                AttendancePreparationRecovery.FOCUS_TARGET
            ),
            preparationUseCase(
                resolution = TargetLocationResolution.Resolved(target()),
                rangeStatus = TargetRangeStatus.Outside(DistanceMeters(101.0))
            )
        )
    }

    @Test
    fun `inside resolved target is ready`() {
        val target = target()
        val range = TargetRangeStatus.Inside(DistanceMeters.Zero)

        assertEquals(
            AttendancePreparationEligibility.Ready(target, range),
            preparationUseCase(
                resolution = TargetLocationResolution.Resolved(target),
                rangeStatus = range
            )
        )
    }

    private fun unavailableWfa(
        reason: TargetUnavailableReason,
        recovery: TargetRecoveryAction
    ) = TargetLocationResolution.Unavailable(WorkMode.WFA, reason, recovery)

    private fun target() = AuthoritativeTargetLocation(
        targetId = TargetLocationId("status:1"),
        mode = WorkMode.WFO,
        source = TargetLocationSource.STATUS_TODAY,
        coordinate = GeoCoordinate(-0.90, 119.88),
        radius = DistanceMeters(100.0),
        displayName = "Kantor Infinite Track"
    )
}
