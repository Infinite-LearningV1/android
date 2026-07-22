package com.example.infinite_track.presentation.screen.attendance.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionReadinessUiStateTest {

    @Test
    fun `default state is a valid loading state`() {
        val state = AttendancePermissionReadinessUiState()

        assertTrue(state.isLoading)
        assertFalse(state.isRefreshing)
        assertTrue(state.requiredItems.isEmpty())
        assertTrue(state.optionalItems.isEmpty())
        assertFalse(state.primaryActionEnabled)
    }

    @Test
    fun `legacy projection keeps required progress independent from optional access`() {
        val state = toAttendancePermissionReadinessUiState(
            foregroundLocationGranted = true,
            cameraGranted = false,
            deviceLocationEnabled = true,
            notificationGranted = true,
            backgroundLocationGranted = true
        )

        assertEquals(2, state.requiredReadyCount)
        assertEquals(3, state.requiredTotalCount)
        assertEquals("2/3 akses wajib siap", state.progressCopy)
        assertFalse(state.canContinueToWorkMode)
        assertEquals(AttendancePermissionAction.REQUEST_CAMERA, state.nextRequiredAction)
    }

    @Test
    fun `legacy optional access does not block work mode`() {
        val state = toAttendancePermissionReadinessUiState(
            foregroundLocationGranted = true,
            cameraGranted = true,
            deviceLocationEnabled = true,
            notificationGranted = false,
            backgroundLocationGranted = false
        )

        assertTrue(state.canContinueToWorkMode)
        assertEquals(AttendancePermissionAction.CONTINUE_TO_WORK_MODE, state.nextRequiredAction)
    }
}
