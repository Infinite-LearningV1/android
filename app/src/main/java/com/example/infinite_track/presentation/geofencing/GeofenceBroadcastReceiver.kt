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
 * BroadcastReceiver untuk menangani event geofence untuk reminder check-in
 * dan warning keluar area berdasarkan state sesi attendance lokal.
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

        val eventType = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> {
                Log.w(TAG, "Unknown geofence transition: $geofenceTransition")
                return
            }
        }

        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GeofenceReceiverEntryPoint::class.java
        )
        val attendancePreference = entryPoint.attendancePreference()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeAttendanceId = attendancePreference.getActiveAttendanceId().first()

                if (activeAttendanceId == null) {
                    attendancePreference.clearNotificationSessionState()
                    Log.d(TAG, "No active session. Handling reminder-only path for event: $eventType")

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

                val hasEnteredActiveSessionArea = attendancePreference.hasEnteredActiveSessionArea().first()

                triggeringGeofences.forEach { geofence ->
                    val requestId = geofence.requestId
                    if (requestId.startsWith("reminder:")) {
                        Log.d(TAG, "Ignoring reminder geofence during active session: $requestId")
                        return@forEach
                    }

                    when (eventType) {
                        "ENTER" -> {
                            attendancePreference.setUserInsideGeofence(true)
                            attendancePreference.setHasEnteredActiveSessionArea(true)
                        }
                        "EXIT" -> attendancePreference.setUserInsideGeofence(false)
                    }

                    val action = GeofenceNotificationPolicy.decide(
                        eventType = eventType,
                        hasActiveSession = true,
                        hasEnteredActiveSessionArea = hasEnteredActiveSessionArea
                    )

                    val friendlyLabel = when {
                        requestId.startsWith("wfa:") -> "Lokasi WFA"
                        else -> requestId
                    }

                    if (action == GeofenceNotificationAction.SHOW_EXIT_WARNING) {
                        NotificationHelper.showExitAreaWarningNotification(context, friendlyLabel)
                    }

                    enqueueLocationEvent(context, geofence, eventType)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun enqueueLocationEvent(context: Context, geofence: Geofence, eventType: String) {
        val locationId = geofence.requestId
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val timestamp = formatter.format(Date())

        val workData = Data.Builder()
            .putString(LocationEventWorker.KEY_EVENT_TYPE, eventType)
            .putString(LocationEventWorker.KEY_LOCATION_ID, locationId)
            .putString(LocationEventWorker.KEY_EVENT_TIMESTAMP, timestamp)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<LocationEventWorker>()
            .setInputData(workData)
            .setConstraints(constraints)
            .addTag("location_event_$locationId")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "Location event enqueued: $eventType for $locationId at $timestamp")
    }
}
