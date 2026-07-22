package com.example.infinite_track.presentation.screen.attendance.face

enum class CameraPermissionRecoveryUi {
    CAMERA,
    RECOVERY_REQUIRED,
    RATIONALE,
    APPLICATION_SETTINGS
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
