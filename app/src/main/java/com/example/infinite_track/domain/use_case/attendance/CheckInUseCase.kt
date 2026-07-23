package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.model.attendance.AttendanceRequestModel
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import javax.inject.Inject

/**
 * Use case for check-in operation following Clean Architecture principles
 * Simplified to only handle check-in process with provided target location
 * ViewModel is responsible for determining the correct target location
 */
class CheckInUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val geofenceManager: GeofenceManager,
    private val attendancePreference: AttendancePreference
) {
    /**
     * Performs check-in operation using real-time GPS coordinates
     * @param request The attendance request model
     * @param targetLocation The target location for geofence setup after successful check-in
     * @return Result containing ActiveAttendanceSession on success or exception on failure
     */
    suspend operator fun invoke(
        request: AttendanceRequestModel,
        targetLocation: com.example.infinite_track.domain.model.attendance.Location
    ): Result<ActiveAttendanceSession> {
        return try {
            // Step 1: Get current real-time GPS coordinates
            val currentLocation = getCurrentLocationUseCase()
            if (currentLocation !is CurrentLocationResult.Success) {
                return Result.failure(
                    IllegalStateException("Failed to get current GPS location. Please enable location services.")
                )
            }
            val currentCoordinate = currentLocation.location.coordinate

            // Step 2: Update request with real-time coordinates
            val updatedRequest = request.copy(
                latitude = currentCoordinate.latitude,
                longitude = currentCoordinate.longitude
            )

            // Step 3: Call repository to perform check-in with updated coordinates
            // Backend will handle location validation
            val checkInResult = attendanceRepository.checkIn(updatedRequest)

            // Step 4: If check-in successful, setup geofence monitoring using provided target location
            if (checkInResult.isSuccess) {
                try {
                    val activeAttendanceId = checkInResult.getOrThrow().idAttendance
                    attendancePreference.saveAttendanceSessionStateKey("active")
                    geofenceManager.registerActiveMonitoringGeofence(
                        location = targetLocation,
                        activeAttendanceId = activeAttendanceId
                    )
                    android.util.Log.d(
                        "CheckInUseCase",
                        "Active monitoring geofence requested for attendance $activeAttendanceId"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CheckInUseCase", "Failed to setup geofence monitoring", e)
                    // Don't fail the entire check-in process if geofence setup fails
                }
            }

            checkInResult

        } catch (e: Exception) {
            android.util.Log.e("CheckInUseCase", "Error during check-in process", e)
            Result.failure(e)
        }
    }
}
