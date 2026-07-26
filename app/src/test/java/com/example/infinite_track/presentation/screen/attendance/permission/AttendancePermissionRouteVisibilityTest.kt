package com.example.infinite_track.presentation.screen.attendance.permission

import com.example.infinite_track.presentation.screen.attendance.shouldAutoOpenPermissionPanel
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendancePermissionPanelVisibilityTest {

    private val attendanceScreenSource = File(
        requireNotNull(System.getProperty("user.dir")),
        "src/main/java/com/example/infinite_track/presentation/screen/attendance/AttendanceScreen.kt"
    ).readText()

    @Test
    fun geofenceRuntimeCannotAutoOpenPermissionPanel() {
        assertFalse(
            attendanceScreenSource.contains("LaunchedEffect(uiState.geofenceRuntime)")
        )
        assertFalse(
            attendanceScreenSource.contains("requiresPermissionReadinessRecovery()")
        )
    }

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
