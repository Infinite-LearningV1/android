package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.data.soucre.network.response.Pagination

/**
 * Domain model for an individual attendance record
 */
data class AttendanceRecord(
	val id: Int,
	val date: String,
	val monthYear: String,
	val timeIn: String,
	val timeOut: String?,
	val workHour: String?,
	val attendanceDate: String? = null,
	val dateLabel: String? = null,
	val modeKey: String? = null,
	val modeLabel: String? = null,
	val timeRange: String? = null,
	val rawTimeIn: String? = null,
	val rawTimeOut: String? = null,
	val rawTimeRange: String? = null,
	val workHourRaw: Double? = null,
	val statusKey: String? = null,
	val statusLabel: String? = null,
	val displayBadgeKey: String? = null,
	val displayBadgeLabel: String? = null,
	val locationLabel: String? = null,
	val category: String? = null,
	val status: String? = null,
	val location: String? = null,
	val notes: String? = null
)

data class AttendancePeriodInfo(
	val type: String,
	val label: String,
	val startDate: String,
	val endDate: String
)

data class AttendanceModeMetricInfo(
	val key: String,
	val label: String,
	val count: Int,
	val percentage: Int
)

data class AttendanceModeDistributionInfo(
	val total: Int,
	val wfo: AttendanceModeMetricInfo,
	val wfa: AttendanceModeMetricInfo,
	val wfh: AttendanceModeMetricInfo
)

/**
 * Domain model for attendance summary information
 */
data class AttendanceSummaryInfo(
	val totalOntime: Int,
	val totalLate: Int,
	val totalEarly: Int,
	val totalAlpha: Int,
	val totalWfo: Int,
	val totalWfa: Int,
	val totalWfh: Int,
	val totalWorkHours: Double?,
	val totalPresent: Int,
	val totalAbsent: Int,
	val totalCountedDays: Int,
	val totalWorkingDays: Int?,
	val attendanceRate: Int?,
	val attendanceRateLabel: String?,
	val attendanceRateDenominator: String?,
	val totalWorkHoursLabel: String?,
	val modeDistribution: AttendanceModeDistributionInfo?
)

/**
 * Domain model for a page of attendance history, including summary and pagination info
 */
data class AttendanceHistoryPage(
	val period: AttendancePeriodInfo,
	val summary: AttendanceSummaryInfo,
	val records: List<AttendanceRecord>,
	val pagination: Pagination
)
