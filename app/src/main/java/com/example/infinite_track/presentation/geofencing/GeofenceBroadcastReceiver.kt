package com.example.infinite_track.presentation.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.example.infinite_track.data.worker.LocationEventWorker
import com.example.infinite_track.utils.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * BroadcastReceiver untuk menangani events geofence secara cerdas
 * Hanya memproses event jika ada sesi kerja yang aktif
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GeofenceReceiverEntryPoint {
        fun attendancePreference(): AttendancePreference
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            Log.e(TAG, "Geofence Error code: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition
        val triggeringGeofences = geofencingEvent?.triggeringGeofences ?: return

        // Convert transition type to string
        val eventType = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> {
                Log.w(TAG, "Unknown geofence transition: $geofenceTransition")
                return
            }
        }

        // Get dependencies using Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GeofenceReceiverEntryPoint::class.java
        )
        val attendancePreference = entryPoint.attendancePreference()

        // Use coroutine to check active session
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if there's an active attendance session
                val activeAttendanceId = attendancePreference.getActiveAttendanceId().first()

                if (activeAttendanceId == null) {
                    Log.d(TAG, "No active session. Handling as reminder mode for event: $eventType")

                    // In reminder mode, only act on ENTER to nudge user to check-in
                    if (eventType == "ENTER") {
                        triggeringGeofences.forEach { geofence ->
                            val locationId = geofence.requestId
                            val friendlyLabel = when {
                                locationId.startsWith("wfa:") -> "Lokasi WFA"
                                else -> locationId
                            }
                            NotificationHelper.showCheckInReminderNotification(context, friendlyLabel)
                        }
                    }
                    return@launch
                }

                Log.d(
                    TAG,
                    "Active session found (ID: $activeAttendanceId). Processing geofence event: $eventType"
                )

                // Update geofence status in preferences based on event type
                when (eventType) {
                    "ENTER" -> attendancePreference.setUserInsideGeofence(true)
                    "EXIT" -> attendancePreference.setUserInsideGeofence(false)
                }

                // Process each triggered geofence
                triggeringGeofences.forEach { geofence ->
                    val requestId = geofence.requestId
                    // Ignore reminder geofences (prefixed) during active session
                    if (requestId.startsWith("reminder:")) {
                        Log.d(TAG, "Ignoring reminder geofence during active session: $requestId")
                        return@forEach
                    }
                    processGeofenceEvent(context, geofence, eventType)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence event", e)
            }
        }
    }

    private fun processGeofenceEvent(context: Context, geofence: Geofence, eventType: String) {
        try {
            val locationId = geofence.requestId // String: supports numeric and WFA ids

            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val timestamp = formatter.format(Date())

            // Derive a user-friendly label for notification
            val friendlyLabel = when {
                locationId.startsWith("wfa:") -> "Lokasi WFA"
                else -> locationId
            }
            NotificationHelper.showGeofenceNotification(context, eventType, friendlyLabel)

            val workData = Data.Builder()
                .putString(LocationEventWorker.KEY_EVENT_TYPE, eventType)
                .putString(LocationEventWorker.KEY_LOCATION_ID, locationId)
                .putString(LocationEventWorker.KEY_EVENT_TIMESTAMP, timestamp)
                .build()

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<LocationEventWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
                .addTag("location_event_$locationId")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            Log.d(TAG, "Location event enqueued: $eventType for $locationId at $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing geofence event for ${geofence.requestId}", e)
        }
    }
}
