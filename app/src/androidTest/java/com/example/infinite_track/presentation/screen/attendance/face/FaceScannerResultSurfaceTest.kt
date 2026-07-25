package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class FaceScannerResultSurfaceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notMatched_showsReasonAndRetry() {
        composeTestRule.setContent {
            FaceResultSurface(
                livenessState = LivenessState.FAILURE,
                failureReason = FaceVerificationFailureReason.NOT_MATCHED,
                capturedFacePreview = null,
                onRetry = {},
                onContinue = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Wajah tidak cocok").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coba Lagi").assertIsDisplayed()
    }

    @Test
    fun success_usesIdentityCopy_notAttendanceSuccess() {
        composeTestRule.setContent {
            FaceResultSurface(
                livenessState = LivenessState.SUCCESS,
                failureReason = null,
                capturedFacePreview = null,
                onRetry = {},
                onContinue = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Identitas terverifikasi").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Absensi berhasil").assertCountEquals(0)
    }

    @Test
    fun multipleFaces_showsGuidance() {
        composeTestRule.setContent {
            FaceResultSurface(
                livenessState = LivenessState.FAILURE,
                failureReason = FaceVerificationFailureReason.MULTIPLE_FACES,
                capturedFacePreview = null,
                onRetry = {},
                onContinue = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Pastikan hanya ada satu wajah", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun lowLight_showsWarningReason() {
        composeTestRule.setContent {
            FaceResultSurface(
                livenessState = LivenessState.FAILURE,
                failureReason = FaceVerificationFailureReason.LOW_LIGHT,
                capturedFacePreview = null,
                onRetry = {},
                onContinue = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Pencahayaan kurang").assertIsDisplayed()
    }

    @Test
    fun notMatched_diagnosticsUseFailureIconsForEveryRow() {
        composeTestRule.setContent {
            FaceDiagnosticsCard(
                model = FaceDiagnosticsUiModel(
                    similarityText = "0.1",
                    thresholdText = "0.15",
                    matched = false
                )
            )
        }

        composeTestRule.onNodeWithTag("diagnostic-Similarity Score-not-matched")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("diagnostic-Threshold-not-matched")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("diagnostic-Result-not-matched")
            .assertIsDisplayed()
    }
}
