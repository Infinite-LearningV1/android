package com.example.infinite_track.domain.model.geofence

import com.example.infinite_track.domain.model.attendance.TodayStatus
import com.example.infinite_track.domain.model.auth.UserModel
import com.example.infinite_track.domain.model.wfa.WfaBookingForDate

enum class GeofenceReconcileReason {
    FOREGROUND_REFRESH,
    CHECK_IN_SUCCEEDED,
    CHECK_OUT_SUCCEEDED,
    BOOT_RECOVERY
}

sealed interface RegistrationReadiness {
    data object Ready : RegistrationReadiness

    data class PermissionRequired(
        val missing: Set<GeofencePermissionRequirement>
    ) : RegistrationReadiness

    data object DeviceLocationDisabled : RegistrationReadiness
    data object PlayServicesUnavailable : RegistrationReadiness
}

enum class GeofencePermissionRequirement { PRECISE_FOREGROUND, BACKGROUND_LOCATION }

enum class NotificationReadiness { READY, PERMISSION_REQUIRED }

data class GeofenceRuntimeReadiness(
    val registration: RegistrationReadiness,
    val notification: NotificationReadiness
)

data class GeofenceRuntimeInputs(
    val todayStatus: TodayStatus,
    val profile: UserModel?,
    val approvedWfaBooking: WfaBookingForDate,
    val readiness: GeofenceRuntimeReadiness,
    val reason: GeofenceReconcileReason
)
