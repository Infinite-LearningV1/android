package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.ApprovedWfaTargetContext
import com.example.infinite_track.domain.model.attendance.AttendanceActionIntent
import com.example.infinite_track.domain.model.attendance.AttendancePreparationEligibility
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitCommand
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationResolution
import com.example.infinite_track.domain.model.attendance.TargetRangeStatus
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceSubmitCommandBuilderTest {

    private val target = AuthoritativeTargetLocation(
        targetId = TargetLocationId("t-1"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.89, 119.87),
        radius = DistanceMeters(100.0),
        displayName = "Target",
        approvedWfaContext = ApprovedWfaTargetContext(bookingId = 42, scheduleDate = "2026-07-27")
    )

    private fun readyState() = AttendanceScreenState(
        preparation = com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationState(
            selectedMode = WorkMode.WFA,
            targetResolution = TargetLocationResolution.Resolved(target),
            eligibility = AttendancePreparationEligibility.Ready(
                target = target,
                range = TargetRangeStatus.Inside(DistanceMeters(10.0))
            )
        )
    )

    @Test
    fun `check-in with resolved target and ready eligibility builds CheckIn command`() {
        val command = AttendanceSubmitCommandBuilder.build(
            AttendanceActionIntent.CHECK_IN,
            readyState()
        )
        assertEquals(
            AttendanceSubmitCommand.CheckIn(workMode = WorkMode.WFA, authoritativeTarget = target),
            command
        )
    }

    @Test
    fun `check-in with unresolved target builds null`() {
        val state = readyState().copy(
            preparation = readyState().preparation.copy(
                targetResolution = TargetLocationResolution.Resolving(WorkMode.WFA)
            )
        )
        assertNull(AttendanceSubmitCommandBuilder.build(AttendanceActionIntent.CHECK_IN, state))
    }

    @Test
    fun `check-in with non-ready eligibility builds null`() {
        val state = readyState().copy(
            preparation = readyState().preparation.copy(
                eligibility = AttendancePreparationEligibility.Resolving
            )
        )
        assertNull(AttendanceSubmitCommandBuilder.build(AttendanceActionIntent.CHECK_IN, state))
    }

    @Test
    fun `check-out uses today status active attendance id`() {
        val state = AttendanceScreenState(todayStatus = todayStatus(activeAttendanceId = 7))
        assertEquals(
            AttendanceSubmitCommand.CheckOut(activeAttendanceId = 7),
            AttendanceSubmitCommandBuilder.build(AttendanceActionIntent.CHECK_OUT, state)
        )
    }

    @Test
    fun `check-out without today status builds command with null id`() {
        assertEquals(
            AttendanceSubmitCommand.CheckOut(activeAttendanceId = null),
            AttendanceSubmitCommandBuilder.build(
                AttendanceActionIntent.CHECK_OUT,
                AttendanceScreenState()
            )
        )
    }
}

private fun todayStatus(activeAttendanceId: Int?) = TodayStatus(
    canCheckIn = activeAttendanceId == null,
    canCheckOut = activeAttendanceId != null,
    checkedInAt = null,
    checkedOutAt = null,
    activeMode = "WFO",
    activeLocation = null,
    todayDate = "2026-07-27",
    isHoliday = false,
    holidayCheckinEnabled = false,
    currentTime = "2026-07-27T08:00:00+08:00",
    checkinWindow = CheckinWindow("07:00:00", "22:00:00"),
    checkoutAutoTime = "23:50:00",
    activeAttendanceId = activeAttendanceId
)
