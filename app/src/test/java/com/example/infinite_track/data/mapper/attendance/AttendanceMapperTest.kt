package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.data.soucre.network.response.AttendanceSessionStateDto
import com.example.infinite_track.data.soucre.network.response.CheckinWindow
import com.example.infinite_track.data.soucre.network.response.TodayStatusData
import com.example.infinite_track.data.soucre.network.response.TodayStatusMeta
import com.example.infinite_track.data.soucre.network.response.TodayStatusResponse
import com.example.infinite_track.domain.model.attendance.AttendanceSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceMapperTest {
    @Test
    fun `maps response owned status fields including unavailable state and null checkout`() {
        val response = TodayStatusResponse(
            success = true,
            data = TodayStatusData(
                canCheckIn = false,
                canCheckOut = null,
                checkedInAt = null,
                checkedOutAt = null,
                activeMode = "Work From Office",
                activeLocation = null,
                todayDate = "2026-07-05",
                isHoliday = false,
                holidayCheckinEnabled = false,
                currentTime = "2026-07-05T18:49:51.000Z",
                checkinWindow = CheckinWindow("07:00:00", "22:00:00"),
                checkoutAutoTime = "23:50:00",
                attendanceSessionState = AttendanceSessionStateDto(id = 4, key = "unavailable", label = "Unavailable"),
                activeAttendanceId = null
            ),
            meta = TodayStatusMeta(cacheTtlSeconds = 300)
        )

        val domain = response.toDomain()

        assertFalse(domain.canCheckOut)
        assertEquals("2026-07-05T18:49:51.000Z", domain.currentTime)
        assertEquals(AttendanceSessionState(4, "unavailable", "Unavailable"), domain.attendanceSessionState)
        assertNull(domain.activeAttendanceId)
        assertEquals(300, domain.cacheTtlSeconds)
    }
}
