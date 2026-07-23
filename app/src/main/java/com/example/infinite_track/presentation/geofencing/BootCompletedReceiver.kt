package com.example.infinite_track.presentation.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun geofenceManager(): GeofenceManager
        fun attendancePreference(): AttendancePreference
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val geofenceManager = entryPoint.geofenceManager()
        val attendancePreference = entryPoint.attendancePreference()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeAttendanceId = attendancePreference.getActiveAttendanceId().firstOrNull()
                val sessionStateKey = attendancePreference.getAttendanceSessionStateKey().firstOrNull()
                val params = attendancePreference.getLastGeofenceParams().firstOrNull()
                if (activeAttendanceId != null && sessionStateKey == "active" && params != null) {
                    Log.d("BootCompletedReceiver", "Re-registering active monitoring geofence after boot: ${params.id}")
                    geofenceManager.addGeofence(params.id, params.coordinate, params.radius)
                } else {
                    Log.d(
                        "BootCompletedReceiver",
                        "Skipping active monitoring restore: attendanceId=$activeAttendanceId, state=$sessionStateKey, hasParams=${params != null}"
                    )
                }

                if (sessionStateKey != "completed" && activeAttendanceId == null) {
                    val reminders = attendancePreference.getReminderGeofences().firstOrNull().orEmpty()
                    reminders.forEach { r ->
                        Log.d("BootCompletedReceiver", "Re-registering reminder geofence after boot: ${r.id}")
                        geofenceManager.addReminderGeofence(r.id, r.coordinate, r.radius)
                    }
                }
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Failed to re-register geofence after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
