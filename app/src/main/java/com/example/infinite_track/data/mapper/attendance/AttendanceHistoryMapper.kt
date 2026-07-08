package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryData
import com.example.infinite_track.data.soucre.network.response.AttendanceItem
import com.example.infinite_track.data.soucre.network.response.AttendanceSummary
import com.example.infinite_track.domain.model.attendance.AttendanceHistoryPage
import com.example.infinite_track.domain.model.attendance.AttendanceModeDistributionInfo
import com.example.infinite_track.domain.model.attendance.AttendanceModeMetricInfo
import com.example.infinite_track.domain.model.attendance.AttendancePeriodInfo
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo

/**
 * Maps AttendanceHistoryData DTO to domain model AttendanceHistoryPage
 */
fun AttendanceHistoryData.toDomain(): AttendanceHistoryPage {
	return AttendanceHistoryPage(
		period = AttendancePeriodInfo(
			type = period.type,
			label = period.label,
			startDate = period.startDate,
			endDate = period.endDate
		),
		summary = summary.toDomain(),
		records = attendances.map { it.toDomain() },
		pagination = pagination
	)
}

/**
 * Maps AttendanceItem DTO to domain model AttendanceRecord
 * Only mapping the fields needed for the UI
 */
fun AttendanceItem.toDomain(): AttendanceRecord {
	return AttendanceRecord(
		id = idAttendance,
		date = date,
		monthYear = monthYear,
		timeIn = timeIn,
		timeOut = timeOut,
		workHour = workHour,
		attendanceDate = attendanceDate,
		dateLabel = dateLabel,
		modeKey = modeKey,
		modeLabel = modeLabel,
		timeRange = timeRange,
		rawTimeIn = rawTimeIn,
		rawTimeOut = rawTimeOut,
		rawTimeRange = rawTimeRange,
		workHourRaw = workHourRaw,
		statusKey = statusKey,
		statusLabel = statusLabel,
		displayBadgeKey = displayBadgeKey,
		displayBadgeLabel = displayBadgeLabel,
		locationLabel = locationLabel,
		category = category,
		status = status,
		location = location,
		notes = notes
	)
}

/**
 * Maps AttendanceSummary DTO to domain model AttendanceSummaryInfo
 */
fun AttendanceSummary.toDomain(): AttendanceSummaryInfo {
	return AttendanceSummaryInfo(
		totalOntime = totalOntime,
		totalLate = totalLate,
		totalEarly = totalEarly,
		totalAlpha = totalAlpha,
		totalWfo = totalWfo,
		totalWfa = totalWfa,
		totalWfh = totalWfh,
		totalWorkHours = totalWorkHours,
		totalPresent = totalPresent,
		totalAbsent = totalAbsent,
		totalCountedDays = totalCountedDays,
		totalWorkingDays = totalWorkingDays,
		attendanceRate = attendanceRate,
		attendanceRateLabel = attendanceRateLabel,
		attendanceRateDenominator = attendanceRateDenominator,
		totalWorkHoursLabel = totalWorkHoursLabel,
		modeDistribution = modeDistribution?.let {
			AttendanceModeDistributionInfo(
				total = it.total,
				wfo = AttendanceModeMetricInfo(it.wfo.key, it.wfo.label, it.wfo.count, it.wfo.percentage),
				wfa = AttendanceModeMetricInfo(it.wfa.key, it.wfa.label, it.wfa.count, it.wfa.percentage),
				wfh = AttendanceModeMetricInfo(it.wfh.key, it.wfh.label, it.wfh.count, it.wfh.percentage)
			)
		}
	)
}
