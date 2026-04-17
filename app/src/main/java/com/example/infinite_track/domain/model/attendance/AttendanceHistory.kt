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
	val attendanceDate: String? = null
)

/**
 * Domain model for attendance summary information
 */
data class AttendanceSummaryInfo(
	val totalOntime: Int,
	val totalLate: Int,
	val totalAlpha: Int,
	val totalWfo: Int,
	val totalWfa: Int
)

/**
 * Domain model for a page of attendance history, including summary and pagination info
 */
data class AttendanceHistoryPage(
	val summary: AttendanceSummaryInfo,
	val records: List<AttendanceRecord>,
	val pagination: Pagination
)
