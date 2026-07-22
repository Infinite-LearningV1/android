package com.example.infinite_track.presentation.screen.attendance.face

enum class CameraPermissionRecoveryUi {
    CAMERA,
    RECOVERY_REQUIRED,
    RATIONALE,
    APPLICATION_SETTINGS
}

internal const val CAMERA_SETTINGS_LAUNCH_FAILURE_FEEDBACK =
    "Pengaturan aplikasi tidak dapat dibuka. Silakan coba lagi atau tutup verifikasi wajah."

internal sealed interface CameraSettingsLaunchResult {
    val feedback: String?

    data object Launched : CameraSettingsLaunchResult {
        override val feedback: String? = null
    }

    data class Failed(
        override val feedback: String
    ) : CameraSettingsLaunchResult
}

internal fun launchCameraSettingsSafely(
    launch: () -> Unit
): CameraSettingsLaunchResult = try {
    launch()
    CameraSettingsLaunchResult.Launched
} catch (_: RuntimeException) {
    // ActivityNotFoundException and SecurityException are RuntimeException subclasses.
    CameraSettingsLaunchResult.Failed(
        feedback = CAMERA_SETTINGS_LAUNCH_FAILURE_FEEDBACK
    )
}

object CameraPermissionRecoveryContract {
    fun resolve(
        isCameraGranted: Boolean,
        hasRequestedRecovery: Boolean,
        shouldShowRationale: Boolean
    ): CameraPermissionRecoveryUi = when {
        isCameraGranted -> CameraPermissionRecoveryUi.CAMERA
        !hasRequestedRecovery -> CameraPermissionRecoveryUi.RECOVERY_REQUIRED
        shouldShowRationale -> CameraPermissionRecoveryUi.RATIONALE
        else -> CameraPermissionRecoveryUi.APPLICATION_SETTINGS
    }
}
