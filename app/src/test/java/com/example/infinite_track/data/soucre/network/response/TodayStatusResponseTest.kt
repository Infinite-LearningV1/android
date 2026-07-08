package com.example.infinite_track.data.soucre.network.response

import com.example.infinite_track.data.mapper.attendance.toDomain
import com.example.infinite_track.domain.model.attendance.AttendanceSessionState
import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayStatusResponseTest {
    @Test
    fun `maps INF-206 payload and defaults ttl to shared ttl when meta is missing`() {
        val response = createResponse(meta = null)

        val domain = response.toDomain()

        assertEquals(AttendanceSessionState(2, "active", "Active Session"), domain.attendanceSessionState)
        assertEquals(123, domain.activeAttendanceId)
        assertEquals("2026-07-05T08:00:00.000Z", domain.checkedInAtIso)
        assertEquals("2026-07-05T17:00:00.000Z", domain.checkedOutAtIso)
        assertEquals(32_400L, domain.workDurationSeconds)
        assertEquals(AuthRuntimePolicy.SHARED_TTL_SECONDS, domain.cacheTtlSeconds)
    }

    @Test
    fun `uses backend ttl for status today cache when meta is available`() {
        val response = createResponse(meta = TodayStatusMeta(cacheTtlSeconds = 120))

        val domain = response.toDomain()

        assertEquals(120, domain.cacheTtlSeconds)
    }

    @Test
    fun `defaults ttl to shared ttl when meta ttl is invalid`() {
        val response = createResponse(meta = TodayStatusMeta(cacheTtlSeconds = 0))

        val domain = response.toDomain()

        assertEquals(AuthRuntimePolicy.SHARED_TTL_SECONDS, domain.cacheTtlSeconds)
    }

    private fun createResponse(meta: TodayStatusMeta?): TodayStatusResponse {
        return TodayStatusResponse(
            success = true,
            data = TodayStatusData(
                canCheckIn = false,
                canCheckOut = true,
                checkedInAt = "08:00:00",
                checkedOutAt = null,
                activeMode = "Work From Office",
                activeLocation = null,
                todayDate = "2026-07-05",
                isHoliday = false,
                holidayCheckinEnabled = false,
                currentTime = "2026-07-05T09:00:00.000Z",
                checkinWindow = CheckinWindow("07:00:00", "09:00:00"),
                checkoutAutoTime = "17:00:00",
                attendanceSessionState = AttendanceSessionStateDto(id = 2, key = "active", label = "Active Session"),
                activeAttendanceId = 123,
                checkedInAtIso = "2026-07-05T08:00:00.000Z",
                checkedOutAtIso = "2026-07-05T17:00:00.000Z",
                workDurationSeconds = 32_400L
            ),
            message = null,
            meta = meta
        )
    }
}
