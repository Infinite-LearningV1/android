package com.example.infinite_track.presentation.screen.attendance.face

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.infinite_track.presentation.design.components.navigation.InfiniteTopBarActionButton
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusPill
import com.example.infinite_track.presentation.design.components.status.InfiniteStatusVariant
import com.example.infinite_track.presentation.design.tokens.InfiniteColors
import com.example.infinite_track.presentation.design.tokens.InfiniteSize

/**
 * Floating glass top bar for the face verification screen: back button, two-line centered
 * title/subtitle, and a right-side intent pill (e.g. "● Check-in"). Reuses Infinite glass tokens.
 */
@Composable
fun FaceVerificationTopBar(
    title: String,
    subtitle: String,
    intentLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(InfiniteColors.AttendanceReportGlassSurface)
            .border(
                BorderStroke(1.dp, InfiniteColors.AttendanceReportGlassBorder),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        InfiniteTopBarActionButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = InfiniteColors.Text,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = InfiniteColors.AttendanceReportBodyText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(999.dp))
                .background(InfiniteColors.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(InfiniteColors.Primary)
            )
            Text(
                text = intentLabel,
                color = InfiniteColors.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Left status pill + right context pill row for the current phase. Reuses [InfiniteStatusPill].
 */
@Composable
fun FaceStatusPillRow(
    state: FaceScannerState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (state.livenessState) {
            LivenessState.SUCCESS -> {
                InfiniteStatusPill(
                    label = "Face matched",
                    variant = InfiniteStatusVariant.Completed,
                    leadingIcon = Icons.Outlined.CheckCircle,
                    size = InfiniteSize.Small
                )
                InfiniteStatusPill(
                    label = "Similarity verified",
                    variant = InfiniteStatusVariant.Neutral,
                    size = InfiniteSize.Small
                )
            }
            LivenessState.FAILURE, LivenessState.TIMEOUT -> {
                InfiniteStatusPill(
                    label = "Verification failed",
                    variant = InfiniteStatusVariant.Rejected,
                    leadingIcon = Icons.Outlined.WarningAmber,
                    size = InfiniteSize.Small
                )
                InfiniteStatusPill(
                    label = "Try again",
                    variant = InfiniteStatusVariant.Neutral,
                    size = InfiniteSize.Small
                )
            }
            LivenessState.IDLE, LivenessState.DETECTING_FACE -> {
                InfiniteStatusPill(
                    label = "No face detected",
                    variant = InfiniteStatusVariant.Pending,
                    leadingIcon = Icons.Outlined.WarningAmber,
                    size = InfiniteSize.Small
                )
                InfiniteStatusPill(
                    label = "Position your face",
                    variant = InfiniteStatusVariant.Neutral,
                    size = InfiniteSize.Small
                )
            }
            else -> {
                InfiniteStatusPill(
                    label = "Face detected",
                    variant = InfiniteStatusVariant.Completed,
                    leadingIcon = Icons.Outlined.CheckCircle,
                    size = InfiniteSize.Small
                )
                InfiniteStatusPill(
                    label = "Challenge ${state.challengeIndex} of ${state.challengeTotal}",
                    variant = InfiniteStatusVariant.Neutral,
                    size = InfiniteSize.Small
                )
            }
        }
    }
}

/**
 * Center-bottom guidance: the current instruction (already localized by the ViewModel) plus the
 * per-challenge countdown when active.
 */
@Composable
fun FaceGuidanceText(
    state: FaceScannerState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = state.instructionText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        if (state.showCountdown) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = InfiniteColors.Secondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Time remaining: ${state.timeRemaining}s",
                    color = Color.White
                )
            }
        }
    }
}
