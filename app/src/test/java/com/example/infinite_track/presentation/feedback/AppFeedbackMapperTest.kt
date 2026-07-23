package com.example.infinite_track.presentation.feedback

import androidx.compose.material3.SnackbarDuration
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFeedbackMapperTest {

    @Test
    fun `login success maps to short semantic snackbar copy`() {
        val visuals = AppFeedbackEvent.LOGIN_SUCCESS.toSnackbarVisuals()

        assertEquals("Login successful", visuals.title)
        assertEquals("Welcome back to Infinite Track.", visuals.message)
        assertEquals(InfiniteSemantic.Success, visuals.semantic)
        assertEquals(SnackbarDuration.Short, visuals.duration)
    }

    @Test
    fun `controller does not replay an event to a later collector`() = runTest {
        val controller = AppFeedbackController()

        controller.emit(AppFeedbackEvent.LOGIN_SUCCESS)
        val replayed = async {
            withTimeoutOrNull(1) { controller.events.first() }
        }

        assertNull(replayed.await())
    }
}
