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
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BootReceiverEntryPoint::class.java
        )
        val geofenceManager = entryPoint.geofenceManager()
        val attendancePreference = entryPoint.attendancePreference()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val params = attendancePreference.getLastGeofenceParams().firstOrNull()
                if (params != null) {
                    val (requestId, latLng, radius) = params
                    val (lat, lng) = latLng
                    Log.d("BootCompletedReceiver", "Re-registering monitoring geofence after boot: $requestId")
                    geofenceManager.addGeofence(requestId, lat, lng, radius.toFloat())
                } else {
                    Log.d("BootCompletedReceiver", "No monitoring geofence to restore.")
                }

                // Restore reminder geofences
                val reminders = attendancePreference.getReminderGeofences().firstOrNull().orEmpty()
                reminders.forEach { r ->
                    Log.d("BootCompletedReceiver", "Re-registering reminder geofence after boot: ${'$'}{r.id}")
                    geofenceManager.addReminderGeofence(r.id, r.latitude, r.longitude, r.radiusMeters)
                }
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Failed to re-register geofence after boot", e)
            }
        }
    }
}