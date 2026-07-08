package com.example.infinite_track.domain.repository

import com.example.infinite_track.domain.model.attendance.AttendanceReportPdfResult

interface AttendanceReportPdfRepository {
    suspend fun previewAttendanceReportPdf(
        period: String,
        startDate: String? = null,
        endDate: String? = null,
        timezone: String? = null
    ): Result<AttendanceReportPdfResult>

    suspend fun exportAttendanceReportPdf(
        period: String,
        startDate: String? = null,
        endDate: String? = null,
        timezone: String? = null
    ): Result<AttendanceReportPdfResult>
}
