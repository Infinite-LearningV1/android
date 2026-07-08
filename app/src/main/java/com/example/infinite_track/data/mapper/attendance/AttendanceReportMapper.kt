package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceReportStatusInfo
import com.example.infinite_track.domain.model.attendance.AttendanceReportStatusKind
import java.util.Locale
import kotlin.math.roundToInt

fun AttendanceRecord.toReportStatus(): AttendanceReportStatusInfo {
    val badgeKey = displayBadgeKey.orEmpty().lowercase(Locale.ROOT)
    val badgeLabel = displayBadgeLabel.orEmpty().trim()
    val statusValue = status.orEmpty().lowercase(Locale.ROOT)
    val categoryValue = category.orEmpty().lowercase(Locale.ROOT)

    return when {
        badgeKey.contains("alpha") || categoryValue.contains("alpha") || categoryValue.contains("absent") || statusValue.contains("alpha") || statusValue.contains("absent") -> {
            AttendanceReportStatusInfo(badgeLabel.ifBlank { "Alpha" }, AttendanceReportStatusKind.Alpha)
        }
        badgeKey.contains("active") || timeOut.isNullOrBlank() -> {
            AttendanceReportStatusInfo(badgeLabel.ifBlank { "Active Session" }, AttendanceReportStatusKind.ActiveSession)
        }
        badgeKey.contains("late") -> {
            AttendanceReportStatusInfo(badgeLabel.ifBlank { "Late" }, AttendanceReportStatusKind.Late)
        }
        badgeKey.contains("ontime") || badgeKey.contains("on_time") || badgeKey.contains("on-time") -> {
            AttendanceReportStatusInfo(badgeLabel.ifBlank { "On Time" }, AttendanceReportStatusKind.OnTime)
        }
        badgeLabel.isNotBlank() -> AttendanceReportStatusInfo(badgeLabel, AttendanceReportStatusKind.Neutral)
        statusValue.isNotBlank() -> AttendanceReportStatusInfo(statusValue.toDisplayLabel(), AttendanceReportStatusKind.Neutral)
        else -> AttendanceReportStatusInfo("Unknown", AttendanceReportStatusKind.Unknown)
    }
}

fun AttendanceRecord.toReportDateLabel(): String {
    return when {
        dateLabel?.isNotBlank() == true -> dateLabel.orEmpty()
        attendanceDate?.isNotBlank() == true -> attendanceDate.orEmpty()
        monthYear.isNotBlank() -> "$date $monthYear"
        else -> date
    }
}

fun AttendanceRecord.toReportWorkModeLabel(): String {
    val backendModeLabel = modeLabel.orEmpty().trim()
    if (backendModeLabel.isNotBlank()) return backendModeLabel

    val raw = category.orEmpty().trim()
    return if (raw.isBlank()) "Work mode unavailable" else raw.toDisplayLabel()
}

fun AttendanceRecord.toReportTimeRangeLabel(): String {
    val backendTimeRange = timeRange.orEmpty().trim()
    if (backendTimeRange.isNotBlank()) return backendTimeRange

    if (isAlphaRecord()) return "No check-in recorded"
    val checkOut = timeOut?.takeIf { it.isNotBlank() } ?: "Active"
    return "$timeIn - $checkOut"
}

fun AttendanceRecord.toReportWorkHourLabel(): String? {
    if (isAlphaRecord() || timeOut.isNullOrBlank()) return null
    return workHour?.takeIf { it.isNotBlank() }
}

fun List<AttendanceRecord>.toReportTotalWorkHoursLabel(fallbackLabel: String? = null): String {
    if (!fallbackLabel.isNullOrBlank()) return fallbackLabel

    val minutes = mapNotNull { record ->
        if (record.isAlphaRecord() || record.timeOut.isNullOrBlank()) null else record.workHour?.toMinutesOrNull()
    }.sum()

    return if (minutes <= 0) "—" else {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        if (remainingMinutes == 0) "${hours}h" else "${hours}h ${remainingMinutes}m"
    }
}

private fun AttendanceRecord.isAlphaRecord(): Boolean {
    val statusValue = status.orEmpty().lowercase(Locale.ROOT)
    val categoryValue = category.orEmpty().lowercase(Locale.ROOT)
    return categoryValue.contains("alpha") || categoryValue.contains("absent") || statusValue.contains("alpha") || statusValue.contains("absent")
}

private fun String.toMinutesOrNull(): Int? {
    val normalized = trim().lowercase(Locale.ROOT)
    if (normalized.isBlank()) return null

    val colonParts = normalized.split(":")
    if (colonParts.size >= 2) {
        val hours = colonParts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = colonParts.getOrNull(1)?.take(2)?.toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    val hourMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*h").find(normalized)
    val minuteMatch = Regex("(\\d+)\\s*m").find(normalized)
    val hours = hourMatch?.groupValues?.getOrNull(1)?.toFloatOrNull()?.let { (it * 60).roundToInt() } ?: 0
    val minutes = minuteMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    return (hours + minutes).takeIf { it > 0 }
}

private fun String.toDisplayLabel(): String {
    return replace("_", " ")
        .replace("-", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
            }
        }
}
