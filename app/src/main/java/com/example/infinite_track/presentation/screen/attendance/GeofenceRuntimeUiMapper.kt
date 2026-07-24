package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.domain.model.geofence.GeofencePermissionRequirement
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeFailure
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeReadiness
import com.example.infinite_track.domain.model.geofence.GeofenceRuntimeResult
import com.example.infinite_track.domain.model.geofence.NotificationReadiness
import com.example.infinite_track.domain.model.geofence.RegistrationReadiness
import javax.inject.Inject

class GeofenceRuntimeUiMapper @Inject constructor() {

    fun map(
        readiness: GeofenceRuntimeReadiness,
        runtime: GeofenceRuntimeResult
    ): GeofenceRuntimeUiState {
        val monitoringAvailable = readiness.registration is RegistrationReadiness.Ready &&
            runtime !is GeofenceRuntimeResult.Degraded
        val notificationAvailable = readiness.notification == NotificationReadiness.READY
        val reason = readiness.registration.toUiReason()
            ?: (runtime as? GeofenceRuntimeResult.Degraded)?.failure?.toUiReason()
            ?: if (!notificationAvailable) {
                GeofenceRuntimeUiReason.NOTIFICATION_PERMISSION_REQUIRED
            } else {
                null
            }

        return GeofenceRuntimeUiState(
            monitoringAvailable = monitoringAvailable,
            notificationAvailable = notificationAvailable,
            reason = reason
        )
    }

    private fun RegistrationReadiness.toUiReason(): GeofenceRuntimeUiReason? = when (this) {
        RegistrationReadiness.Ready -> null
        RegistrationReadiness.DeviceLocationDisabled ->
            GeofenceRuntimeUiReason.DEVICE_LOCATION_DISABLED
        RegistrationReadiness.PlayServicesUnavailable ->
            GeofenceRuntimeUiReason.PLAY_SERVICES_UNAVAILABLE
        is RegistrationReadiness.PermissionRequired -> missing.toUiReason()
    }

    private fun GeofenceRuntimeFailure.toUiReason(): GeofenceRuntimeUiReason = when (this) {
        is GeofenceRuntimeFailure.PermissionNotGranted -> missing.toUiReason()
        GeofenceRuntimeFailure.DeviceLocationDisabled ->
            GeofenceRuntimeUiReason.DEVICE_LOCATION_DISABLED
        GeofenceRuntimeFailure.PlayServicesUnavailable ->
            GeofenceRuntimeUiReason.PLAY_SERVICES_UNAVAILABLE
        else -> GeofenceRuntimeUiReason.REGISTRATION_DEGRADED
    }

    private fun Set<GeofencePermissionRequirement>.toUiReason(): GeofenceRuntimeUiReason =
        if (GeofencePermissionRequirement.PRECISE_FOREGROUND in this) {
            GeofenceRuntimeUiReason.PRECISE_LOCATION_REQUIRED
        } else {
            GeofenceRuntimeUiReason.BACKGROUND_LOCATION_REQUIRED
        }
}
