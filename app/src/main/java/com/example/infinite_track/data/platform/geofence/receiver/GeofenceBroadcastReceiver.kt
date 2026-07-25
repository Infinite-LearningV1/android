package com.example.infinite_track.data.platform.geofence.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.infinite_track.data.platform.geofence.event.GeofenceEventProcessor
import com.example.infinite_track.data.platform.geofence.event.GeofenceTransition
import com.example.infinite_track.data.platform.geofence.event.GeofenceTransitionEvent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                intent.toTransitionEventOrNull()?.let { event ->
                    val processor = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        GeofenceReceiverEntryPoint::class.java
                    ).geofenceEventProcessor()
                    processor.process(event)
                }
            } catch (exception: Throwable) {
                Log.e(TAG, "Unable to process geofence event", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun Intent.toTransitionEventOrNull(): GeofenceTransitionEvent? {
        val geofencingEvent = GeofencingEvent.fromIntent(this) ?: return null
        if (geofencingEvent.hasError()) {
            Log.w(TAG, "Ignoring Play Services geofence error ${geofencingEvent.errorCode}")
            return null
        }
        val transition = geofencingEvent.geofenceTransition.toTransitionOrNull() ?: return null
        val requestIds = geofencingEvent.triggeringGeofences
            .orEmpty()
            .map(Geofence::getRequestId)
            .toSet()
            .takeIf(Set<String>::isNotEmpty)
            ?: return null
        return GeofenceTransitionEvent(
            transition = transition,
            requestIds = requestIds,
            occurredAt = Instant.now()
        )
    }

    private fun Int.toTransitionOrNull(): GeofenceTransition? = when (this) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransition.ENTER
        Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransition.EXIT
        Geofence.GEOFENCE_TRANSITION_DWELL -> GeofenceTransition.DWELL
        else -> null
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GeofenceReceiverEntryPoint {
        fun geofenceEventProcessor(): GeofenceEventProcessor
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
