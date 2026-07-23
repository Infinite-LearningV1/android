package com.example.infinite_track.presentation.feedback

import androidx.compose.material3.SnackbarDuration
import com.example.infinite_track.R
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
    fun `events map to stable resource ids and semantic duration policy`() {
        val expectations = listOf(
            ExpectedFeedback(
                event = AppFeedbackEvent.LOGIN_SUCCESS,
                titleRes = R.string.app_feedback_login_success_title,
                messageRes = R.string.app_feedback_login_success_message,
                semantic = InfiniteSemantic.Success,
                duration = SnackbarDuration.Short
            ),
            ExpectedFeedback(
                event = AppFeedbackEvent.LOGOUT_SUCCESS,
                titleRes = R.string.app_feedback_logout_success_title,
                messageRes = R.string.app_feedback_logout_success_message,
                semantic = InfiniteSemantic.Success,
                duration = SnackbarDuration.Short
            ),
            ExpectedFeedback(
                event = AppFeedbackEvent.LOGOUT_REMOTE_WARNING,
                titleRes = R.string.app_feedback_logout_remote_warning_title,
                messageRes = R.string.app_feedback_logout_remote_warning_message,
                semantic = InfiniteSemantic.Warning,
                duration = SnackbarDuration.Long
            )
        )

        expectations.forEach { expected ->
            val resource = expected.event.toResourceModel()

            assertEquals(expected.titleRes, resource.titleRes)
            assertEquals(expected.messageRes, resource.messageRes)
            assertEquals(expected.semantic, resource.semantic)
            assertEquals(expected.duration, resource.duration)
        }
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

    private data class ExpectedFeedback(
        val event: AppFeedbackEvent,
        val titleRes: Int,
        val messageRes: Int,
        val semantic: InfiniteSemantic,
        val duration: SnackbarDuration
    )
}
