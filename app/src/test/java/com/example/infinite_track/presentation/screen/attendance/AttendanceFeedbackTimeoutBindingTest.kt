package com.example.infinite_track.presentation.screen.attendance

import androidx.compose.material3.SnackbarDuration
import com.example.infinite_track.presentation.design.components.status.InfiniteSnackbarTimeout
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceFeedbackTimeoutBindingTest {

    @Test
    fun `attendance feedback binds short and long to shared exact timeout contract`() {
        assertEquals(
            InfiniteSnackbarTimeout.SHORT,
            AttendanceTransientFeedbackDuration.SHORT.toInfiniteSnackbarTimeout()
        )
        assertEquals(
            InfiniteSnackbarTimeout.LONG,
            AttendanceTransientFeedbackDuration.LONG.toInfiniteSnackbarTimeout()
        )

        val visuals = AttendanceTransientFeedback(
            id = 1L,
            message = "Unable to read location.",
            semantic = InfiniteSemantic.Error,
            duration = AttendanceTransientFeedbackDuration.LONG
        ).toInfiniteSnackbarVisuals()
        assertEquals(SnackbarDuration.Indefinite, visuals.duration)
        assertEquals(8_000L, visuals.autoDismissTimeoutMillis)
    }
}
