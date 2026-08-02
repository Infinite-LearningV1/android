package com.example.infinite_track.presentation.screen.attendance

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.infinite_track.domain.model.attendance.WorkMode
import com.example.infinite_track.presentation.components.button.attendance.AttendancePreparationEvent
import com.example.infinite_track.presentation.components.button.attendance.WorkModePreparationContent
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationPrimaryAction
import com.example.infinite_track.presentation.screen.attendance.preparation.AttendancePreparationUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.TargetLocationSummaryUiModel
import com.example.infinite_track.presentation.screen.attendance.preparation.WorkModeOptionUiModel
import com.example.infinite_track.presentation.theme.Infinite_TrackTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkModeTargetLocationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun workModeCardsExposeExactlyOneSelectedMode() {
        render(model(WorkMode.WFA))

        composeRule.onNodeWithText("Work From Office").assertIsDisplayed()
        composeRule.onNodeWithText("Work From Home").assertIsDisplayed()
        composeRule.onNodeWithText("Work From Anywhere").assertIsDisplayed()
        composeRule.onAllNodes(hasStateDescription("Dipilih")).assertCountEquals(1)
    }

    @Test
    fun attendanceSheetDoesNotOwnWfaDiscoveryOrSearch() {
        render(model(WorkMode.WFA))

        composeRule.onNodeWithText("Cari lokasi WFA").assertDoesNotExist()
        composeRule.onNodeWithText("Rekomendasi lokasi WFA").assertDoesNotExist()
    }

    @Test
    fun authoritativeTargetSummaryRemainsVisible() {
        render(model(WorkMode.WFA))

        composeRule.onNodeWithText("Target Lokasi").assertIsDisplayed()
        composeRule.onNodeWithText("WFA Disetujui").assertIsDisplayed()
        composeRule.onNodeWithText("Booking WFA disetujui").assertIsDisplayed()
        composeRule.onNodeWithText("Radius 100 m").assertIsDisplayed()
    }

    @Test
    fun contentEmitsOnlyModeAndPrimaryEvents() {
        val events = mutableListOf<AttendancePreparationEvent>()
        render(model(WorkMode.WFA), events::add)

        composeRule.onNode(hasText("Work From Home") and hasClickAction()).performClick()
        composeRule.onNodeWithTag("attendancePrimaryAction").performScrollTo().performClick()

        assertEquals(
            listOf(
                AttendancePreparationEvent.ModeSelected(WorkMode.WFH),
                AttendancePreparationEvent.PrimaryActionClicked(
                    AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION
                )
            ),
            events
        )
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

    private fun model(selectedMode: WorkMode) = AttendancePreparationUiModel(
        modeOptions = WorkMode.values().map { mode ->
            WorkModeOptionUiModel(
                mode = mode,
                title = mode.displayLabel,
                supportingText = mode.shortLabel,
                isSelected = mode == selectedMode
            )
        },
        targetSummary = TargetLocationSummaryUiModel(
            displayName = "WFA Disetujui",
            sourceLabel = "Booking WFA disetujui",
            radiusText = "Radius 100 m",
            distanceText = "Jarak 25 m",
            rangeText = "Di dalam jangkauan"
        ),
        statusMessage = "Lokasi target siap digunakan untuk kehadiran.",
        primaryAction = AttendancePreparationPrimaryAction.CONTINUE_TO_FACE_VERIFICATION,
        primaryActionLabel = "Lanjut ke Verifikasi Wajah",
        isPrimaryActionEnabled = true
    )
}
