package com.example.infinite_track.presentation.screen.attendance.face

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionRecoveryContractTest {

    @Test
    fun `missing camera starts as explicit recovery without auto request`() {
        assertEquals(
            CameraPermissionRecoveryUi.RECOVERY_REQUIRED,
            CameraPermissionRecoveryContract.resolve(
                isCameraGranted = false,
                hasRequestedRecovery = false,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `denial with rationale remains retryable recovery`() {
        assertEquals(
            CameraPermissionRecoveryUi.RATIONALE,
            CameraPermissionRecoveryContract.resolve(
                isCameraGranted = false,
                hasRequestedRecovery = true,
                shouldShowRationale = true
            )
        )
    }

    @Test
    fun `denial without rationale after own request requires app settings`() {
        assertEquals(
            CameraPermissionRecoveryUi.APPLICATION_SETTINGS,
            CameraPermissionRecoveryContract.resolve(
                isCameraGranted = false,
                hasRequestedRecovery = true,
                shouldShowRationale = false
            )
        )
    }

    @Test
    fun `granted camera always opens scanner`() {
        assertEquals(
            CameraPermissionRecoveryUi.CAMERA,
            CameraPermissionRecoveryContract.resolve(
                isCameraGranted = true,
                hasRequestedRecovery = true,
                shouldShowRationale = false
            )
        )
    }
}
