package com.example.infinite_track.domain.model.attendance

import com.example.infinite_track.domain.model.auth.AuthRuntimePolicy

/**
 * Domain model representing the current day's attendance status
 * Clean and free from external dependencies
 */
data class TodayStatus(
    val canCheckIn: Boolean,
    val canCheckOut: Boolean, // Changed from Boolean? to Boolean to fix type mismatch
    val checkedInAt: String?,
    val checkedOutAt: String?,
    val activeMode: String,
    val activeLocation: Location?,
    val todayDate: String,
    val isHoliday: Boolean,
    val holidayCheckinEnabled: Boolean,
    val currentTime: String,
    val checkinWindow: CheckinWindow,
    val checkoutAutoTime: String,
    val attendanceSessionState: AttendanceSessionState? = null,
    val activeAttendanceId: Int? = null,
    val cacheTtlSeconds: Int = AuthRuntimePolicy.SHARED_TTL_SECONDS
)

/**
 * Domain model representing backend attendance session state.
 */
data class AttendanceSessionState(
    val id: Int?,
    val key: String?,
    val label: String?
)

/**
 * Domain model representing check-in window times
 */
data class CheckinWindow(
    val startTime: String,
    val endTime: String
)

/**
 * Domain model representing a location for attendance
 */
data class Location(
    val locationId: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val category: String
)


/**
 * Domain model representing an active attendance session
 * Used for newly created or updated attendance sessions
 */
data class ActiveAttendanceSession(
    val idAttendance: Int,
    val userId: Int,
    val categoryId: Int,
    val statusId: Int,
    val timeIn: String,
    val timeOut: String?,
    val workHour: String?,
    val attendanceDate: String,
    val notes: String?
)

/**
 * Domain model for attendance request
 */
data class AttendanceRequestModel(
    val categoryId: Int,
    val latitude: Double,
    val longitude: Double,
    val notes: String,
    val bookingId: Int? = null,
    val type: String
)
