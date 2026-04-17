package com.example.infinite_track.data.mapper.attendance

import com.example.infinite_track.data.soucre.network.response.AttendanceHistoryData
import com.example.infinite_track.data.soucre.network.response.AttendanceItem
import com.example.infinite_track.data.soucre.network.response.AttendanceSummary
import com.example.infinite_track.domain.model.attendance.AttendanceHistoryPage
import com.example.infinite_track.domain.model.attendance.AttendanceRecord
import com.example.infinite_track.domain.model.attendance.AttendanceSummaryInfo

/**
 * Maps AttendanceHistoryData DTO to domain model AttendanceHistoryPage
 */
fun AttendanceHistoryData.toDomain(): AttendanceHistoryPage {
	return AttendanceHistoryPage(
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
		attendanceDate = attendanceDate
	)
}

/**
 * Maps AttendanceSummary DTO to domain model AttendanceSummaryInfo
 */
fun AttendanceSummary.toDomain(): AttendanceSummaryInfo {
	return AttendanceSummaryInfo(
		totalOntime = totalOntime,
		totalLate = totalLate,
		totalAlpha = totalAlpha,
		totalWfo = totalWfo,
		totalWfa = totalWfa
	)
}
