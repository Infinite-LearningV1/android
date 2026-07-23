package com.example.infinite_track.presentation.screen.attendance.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionRouteVisibilityTest {

    @Test
    fun gateStaysInvisibleWhileReadinessIsLoading() {
        assertFalse(
            shouldRenderPermissionReadiness(
                AttendancePermissionReadinessUiState(isLoading = true)
            )
        )
    }

    @Test
    fun gateStaysInvisibleWhenRequiredAccessIsAlreadyReady() {
        assertFalse(
            shouldRenderPermissionReadiness(
                AttendancePermissionReadinessUiState(
                    isLoading = false,
                    canContinue = true
                )
            )
        )
    }

    @Test
    fun gateRendersOnlyWhenRequiredAccessNeedsAttention() {
        assertTrue(
            shouldRenderPermissionReadiness(
                AttendancePermissionReadinessUiState(
                    isLoading = false,
                    canContinue = false
                )
            )
        )
    }
}
