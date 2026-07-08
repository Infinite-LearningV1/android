package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.CheckinWindow
import com.example.infinite_track.domain.model.attendance.Location
import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.attendance.WorkMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceActionResolverTest {

    @Test
    fun resolve_returnsReadyCheckInWhenBackendAllowsCheckInAndTargetExists() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = null, canCheckIn = true, canCheckOut = false),
            selectedWorkMode = WorkMode.WFO,
            targetLocation = location(),
            wfoLocation = location()
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
    fun resolve_returnsReadyCheckOutWhenBackendAllowsCheckOutAndTargetExists() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = "08:00", canCheckIn = false, canCheckOut = true),
            selectedWorkMode = WorkMode.WFO,
            targetLocation = location(),
            wfoLocation = location()
        )

        val actionState = AttendanceActionResolver.resolve(state)

        assertEquals(
            AttendanceActionState.Ready(
                intent = AttendanceActionIntent.CHECK_OUT,
                label = "Check-out di sini"
            ),
            actionState
        )
    }

    @Test
    fun resolve_returnsReadyCheckOutWhenTargetLocationMissing() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = "08:00", canCheckIn = false, canCheckOut = true),
            selectedWorkMode = WorkMode.WFA,
            targetLocation = null,
            wfoLocation = null,
            selectedWfaLocation = null
        )

        val actionState = AttendanceActionResolver.resolve(state)

        assertEquals(
            AttendanceActionState.Ready(
                intent = AttendanceActionIntent.CHECK_OUT,
                label = "Check-out di sini"
            ),
            actionState
        )
    }

    @Test
    fun resolve_returnsCompletedWhenBackendAllowsNoAction() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = "08:00", canCheckIn = false, canCheckOut = false),
            selectedWorkMode = WorkMode.WFO,
            targetLocation = location(),
            wfoLocation = location()
        )

        assertEquals(AttendanceActionState.Completed, AttendanceActionResolver.resolve(state))
    }

    @Test
    fun resolve_blocksWfhWhenHomeLocationMissing() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = null, canCheckIn = true, canCheckOut = false),
            selectedWorkMode = WorkMode.WFH,
            wfhLocation = null
        )

        val actionState = AttendanceActionResolver.resolve(state)

        assertTrue(actionState is AttendanceActionState.Blocked)
        assertEquals(
            AttendanceBlockReason.WFH_LOCATION_MISSING,
            (actionState as AttendanceActionState.Blocked).reason
        )
    }

    @Test
    fun resolve_blocksWfoWhenTargetLocationMissing() {
        val state = AttendanceScreenState(
            todayStatus = todayStatus(checkedInAt = null, canCheckIn = true, canCheckOut = false),
            selectedWorkMode = WorkMode.WFO,
            targetLocation = null,
            wfoLocation = null
        )

        val actionState = AttendanceActionResolver.resolve(state)

        assertTrue(actionState is AttendanceActionState.Blocked)
        assertEquals(
            AttendanceBlockReason.TARGET_LOCATION_UNAVAILABLE,
            (actionState as AttendanceActionState.Blocked).reason
        )
    }

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
            activeLocation = location(),
            todayDate = "2026-07-09",
            isHoliday = false,
            holidayCheckinEnabled = false,
            currentTime = "08:00",
            checkinWindow = CheckinWindow(startTime = "07:00", endTime = "09:00"),
            checkoutAutoTime = "17:00"
        )
    }

    private fun location(): Location {
        return Location(
            locationId = 1,
            description = "Office",
            latitude = 0.0,
            longitude = 0.0,
            radius = 100,
            category = "WFO"
        )
    }
}
