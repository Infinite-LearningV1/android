package com.example.infinite_track.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.infinite_track.data.soucre.network.request.LocationEventRequest
import com.example.infinite_track.domain.repository.AttendanceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
/**
 * Worker for sending location events to backend
 * This worker handles the background task of sending geofence events (ENTER/EXIT) to the server
 */

@HiltWorker
class LocationEventWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val attendanceRepository: AttendanceRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "LocationEventWorker"

        // Input data keys
        const val KEY_EVENT_TYPE = "event_type"
        const val KEY_LOCATION_ID = "location_id" // now String
        const val KEY_EVENT_TIMESTAMP = "event_timestamp"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting location event work")

            // Extract data from input
            val eventType = inputData.getString(KEY_EVENT_TYPE)
            val locationId = inputData.getString(KEY_LOCATION_ID)
            val eventTimestamp = inputData.getString(KEY_EVENT_TIMESTAMP)

            // Validate input data
            if (eventType.isNullOrBlank() || locationId.isNullOrBlank() || eventTimestamp.isNullOrBlank()) {
                Log.e(
                    TAG,
                    "Invalid input data: eventType=$eventType, locationId=$locationId, timestamp=$eventTimestamp"
                )
                return Result.failure()
            }

            // Create request object
            val request = LocationEventRequest(
                eventType = eventType,
                locationId = locationId,
                eventTimestamp = eventTimestamp
            )

            // Send location event to backend
            val result = attendanceRepository.sendLocationEvent(request)

            if (result.isSuccess) {
                Log.d(TAG, "Location event sent successfully: $eventType for location $locationId")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send location event: ${result.exceptionOrNull()?.message}")
                // Retry the work if it fails
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationEventWorker", e)
            Result.retry()
        }
    }
}
