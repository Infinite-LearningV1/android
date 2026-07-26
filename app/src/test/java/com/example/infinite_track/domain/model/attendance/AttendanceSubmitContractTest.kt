package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceSubmitContractTest {

    private fun target(mode: WorkMode, wfa: ApprovedWfaTargetContext? = null) =
        AuthoritativeTargetLocation(
            targetId = TargetLocationId("t-1"),
            mode = mode,
            source = when (mode) {
                WorkMode.WFO -> TargetLocationSource.STATUS_TODAY
                WorkMode.WFH -> TargetLocationSource.ADMIN_PROFILE
                WorkMode.WFA -> TargetLocationSource.APPROVED_WFA_BOOKING
            },
            coordinate = GeoCoordinate(-0.89, 119.87),
            radius = DistanceMeters(100.0),
            displayName = "Target",
            approvedWfaContext = wfa
        )

    @Test
    fun `check-in command exposes CHECK_IN intent`() {
        val command = AttendanceSubmitCommand.CheckIn(
            workMode = WorkMode.WFO,
            authoritativeTarget = target(WorkMode.WFO)
        )
        assertEquals(AttendanceActionIntent.CHECK_IN, command.intent)
    }

    @Test
    fun `check-out command exposes CHECK_OUT intent and nullable id`() {
        val command = AttendanceSubmitCommand.CheckOut(activeAttendanceId = null)
        assertEquals(AttendanceActionIntent.CHECK_OUT, command.intent)
        assertNull(command.activeAttendanceId)
    }

    @Test
    fun `backend rejected failure carries safe reason only`() {
        val failure = AttendanceSubmitFailure.BackendRejected(safeReason = "Absensi ditolak")
        assertEquals("Absensi ditolak", failure.safeReason)
    }
}
