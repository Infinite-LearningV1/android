package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class FaceVerificationRedesignScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun liveness_showsVerifyAndChallengeSheet() {
        composeTestRule.setContent {
            FaceVerificationBottomSheet(
                state = FaceScannerState(
                    livenessState = LivenessState.WAITING_FOR_LIVENESS,
                    challengeIndex = 2,
                    readyToVerify = false
                ),
                onVerify = {},
                onContinue = {},
                onTryAgain = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Verify your liveness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verify").assertIsDisplayed()
    }

    @Test
    fun verified_showsContinueAndIdentityCopy_notAttendanceSuccess() {
        composeTestRule.setContent {
            FaceVerificationBottomSheet(
                state = FaceScannerState(livenessState = LivenessState.SUCCESS),
                onVerify = {},
                onContinue = {},
                onTryAgain = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Identity Verified").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue to Attendance").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Attendance recorded").assertCountEquals(0)
    }

    @Test
    fun noFace_showsTryAgain() {
        composeTestRule.setContent {
            FaceVerificationBottomSheet(
                state = FaceScannerState(livenessState = LivenessState.DETECTING_FACE),
                onVerify = {},
                onContinue = {},
                onTryAgain = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("No Face Detected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try Again").assertIsDisplayed()
    }
}
