package com.example.infinite_track.presentation.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.ExistingWorkPolicy
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
        private const val ACTIVE_SESSION_STATE = "active"
        private const val REMINDER_COOLDOWN_MILLIS = 45 * 60 * 1000L
        private const val ACTIVE_ALERT_COOLDOWN_MILLIS = 7 * 60 * 1000L
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
        val pendingResult = goAsync()

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
                val activeAttendanceId = attendancePreference.getActiveAttendanceId().first()
                val sessionStateKey = attendancePreference.getAttendanceSessionStateKey().first()
                val hasActiveSessionTruth = activeAttendanceId != null && sessionStateKey == ACTIVE_SESSION_STATE

                if (!hasActiveSessionTruth) {
                    Log.d(
                        TAG,
                        "No active session truth. Handling as reminder mode for event=$eventType, attendanceId=$activeAttendanceId, state=$sessionStateKey"
                    )

                    if (eventType == "ENTER" && sessionStateKey != "completed") {
                        triggeringGeofences.forEach { geofence ->
                            val locationId = geofence.requestId
                            if (!locationId.startsWith("reminder:")) return@forEach
                            val friendlyLabel = reminderLabel(locationId)
                            val cooldownKey = "reminder:$locationId"
                            val canNotify = attendancePreference.canNotifyWithCooldown(
                                key = cooldownKey,
                                nowMillis = System.currentTimeMillis(),
                                cooldownMillis = REMINDER_COOLDOWN_MILLIS
                            )
                            if (canNotify) {
                                NotificationHelper.showCheckInReminderNotification(context, friendlyLabel)
                            } else {
                                Log.d(TAG, "Reminder notification skipped by cooldown: $locationId")
                            }
                        }
                    }
                    return@launch
                }

                Log.d(
                    TAG,
                    "Active session truth found (ID: $activeAttendanceId, state=$sessionStateKey). Processing geofence event: $eventType"
                )

                when (eventType) {
                    "ENTER" -> attendancePreference.setUserInsideGeofence(true)
                    "EXIT" -> attendancePreference.setUserInsideGeofence(false)
                }

                triggeringGeofences.forEach { geofence ->
                    val requestId = geofence.requestId
                    if (requestId.startsWith("reminder:")) {
                        Log.d(TAG, "Ignoring reminder geofence during active session: $requestId")
                        return@forEach
                    }
                    processGeofenceEvent(context, geofence, eventType, activeAttendanceId!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing geofence event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processGeofenceEvent(
        context: Context,
        geofence: Geofence,
        eventType: String,
        activeAttendanceId: Int
    ) {
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
            val cooldownKey = "active:$activeAttendanceId:$eventType:$locationId"
            val canNotify = EntryPointAccessors.fromApplication(
                context.applicationContext,
                GeofenceReceiverEntryPoint::class.java
            ).attendancePreference().canNotifyWithCooldown(
                key = cooldownKey,
                nowMillis = System.currentTimeMillis(),
                cooldownMillis = ACTIVE_ALERT_COOLDOWN_MILLIS
            )
            if (canNotify) {
                NotificationHelper.showGeofenceNotification(context, eventType, friendlyLabel)
            } else {
                Log.d(TAG, "Active monitoring notification skipped by cooldown: $cooldownKey")
            }

            val workData = Data.Builder()
                .putString(LocationEventWorker.KEY_EVENT_TYPE, eventType)
                .putString(LocationEventWorker.KEY_LOCATION_ID, locationId)
                .putString(LocationEventWorker.KEY_EVENT_TIMESTAMP, timestamp)
                .putInt(LocationEventWorker.KEY_ACTIVE_ATTENDANCE_ID, activeAttendanceId)
                .build()

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<LocationEventWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
                .addTag("location_event_$locationId")
                .build()

            val uniqueWorkName = "location_event_${activeAttendanceId}_${locationId}_${eventType}"
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d(TAG, "Location event enqueued uniquely: $uniqueWorkName at $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing geofence event for ${geofence.requestId}", e)
        }
    }

    private fun reminderLabel(requestId: String): String {
        return when {
            requestId.startsWith("reminder:wfh:") -> "Lokasi WFH"
            requestId.startsWith("reminder:wfa:") -> "Lokasi WFA"
            requestId.startsWith("reminder:primary:") -> "Lokasi utama attendance"
            else -> requestId.removePrefix("reminder:")
        }
    }
}
