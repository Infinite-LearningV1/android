package com.example.infinite_track.domain.use_case.history

import com.example.infinite_track.domain.model.attendance.AttendanceReportPdfResult
import com.example.infinite_track.domain.repository.AttendanceReportPdfRepository
import javax.inject.Inject

enum class AttendanceReportPdfMode {
    Preview,
    Export
}

class ExportAttendanceReportPdfUseCase @Inject constructor(
    private val attendanceReportPdfRepository: AttendanceReportPdfRepository
) {
    suspend operator fun invoke(
        mode: AttendanceReportPdfMode,
        period: String,
        startDate: String? = null,
        endDate: String? = null,
        timezone: String? = null
    ): Result<AttendanceReportPdfResult> {
        return when (mode) {
            AttendanceReportPdfMode.Preview -> attendanceReportPdfRepository.previewAttendanceReportPdf(period, startDate, endDate, timezone)
            AttendanceReportPdfMode.Export -> attendanceReportPdfRepository.exportAttendanceReportPdf(period, startDate, endDate, timezone)
        }
    }
}
