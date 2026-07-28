package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.R
import com.example.infinite_track.domain.model.booking.SubmittedWfaRequest
import com.example.infinite_track.domain.model.booking.WfaCandidateLocation
import com.example.infinite_track.domain.model.booking.WfaRequestConfig
import com.example.infinite_track.domain.model.booking.WfaRequestDraft
import com.example.infinite_track.domain.model.booking.WfaRequestFailure
import com.example.infinite_track.domain.model.booking.WfaRequestFieldError
import com.example.infinite_track.domain.model.booking.WfaRequestFieldErrors
import com.example.infinite_track.domain.model.booking.WfaRequestReason
import com.example.infinite_track.domain.model.booking.WfaRequestStatus
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackDropDown
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackDropDownOption
import com.example.infinite_track.presentation.components.textfield.InfiniteTrackTextArea
import com.example.infinite_track.presentation.design.components.data.InfiniteChecklistCard
import com.example.infinite_track.presentation.design.components.data.InfiniteChecklistItem
import com.example.infinite_track.presentation.design.components.state.InfiniteResultHero
import com.example.infinite_track.presentation.design.tokens.InfiniteSemantic
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
    fun sharedEligibilityChecklistShowsOnlySupportedFacts() {
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteChecklistCard(
                    title = stringResource(R.string.wfa_request_eligibility_title),
                    items = listOf(
                        InfiniteChecklistItem(stringResource(R.string.wfa_request_eligibility_valid_location)),
                        InfiniteChecklistItem(stringResource(R.string.wfa_request_eligibility_server_radius)),
                        InfiniteChecklistItem(stringResource(R.string.wfa_request_eligibility_review_before_submission))
                    )
                )
            }
        }

        composeRule.onNodeWithText("Lokasi yang dipilih memiliki koordinat valid.").assertIsDisplayed()
        composeRule.onNodeWithText("Radius validasi dimuat dari konfigurasi server.").assertIsDisplayed()
        composeRule.onNodeWithText("Data wajib dapat ditinjau sebelum dikirim.").assertIsDisplayed()
        composeRule.onNodeWithText("Tidak ada jadwal WFA yang bentrok").assertDoesNotExist()
    }

    @Test
    fun sharedSuccessResultHeroShowsLocalizedTitleAndMessage() {
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteResultHero(
                    title = stringResource(R.string.wfa_request_success),
                    message = stringResource(R.string.wfa_request_success_message),
                    semantic = InfiniteSemantic.Success
                )
            }
        }

        composeRule.onNodeWithText("Permintaan berhasil dikirim").assertIsDisplayed()
        composeRule.onNodeWithText("Permintaan WFA Anda telah dikirim dan menunggu peninjauan.").assertIsDisplayed()
    }

    @Test
    fun formKeepsBothReferencePanelsInOneTransparentScrollableDestinationAndExposesClose() {
        var closeCount = 0
        composeRule.setContent {
            Infinite_TrackTheme {
                WfaRequestFormScreen(
                    uiState = editingState(),
                    onEvent = {},
                    onBack = {},
                    onClose = { closeCount += 1 }
                )
            }
        }

        composeRule.onNodeWithTag("wfaLocationMap").assertExists()
        composeRule.onNodeWithTag("wfaEmployeeCard").assertExists()
        composeRule.onNodeWithTag("wfaRequestDetails").assertExists()
        composeRule.onNodeWithTag("wfaEligibilityCard").assertExists()
        composeRule.onNodeWithTag("wfaReviewAction").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wfaScreenOwnedOpaqueBackground").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.runOnIdle { assertEquals(1, closeCount) }
    }

    @Test
    fun formShowsServerPolicyAndConditionalOtherFieldWithoutEditableRadius() {
        val state = editingState()
        renderForm(state)

        composeRule.onNodeWithText("Alya Putri").assertIsDisplayed()
        composeRule.onNodeWithText("Kafe Taman").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaRadiusReadOnly").assertIsDisplayed()
        composeRule.onNodeWithText("Radius kebijakan", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Radius", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Keperluan keluarga").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaOtherReason").assertDoesNotExist()

    }

    @Test
    fun otherReasonFieldAppearsOnlyForServerOtherChoice() {
        val state = editingState().let { it.copy(draft = it.draft.copy(reasonId = 2)) }
        renderForm(state)
        composeRule.onNodeWithTag("wfaOtherReason").assertIsDisplayed()
    }

    @Test
    fun sharedRequestInputsKeepSelectionAndNotesCountInCallerState() {
        composeRule.setContent {
            Infinite_TrackTheme {
                var selectedReason by remember { mutableStateOf<String?>(null) }
                Column {
                    InfiniteTrackDropDown(
                        selectedValue = selectedReason,
                        onSelected = { selectedReason = it },
                        items = listOf("Keperluan keluarga", "Lainnya"),
                        placeholder = "Pilih alasan",
                        modifier = Modifier.testTag("wfaReasonDropdown")
                    )
                    if (selectedReason == "Lainnya") {
                        InfiniteTrackTextArea(
                            value = "",
                            label = "Alasan lainnya",
                            onValueChange = {},
                            modifier = Modifier.testTag("wfaOtherReason")
                        )
                    }
                    InfiniteTrackTextArea(
                        value = "Butuh ruang tenang",
                        label = "Catatan",
                        onValueChange = {},
                        maxLength = 250,
                        showCharacterCount = true
                    )
                }
            }
        }

        composeRule.onNodeWithTag("wfaReasonDropdown").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Lainnya").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaOtherReason").assertIsDisplayed()
        composeRule.onNodeWithText("18/250").assertIsDisplayed()
    }

    @Test
    fun sharedRequestDropdownClosesAndDisablesItsTaggedTriggerWhenCallerDisablesIt() {
        val enabled = mutableStateOf(true)
        val selections = mutableListOf<String>()
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteTrackDropDown(
                    selectedValue = null,
                    onSelected = selections::add,
                    items = listOf("Lainnya"),
                    placeholder = "Pilih alasan",
                    enabled = enabled.value,
                    modifier = Modifier.testTag("wfaReasonDropdown")
                )
            }
        }

        composeRule.onNodeWithTag("wfaReasonDropdown").assertHasClickAction().performClick()
        composeRule.onNodeWithText("Lainnya").assertIsDisplayed()
        composeRule.runOnIdle { enabled.value = false }

        composeRule.onNodeWithTag("wfaReasonDropdown").assertIsNotEnabled()
        composeRule.onNodeWithText("Lainnya").assertDoesNotExist()
        assertEquals(emptyList<String>(), selections)
    }

    @Test
    fun stringDropdownKeepsCallerSelectionWhenItemsRefresh() {
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteTrackDropDown(
                    selectedValue = "Alasan tersimpan",
                    onSelected = {},
                    items = listOf("Alasan aktif"),
                    placeholder = "Pilih alasan"
                )
            }
        }

        composeRule.onNodeWithText("Alasan tersimpan").assertIsDisplayed()
    }

    @Test
    fun keyedDropdownPreservesIdentityForDuplicateLabels() {
        val selections = mutableListOf<Long>()
        composeRule.setContent {
            Infinite_TrackTheme {
                InfiniteTrackDropDown(
                    selectedKey = 1L,
                    onSelected = { selections += it },
                    options = listOf(
                        InfiniteTrackDropDownOption(
                            key = 1L,
                            label = "Alasan sama",
                            testTag = "reason-1"
                        ),
                        InfiniteTrackDropDownOption(
                            key = 2L,
                            label = "Alasan sama",
                            testTag = "reason-2"
                        )
                    ),
                    modifier = Modifier.testTag("keyedReasonDropdown")
                )
            }
        }

        composeRule.onNodeWithTag("keyedReasonDropdown").performClick()
        composeRule.onNodeWithTag("reason-2").performClick()

        assertEquals(listOf(2L), selections)
    }

    @Test
    fun formPreservesReasonIdentityWhenSelectingTaggedDuplicateLabelOption() {
        val duplicateLabelState = editingState().let { state ->
            state.copy(
                config = WfaRequestConfig(
                    radiusMeters = 100,
                    reasons = listOf(
                        WfaRequestReason(1, "Alasan sama", false),
                        WfaRequestReason(2, "Alasan sama", true)
                    )
                ),
                draft = state.draft.copy(reasonId = 1)
            )
        }
        val events = mutableListOf<WfaRequestEvent>()
        renderForm(duplicateLabelState, events::add)

        composeRule.onNodeWithTag("wfaReasonDropdown").performScrollTo().performClick()
        composeRule.onNodeWithTag("wfaReason-2").assertExists().performClick()

        assertEquals(listOf(WfaRequestEvent.ReasonSelected(2)), events)
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
    fun successResultShowsBackendConfirmedFieldsAndDestinations() {
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
            Infinite_TrackTheme { WfaRequestResultScreen(success, {}, {}, {}, {}) }
        }
        composeRule.onNodeWithText("ID booking: 9123").assertIsDisplayed()
        composeRule.onNodeWithText("Lokasi: Kafe Taman").assertIsDisplayed()
        composeRule.onNodeWithText("Alasan: Keperluan keluarga").assertIsDisplayed()
        composeRule.onNodeWithText("Radius diterapkan: 100 m").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaDoneAction").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaHomeAction").assertIsDisplayed()
    }

    @Test
    fun failureResultShowsSafeRecovery() {
        val base = editingState()
        val events = mutableListOf<WfaRequestEvent>()
        val failure = base.copy(phase = WfaRequestPhase.Failure, failure = WfaRequestFailure.NetworkUnavailable)
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestResultScreen(failure, events::add, {}, {}, {}) }
        }
        composeRule.onNodeWithText("Koneksi bermasalah").assertIsDisplayed()
        composeRule.onNodeWithText("Periksa koneksi internet lalu coba kirim kembali.").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaFailurePrimaryAction").performClick()
        assertEquals(listOf(WfaRequestEvent.RetrySubmitClicked), events)
    }

    @Test
    fun retrySubmittingResultHasNoEnabledRecoveryControls() {
        val state = editingState().copy(phase = WfaRequestPhase.Submitting)
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestResultScreen(state, {}, {}, {}, {}) }
        }

        composeRule.onNodeWithTag("wfaResultSubmitting").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaFailurePrimaryAction").assertDoesNotExist()
        composeRule.onNodeWithTag("wfaDoneAction").assertDoesNotExist()
    }

    @Test
    fun narrowLargeFontResultKeepsBothExitActionsReachable() {
        val state = editingState().copy(
            phase = WfaRequestPhase.Success,
            submitResult = SubmittedWfaRequest(
                9123, LocalDate.of(2026, 8, 4), WfaRequestStatus.PENDING,
                location, "Keperluan keluarga", 100, null
            )
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Infinite_TrackTheme {
                    Box(Modifier.width(320.dp).fillMaxSize()) {
                        WfaRequestResultScreen(state, {}, {}, {}, {})
                    }
                }
            }
        }

        composeRule.onNodeWithTag("wfaDoneAction").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wfaHomeAction").performScrollTo().assertIsDisplayed()
    }

    private fun renderForm(
        state: WfaRequestUiState,
        onEvent: (WfaRequestEvent) -> Unit = {}
    ) {
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestFormScreen(state, onEvent, onBack = {}) }
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
