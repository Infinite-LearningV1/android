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
import com.example.infinite_track.data.soucre.local.preferences.ReminderGeofence
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasForegroundLocationPermission() && hasBackgroundLocationPermission()
    }

    /**
     * Get detailed permission status for UI feedback
     */
    fun getPermissionStatusMessage(): String {
        return when {
            hasAllRequiredPermissions() -> "Semua izin lokasi telah diberikan"
            !hasForegroundLocationPermission() -> "Izin lokasi diperlukan untuk fitur geofencing"
            !hasBackgroundLocationPermission() -> "Izin lokasi latar belakang diperlukan untuk pemantauan area kerja secara otomatis"
            else -> "Status izin tidak diketahui"
        }
    }

    /**
     * Remove all geofences registered by this application
     * This ensures we never hit the system limit of ~100 geofences
     */
    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d(TAG, "Semua geofence berhasil dihapus")
                ioScope.launch {
                    attendancePreference.clearLastGeofenceRequestId()
                    attendancePreference.clearLastGeofenceParams()
                    attendancePreference.clearReminderGeofences()
                }
            }
            addOnFailureListener { exception ->
                Log.e(TAG, "Gagal menghapus semua geofence", exception)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(
        id: String, 
        latitude: Double, 
        longitude: Double, 
        radius: Float,
        onPermissionError: ((String) -> Unit)? = null
    ) {
        // Enhanced permission guards with user feedback
        if (!hasForegroundLocationPermission()) {
            val errorMsg = "Izin lokasi (FINE/COARSE) diperlukan untuk fitur geofencing. Silakan berikan izin di pengaturan aplikasi."
            Log.e(TAG, errorMsg)
            onPermissionError?.invoke(errorMsg)
            return
        }
        if (!hasBackgroundLocationPermission()) {
            val errorMsg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "Izin lokasi latar belakang diperlukan untuk pemantauan area kerja otomatis (Android 10+). " +
                "Pilih 'Izinkan sepanjang waktu' di pengaturan lokasi aplikasi."
            } else {
                "Izin lokasi latar belakang tidak tersedia."
            }
            Log.e(TAG, errorMsg)
            onPermissionError?.invoke(errorMsg)
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
                // STEP 1: Always remove all existing geofences first (clean slate approach)
                Log.d(TAG, "Membersihkan semua geofence sebelum menambah yang baru...")

                geofencingClient.removeGeofences(geofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.d(TAG, "Semua geofence berhasil dihapus, sekarang menambah geofence baru...")

                        // STEP 2: Create and add the new geofence ONLY after removal is successful
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

                        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                            addOnSuccessListener {
                                Log.d(
                                    TAG,
                                    "Geofence berhasil ditambahkan: $requestId (lat: $latitude, lng: $longitude, radius: ${safeRadius}m)"
                                )
                                ioScope.launch {
                                    attendancePreference.saveLastGeofenceRequestId(requestId)
                                    attendancePreference.saveLastGeofenceParams(requestId, latitude, longitude, safeRadius)
                                }
                            }
                            addOnFailureListener { exception ->
                                val status = (exception as? ApiException)?.statusCode
                                val statusText = status?.let { GeofenceStatusCodes.getStatusCodeString(it) }
                                Log.e(TAG, "Gagal menambahkan geofence: $requestId (${status ?: "?"}: ${statusText ?: exception.message})", exception)
                            }
                        }
                    }
                    addOnFailureListener { exception ->
                        Log.e(
                            TAG,
                            "Gagal menghapus semua geofence, geofence baru tidak akan ditambahkan",
                            exception
                        )
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
        if (!hasForegroundLocationPermission()) {
            Log.e(TAG, "Tidak ada izin ACCESS_FINE_LOCATION. Reminder geofence tidak dapat ditambahkan.")
            return
        }
        if (!hasBackgroundLocationPermission()) {
            Log.e(TAG, "Tidak ada izin ACCESS_BACKGROUND_LOCATION. Reminder geofence gagal (API 29+).")
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

    @SuppressLint("MissingPermission")
    fun restoreGeofencesAfterBoot(
        monitoringGeofence: Triple<String, Pair<Double, Double>, Float>?,
        reminderGeofences: List<ReminderGeofence>
    ) {
        if (!hasForegroundLocationPermission()) {
            Log.e(TAG, "Tidak ada izin ACCESS_FINE_LOCATION. Geofence tidak dapat dipulihkan setelah reboot.")
            return
        }
        if (!hasBackgroundLocationPermission()) {
            Log.e(TAG, "Tidak ada izin ACCESS_BACKGROUND_LOCATION. Geofence gagal dipulihkan setelah reboot (API 29+).")
            return
        }

        val geofences = mutableListOf<Geofence>()
        monitoringGeofence?.let { (requestId, latLng, radius) ->
            val (lat, lng) = latLng
            geofences.add(
                Geofence.Builder()
                    .setRequestId(requestId)
                    .setCircularRegion(lat, lng, radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            )
        }
        reminderGeofences.forEach { reminder ->
            geofences.add(
                Geofence.Builder()
                    .setRequestId(reminder.id)
                    .setCircularRegion(reminder.latitude, reminder.longitude, reminder.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            )
        }

        if (geofences.isEmpty()) {
            Log.d(TAG, "No geofences to restore after boot.")
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                Log.d(TAG, "Membersihkan semua geofence sebelum memulihkan setelah reboot...")
                geofencingClient.removeGeofences(geofencePendingIntent).run {
                    addOnSuccessListener {
                        val geofencingRequestBuilder = GeofencingRequest.Builder()
                            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        geofences.forEach { geofencingRequestBuilder.addGeofence(it) }

                        geofencingClient.addGeofences(
                            geofencingRequestBuilder.build(),
                            geofencePendingIntent
                        ).run {
                            addOnSuccessListener {
                                Log.d(TAG, "Berhasil memulihkan ${geofences.size} geofence setelah reboot")
                            }
                            addOnFailureListener { exception ->
                                val status = (exception as? ApiException)?.statusCode
                                val statusText = status?.let { GeofenceStatusCodes.getStatusCodeString(it) }
                                Log.e(TAG, "Gagal memulihkan geofence setelah reboot (${status ?: "?"}: ${statusText ?: exception.message})", exception)
                            }
                        }
                    }
                    addOnFailureListener { exception ->
                        Log.e(TAG, "Gagal menghapus geofence lama sebelum restore reboot", exception)
                    }
                }
            }
            .addOnFailureListener { exception ->
                val status = (exception as? ApiException)?.statusCode
                val statusText = status?.let { GeofenceStatusCodes.getStatusCodeString(it) }
                Log.e(TAG, "Location settings tidak memenuhi syarat untuk memulihkan Geofencing (${status ?: "?"}: ${statusText ?: exception.message}). Pastikan Location diaktifkan.")
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
}
