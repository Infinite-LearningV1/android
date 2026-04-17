package com.example.infinite_track.domain.use_case.attendance

import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.domain.model.attendance.ActiveAttendanceSession
import com.example.infinite_track.domain.repository.AttendanceRepository
import com.example.infinite_track.domain.use_case.location.GetCurrentCoordinatesUseCase
import com.example.infinite_track.presentation.geofencing.GeofenceManager
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Use case for check-out operation
 * Orchestrates the check-out process by getting attendance info, coordinates, and managing geofence
 */
class CheckOutUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val getCurrentCoordinatesUseCase: GetCurrentCoordinatesUseCase,
    private val geofenceManager: GeofenceManager,
    private val attendancePreference: AttendancePreference
) {
    /**
     * Performs check-out operation
     * No parameters needed - gets all required data from repository and current location
     * @return Result containing ActiveAttendanceSession on success or exception on failure
     */
    suspend operator fun invoke(): Result<ActiveAttendanceSession> {
        return try {
            // 1. Get active attendance ID
            val attendanceId = attendanceRepository.getActiveAttendanceId()
                ?: return Result.failure(Exception("No active attendance session found"))

            // 2. Get current real-time GPS coordinates (strict, no DB fallback)
            val coordinatesResult = getCurrentCoordinatesUseCase(useRealTimeGPS = true)
            if (coordinatesResult.isFailure) {
                return Result.failure(
                    coordinatesResult.exceptionOrNull()
                        ?: Exception("Failed to get current location")
                )
            }
            val currentCoordinates = coordinatesResult.getOrNull()!!

            // 3. Call repository to perform check-out
            // Backend will handle location validation
            val checkOutResult = attendanceRepository.checkOut(
                attendanceId = attendanceId,
                latitude = currentCoordinates.first,
                longitude = currentCoordinates.second
            )

            // 4. If check-out successful, remove geofence using stored request ID
            if (checkOutResult.isSuccess) {
                try {
                    val lastRequestId = attendancePreference.getLastGeofenceRequestId().firstOrNull()
                    if (lastRequestId != null) {
                        geofenceManager.removeGeofence(lastRequestId)
                    } else {
                        geofenceManager.removeAllGeofences()
                    }
                    android.util.Log.d(
                        "CheckOutUseCase",
                        "Geofence removed using stored request ID: ${lastRequestId ?: "ALL"}"
                    )

                    // Re-register reminder geofences (WFO/WFH) so reminders work after checkout
                    val reminders = attendancePreference.getReminderGeofences().firstOrNull().orEmpty()
                    reminders.forEach { r ->
                        geofenceManager.addReminderGeofence(r.id, r.latitude, r.longitude, r.radiusMeters)
                    }
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
