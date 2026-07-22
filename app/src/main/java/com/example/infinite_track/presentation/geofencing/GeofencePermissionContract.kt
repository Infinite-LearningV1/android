package com.example.infinite_track.presentation.geofencing

enum class GeofencePermissionCapability {
    READY,
    PRECISE_FOREGROUND_REQUIRED,
    BACKGROUND_LOCATION_DEGRADED
}

data class GeofencePermissionDecision(
    val capability: GeofencePermissionCapability,
    val canRegisterAutomaticMonitoring: Boolean,
    val message: String
)

object GeofencePermissionContract {
    fun evaluate(
        hasPreciseForegroundLocation: Boolean,
        hasBackgroundLocation: Boolean
    ): GeofencePermissionDecision = when {
        !hasPreciseForegroundLocation -> GeofencePermissionDecision(
            capability = GeofencePermissionCapability.PRECISE_FOREGROUND_REQUIRED,
            canRegisterAutomaticMonitoring = false,
            message = "Lokasi presisi diperlukan sebelum pemantauan area kerja dapat digunakan. Kembali ke layar kesiapan untuk memulihkan akses."
        )

        !hasBackgroundLocation -> GeofencePermissionDecision(
            capability = GeofencePermissionCapability.BACKGROUND_LOCATION_DEGRADED,
            canRegisterAutomaticMonitoring = false,
            message = "Lokasi latar belakang belum aktif. Pemantauan area kerja berjalan terbatas, tetapi absensi manual tetap bisa digunakan."
        )

        else -> GeofencePermissionDecision(
            capability = GeofencePermissionCapability.READY,
            canRegisterAutomaticMonitoring = true,
            message = "Izin pemantauan geofence telah diberikan"
        )
    }
}
