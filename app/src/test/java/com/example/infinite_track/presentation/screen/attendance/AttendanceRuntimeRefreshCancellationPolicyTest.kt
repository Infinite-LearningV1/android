package com.example.infinite_track.presentation.screen.attendance

import com.example.infinite_track.utils.UiState
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AttendanceRuntimeRefreshCancellationPolicyTest {

    @Test
    fun `cancellation propagates without replacing retryable error or runtime state`() = runTest {
        val before = AttendanceScreenState(
            uiState = UiState.Success(Unit),
            geofenceRuntime = GeofenceRuntimeUiState(
                monitoringAvailable = true,
                notificationAvailable = true
            )
        )
        var rendered = before

        try {
            runAttendanceRuntimeRefresh(
                operation = { throw CancellationException("refresh cancelled") },
                onUnexpectedFailure = {
                    rendered = before.copy(
                        uiState = UiState.Error("retryable"),
                        geofenceRuntime = GeofenceRuntimeUiState()
                    )
                }
            )
            fail("Expected refresh cancellation to propagate")
        } catch (_: CancellationException) {
        }

        assertEquals(before, rendered)
    }
}
