package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.ApprovedWfaTargetContext
import com.example.infinite_track.domain.model.attendance.AuthoritativeTargetLocation
import com.example.infinite_track.domain.model.attendance.TargetLocationId
import com.example.infinite_track.domain.model.attendance.TargetLocationSource
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceCheckInRequestFactoryTest {

    @Test
    fun `WFA request uses approved booking id from target context`() {
        val request = AttendanceCheckInRequestFactory.create(
            workMode = WorkMode.WFA,
            authoritativeTarget = approvedWfaTarget(bookingId = 88)
        )

        assertEquals(3, request.categoryId)
        assertEquals(88, request.bookingId)
        assertEquals("checkin", request.type)
        assertEquals("Check-in via mobile app", request.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WFA request rejects target without approved booking context`() {
        AttendanceCheckInRequestFactory.create(
            workMode = WorkMode.WFA,
            authoritativeTarget = approvedWfaTarget(bookingId = 88).copy(
                approvedWfaContext = null
            )
        )
    }

    @Test
    fun `WFO request ignores WFA booking context`() {
        val target = approvedWfaTarget(bookingId = 88).copy(mode = WorkMode.WFO)

        val request = AttendanceCheckInRequestFactory.create(
            workMode = WorkMode.WFO,
            authoritativeTarget = target
        )

        assertEquals(1, request.categoryId)
        assertEquals(null, request.bookingId)
    }

    private fun approvedWfaTarget(bookingId: Int) = AuthoritativeTargetLocation(
        targetId = TargetLocationId("booking:$bookingId"),
        mode = WorkMode.WFA,
        source = TargetLocationSource.APPROVED_WFA_BOOKING,
        coordinate = GeoCoordinate(-0.90, 119.88),
        radius = DistanceMeters(100.0),
        displayName = "WFA disetujui",
        approvedWfaContext = ApprovedWfaTargetContext(
            bookingId = bookingId,
            scheduleDate = "2026-07-23"
        )
    )
}
