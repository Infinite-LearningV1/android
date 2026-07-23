package com.example.infinite_track.presentation.screen.attendance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.components.button.attendance.AttendancePreparationEvent
import com.example.infinite_track.presentation.components.button.attendance.WorkModePreparationContent
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationPrimaryAction
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationSecondaryAction
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.TargetLocationSummaryUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaDiscoveryUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WfaRecommendationUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WorkModeOptionUiModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkModeTargetLocationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun workModeCardsExposeOneSelectedModeAndWfaOnlySearch() {
        render(wfaReadyUiModel())

        composeRule.onNodeWithText("Work From Office").assertIsDisplayed()
        composeRule.onNodeWithText("Work From Home").assertIsDisplayed()
        composeRule.onNodeWithText("Work From Anywhere").assertIsDisplayed()
        composeRule.onAllNodes(hasStateDescription("Dipilih")).assertCountEquals(1)
        composeRule.onNodeWithText("Cari lokasi WFA").assertIsDisplayed()
    }

    @Test
    fun WFHHasAdminCopyAndNoSearchOrEditAction() {
        render(wfhReadyUiModel())

        composeRule.onNodeWithText("Ditetapkan oleh admin").assertIsDisplayed()
        composeRule.onNodeWithText("Cari lokasi WFA").assertDoesNotExist()
        composeRule.onNodeWithText("Ubah lokasi").assertDoesNotExist()
    }

    @Test
    fun wfaLoadingEmptyAndFailureStatesExposeTruthfulCopy() {
        render(
            wfaReadyUiModel(
                discovery = WfaDiscoveryUiModel.Loading
            )
        )
        composeRule.onNodeWithText("Memuat rekomendasi lokasi WFA...").assertIsDisplayed()
    }

    @Test
    fun wfaEmptyStateUsesModelCopy() {
        render(
            wfaReadyUiModel(
                discovery = WfaDiscoveryUiModel.Empty("Belum ada rekomendasi lokasi WFA.")
            )
        )

        composeRule.onNodeWithText("Belum ada rekomendasi lokasi WFA.").assertIsDisplayed()
    }

    @Test
    fun wfaFailureStateUsesModelCopy() {
        render(
            wfaReadyUiModel(
                discovery = WfaDiscoveryUiModel.Failure(
                    message = "Rekomendasi lokasi WFA gagal dimuat.",
                    retryable = true
                ),
                primaryAction = AttendancePreparationPrimaryAction.RETRY_WFA_DISCOVERY,
                primaryActionLabel = "Coba lagi"
            )
        )

        composeRule.onNodeWithText("Rekomendasi lokasi WFA gagal dimuat.").assertIsDisplayed()
        composeRule.onNodeWithText("Coba lagi").assertIsDisplayed()
    }

    @Test
    fun wfaContentShowsBackendFieldsAndSelectedSemanticsWithoutRatingOrPhoto() {
        render(
            wfaReadyUiModel(
                discovery = WfaDiscoveryUiModel.Content(
                    rows = listOf(selectedRecommendation),
                    selectedKey = selectedRecommendation.stableKey,
                    searchPreviewName = null
                )
            )
        )

        composeRule.onNodeWithText("Rekomendasi lokasi WFA").assertIsDisplayed()
        composeRule.onNodeWithText("Cafe Palu").assertIsDisplayed()
        composeRule.onNodeWithText("Cafe • 1,25 km").assertIsDisplayed()
        composeRule.onNodeWithText("Skor WFA 91 • Sangat sesuai").assertIsDisplayed()
        composeRule.onNode(
            hasText("Cafe Palu") and hasStateDescription("Dipilih")
        ).assertIsDisplayed()
        composeRule.onNodeWithText("rating", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("foto", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun targetSummaryShowsSourceRadiusDistanceAndRange() {
        render(wfaReadyUiModel())

        composeRule.onNodeWithText("Target Lokasi").assertIsDisplayed()
        composeRule.onNodeWithText("WFA Disetujui").assertIsDisplayed()
        composeRule.onNodeWithText("Booking WFA disetujui").assertIsDisplayed()
        composeRule.onNodeWithText("Radius 100 m").assertIsDisplayed()
        composeRule.onNodeWithText("Jarak 25 m").assertIsDisplayed()
        composeRule.onNodeWithText("Di dalam jangkauan").assertIsDisplayed()
    }

    @Test
    fun preparationContentEmitsModeRecommendationSearchAndPrimaryEvents() {
        val events = mutableListOf<AttendancePreparationEvent>()
        render(
            wfaReadyUiModel(
                discovery = WfaDiscoveryUiModel.Content(
                    rows = listOf(selectedRecommendation),
                    selectedKey = null,
                    searchPreviewName = null
                )
            ),
            onEvent = events::add
        )

        composeRule.onNode(
            hasText("Work From Home") and hasClickAction()
        ).performClick()
        composeRule.onNode(
            hasText("Cafe Palu") and hasClickAction()
        ).performClick()
        composeRule.onNodeWithContentDescription("Cari lokasi WFA").performClick()
        composeRule.onNodeWithTag("attendancePrimaryAction").performScrollTo().performClick()

        assertEquals(
            listOf(
                AttendancePreparationEvent.ModeSelected(WorkMode.WFH),
                AttendancePreparationEvent.RecommendationSelected(selectedRecommendation.stableKey),
                AttendancePreparationEvent.SearchWfaLocation,
                AttendancePreparationEvent.PrimaryActionClicked(
                    AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION
                )
            ),
            events
        )
    }

    @Test
    fun width320AndFontScaleTwoKeepCardsAndOnePrimaryActionUsable() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                Infinite_TrackTheme {
                    Box(
                        Modifier
                            .width(320.dp)
                            .fillMaxSize()
                            .testTag("screenHost")
                    ) {
                        WorkModePreparationContent(
                            model = wfaReadyUiModel(
                                discovery = WfaDiscoveryUiModel.Content(
                                    rows = listOf(selectedRecommendation),
                                    selectedKey = selectedRecommendation.stableKey,
                                    searchPreviewName = null
                                )
                            ),
                            onEvent = {},
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        composeRule.onAllNodes(hasTestTag("attendancePrimaryAction")).assertCountEquals(1)
        val workModeCard = composeRule.onNode(
            hasText("Work From Office") and hasClickAction()
        )
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
        val primaryAction = composeRule.onNodeWithTag("attendancePrimaryAction")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()

        val hostBounds = composeRule.onNodeWithTag("screenHost").getUnclippedBoundsInRoot()
        val cardBounds = workModeCard.getUnclippedBoundsInRoot()
        val actionBounds = primaryAction.getUnclippedBoundsInRoot()
        assertTrue(cardBounds.left >= hostBounds.left && cardBounds.right <= hostBounds.right)
        assertTrue(actionBounds.left >= hostBounds.left && actionBounds.right <= hostBounds.right)
    }

    private fun render(
        model: AttendancePreparationUiModel,
        onEvent: (AttendancePreparationEvent) -> Unit = {}
    ) {
        composeRule.setContent {
            Infinite_TrackTheme {
                WorkModePreparationContent(model = model, onEvent = onEvent)
            }
        }
    }

    private fun wfaReadyUiModel(
        discovery: WfaDiscoveryUiModel = WfaDiscoveryUiModel.Empty(
            "Belum ada rekomendasi lokasi WFA."
        ),
        primaryAction: AttendancePreparationPrimaryAction =
            AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
        primaryActionLabel: String = "Lanjut ke Verifikasi Wajah"
    ) = preparationUiModel(
        selectedMode = WorkMode.WFA,
        target = TargetLocationSummaryUiModel(
            displayName = "WFA Disetujui",
            sourceLabel = "Booking WFA disetujui",
            radiusText = "Radius 100 m",
            distanceText = "Jarak 25 m",
            rangeText = "Di dalam jangkauan"
        ),
        discovery = discovery,
        primaryAction = primaryAction,
        primaryActionLabel = primaryActionLabel,
        secondaryAction = AttendancePreparationSecondaryAction.SEARCH_WFA_LOCATION,
        secondaryActionLabel = "Cari lokasi WFA"
    )

    private fun wfhReadyUiModel() = preparationUiModel(
        selectedMode = WorkMode.WFH,
        target = TargetLocationSummaryUiModel(
            displayName = "Rumah terdaftar",
            sourceLabel = "Profil yang ditetapkan admin",
            radiusText = "Radius 100 m",
            distanceText = "Jarak 12 m",
            rangeText = "Di dalam jangkauan"
        ),
        discovery = WfaDiscoveryUiModel.Hidden,
        secondaryAction = null,
        secondaryActionLabel = null
    )

    private fun preparationUiModel(
        selectedMode: WorkMode,
        target: TargetLocationSummaryUiModel,
        discovery: WfaDiscoveryUiModel,
        primaryAction: AttendancePreparationPrimaryAction =
            AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
        primaryActionLabel: String = "Lanjut ke Verifikasi Wajah",
        secondaryAction: AttendancePreparationSecondaryAction?,
        secondaryActionLabel: String?
    ) = AttendancePreparationUiModel(
        modeOptions = WorkMode.values().map { mode ->
            WorkModeOptionUiModel(
                mode = mode,
                title = when (mode) {
                    WorkMode.WFO -> "Work From Office"
                    WorkMode.WFH -> "Work From Home"
                    WorkMode.WFA -> "Work From Anywhere"
                },
                supportingText = when (mode) {
                    WorkMode.WFO -> "Lokasi kantor yang ditetapkan"
                    WorkMode.WFH -> "Lokasi rumah yang ditetapkan admin"
                    WorkMode.WFA -> "Memerlukan booking yang disetujui"
                },
                isSelected = mode == selectedMode
            )
        },
        targetSummary = target,
        statusMessage = "Lokasi target siap digunakan untuk kehadiran.",
        wfaDiscovery = discovery,
        primaryAction = primaryAction,
        primaryActionLabel = primaryActionLabel,
        isPrimaryActionEnabled = true,
        secondaryAction = secondaryAction,
        secondaryActionLabel = secondaryActionLabel
    )

    private val selectedRecommendation = WfaRecommendationUiModel(
        stableKey = "cafe-palu",
        name = "Cafe Palu",
        supportingText = "Cafe • 1,25 km",
        suitabilityText = "Skor WFA 91 • Sangat sesuai",
    )
}
