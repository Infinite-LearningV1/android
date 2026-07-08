package com.example.infinite_track.presentation.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceReportUiMapperTest {

    @Test
    fun reportStatus_returnsAlpha_whenStatusIndicatesAlpha() {
        val record = attendanceRecord(
            status = "alpha",
            category = "WFO",
            timeOut = null,
            workHour = null
        )

        val status = record.reportStatus()

        assertEquals("Alpha", status.label)
        assertEquals(InfiniteStatusVariant.Alpha, status.variant)
    }

    @Test
    fun reportStatus_returnsActiveSession_whenCheckoutIsMissingForNonAlphaRecord() {
        val record = attendanceRecord(
            status = "on_time",
            category = "WFA",
            timeOut = null,
            workHour = null
        )

        val status = record.reportStatus()

        assertEquals("Active Session", status.label)
        assertEquals(InfiniteStatusVariant.Active, status.variant)
        assertNull(record.workHourLabel())
    }

    @Test
    fun totalWorkHoursLabel_excludesAlphaAndActiveSessionRecords() {
        val records = listOf(
            attendanceRecord(status = "on_time", timeOut = "17:00", workHour = "08:00"),
            attendanceRecord(status = "late", timeOut = "17:30", workHour = "07:30"),
            attendanceRecord(status = "alpha", timeOut = null, workHour = null),
            attendanceRecord(status = "on_time", timeOut = null, workHour = "02:00")
        )

        assertEquals("15h 30m", records.totalWorkHoursLabel())
    }

    @Test
    fun workModeLabel_formatsBackendCategoryWithoutInventingMode() {
        val record = attendanceRecord(category = "work_from_office")

        assertEquals("Work From Office", record.workModeLabel())
    }

    @Test
    fun reportStatus_returnsUnknown_whenBackendStatusIsMissing() {
        val record = attendanceRecord(status = "", timeOut = "17:00")

        val status = record.reportStatus()

        assertEquals("Unknown", status.label)
        assertEquals(InfiniteStatusVariant.Unknown, status.variant)
    }

    private fun attendanceRecord(
        status: String = "on_time",
        category: String = "WFO",
        timeOut: String? = "17:00",
        workHour: String? = "08:00"
    ): AttendanceRecord {
        return AttendanceRecord(
            id = 1,
            date = "08",
            monthYear = "Jul 2026",
            timeIn = "08:00",
            timeOut = timeOut,
            workHour = workHour,
            attendanceDate = "2026-07-08",
            category = category,
            status = status,
            location = "Palu"
        )
    }
}
