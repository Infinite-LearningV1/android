package com.example.infinite_track.domain.model.attendance

import android.net.Uri

data class AttendanceReportPdfResult(
    val fileName: String,
    val localUri: Uri,
    val mimeType: String = "application/pdf"
)
