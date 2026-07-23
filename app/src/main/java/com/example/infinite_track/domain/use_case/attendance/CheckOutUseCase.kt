package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.model.location.CurrentLocationResult
import com.example.infinite_track.domain.use_case.location.GetCurrentLocationUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import javax.inject.Inject

/**
 * Use case for check-out operation
 * Orchestrates the check-out process by getting attendance info, coordinates, and managing geofence
 */
class CheckOutUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val geofenceManager: GeofenceManager,
    private val attendancePreference: AttendancePreference
) {
    /**
     * Performs check-out operation
     * No parameters needed - gets all required data from repository and current location
     * @return Result containing ActiveAttendanceSession on success or exception on failure
     */
    suspend operator fun invoke(attendanceId: Int? = null): Result<ActiveAttendanceSession> {
        return try {
            // 1. Prefer the active attendance ID from caller status, refresh status if needed, then fallback to preference
            val resolvedAttendanceId = attendanceId
                ?: attendanceRepository.getTodayStatus(forceRefresh = true).getOrNull()?.activeAttendanceId
                ?: attendanceRepository.getActiveAttendanceId()
                ?: return Result.failure(Exception("No active attendance session found. Please refresh attendance status and try again."))

            // 2. Get current real-time GPS coordinates (strict, no DB fallback)
            val currentLocation = getCurrentLocationUseCase()
            if (currentLocation !is CurrentLocationResult.Success) {
                return Result.failure(IllegalStateException("Failed to get current location"))
            }
            val currentCoordinate = currentLocation.location.coordinate

            // 3. Call repository to perform check-out
            // Backend will handle location validation
            val checkOutResult = attendanceRepository.checkOut(
                attendanceId = resolvedAttendanceId,
                latitude = currentCoordinate.latitude,
                longitude = currentCoordinate.longitude
            )

            // 4. If check-out successful, remove geofence using stored request ID
            if (checkOutResult.isSuccess) {
                try {
                    geofenceManager.removeActiveMonitoringGeofence()
                    attendancePreference.saveAttendanceSessionStateKey("completed")
                    android.util.Log.d(
                        "CheckOutUseCase",
                        "Active monitoring geofence removed after checkout"
                    )

                    // Re-register only the persisted reminder geofences. Receiver still gates notification by can-check-in/session truth.
                    geofenceManager.restoreReminderGeofences()
                } catch (e: Exception) {
                    android.util.Log.e("CheckOutUseCase", "Failed to remove geofence", e)
                    // Don't fail the entire check-out process if geofence removal fails
                }
            }

            checkOutResult

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
