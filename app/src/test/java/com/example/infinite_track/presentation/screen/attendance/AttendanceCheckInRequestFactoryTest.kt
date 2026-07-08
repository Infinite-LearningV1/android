package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.attendance.WorkMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceCheckInRequestFactoryTest {

    @Test
    fun create_preservesBookingIdForWfaRequests() {
        val request = AttendanceCheckInRequestFactory.create(
            workMode = WorkMode.WFA,
            bookingId = 456
        )

        assertEquals(3, request.categoryId)
        assertEquals(456, request.bookingId)
        assertEquals("checkin", request.type)
        assertEquals("Check-in via mobile app", request.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_rejectsWfaRequestWithoutBookingId() {
        AttendanceCheckInRequestFactory.create(
            workMode = WorkMode.WFA,
            bookingId = null
        )
    }
}
