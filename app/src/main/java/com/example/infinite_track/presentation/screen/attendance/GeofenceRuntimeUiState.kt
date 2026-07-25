package com.example.infinite_track.presentation.screen.attendance

enum class GeofenceRuntimeUiReason {
    PRECISE_LOCATION_REQUIRED,
    BACKGROUND_LOCATION_REQUIRED,
    DEVICE_LOCATION_DISABLED,
    PLAY_SERVICES_UNAVAILABLE,
    NOTIFICATION_PERMISSION_REQUIRED,
    REGISTRATION_DEGRADED
}

data class GeofenceRuntimeUiState(
    val monitoringAvailable: Boolean = false,
    val notificationAvailable: Boolean = false,
    val reason: GeofenceRuntimeUiReason? = null
)

internal fun GeofenceRuntimeUiState.requiresPermissionReadinessRecovery(): Boolean =
    !monitoringAvailable && reason in setOf(
        GeofenceRuntimeUiReason.PRECISE_LOCATION_REQUIRED,
        GeofenceRuntimeUiReason.BACKGROUND_LOCATION_REQUIRED,
        GeofenceRuntimeUiReason.DEVICE_LOCATION_DISABLED,
        GeofenceRuntimeUiReason.PLAY_SERVICES_UNAVAILABLE
    )
