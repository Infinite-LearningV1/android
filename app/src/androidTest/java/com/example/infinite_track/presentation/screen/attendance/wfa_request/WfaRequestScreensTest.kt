package com.example.infinite_track.presentation.screen.attendance.wfa_request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
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
import com.example.infinite_track.domain.model.location.DistanceMeters
import com.example.infinite_track.domain.model.location.GeoCoordinate
import com.example.infinite_track.domain.model.wfa.WfaFacilityAvailability
import com.example.infinite_track.domain.model.wfa.WfaFacilityEvidence
import com.example.infinite_track.domain.model.wfa.WfaRecommendation
import com.example.infinite_track.domain.model.wfa.WfaRecommendationFailure
import com.example.infinite_track.domain.model.wfa.WfaRecommendationStatus
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
        composeRule.onNodeWithText(
            "Permintaan WFA Anda berhasil dikirim. Lihat status permintaan untuk pembaruan."
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Permintaan WFA Anda telah dikirim dan menunggu peninjauan."
        ).assertDoesNotExist()
    }

    @Test
    fun formKeepsBothReferencePanelsInOneTransparentScrollableDestinationAndExposesClose() {
        var closeCount = 0
        composeRule.setContent {
            Infinite_TrackTheme {
                TransparentPageProbe {
                    WfaRequestFormScreen(
                        uiState = editingState(),
                        onEvent = {},
                        onBack = {},
                        onClose = { closeCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("wfaRecommendationMap").assertExists()
        composeRule.onNodeWithText("04/08/2026").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaEmployeeCard").assertExists()
        composeRule.onNodeWithTag("wfaRequestDetails").assertExists()
        composeRule.onNodeWithTag("wfaEligibilityCard").assertExists()
        composeRule.onNodeWithTag("wfaReviewAction").performScrollTo().assertIsDisplayed()
        assertPageBackdropIsVisible()

        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.runOnIdle { assertEquals(1, closeCount) }
    }

    @Test
    fun formRendersRecommendationStatesAndSearchFallback() {
        val state = mutableStateOf(
            editingState().copy(recommendationState = WfaRequestRecommendationState.Loading)
        )
        composeRule.setContent {
            Infinite_TrackTheme { WfaRequestFormScreen(state.value, {}, onBack = {}) }
        }
        composeRule.onNodeWithTag("wfaRecommendationLoading").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaSearchFallback").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = editingState().copy(
                recommendationState = WfaRequestRecommendationState.Empty
            )
        }
        composeRule.onNodeWithTag("wfaRecommendationEmpty").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = editingState().copy(
                recommendationState = WfaRequestRecommendationState.Failure(
                    WfaRecommendationFailure.NetworkUnavailable,
                    retryable = true
                )
            )
        }
        composeRule.onNodeWithTag("wfaRecommendationFailure").assertIsDisplayed()
    }

    @Test
    fun nonRankedRecommendationUsesTruthfulCopyWithoutSyntheticZero() {
        val state = editingState().copy(
            recommendationState = WfaRequestRecommendationState.Content(
                listOf(recommendation(status = WfaRecommendationStatus.InsufficientFacilityData))
            )
        )
        renderForm(state)

        composeRule.onNodeWithText("Data fasilitas belum cukup untuk memberi skor.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Skor 0", substring = true).assertDoesNotExist()
    }

    @Test
    fun recommendationShowsPlaceTypeAndFacilityConfidence() {
        val state = editingState().copy(
            recommendationState = WfaRequestRecommendationState.Content(
                listOf(recommendation().copy(placeType = "coworking_space", facilityConfidence = 72))
            )
        )
        renderForm(state)

        composeRule.onNodeWithText("Tipe Coworking space")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Keyakinan fasilitas 72%")
            .performScrollTo()
            .assertIsDisplayed()
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
    fun invalidCoordinateFormShowsTruthfulLocationStatusAndFact() {
        val invalidLocation = location.copy(latitude = 91.0)
        val base = editingState()
        val state = base.copy(
            location = invalidLocation,
            draft = base.draft.copy(location = invalidLocation)
        )

        renderForm(state)

        composeRule.onNodeWithTag("wfaLocationStatus").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Koordinat tidak valid").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Koordinat lokasi belum valid. Pilih kembali lokasi untuk melanjutkan."
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lokasi valid").assertDoesNotExist()
        composeRule.onNodeWithText(
            "Lokasi yang dipilih memiliki koordinat valid."
        ).assertDoesNotExist()
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
    fun reviewShowsReferenceSectionsOnTransparentPageAndExposesClose() {
        val events = mutableListOf<WfaRequestEvent>()
        var closeCount = 0
        val state = editingState().copy(phase = WfaRequestPhase.Reviewing)
        composeRule.setContent {
            Infinite_TrackTheme {
                TransparentPageProbe {
                    WfaRequestReviewScreen(
                        uiState = state,
                        onEvent = events::add,
                        onBack = {},
                        onClose = { closeCount += 1 }
                    )
                }
            }
        }

        composeRule.onNodeWithTag("wfaReviewLocationMap").assertExists()
        composeRule.onNodeWithTag("wfaReviewDetailsCard").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wfaReviewEmployeeCard").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2026-08-04").assertIsDisplayed()
        composeRule.onNodeWithText("Keperluan keluarga").assertIsDisplayed()
        composeRule.onNodeWithText("Kafe Taman").assertIsDisplayed()
        composeRule.onNodeWithText("Butuh ruang tenang").assertIsDisplayed()
        composeRule.onNodeWithTag("wfaConfirmAction").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wfaEditAction").performScrollTo().assertIsDisplayed()
        assertPageBackdropIsVisible()

        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.runOnIdle { assertEquals(1, closeCount) }

        composeRule.onNodeWithTag("wfaConfirmAction").performScrollTo().performClick()
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
    fun successResultShowsOnlyProvenBackendConfirmedFieldsAndDestinations() {
        val base = editingState()
        val fallbackLocation = WfaCandidateLocation(
            -0.91,
            119.86,
            "Hub DTO Fallback",
            "Jl. Server 5, Palu"
        )
        val success = base.copy(
            phase = WfaRequestPhase.Success,
            submitResult = SubmittedWfaRequest(
                bookingId = 9123,
                scheduleDate = LocalDate.of(2026, 8, 5),
                status = WfaRequestStatus.APPROVED,
                location = fallbackLocation,
                reasonLabel = "Kunjungan klien",
                radiusMeters = 175,
                submittedAt = null
            )
        )
        composeRule.setContent {
            Infinite_TrackTheme {
                TransparentPageProbe {
                    WfaRequestResultScreen(
                        uiState = success,
                        onEvent = {},
                        onDone = {},
                        onHome = {},
                        onBack = {},
                        onClose = {}
                    )
                }
            }
        }
        composeRule.onNodeWithTag("wfaSuccessHero").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Permintaan WFA Anda berhasil dikirim. Lihat status permintaan untuk pembaruan."
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Permintaan WFA Anda telah dikirim dan menunggu peninjauan."
        ).assertDoesNotExist()
        composeRule.onNodeWithTag("wfaResultDetailsCard").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ID booking: 9123").assertIsDisplayed()
        composeRule.onNodeWithText("Tanggal WFA: 2026-08-05").assertIsDisplayed()
        composeRule.onNodeWithText("Approved").assertIsDisplayed()
        composeRule.onNodeWithText("Alasan: Kunjungan klien").assertIsDisplayed()
        composeRule.onNodeWithText("Radius diterapkan: 175 m").assertIsDisplayed()
        composeRule.onNodeWithText("Lokasi: Hub DTO Fallback").assertDoesNotExist()
        composeRule.onNodeWithText("Lokasi: Kafe Taman").assertDoesNotExist()
        composeRule.onNodeWithTag("wfaDoneAction").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("wfaHomeAction").performScrollTo().assertIsDisplayed()
        assertPageBackdropIsVisible()
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

    @Composable
    private fun TransparentPageProbe(content: @Composable () -> Unit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(TransparentPageProbeColor)
                .testTag(TransparentPageProbeTag)
        ) {
            content()
        }
    }

    private fun assertPageBackdropIsVisible() {
        val capture = composeRule.onNodeWithTag(TransparentPageProbeTag).captureToImage()
        val sampledColor = capture.toPixelMap()[0, capture.height / 2]

        assertEquals(TransparentPageProbeColor.toArgb(), sampledColor.toArgb())
    }

    private fun editingState() = WfaRequestUiState(
        phase = WfaRequestPhase.Editing,
        employee = WfaEmployeeSummary("Alya Putri", "Product"),
        location = location,
        minimumScheduleDate = LocalDate.of(2026, 8, 3),
        currentCoordinate = GeoCoordinate(-0.89, 119.87),
        recommendationState = WfaRequestRecommendationState.Content(
            recommendations = listOf(recommendation()),
            selectedKey = "place-1"
        ),
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
        val TransparentPageProbeColor = Color.Magenta
        const val TransparentPageProbeTag = "wfaTransparentPageProbe"
        val location = WfaCandidateLocation(-0.89, 119.87, "Kafe Taman", "Jl. Merdeka 10, Palu")

        fun recommendation(
            status: WfaRecommendationStatus = WfaRecommendationStatus.Ranked
        ) = WfaRecommendation(
            stableKey = "place-1",
            placeId = "place-1",
            name = "Kafe Taman",
            address = "Jl. Merdeka 10, Palu",
            coordinate = GeoCoordinate(-0.89, 119.87),
            placeType = "cafe",
            distanceMeters = DistanceMeters(125.0),
            status = status,
            finalRank = if (status == WfaRecommendationStatus.Ranked) 1 else null,
            finalScore = if (status == WfaRecommendationStatus.Ranked) 88.0 else null,
            finalLabel = if (status == WfaRecommendationStatus.Ranked) "Direkomendasikan" else null,
            facilityScore = null,
            facilityConfidence = 0,
            facilities = WfaFacilityEvidence(
                internetAccess = WfaFacilityAvailability.UNKNOWN,
                openingHours = WfaFacilityAvailability.UNKNOWN,
                toilets = WfaFacilityAvailability.UNKNOWN,
                airConditioning = WfaFacilityAvailability.UNKNOWN,
                wheelchairAccessibility = WfaFacilityAvailability.UNKNOWN
            )
        )
    }
}
