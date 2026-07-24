package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.infinite_track.presentation.components.button.ButtonStateType
import com.example.infinite_track.presentation.components.button.ButtonStyle
import com.example.infinite_track.presentation.components.button.StatefulButton
import com.example.infinite_track.presentation.design.tokens.InfiniteColors

private enum class SheetPhase { NO_FACE, LIVENESS, VERIFIED, FAILED }

private fun sheetPhaseOf(state: FaceScannerState): SheetPhase = when (state.livenessState) {
    LivenessState.IDLE, LivenessState.DETECTING_FACE -> SheetPhase.NO_FACE
    LivenessState.SUCCESS -> SheetPhase.VERIFIED
    LivenessState.FAILURE, LivenessState.TIMEOUT -> SheetPhase.FAILED
    else -> SheetPhase.LIVENESS
}

/**
 * Phase-aware bottom sheet for the face verification screen. Renders honest copy and the
 * available actions per phase; Verify is enabled only once all liveness challenges pass.
 * Verified copy never claims attendance success.
 */
@Composable
fun FaceVerificationBottomSheet(
    state: FaceScannerState,
    onVerify: () -> Unit,
    onContinue: () -> Unit,
    onTryAgain: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = sheetPhaseOf(state)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(InfiniteColors.Surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(InfiniteColors.Neutral.copy(alpha = 0.4f))
        )

        val title = when (phase) {
            SheetPhase.NO_FACE -> "No Face Detected"
            SheetPhase.LIVENESS -> "Verify your liveness"
            SheetPhase.VERIFIED -> "Identity Verified"
            SheetPhase.FAILED -> "Verification failed"
        }
        val body = when (phase) {
            SheetPhase.NO_FACE ->
                "We can't find a face in the camera. Please align your face within the frame to continue."
            SheetPhase.LIVENESS ->
                "Completing the liveness challenges confirms your presence. Attendance will be submitted next."
            SheetPhase.VERIFIED ->
                "Face verification was successful. Attendance will be submitted next."
            SheetPhase.FAILED ->
                (state.errorMessage ?: "Please try again.")
        }

        Text(
            text = title,
            color = InfiniteColors.Text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            color = InfiniteColors.AttendanceReportBodyText,
            textAlign = TextAlign.Center
        )

        when (phase) {
            SheetPhase.VERIFIED -> {
                StatefulButton(
                    text = "Continue to Attendance",
                    onClick = onContinue,
                    style = ButtonStyle.Elevated,
                    stateType = ButtonStateType.Default
                )
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Back", color = InfiniteColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
            SheetPhase.LIVENESS -> {
                StatefulButton(
                    text = "Verify",
                    onClick = onVerify,
                    enabled = state.readyToVerify,
                    style = ButtonStyle.Elevated,
                    stateType = ButtonStateType.Default
                )
                StatefulButton(
                    text = "Try Again",
                    onClick = onTryAgain,
                    style = ButtonStyle.Outlined,
                    stateType = ButtonStateType.Default
                )
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Cancel", color = InfiniteColors.Neutral)
                }
            }
            else -> {
                StatefulButton(
                    text = "Try Again",
                    onClick = onTryAgain,
                    style = ButtonStyle.Elevated,
                    stateType = ButtonStateType.Default
                )
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Cancel", color = InfiniteColors.Neutral)
                }
            }
        }
    }
}
