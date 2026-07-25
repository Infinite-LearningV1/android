package com.example.infinite_track.presentation.screen.attendance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeofenceRuntimePresentationDecisionTest {

    @Test
    fun `registration blockers open the existing permission readiness recovery`() {
        assertTrue(
            GeofenceRuntimeUiState(
                reason = GeofenceRuntimeUiReason.BACKGROUND_LOCATION_REQUIRED
            ).requiresPermissionReadinessRecovery()
        )
        assertTrue(
            GeofenceRuntimeUiState(
                reason = GeofenceRuntimeUiReason.DEVICE_LOCATION_DISABLED
            ).requiresPermissionReadinessRecovery()
        )
    }

    @Test
    fun `notification denial and generic degradation do not claim monitoring recovery`() {
        assertFalse(
            GeofenceRuntimeUiState(
                monitoringAvailable = true,
                notificationAvailable = false,
                reason = GeofenceRuntimeUiReason.NOTIFICATION_PERMISSION_REQUIRED
            ).requiresPermissionReadinessRecovery()
        )
        assertFalse(
            GeofenceRuntimeUiState(
                reason = GeofenceRuntimeUiReason.REGISTRATION_DEGRADED
            ).requiresPermissionReadinessRecovery()
        )
    }
}
