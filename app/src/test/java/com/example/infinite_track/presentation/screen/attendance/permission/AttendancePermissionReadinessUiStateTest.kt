package com.example.infinite_track.presentation.screen.attendance.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionReadinessUiStateTest {

    @Test
    fun `required progress counts foreground camera and device location only`() {
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
    fun `notification denied does not block work mode`() {
        val state = toAttendancePermissionReadinessUiState(
            foregroundLocationGranted = true,
            cameraGranted = true,
            deviceLocationEnabled = true,
            notificationGranted = false,
            backgroundLocationGranted = true
        )

        assertTrue(state.canContinueToWorkMode)
        assertEquals(AttendancePermissionAction.CONTINUE_TO_WORK_MODE, state.nextRequiredAction)
    }

    @Test
    fun `background location denied does not block work mode`() {
        val state = toAttendancePermissionReadinessUiState(
            foregroundLocationGranted = true,
            cameraGranted = true,
            deviceLocationEnabled = true,
            notificationGranted = true,
            backgroundLocationGranted = false
        )

        assertTrue(state.canContinueToWorkMode)
        assertEquals(AttendancePermissionAction.CONTINUE_TO_WORK_MODE, state.nextRequiredAction)
    }

    @Test
    fun `device location setting is a hard blocker`() {
        val state = toAttendancePermissionReadinessUiState(
            foregroundLocationGranted = true,
            cameraGranted = true,
            deviceLocationEnabled = false,
            notificationGranted = true,
            backgroundLocationGranted = true
        )

        assertFalse(state.canContinueToWorkMode)
        assertEquals(AttendancePermissionAction.OPEN_DEVICE_LOCATION_SETTINGS, state.nextRequiredAction)
    }
}
