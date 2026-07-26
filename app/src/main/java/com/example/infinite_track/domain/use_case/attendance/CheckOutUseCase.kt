package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import javax.inject.Inject

/**
 * Use case for check-out operation
 * Orchestrates the check-out process by getting attendance info and coordinates.
 */
class CheckOutUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) {
    /**
     * Performs check-out operation
     * No parameters needed - gets all required data from repository and current location
     * @return Result containing ActiveAttendanceSession on success or exception on failure
     */
    suspend operator fun invoke(attendanceId: Int? = null): Result<ActiveAttendanceSession> {
        return try {
            val resolvedAttendanceId = attendanceId
                ?: attendanceRepository.getTodayStatus(forceRefresh = true).getOrNull()?.activeAttendanceId
                ?: attendanceRepository.getActiveAttendanceId()
                ?: return Result.failure(Exception("No active attendance session found. Please refresh attendance status and try again."))

            val currentLocation = getCurrentLocationUseCase()
            if (currentLocation !is CurrentLocationResult.Success) {
                return Result.failure(IllegalStateException("Failed to get current location"))
            }
            val currentCoordinate = currentLocation.location.coordinate

            // Temporary adapter until SubmitAttendanceUseCase replaces this use case.
            val submitResult = attendanceRepository.checkOut(
                attendanceId = resolvedAttendanceId,
                latitude = currentCoordinate.latitude,
                longitude = currentCoordinate.longitude
            )
            when (submitResult) {
                is AttendanceSubmitResult.Success -> Result.success(submitResult.session)
                is AttendanceSubmitResult.Failure ->
                    Result.failure(Exception(submitResult.failure.toString()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
