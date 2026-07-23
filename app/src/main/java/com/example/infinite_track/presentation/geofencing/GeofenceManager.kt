package com.example.infinite_track.presentation.geofencing

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.infinite_track.data.soucre.local.preferences.AttendancePreference
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val attendancePreference: AttendancePreference
) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val settingsClient = LocationServices.getSettingsClient(context)
    private val TAG = "GeofenceManager"

    // Dedicated scope for lightweight preference operations
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun hasForegroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Check if permissions for geofence registration are granted.
     * Basic/manual attendance readiness is owned by the embedded permission panel.
     */
    fun hasAllRequiredPermissions(): Boolean {
        return currentPermissionDecision().canRegisterAutomaticMonitoring
    }

    /**
     * Get detailed geofence monitoring permission status for UI feedback.
     */
    fun getPermissionStatusMessage(): String {
        return currentPermissionDecision().message
    }

    /**
     * Full geofence teardown. Keep this for logout/auth runtime cleanup only.
     */
    fun removeAllGeofencesForLogoutOnly() {
        ioScope.launch {
            try {
                removeAllGeofencesForLogoutOnlyAwait()
            } catch (e: CancellationException) {
                throw e
            } catch (exception: Exception) {
                Log.e(TAG, "Gagal menghapus semua geofence", exception)
            }
        }
    }

    /**
     * Backward-compatible alias. New normal mode switches must use typed lifecycle APIs.
     */
    fun removeAllGeofences() = removeAllGeofencesForLogoutOnly()

    /**
     * Awaitable geofence cleanup for auth/runtime teardown semantics.
     */
    suspend fun removeAllGeofencesForLogoutOnlyAwait() {
        try {
            geofencingClient.removeGeofences(geofencePendingIntent).awaitTask()
            Log.d(TAG, "Semua geofence berhasil dihapus untuk logout/full teardown")
            attendancePreference.clearLastGeofenceRequestId()
            attendancePreference.clearLastGeofenceParams()
            attendancePreference.clearReminderGeofences()
            attendancePreference.clearNotificationCooldowns()
        } catch (e: CancellationException) {
            throw e
        } catch (exception: Exception) {
            Log.e(TAG, "Gagal menghapus semua geofence", exception)
            throw exception
        }
    }

    suspend fun removeAllGeofencesAwait() = removeAllGeofencesForLogoutOnlyAwait()

    @SuppressLint("MissingPermission")
    fun addGeofence(
        id: String, 
        latitude: Double, 
        longitude: Double, 
        radius: Float,
        onPermissionError: ((String) -> Unit)? = null
    ) {
        val initialPermissionDecision = currentPermissionDecision()
        if (!initialPermissionDecision.canRegisterAutomaticMonitoring) {
            Log.e(TAG, initialPermissionDecision.message)
            onPermissionError?.invoke(initialPermissionDecision.message)
            return
        }

        val safeRadius = radius

        // Check device location settings first (GPS/location must be ON)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                val latestPermissionDecision = currentPermissionDecision()
                if (!latestPermissionDecision.canRegisterAutomaticMonitoring) {
                    Log.e(TAG, latestPermissionDecision.message)
                    onPermissionError?.invoke(latestPermissionDecision.message)
                    return@addOnSuccessListener
                }
                val requestId = id
                val geofence = Geofence.Builder()
                    .setRequestId(requestId)
                    .setCircularRegion(latitude, longitude, safeRadius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()

                val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()

                val beforePlayServicesDecision = currentPermissionDecision()
                if (!beforePlayServicesDecision.canRegisterAutomaticMonitoring) {
                    Log.e(TAG, beforePlayServicesDecision.message)
                    onPermissionError?.invoke(beforePlayServicesDecision.message)
                    return@addOnSuccessListener
                }

                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.d(
                            TAG,
                            "Active geofence berhasil ditambahkan: $requestId (lat: $latitude, lng: $longitude, radius: ${safeRadius}m)"
                        )
                        ioScope.launch {
                            attendancePreference.saveLastGeofenceRequestId(requestId)
                            attendancePreference.saveLastGeofenceParams(requestId, latitude, longitude, safeRadius)
                        }
                    }
                    addOnFailureListener { exception ->
                        val status = (exception as? ApiException)?.statusCode
                        val statusText = status?.let { GeofenceStatusCodes.getStatusCodeString(it) }
                        Log.e(TAG, "Gagal menambahkan active geofence: $requestId (${status ?: "?"}: ${statusText ?: exception.message})", exception)
                    }
                }
            }
            .addOnFailureListener { exception ->
                val status = (exception as? ApiException)?.statusCode
                val statusText = status?.let { GeofenceStatusCodes.getStatusCodeString(it) }
                Log.e(TAG, "Location settings tidak memenuhi syarat untuk Geofencing (${status ?: "?"}: ${statusText ?: exception.message}). Pastikan Location diaktifkan.")
            }
    }

    fun removeGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                Log.d(TAG, "Geofence berhasil dihapus: $id")
                ioScope.launch {
                    val last = attendancePreference.getLastGeofenceRequestId().first()
                    if (last == id) {
                        attendancePreference.clearLastGeofenceRequestId()
                        attendancePreference.clearLastGeofenceParams()
                    }
                }
            }
            addOnFailureListener { Log.e(TAG, "Gagal menghapus geofence: $id", it) }
        }
    }

    /**
     * Add reminder geofence WITHOUT clearing existing ones
     */
    @SuppressLint("MissingPermission")
    fun addReminderGeofence(id: String, latitude: Double, longitude: Double, radius: Float) {
        val initialPermissionDecision = currentPermissionDecision()
        if (!initialPermissionDecision.canRegisterAutomaticMonitoring) {
            Log.e(TAG, initialPermissionDecision.message)
            return
        }

        val safeRadius = radius

        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, safeRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val beforePlayServicesDecision = currentPermissionDecision()
        if (!beforePlayServicesDecision.canRegisterAutomaticMonitoring) {
            Log.e(TAG, beforePlayServicesDecision.message)
            return
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "Reminder geofence ditambahkan: $id")
                ioScope.launch {
                    attendancePreference.addReminderGeofences(
                        listOf(
                            com.example.infinite_track.data.soucre.local.preferences.ReminderGeofence(
                                id = id,
                                latitude = latitude,
                                longitude = longitude,
                                radiusMeters = safeRadius
                            )
                        )
                    )
                }
            }
            addOnFailureListener { exception ->
                val status = (exception as? ApiException)?.statusCode
                val statusText = status?.let { GeofenceStatusCodes.getStatusCodeString(it) }
                Log.e(TAG, "Gagal menambahkan reminder geofence: $id (${status ?: "?"}: ${statusText ?: exception.message})", exception)
            }
        }
    }

    fun registerReminderGeofences(candidates: List<ReminderGeofenceCandidate>) {
        if (candidates.isEmpty()) {
            Log.d(TAG, "Tidak ada reminder geofence candidate untuk diregister")
            return
        }
        Log.d(
            TAG,
            "Registering reminder candidates: " + candidates.joinToString { "${it.id}:${it.source}" }
        )
        candidates.forEach { candidate ->
            addReminderGeofence(
                id = candidate.id,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                radius = candidate.radiusMeters
            )
        }
    }

    fun removeReminderGeofences() {
        ioScope.launch {
            val reminders = attendancePreference.getReminderGeofences().first()
            if (reminders.isEmpty()) {
                Log.d(TAG, "Tidak ada reminder geofence untuk dihapus")
                return@launch
            }
            geofencingClient.removeGeofences(reminders.map { it.id }).run {
                addOnSuccessListener {
                    Log.d(TAG, "Reminder geofences dilepas dari Play Services: ${reminders.map { it.id }}")
                }
                addOnFailureListener { Log.e(TAG, "Gagal menghapus reminder geofences", it) }
            }
        }
    }

    fun restoreReminderGeofences() {
        ioScope.launch {
            val reminders = attendancePreference.getReminderGeofences().first()
            reminders.forEach { reminder ->
                addReminderGeofence(reminder.id, reminder.latitude, reminder.longitude, reminder.radiusMeters)
            }
            Log.d(TAG, "Reminder geofences restored: ${reminders.map { it.id }}")
        }
    }

    fun registerActiveMonitoringGeofence(
        location: com.example.infinite_track.domain.model.attendance.Location,
        activeAttendanceId: Int
    ) {
        removeReminderGeofences()
        val requestId = buildActiveMonitoringRequestId(location, activeAttendanceId)
        addGeofence(
            id = requestId,
            latitude = location.latitude,
            longitude = location.longitude,
            radius = location.radius.toFloat()
        )
        Log.d(TAG, "Active monitoring geofence requested: $requestId for attendance=$activeAttendanceId")
    }

    fun removeActiveMonitoringGeofence() {
        ioScope.launch {
            val lastRequestId = attendancePreference.getLastGeofenceRequestId().first()
            if (lastRequestId == null) {
                Log.d(TAG, "Tidak ada active monitoring geofence untuk dihapus")
                return@launch
            }
            geofencingClient.removeGeofences(listOf(lastRequestId)).run {
                addOnSuccessListener {
                    Log.d(TAG, "Active monitoring geofence dihapus: $lastRequestId")
                    ioScope.launch {
                        attendancePreference.clearLastGeofenceRequestId()
                        attendancePreference.clearLastGeofenceParams()
                        attendancePreference.setUserInsideGeofence(false)
                    }
                }
                addOnFailureListener { Log.e(TAG, "Gagal menghapus active monitoring geofence: $lastRequestId", it) }
            }
        }
    }

    fun removeReminderGeofence(id: String) {
        geofencingClient.removeGeofences(listOf(id)).run {
            addOnSuccessListener {
                Log.d(TAG, "Reminder geofence dihapus: $id")
                ioScope.launch { attendancePreference.removeReminderGeofence(id) }
            }
            addOnFailureListener { Log.e(TAG, "Gagal menghapus reminder geofence: $id", it) }
        }
    }

    private fun buildActiveMonitoringRequestId(
        location: com.example.infinite_track.domain.model.attendance.Location,
        activeAttendanceId: Int
    ): String {
        return if (location.locationId != 0) {
            "active:$activeAttendanceId:${location.locationId}"
        } else {
            val lat = String.format("%.6f", location.latitude)
            val lng = String.format("%.6f", location.longitude)
            "active:$activeAttendanceId:wfa:$lat,$lng"
        }
    }

    private fun currentPermissionDecision(): GeofencePermissionDecision =
        GeofencePermissionContract.evaluate(
            hasPreciseForegroundLocation = hasForegroundLocationPermission(),
            hasBackgroundLocation = hasBackgroundLocationPermission()
        )
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel(CancellationException("Google Play services task was cancelled"))
        }
    }
}
