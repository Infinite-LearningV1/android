package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.AttendancePreparationBlockReason
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendancePreparationRecovery
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceActionResolverTest {

    @Test
    fun `ready preparation allows check in when backend allows it`() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = null, canCheckIn = true, canCheckOut = false),
            preparation = readyPreparation()
        )

        val actionState = AttendanceActionResolver.resolve(state)

        assertEquals(
            AttendanceActionState.Ready(
                intent = AttendanceActionIntent.CHECK_IN,
                label = "Check-in di sini"
            ),
            actionState
        )
    }

    @Test
    fun `checkout readiness ignores selected work mode`() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = "08:00", canCheckIn = false, canCheckOut = true),
            preparation = unresolvedWfaPreparation()
        )

        assertTrue(AttendanceActionResolver.resolve(state) is AttendanceActionState.Ready)
    }

    @Test
    fun `backend with no available action is completed regardless of preparation`() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = "08:00", canCheckIn = false, canCheckOut = false),
            preparation = unresolvedWfaPreparation()
        )

        assertEquals(AttendanceActionState.Completed, AttendanceActionResolver.resolve(state))
    }

    @Test
    fun `resolving preparation keeps check in loading`() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = null, canCheckIn = true, canCheckOut = false),
            preparation = unresolvedWfaPreparation()
        )

        assertEquals(AttendanceActionState.Loading, AttendanceActionResolver.resolve(state))
    }

    @Test
    fun `typed WFA lifecycle blocker prevents check in`() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = null, canCheckIn = true, canCheckOut = false),
            preparation = unresolvedWfaPreparation().copy(
                eligibility = AttendancePreparationEligibility.Blocked(
                    reason = AttendancePreparationBlockReason.WFA_PENDING,
                    recovery = AttendancePreparationRecovery.OPEN_WFA_REQUESTS
                )
            )
        )

        val actionState = AttendanceActionResolver.resolve(state)

        assertTrue(actionState is AttendanceActionState.Blocked)
        assertEquals(
            AttendanceBlockReason.WFA_BOOKING_REQUIRED,
            (actionState as AttendanceActionState.Blocked).reason
        )
    }

    private fun readyPreparation(): AttendancePreparationState {
        val target = AuthoritativeTargetLocation(
            targetId = TargetLocationId("status:1"),
            mode = WorkMode.WFO,
            source = TargetLocationSource.STATUS_TODAY,
            coordinate = GeoCoordinate(-0.90, 119.88),
            radius = DistanceMeters(100.0),
            displayName = "Office"
        )
        val range = TargetRangeStatus.Inside(DistanceMeters(10.0))
        return AttendancePreparationState(
            selectedMode = WorkMode.WFO,
            targetResolution = TargetLocationResolution.Resolved(target),
            rangeStatus = range,
            eligibility = AttendancePreparationEligibility.Ready(target, range)
        )
    }

    private fun unresolvedWfaPreparation() = AttendancePreparationState(
        selectedMode = WorkMode.WFA,
        targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA),
        wfaDiscovery = WfaDiscoveryState.Loading,
        eligibility = AttendancePreparationEligibility.Resolving
    )

    private fun todayStatus(
        checkedInAt: String?,
        canCheckIn: Boolean,
        canCheckOut: Boolean
    ): TodayStatus {
        return TodayStatus(
            canCheckIn = canCheckIn,
            canCheckOut = canCheckOut,
            checkedInAt = checkedInAt,
            checkedOutAt = null,
            activeMode = "Work From Office",
            activeLocation = Location(
                locationId = 1,
                description = "Office",
                latitude = -0.90,
                longitude = 119.88,
                radius = 100,
                category = "WFO"
            ),
            todayDate = "2026-07-23",
            isHoliday = false,
            holidayCheckinEnabled = false,
            currentTime = "08:00",
            checkinWindow = CheckinWindow(startTime = "07:00", endTime = "09:00"),
            checkoutAutoTime = "17:00"
        )
    }
}
