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
import java.time.Duration
import java.time.Instant
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
        const val KEY_ACTIVE_ATTENDANCE_ID = "active_attendance_id"

        private const val MAX_RETRY_ATTEMPTS = 3
        private val MAX_EVENT_AGE = Duration.ofHours(6)
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting location event work")

            // Extract data from input
            val eventType = inputData.getString(KEY_EVENT_TYPE)
            val locationId = inputData.getString(KEY_LOCATION_ID)
            val eventTimestamp = inputData.getString(KEY_EVENT_TIMESTAMP)
            val activeAttendanceId = inputData.getInt(KEY_ACTIVE_ATTENDANCE_ID, -1)

            // Validate input data
            if (eventType.isNullOrBlank() || locationId.isNullOrBlank() || eventTimestamp.isNullOrBlank() || activeAttendanceId <= 0) {
                Log.e(
                    TAG,
                    "Invalid input data: eventType=$eventType, locationId=$locationId, timestamp=$eventTimestamp, activeAttendanceId=$activeAttendanceId"
                )
                return Result.failure()
            }

            if (isStale(eventTimestamp)) {
                Log.w(TAG, "Dropping stale location event: $eventType for $locationId at $eventTimestamp")
                return Result.success()
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
                val message = result.exceptionOrNull()?.message.orEmpty()
                Log.e(TAG, "Failed to send location event: $message")
                if (shouldRetry(message)) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationEventWorker", e)
            if (shouldRetry(e.message.orEmpty())) Result.retry() else Result.failure()
        }
    }

    private fun isStale(eventTimestamp: String): Boolean {
        return try {
            val eventInstant = Instant.parse(eventTimestamp)
            Duration.between(eventInstant, Instant.now()) > MAX_EVENT_AGE
        } catch (exception: Exception) {
            Log.w(TAG, "Invalid event timestamp format, treating as permanent failure: $eventTimestamp", exception)
            true
        }
    }

    private fun shouldRetry(message: String): Boolean {
        if (runAttemptCount >= MAX_RETRY_ATTEMPTS) return false
        val lower = message.lowercase()
        if (lower.contains("400") || lower.contains("401") || lower.contains("403") || lower.contains("404") || lower.contains("422")) {
            return false
        }
        return true
    }
}
