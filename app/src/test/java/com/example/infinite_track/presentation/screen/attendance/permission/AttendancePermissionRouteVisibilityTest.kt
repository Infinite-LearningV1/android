package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.presentation.screen.attendance.shouldAutoOpenPermissionPanel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionPanelVisibilityTest {

    @Test
    fun panelStaysClosedWhileReadinessIsLoading() {
        assertFalse(
            shouldAutoOpenPermissionPanel(
                isLoading = true,
                canContinue = false,
                initialCheckHandled = false
            )
        )
    }

    @Test
    fun panelStaysClosedWhenRequiredAccessIsAlreadyReady() {
        assertFalse(
            shouldAutoOpenPermissionPanel(
                isLoading = false,
                canContinue = true,
                initialCheckHandled = false
            )
        )
    }

    @Test
    fun panelAutoOpensOnceWhenRequiredAccessNeedsAttention() {
        assertTrue(
            shouldAutoOpenPermissionPanel(
                isLoading = false,
                canContinue = false,
                initialCheckHandled = false
            )
        )
        assertFalse(
            shouldAutoOpenPermissionPanel(
                isLoading = false,
                canContinue = false,
                initialCheckHandled = true
            )
        )
    }
}
