package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.model.attendance.AttendanceSubmitResult
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import javax.inject.Inject

/**
 * Use case for check-in operation following Clean Architecture principles
 * Captures the current GPS location and submits the check-in request.
 */
class CheckInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) {
    /**
     * Performs check-in operation using real-time GPS coordinates
     * @param request The attendance request model
     * @return Result containing ActiveAttendanceSession on success or exception on failure
     */
    suspend operator fun invoke(request: AttendanceRequestModel): Result<ActiveAttendanceSession> {
        return try {
            val currentLocation = getCurrentLocationUseCase()
            if (currentLocation !is CurrentLocationResult.Success) {
                return Result.failure(
                    IllegalStateException("Failed to get current GPS location. Please enable location services.")
                )
            }
            val currentCoordinate = currentLocation.location.coordinate

            val updatedRequest = request.copy(
                latitude = currentCoordinate.latitude,
                longitude = currentCoordinate.longitude
            )

            // Temporary adapter until SubmitAttendanceUseCase replaces this use case.
            when (val submitResult = attendanceRepository.checkIn(updatedRequest)) {
                is AttendanceSubmitResult.Success -> Result.success(submitResult.session)
                is AttendanceSubmitResult.Failure ->
                    Result.failure(Exception(submitResult.failure.toString()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
