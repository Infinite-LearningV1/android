package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.domain.model.booking.SubmittedWfaRequest
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestFieldErrors
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.domain.model.booking.WfaRequestStatus
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WfaRequestScreensTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun formShowsServerPolicyAndConditionalOtherFieldWithoutEditableRadius() {
        var state = editingState()
        renderForm(state)

        composeRule.onNodeWithText("Alya Putri").assertIsDisplayed()
        composeRule.onNodeWithText("Kafe Taman").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaRadiusReadOnly").assertIsDisplayed()
        composeRule.onNodeWithText("Radius kebijakan", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Radius", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Keperluan keluarga").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaOtherReason").assertDoesNotExist()

        state = state.copy(draft = state.draft.copy(reasonId = 2))
        renderForm(state)
        composeRule.onNodeWithTag("wfaOtherReason").assertIsDisplayed()
    }

    @Test
    fun fieldErrorIsRenderedNearConditionalOtherControl() {
        val state = editingState().copy(
            draft = editingState().draft.copy(reasonId = 2),
            fieldErrors = WfaRequestFieldErrors(otherReason = WfaRequestFieldError.OTHER_REASON_REQUIRED)
        )
        renderForm(state)

        composeRule.onNodeWithTag("wfaOtherReason").assertIsDisplayed()
        composeRule.onNodeWithText("Jelaskan alasan lainnya").assertIsDisplayed()
    }

    @Test
    fun reviewShowsExactDraftAndConfirmEmitsOnlyOneEvent() {
        val events = mutableListOf<WfaRequestEvent>()
        val state = editingState().copy(phase = WfaRequestPhase.Reviewing)
        composeRule.setContent {
            Infinite_TrackTheme {
                WfaRequestReviewScreen(state, events::add, onBack = {})
            }
        }

        composeRule.onNodeWithText("2026-08-04").assertIsDisplayed()
        composeRule.onNodeWithText("Keperluan keluarga").assertIsDisplayed()
        composeRule.onNodeWithText("Kafe Taman").assertIsDisplayed()
        composeRule.onNodeWithText("Butuh ruang tenang").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaConfirmAction").performClick()
        assertEquals(listOf(WfaRequestEvent.SubmitConfirmed), events)
    }

    @Test
    fun submittingDisablesConfirmation() {
        val state = editingState().copy(phase = WfaRequestPhase.Submitting)
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestReviewScreen(state, {}, onBack = {}) }
        }

        composeRule.onNodeWithTag("wfaConfirmAction").assertIsNotEnabled()
        composeRule.onNodeWithTag("wfaEditAction").assertIsNotEnabled()
    }

    @Test
    fun resultShowsBookingIdAndSafeFailureRecovery() {
        val base = editingState()
        val success = base.copy(
            phase = WfaRequestPhase.Success,
            submitResult = SubmittedWfaRequest(
                bookingId = 9123,
                scheduleDate = LocalDate.of(2026, 8, 4),
                status = WfaRequestStatus.PENDING,
                location = location,
                reasonLabel = "Keperluan keluarga",
                radiusMeters = 100,
                submittedAt = null
            )
        )
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestResultScreen(success, {}, {}, {}) }
        }
        composeRule.onNodeWithText("ID booking: 9123").assertIsDisplayed()

        val events = mutableListOf<WfaRequestEvent>()
        val failure = base.copy(phase = WfaRequestPhase.Failure, failure = WfaRequestFailure.NetworkUnavailable)
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestResultScreen(failure, events::add, {}, {}) }
        }
        composeRule.onNodeWithText("Koneksi bermasalah").assertIsDisplayed()
        composeRule.onNodeWithText("Periksa koneksi internet lalu coba kirim kembali.").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaFailurePrimaryAction").performClick()
        assertEquals(listOf(WfaRequestEvent.RetrySubmitClicked), events)
    }

    private fun renderForm(state: WfaRequestUiState) {
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestFormScreen(state, {}, onBack = {}) }
        }
    }

    private fun editingState() = WfaRequestUiState(
        phase = WfaRequestPhase.Editing,
        employee = WfaEmployeeSummary("Alya Putri", "Product"),
        location = location,
        config = WfaRequestConfig(
            radiusMeters = 100,
            reasons = listOf(
                WfaRequestReason(1, "Keperluan keluarga", false),
                WfaRequestReason(2, "Lainnya", true)
            )
        ),
        draft = WfaRequestDraft(
            scheduleDate = LocalDate.of(2026, 8, 4),
            reasonId = 1,
            otherReasonText = "",
            notes = "Butuh ruang tenang",
            location = location
        )
    )

    private companion object {
        val location = WfaCandidateLocation(-0.89, 119.87, "Kafe Taman", "Jl. Merdeka 10, Palu")
    }
}
