package com.example.infinite_track.presentation.screen.attendance.permission

import androidx.compose.material3.SnackbarDuration
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarTimeout
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendancePermissionFeedbackTimeoutBindingTest {

    @Test
    fun `permission feedback binds short and long to shared exact timeout contract`() {
        assertEquals(
            InfiniteSnackbarTimeout.SHORT,
            AttendanceFeedbackDuration.SHORT.toInfiniteSnackbarTimeout()
        )
        assertEquals(
            InfiniteSnackbarTimeout.LONG,
            AttendanceFeedbackDuration.LONG.toInfiniteSnackbarTimeout()
        )

        val visuals = AttendancePermissionFeedback(
            id = "feedback-1",
            message = "Unable to open settings.",
            semantic = InfiniteSemantic.Error,
            duration = AttendanceFeedbackDuration.LONG
        ).toInfiniteSnackbarVisuals()
        assertEquals(SnackbarDuration.Indefinite, visuals.duration)
        assertEquals(8_000L, visuals.autoDismissTimeoutMillis)
    }
}
