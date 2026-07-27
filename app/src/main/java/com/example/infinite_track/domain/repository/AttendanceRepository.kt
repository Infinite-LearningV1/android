package com.example.infinite_track.domain.repository

import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.attendance.TodayStatus

/**
 * Repository interface for Attendance operations following Clean Architecture principles.
 * This interface defines the contract for attendance-related operations
 * to be implemented by concrete repository classes.
 */
interface AttendanceRepository {
    /**
     * Gets the current day's attendance status
     */
    suspend fun getTodayStatus(forceRefresh: Boolean = false): Result<TodayStatus>

    /**
     * Clears the cached current day's attendance status.
     */
    suspend fun clearTodayStatusCache()

    /**
     * Performs check-in mutation. Returns a typed submit outcome; transport
     * failures are classified in the data layer and never leak upward.
     * @param request The attendance request containing necessary data for check-in
     */
    suspend fun checkIn(request: AttendanceRequestModel): AttendanceSubmitResult

    /**
     * Performs check-out mutation for the active attendance session.
     * @param attendanceId The ID of the active attendance session
     * @param latitude Current user latitude
     * @param longitude Current user longitude
     */
    suspend fun checkOut(
        attendanceId: Int,
        latitude: Double,
        longitude: Double
    ): AttendanceSubmitResult

    /**
     * Retrieves the active attendance ID from preferences
     */
    suspend fun getActiveAttendanceId(): Int?

    /**
     * Sends location event (ENTER/EXIT) to backend
     * @param request The location event request containing event type, location ID, and timestamp
     */
    suspend fun sendLocationEvent(request: LocationEventRequest): Result<Unit>
}
