package com.example.infinite_track.data.platform.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.infinite_track.data.platform.geofence.store.PersistedRegistrationKind
import com.example.infinite_track.domain.model.geofence.GeofencePermissionRequirement
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import com.example.infinite_track.presentation.geofencing.GeofenceBroadcastReceiver
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class PlayServicesGeofencingPlatformClient @Inject constructor(
    @ApplicationContext private val context: Context
) : GeofencingPlatformClient {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun observeReadiness(): Flow<GeofenceRuntimeReadiness> = flow {
        emit(currentReadiness())
    }

    override suspend fun removeOwnedGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent).awaitTask()
    }

    @SuppressLint("MissingPermission")
    override suspend fun addAll(registrations: List<PlatformGeofenceRegistration>) {
        require(registrations.isNotEmpty()) { "At least one registration is required" }
        val geofences = registrations.map(::buildGeofence)
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
        geofencingClient.addGeofences(request, geofencePendingIntent).awaitTask()
    }

    private fun buildGeofence(registration: PlatformGeofenceRegistration): Geofence {
        val builder = Geofence.Builder()
            .setRequestId(registration.requestId)
            .setCircularRegion(
                registration.coordinate.latitude,
                registration.coordinate.longitude,
                registration.radius.value.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

        return when (registration.kind) {
            PersistedRegistrationKind.REMINDER -> builder
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .setLoiteringDelay(REMINDER_LOITERING_DELAY_MILLIS)
                .build()
            PersistedRegistrationKind.ACTIVE -> builder
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }
    }

    private suspend fun currentReadiness(): GeofenceRuntimeReadiness {
        val missingPermissions = buildSet {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                add(GeofencePermissionRequirement.PRECISE_FOREGROUND)
            }
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) {
                add(GeofencePermissionRequirement.BACKGROUND_LOCATION)
            }
        }
        val registrationReadiness = when {
            missingPermissions.isNotEmpty() -> RegistrationReadiness.PermissionRequired(
                missingPermissions
            )
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) !=
                ConnectionResult.SUCCESS -> RegistrationReadiness.PlayServicesUnavailable
            !isDeviceLocationEnabled() -> RegistrationReadiness.DeviceLocationDisabled
            else -> RegistrationReadiness.Ready
        }
        val notificationReadiness = if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            NotificationReadiness.READY
        } else {
            NotificationReadiness.PERMISSION_REQUIRED
        }
        return GeofenceRuntimeReadiness(registrationReadiness, notificationReadiness)
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private suspend fun isDeviceLocationEnabled(): Boolean {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 0)
            .build()
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()
        return runCatching {
            LocationServices.getSettingsClient(context).checkLocationSettings(request).awaitTask()
            true
        }.getOrElse { exception ->
            if (exception is CancellationException) throw exception
            false
        }
    }

    private companion object {
        const val REMINDER_LOITERING_DELAY_MILLIS = 120_000
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) continuation.resumeWithException(exception)
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel(CancellationException("Google Play services task was cancelled"))
        }
    }
}
