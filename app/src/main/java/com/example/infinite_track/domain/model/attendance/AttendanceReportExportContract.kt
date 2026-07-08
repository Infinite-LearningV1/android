package com.example.infinite_track.domain.model.attendance

object AttendanceReportExportContract {
    const val PERSONAL_PDF_PREVIEW_PATH = "/api/attendance/history/personal/pdf"
    const val EXPORT_PDF_PATH = "/api/attendance/history/export.pdf"

    fun exportPdfPath(period: String): String {
        return "$EXPORT_PDF_PATH?period=$period"
    }
}
