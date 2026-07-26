package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.infinite_track.R
import org.junit.Rule
import org.junit.Test

class FaceVerificationRedesignScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun liveness_disablesVerifyBeforeAllChallengesPass() {
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
        composeTestRule.onNodeWithText("Verify")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun liveness_enablesVerifyAfterAllChallengesPass() {
        composeTestRule.setContent {
            FaceVerificationBottomSheet(
                state = FaceScannerState(
                    livenessState = LivenessState.LIVENESS_DETECTED,
                    challengeIndex = 4,
                    readyToVerify = true
                ),
                onVerify = {},
                onContinue = {},
                onTryAgain = {},
                onCancel = {}
            )
        }

        composeTestRule.onNodeWithText("Verify")
            .assertIsDisplayed()
            .assertIsEnabled()
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

    @Test
    fun livenessFrame_showsBorderRailNodesMatchingProgress() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(
                    livenessState = LivenessState.WAITING_FOR_LIVENESS,
                    challengeIndex = 2,
                    challengeTotal = 4
                )
            )
        }

        // Node 1 passed -> renders a check icon, not the number.
        composeTestRule.onAllNodesWithText("1").assertCountEquals(0)
        // Active node 2 and pending nodes 3, 4 render their numbers.
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun readyToVerifyFrame_showsAllNodesPassed() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(
                    livenessState = LivenessState.LIVENESS_DETECTED,
                    challengeIndex = 4,
                    challengeTotal = 4,
                    readyToVerify = true
                )
            )
        }

        composeTestRule
            .onAllNodesWithTag("face_frame_rail_node_passed")
            .assertCountEquals(4)
    }

    @Test
    fun successFrame_showsCheckBadgeWithDescription() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(livenessState = LivenessState.SUCCESS)
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.face_frame_badge_verified)
            )
            .assertIsDisplayed()
    }

    @Test
    fun failureFrame_showsCrossBadgeWithDescription() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(livenessState = LivenessState.FAILURE)
            )
        }

        composeTestRule
            .onNodeWithContentDescription(
                targetContext.getString(R.string.face_frame_badge_not_matched)
            )
            .assertIsDisplayed()
    }

    @Test
    fun timeoutFrame_showsTimeoutContent() {
        composeTestRule.setContent {
            FaceVerificationFrame(
                state = FaceScannerState(livenessState = LivenessState.TIMEOUT)
            )
        }

        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.face_frame_timeout_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.face_frame_timeout_countdown))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(targetContext.getString(R.string.face_frame_timeout_times_up))
            .assertIsDisplayed()
    }
}
