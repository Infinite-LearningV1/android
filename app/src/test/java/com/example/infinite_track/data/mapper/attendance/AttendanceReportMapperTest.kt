package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceReportStatusKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceReportMapperTest {

    @Test
    fun toReportStatus_returnsAlpha_whenStatusIndicatesAlpha() {
        val record = attendanceRecord(
            status = "alpha",
            category = "WFO",
            timeOut = null,
            workHour = null
        )

        val status = record.toReportStatus()

        assertEquals("Alpha", status.label)
        assertEquals(AttendanceReportStatusKind.Alpha, status.kind)
    }

    @Test
    fun toReportStatus_returnsActiveSession_whenCheckoutIsMissingForNonAlphaRecord() {
        val record = attendanceRecord(
            status = "on_time",
            category = "WFA",
            timeOut = null,
            workHour = null
        )

        val status = record.toReportStatus()

        assertEquals("Active Session", status.label)
        assertEquals(AttendanceReportStatusKind.ActiveSession, status.kind)
        assertNull(record.toReportWorkHourLabel())
    }

    @Test
    fun toReportTotalWorkHoursLabel_excludesAlphaAndActiveSessionRecords() {
        val records = listOf(
            attendanceRecord(status = "on_time", timeOut = "17:00", workHour = "08:00"),
            attendanceRecord(status = "late", timeOut = "17:30", workHour = "07:30"),
            attendanceRecord(status = "alpha", timeOut = null, workHour = null),
            attendanceRecord(status = "on_time", timeOut = null, workHour = "02:00")
        )

        assertEquals("15h 30m", records.toReportTotalWorkHoursLabel())
    }

    @Test
    fun toReportWorkModeLabel_formatsBackendCategoryWithoutInventingMode() {
        val record = attendanceRecord(category = "work_from_office")

        assertEquals("Work From Office", record.toReportWorkModeLabel())
    }

    @Test
    fun toReportStatus_returnsUnknown_whenBackendStatusIsMissing() {
        val record = attendanceRecord(status = "", timeOut = "17:00")

        val status = record.toReportStatus()

        assertEquals("Unknown", status.label)
        assertEquals(AttendanceReportStatusKind.Unknown, status.kind)
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
