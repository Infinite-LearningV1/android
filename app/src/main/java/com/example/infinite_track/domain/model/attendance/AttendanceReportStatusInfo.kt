package com.example.infinite_track.domain.model.attendance

enum class AttendanceReportStatusKind {
    ActiveSession,
    OnTime,
    Late,
    Alpha,
    Unknown,
    Neutral
}

data class AttendanceReportStatusInfo(
    val label: String,
    val kind: AttendanceReportStatusKind
)
