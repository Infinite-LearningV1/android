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
                val params = attendancePreference.getLastGeofenceParams().firstOrNull()
                val reminders = attendancePreference.getReminderGeofences().firstOrNull().orEmpty()

                if (params == null && reminders.isEmpty()) {
                    Log.d("BootCompletedReceiver", "No geofences to restore after boot.")
                    return@launch
                }

                Log.d("BootCompletedReceiver", "Restoring persisted geofences after boot")
                geofenceManager.restoreGeofencesAfterBoot(
                    monitoringGeofence = params,
                    reminderGeofences = reminders
                )
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Failed to re-register geofence after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}